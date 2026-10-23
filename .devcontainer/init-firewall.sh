#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# 1. Extract Docker DNS info BEFORE any flushing
DOCKER_DNS_RULES=$(iptables-save -t nat | grep "127\.0\.0\.11" || true)

# Flush existing rules and delete existing ipsets
iptables -F
iptables -X
iptables -t nat -F
iptables -t nat -X
iptables -t mangle -F
iptables -t mangle -X
ipset destroy allowed-domains 2>/dev/null || true

# Reset default policies to ACCEPT so a re-run starts from a clean slate
# (an earlier run may have left them at DROP, which would block the
# GitHub/DNS fetches below)
iptables -P INPUT ACCEPT
iptables -P FORWARD ACCEPT
iptables -P OUTPUT ACCEPT
if command -v ip6tables >/dev/null 2>&1 && ip6tables -L >/dev/null 2>&1; then
    ip6tables -F
    ip6tables -X
    ip6tables -P INPUT ACCEPT
    ip6tables -P FORWARD ACCEPT
    ip6tables -P OUTPUT ACCEPT
fi

# 2. Selectively restore ONLY internal Docker DNS resolution
if [ -n "$DOCKER_DNS_RULES" ]; then
    echo "Restoring Docker DNS rules..."
    iptables -t nat -N DOCKER_OUTPUT 2>/dev/null || true
    iptables -t nat -N DOCKER_POSTROUTING 2>/dev/null || true
    echo "$DOCKER_DNS_RULES" | xargs -L 1 iptables -t nat
else
    echo "No Docker DNS rules to restore"
fi

# Create ipset with CIDR support
ipset create allowed-domains hash:net

# Collect all domains to resolve
DOMAINS_TO_RESOLVE=()


# === Module: core.conf ===
# Core firewall rules - always included
# DNS, SSH, localhost, OAuth callback

# Allow outbound DNS
iptables -A OUTPUT -p udp --dport 53 -j ACCEPT
# Allow inbound DNS responses
iptables -A INPUT -p udp --sport 53 -j ACCEPT
# Allow outbound SSH
iptables -A OUTPUT -p tcp --dport 22 -j ACCEPT
# Allow inbound SSH responses
iptables -A INPUT -p tcp --sport 22 -m state --state ESTABLISHED -j ACCEPT
# Allow localhost
iptables -A INPUT -i lo -j ACCEPT
iptables -A OUTPUT -o lo -j ACCEPT
# Allow incoming connections on port 3000 for OAuth callback
iptables -A INPUT -p tcp --dport 3000 -j ACCEPT

# === Module: github.conf ===
# GitHub firewall rules - always included
# Allows access to GitHub services

# Fetch GitHub meta information and add their IP ranges
echo "Fetching GitHub IP ranges..."
gh_ranges=$(curl -s --connect-timeout 10 https://api.github.com/meta || true)
if [ -z "$gh_ranges" ]; then
    echo "ERROR: Failed to fetch GitHub IP ranges"
    exit 1
fi

if ! echo "$gh_ranges" | jq -e '.web and .api and .git' >/dev/null; then
    echo "ERROR: GitHub API response missing required fields"
    exit 1
fi

echo "Processing GitHub IPs..."
while read -r cidr; do
    if [[ ! "$cidr" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}/[0-9]{1,2}$ ]]; then
        echo "ERROR: Invalid CIDR range from GitHub meta: $cidr"
        exit 1
    fi
    echo "Adding GitHub range $cidr"
    ipset add -exist allowed-domains "$cidr"
done < <(echo "$gh_ranges" | jq -r '(.web + .api + .git)[]' | aggregate -q)

# Add github.com domain for additional resolution
DOMAINS_TO_RESOLVE+=("github.com")

# === Module: anthropic.conf ===
# Anthropic firewall rules - always included
# Allows access to Anthropic/Claude API services

DOMAINS_TO_RESOLVE+=(
    "api.anthropic.com"
    "sentry.io"
    "statsig.com"
)

# === Module: package-managers.conf ===
# Package manager firewall rules - always included
# Allows access to npm, PyPI, Conda, and Pixi

DOMAINS_TO_RESOLVE+=(
    "registry.npmjs.org"
    "pypi.org"
    "files.pythonhosted.org"
    "conda.anaconda.org"
    "repo.anaconda.com"
    "conda-forge.org"
    "prefix.dev"
    "conda-mapping.prefix.dev"
)

# === Module: vscode.conf ===
# VS Code firewall rules - always included
# Allows the VS Code server to download extensions and updates. Without this
# the postStart firewall blocks extension installs at container start
# ("Failed downloading vsix ... ETIMEDOUT" for *.gallery.vsassets.io).

DOMAINS_TO_RESOLVE+=(
    "marketplace.visualstudio.com"
    "update.code.visualstudio.com"
    "vscode.download.prss.microsoft.com"
    "vscode.blob.core.windows.net"
)

# Extension packages are served from per-publisher hosts:
#   <publisher>.gallery.vsassets.io     (metadata, redirects to the CDN)
#   <publisher>.gallerycdn.vsassets.io  (VSIX download, Akamai)
# Derive the publishers from the extension list in devcontainer.json so the
# allowlist follows the configuration instead of a hardcoded list. The file is
# JSONC, so strip line comments before parsing.
# NOTE: the gallerycdn hosts sit on Akamai with 20 s TTLs and rotate across
# several /24s, so the snapshot taken here can go stale; re-run this script if
# an extension install fails later in a long-lived container.
devcontainer_json="${DEVCONTAINER_JSON:-/workspace/.devcontainer/devcontainer.json}"
if [ -f "$devcontainer_json" ]; then
    publishers=$(sed -E 's#^[[:space:]]*//.*$##' "$devcontainer_json" \
        | jq -r '.customizations.vscode.extensions[]? | split(".")[0] | ascii_downcase' 2>/dev/null \
        | grep -E '^[a-z0-9-]+$' | sort -u || true)
    if [ -z "$publishers" ]; then
        echo "WARNING: No VS Code extension publishers found in $devcontainer_json"
    fi
    while read -r publisher; do
        [ -n "$publisher" ] || continue
        DOMAINS_TO_RESOLVE+=(
            "$publisher.gallery.vsassets.io"
            "$publisher.gallerycdn.vsassets.io"
        )
    done < <(echo "$publishers")
else
    echo "WARNING: $devcontainer_json not found - VS Code extension hosts not allowlisted"
fi

# === Module: java-repos.conf ===
# Java firewall rules - java profile
# Allows access to Maven Central, Gradle, and Java package repositories

DOMAINS_TO_RESOLVE+=(
    "repo1.maven.org"
    "repo.maven.apache.org"
    "central.sonatype.com"
    "maven.scijava.org"
    "services.gradle.org"
    "plugins.gradle.org"
#    "downloads.gradle-dn.com"
    "deepwiki.com"
    "artifacts.unidata.ucar.edu"
    "artifacts.openmicroscopy.org"
)

# === Module: academic.conf ===
# Academic firewall rules - research profile
# Allows access to academic databases, publishers, and research services.
# NOTE: entries are exact hostnames (A-record allowlist, no wildcards) —
# subdomains must be listed explicitly.

DOMAINS_TO_RESOLVE+=(
    "semanticscholar.org"
    "api.semanticscholar.org"
    "www.semanticscholar.org"
    "orcid.org"
    "scholar.google.com"

    # arXiv — export.arxiv.org is the official API/OAI endpoint
    "arxiv.org"
    "export.arxiv.org"
    "static.arxiv.org"

    # bioRxiv / medRxiv (site + official API)
    "www.biorxiv.org"
    "api.biorxiv.org"
    "www.medrxiv.org"

    # DOI resolution + scholarly metadata APIs
    "doi.org"
    "dx.doi.org"
    "api.crossref.org"
    "api.openalex.org"

    # PubMed / PMC / Europe PMC (incl. E-utilities API)
    "pubmed.ncbi.nlm.nih.gov"
    "pmc.ncbi.nlm.nih.gov"
    "www.ncbi.nlm.nih.gov"
    "eutils.ncbi.nlm.nih.gov"
    "europepmc.org"
    "www.ebi.ac.uk"

    # Major publishers (read-only; paywalls still apply)
    "www.nature.com"
    "www.science.org"
    "www.sciencedirect.com"
    "link.springer.com"
    "onlinelibrary.wiley.com"
    "academic.oup.com"
    "www.cell.com"
    "journals.plos.org"
    "www.pnas.org"
    "elifesciences.org"
    "ieeexplore.ieee.org"
    "dl.acm.org"
    "www.frontiersin.org"

    # ML publishing venues
    "openreview.net"
    "proceedings.mlr.press"
    "proceedings.neurips.cc"

    # Preprint/data infrastructure
    "osf.io"
    "zenodo.org"
)

# === Module: news.conf ===
# News and general reference - research profile
# Read-only news outlets and reference sites. The big outlets sit behind
# CDNs whose IPs rotate — if one becomes unreachable in a long-lived
# container, re-run: sudo /usr/local/bin/init-firewall.sh

DOMAINS_TO_RESOLVE+=(
    # Wire services and broadsheets (free or partially free)
    "www.reuters.com"
    "apnews.com"
    "www.bbc.com"
    "www.bbc.co.uk"
    "www.theguardian.com"
    "www.npr.org"
    "text.npr.org"
    "www.nytimes.com"

    # Science/tech news
    "www.quantamagazine.org"
    "www.statnews.com"
    "arstechnica.com"
    "news.ycombinator.com"

    # Reference
    "en.wikipedia.org"
)

# === Resolve and add domains ===
for domain in "${DOMAINS_TO_RESOLVE[@]}"; do
    echo "Resolving $domain..."
    # Retry a few times: at container start DNS may not be ready yet, and a
    # domain skipped here stays unreachable until the script is re-run.
    # "|| true": under set -euo pipefail an unresolvable domain (e.g. an
    # internal Janelia host while off the VPN) would otherwise abort the
    # script before the DROP policies are applied, leaving the firewall open
    ips=""
    for attempt in 1 2 3; do
        ips=$(dig +short +time=3 +tries=1 A "$domain" | grep -E '^([0-9]{1,3}\.){3}[0-9]{1,3}$' || true)
        [ -n "$ips" ] && break
        sleep 1
    done
    if [ -z "$ips" ]; then
        echo "WARNING: Failed to resolve $domain after 3 attempts - skipping"
        continue
    fi

    while read -r ip; do
        if [[ ! "$ip" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
            echo "WARNING: Invalid IP from DNS for $domain: $ip"
            continue
        fi
        echo "Adding $ip for $domain"
        ipset add allowed-domains "$ip" 2>/dev/null || true
    done < <(echo "$ips")
done

# Get host IP from default route
HOST_IP=$(ip route | grep default | cut -d" " -f3)
if [ -z "$HOST_IP" ]; then
    echo "ERROR: Failed to detect host IP"
    exit 1
fi

HOST_NETWORK=$(echo "$HOST_IP" | sed "s/\.[0-9]*$/.0\/24/")
echo "Host network detected as: $HOST_NETWORK"

# Set up remaining iptables rules
iptables -A INPUT -s "$HOST_NETWORK" -j ACCEPT
iptables -A OUTPUT -d "$HOST_NETWORK" -j ACCEPT

# Set default policies to DROP first
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT DROP

# First allow established connections for already approved traffic
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
iptables -A OUTPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# Then allow only specific outbound traffic to allowed domains
iptables -A OUTPUT -m set --match-set allowed-domains dst -j ACCEPT

# Reject (not silently drop) all other outbound traffic so blocked connections
# fail immediately instead of hanging until the client's connect timeout.
iptables -A OUTPUT -j REJECT --reject-with icmp-admin-prohibited

# IPv6: the allowlist is IPv4-only (A records), so any dual-stack destination
# would bypass the firewall over IPv6 (e.g. podman/pasta gives the container
# the host's global IPv6 address). Reject all IPv6 except loopback and DNS, so
# clients fall back to IPv4 immediately.
if command -v ip6tables >/dev/null 2>&1 && ip6tables -L >/dev/null 2>&1; then
    ip6tables -F
    ip6tables -X
    ip6tables -P INPUT DROP
    ip6tables -P FORWARD DROP
    ip6tables -P OUTPUT DROP
    ip6tables -A INPUT -i lo -j ACCEPT
    ip6tables -A OUTPUT -o lo -j ACCEPT
    # ICMPv6 (neighbor discovery) — without it the gateway is unreachable
    # and the DNS exceptions below cannot work
    ip6tables -A INPUT -p icmpv6 -j ACCEPT
    ip6tables -A OUTPUT -p icmpv6 -j ACCEPT
    # Keep DNS working if the configured resolver is IPv6
    ip6tables -A OUTPUT -p udp --dport 53 -j ACCEPT
    ip6tables -A OUTPUT -p tcp --dport 53 -j ACCEPT
    ip6tables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
    # Fail fast (see the IPv4 REJECT rule above)
    ip6tables -A OUTPUT -j REJECT --reject-with icmp6-adm-prohibited
fi

echo "Firewall configuration complete"
echo "Verifying firewall rules..."
if curl --connect-timeout 5 https://example.com >/dev/null 2>&1; then
    echo "ERROR: Firewall verification failed - was able to reach https://example.com"
    exit 1
else
    echo "Firewall verification passed - unable to reach https://example.com as expected"
fi

# Verify GitHub API access
if ! curl --connect-timeout 5 https://api.github.com/zen >/dev/null 2>&1; then
    echo "ERROR: Firewall verification failed - unable to reach https://api.github.com"
    exit 1
else
    echo "Firewall verification passed - able to reach https://api.github.com as expected"
fi
