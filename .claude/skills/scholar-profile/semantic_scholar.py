#!/usr/bin/env python3
"""Fetch researcher profile and publications from Semantic Scholar API."""

import json
import sys
import time
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
from urllib.parse import quote

API_BASE = "https://api.semanticscholar.org/graph/v1"
ORCID_API = "https://pub.orcid.org/v3.0"
HEADERS = {"User-Agent": "ResearchProfileFetcher/1.0"}
ORCID_HEADERS = {"Accept": "application/json", "User-Agent": "ResearchProfileFetcher/1.0"}

def fetch_json(url, retries=3):
    """Fetch JSON from URL with retries and rate limit handling."""
    for i in range(retries):
        try:
            req = Request(url, headers=HEADERS)
            resp = urlopen(req, timeout=30)
            return json.loads(resp.read().decode("utf-8"))
        except HTTPError as e:
            if e.code == 429:  # Rate limited
                wait = 2 ** (i + 1)
                print(f"Rate limited, waiting {wait}s...", file=sys.stderr)
                time.sleep(wait)
            elif i == retries - 1:
                raise
            else:
                time.sleep(1)
        except URLError:
            if i == retries - 1:
                raise
            time.sleep(1)
    return None

def fetch_orcid_json(url, retries=3):
    """Fetch JSON from ORCID API."""
    for i in range(retries):
        try:
            req = Request(url, headers=ORCID_HEADERS)
            resp = urlopen(req, timeout=30)
            return json.loads(resp.read().decode("utf-8"))
        except (HTTPError, URLError):
            if i == retries - 1:
                raise
            time.sleep(1)
    return None

def get_orcid_profile(orcid_id):
    """Fetch name and DOIs from ORCID to help match in Semantic Scholar."""
    # Get name
    person_url = f"{ORCID_API}/{orcid_id}/person"
    person = fetch_orcid_json(person_url)
    if not person:
        return None

    name_data = person.get("name", {}) or {}
    given = name_data.get("given-names", {}).get("value", "") if name_data else ""
    family = name_data.get("family-name", {}).get("value", "") if name_data else ""
    name = f"{given} {family}".strip()

    # Get DOIs from works for matching
    works_url = f"{ORCID_API}/{orcid_id}/works"
    works = fetch_orcid_json(works_url)
    dois = set()
    if works:
        for group in works.get("group", []):
            for ext_id in group.get("external-ids", {}).get("external-id", []):
                if ext_id.get("external-id-type", "").lower() == "doi":
                    dois.add(ext_id.get("external-id-value", "").lower())

    return {"orcid": orcid_id, "name": name, "dois": list(dois)}

def search_author(query):
    """Search for an author by name. Returns list of matching authors."""
    url = f"{API_BASE}/author/search?query={quote(query)}&fields=name,affiliations,homepage,paperCount,citationCount,hIndex"
    data = fetch_json(url)
    return data.get("data", []) if data else []

def get_author_by_id(author_id):
    """Get author profile by Semantic Scholar author ID."""
    fields = "name,affiliations,homepage,paperCount,citationCount,hIndex"
    url = f"{API_BASE}/author/{author_id}?fields={fields}"
    return fetch_json(url)

def get_author_papers(author_id, limit=1000, offset=0):
    """Get all papers for an author with full bibliographic details."""
    fields = "title,year,authors,venue,publicationVenue,publicationTypes,externalIds,citationCount,abstract,isOpenAccess,openAccessPdf,fieldsOfStudy"
    papers = []
    batch_size = 100  # API max per request

    while offset < limit:
        url = f"{API_BASE}/author/{author_id}/papers?fields={fields}&limit={batch_size}&offset={offset}"
        data = fetch_json(url)
        if not data or not data.get("data"):
            break
        papers.extend(data["data"])
        if len(data["data"]) < batch_size:
            break
        offset += batch_size
        if offset < limit and offset % 200 == 0:
            print(f"Fetched {offset} papers...", file=sys.stderr)
        time.sleep(0.5)  # Rate limiting

    return papers

def format_paper(paper, target_author_id=None):
    """Format a paper into standard bibliographic format."""
    # Full author list with positions
    authors = paper.get("authors") or []
    author_names = [a.get("name", "") for a in authors if a]

    # Find target author's position if provided
    author_position = None
    if target_author_id:
        for i, a in enumerate(authors):
            if a and a.get("authorId") == target_author_id:
                author_position = i + 1
                break

    # Venue information
    venue_info = paper.get("publicationVenue") or {}
    venue_name = venue_info.get("name") or paper.get("venue") or ""
    venue_type = venue_info.get("type") or ""

    # External IDs (DOI, ArXiv, etc.)
    external_ids = paper.get("externalIds") or {}

    pub = {
        "title": paper.get("title") or "",
        "year": paper.get("year"),
        "authors": ", ".join(author_names),
        "author_list": author_names,
        "author_count": len(authors),
        "author_position": author_position,
        "venue": venue_name,
        "venue_type": venue_type,
        "publication_types": paper.get("publicationTypes") or [],
        "citations": paper.get("citationCount") or 0,
        "fields_of_study": paper.get("fieldsOfStudy") or [],
        "is_open_access": paper.get("isOpenAccess") or False,
    }

    # Add external IDs
    if external_ids.get("DOI"):
        pub["doi"] = external_ids["DOI"]
    if external_ids.get("ArXiv"):
        pub["arxiv"] = external_ids["ArXiv"]
    if external_ids.get("PubMed"):
        pub["pubmed"] = external_ids["PubMed"]
    if external_ids.get("CorpusId"):
        pub["semantic_scholar_id"] = external_ids["CorpusId"]
        pub["url"] = f"https://www.semanticscholar.org/paper/{external_ids['CorpusId']}"

    # Open access PDF
    if paper.get("openAccessPdf"):
        pub["pdf_url"] = paper["openAccessPdf"].get("url")

    # Abstract (if available)
    if paper.get("abstract"):
        pub["abstract"] = paper["abstract"]

    return pub

def get_profile(author_id=None, name=None, orcid=None):
    """Fetch complete author profile with all publications."""

    # If ORCID provided, lookup name and DOIs for matching
    orcid_info = None
    if orcid and not author_id:
        print(f"Looking up ORCID: {orcid}", file=sys.stderr)
        orcid_info = get_orcid_profile(orcid)
        if not orcid_info or not orcid_info.get("name"):
            raise RuntimeError(f"Could not find ORCID profile: {orcid}")
        name = orcid_info["name"]
        print(f"Found name: {name}", file=sys.stderr)

    # If name provided, search for author
    if name and not author_id:
        results = search_author(name)
        if not results:
            raise RuntimeError(f"No author found for: {name}")

        # If we have ORCID DOIs, try to match by checking each candidate's papers
        if orcid_info and orcid_info.get("dois") and len(results) > 1:
            orcid_dois = set(d.lower() for d in orcid_info["dois"])
            print(f"Matching using {len(orcid_dois)} DOIs from ORCID...", file=sys.stderr)

            best_match = None
            best_score = 0
            for r in results[:10]:
                cand_id = r.get("authorId")
                # Fetch a sample of papers to check DOIs
                papers = get_author_papers(cand_id, limit=50, offset=0)
                cand_dois = set()
                for p in papers:
                    ext_ids = p.get("externalIds") or {}
                    if ext_ids.get("DOI"):
                        cand_dois.add(ext_ids["DOI"].lower())

                overlap = len(orcid_dois & cand_dois)
                if overlap > best_score:
                    best_score = overlap
                    best_match = cand_id
                    print(f"  {r.get('name')} ({cand_id}): {overlap} DOI matches", file=sys.stderr)

            if best_match and best_score > 0:
                print(f"Best match: {best_match} with {best_score} DOI overlaps", file=sys.stderr)
                author_id = best_match

        # If still no single match, return disambiguation list
        if not author_id and len(results) > 1:
            return {
                "status": "multiple_matches",
                "message": f"Found {len(results)} authors matching '{name}'. Please specify author_id.",
                "matches": [{
                    "author_id": r.get("authorId"),
                    "name": r.get("name"),
                    "affiliations": r.get("affiliations", []),
                    "paper_count": r.get("paperCount", 0),
                    "citation_count": r.get("citationCount", 0),
                    "h_index": r.get("hIndex", 0),
                } for r in results[:10]]
            }

        if not author_id:
            author_id = results[0].get("authorId")

    if not author_id:
        raise RuntimeError("Either author_id or name must be provided")

    # Get author profile
    author = get_author_by_id(author_id)
    if not author:
        raise RuntimeError(f"Author not found: {author_id}")

    profile = {
        "author_id": author_id,
        "name": author.get("name", ""),
        "affiliations": author.get("affiliations", []),
        "homepage": author.get("homepage"),
        "citations": {
            "total": author.get("citationCount", 0),
            "h_index": author.get("hIndex", 0),
        },
        "paper_count": author.get("paperCount", 0),
        "profile_url": f"https://www.semanticscholar.org/author/{author_id}",
    }

    # Get all papers
    print(f"Fetching papers for {profile['name']}...", file=sys.stderr)
    papers_raw = get_author_papers(author_id)

    # Format papers
    publications = [format_paper(p, author_id) for p in papers_raw]

    # Sort by year descending (newest first), None years at end
    publications.sort(key=lambda x: (x.get("year") is None, -(x.get("year") or 0)))

    profile["publications"] = publications
    profile["publication_count"] = len(publications)

    # Summary stats
    pubs_with_year = [p for p in publications if p.get("year")]
    if pubs_with_year:
        profile["years_active"] = f"{min(p['year'] for p in pubs_with_year)}-{max(p['year'] for p in pubs_with_year)}"
        profile["top_cited"] = sorted(publications, key=lambda x: x.get("citations", 0), reverse=True)[:5]

    # Publication type breakdown
    type_counts = {}
    for pub in publications:
        for pt in pub.get("publication_types", ["unknown"]):
            type_counts[pt] = type_counts.get(pt, 0) + 1
    profile["publication_types"] = type_counts

    return profile

def main():
    import argparse
    parser = argparse.ArgumentParser(
        description="Fetch researcher profile from Semantic Scholar",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --id 1741101               # Fetch by Semantic Scholar author ID
  %(prog)s --name "Yoshua Bengio"     # Search by name
  %(prog)s --name "John Smith"        # Returns matches for disambiguation
  %(prog)s --orcid 0000-0002-1825-0097  # Lookup via ORCID (auto-matches using DOIs)
        """
    )
    parser.add_argument("--id", dest="author_id", help="Semantic Scholar author ID")
    parser.add_argument("--name", help="Author name to search for")
    parser.add_argument("--orcid", help="ORCID ID (will lookup name and match using DOIs)")
    args = parser.parse_args()

    if not args.author_id and not args.name and not args.orcid:
        parser.error("Either --id, --name, or --orcid must be provided")

    try:
        result = get_profile(author_id=args.author_id, name=args.name, orcid=args.orcid)
        print(json.dumps(result, indent=2, ensure_ascii=False))
    except Exception as e:
        print(json.dumps({"error": str(e)}, indent=2), file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
