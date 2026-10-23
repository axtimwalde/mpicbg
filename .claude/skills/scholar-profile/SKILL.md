---
name: scholar-profile
description: Analyze a researcher's Semantic Scholar profile — publication record, citation metrics, research areas, collaboration patterns, and academic impact. Use for reviewing candidate credentials, preparing for meetings, or understanding a researcher's background.
---

# Scholar Profile Analysis

Analyze a researcher's academic profile using Semantic Scholar data and web research to extract meaningful insights about their academic contributions, research trajectories, and scholarly impact.

## Tool

Use `.claude/skills/scholar-profile/semantic_scholar.py`
usage: semantic_scholar.py [-h] [--id AUTHOR_ID] [--name NAME] [--orcid ORCID]

Fetch researcher profile from Semantic Scholar

options:
-h, --help show this help message and exit
--id AUTHOR_ID Semantic Scholar author ID
--name NAME Author name to search for
--orcid ORCID ORCID ID (will lookup name and match using DOIs)

Examples:
semantic_scholar.py --id 1741101 # Fetch by Semantic Scholar author ID
semantic_scholar.py --name "Yoshua Bengio" # Search by name
semantic_scholar.py --name "John Smith" # Returns matches for disambiguation
semantic_scholar.py --orcid 0000-0002-1825-0097 # Lookup via ORCID (auto-matches using DOIs)

## Analysis Framework

From the Semantic Scholar data and supplementary web searches, extract:

### 1. Core Metrics
- Total citations and citation trajectory over time
- h-index and i10-index with interpretation relative to career stage and field
- Publication count and publication rate

### 2. Research Focus
- Primary research areas (from paper titles, venues, fields of study)
- Evolution of interests over time
- Interdisciplinary breadth vs. specialization

### 3. Publication Analysis
- Most cited works (top 5-10) with significance
- Recent publications (last 2-3 years) for current directions
- Publication venues (journals, conferences, tier)
- Preprint activity (arXiv, bioRxiv, etc.) if visible

### 4. Collaboration Patterns
- Frequent co-authors and research groups
- Institutional affiliations (current and past)

### 5. Impact Assessment
- Field-normalized metric interpretation
- Landmark vs. incremental contributions
- Highly cited reviews, methods papers, or foundational works

## Supplementary Research

After Semantic Scholar analysis, search the web for:
- Current institutional affiliation and role
- Lab/group webpage
- GitHub, ORCID, LinkedIn profiles
- Any connection to the user's organization (use information about people and projects available to you)

## Output

Provide a structured summary with:

```
## Profile Overview
- Name, current affiliation, listed research interests
- Career metrics snapshot (citations, h-index, i10-index)

## Research Areas
- Primary focus areas with brief descriptions
- Notable methodological or domain expertise

## Key Publications
- Top cited works with citation counts and significance
- Recent notable works

## Collaboration Network
- Key collaborators and institutions
- Research group affiliations

## Assessment
- Academic standing interpretation
- Research trajectory observations
- Notable strengths or unique contributions
```

## Guidelines

1. **Be objective** — report metrics factually with field-appropriate context
2. **Note limitations** — incomplete profiles, author conflation, sparse data
3. **Field context** — h-index of 30 means different things in math vs. biomedicine
4. **Recency** — distinguish historical impact from current activity
5. **Connections** — note any relationship to the user's organization if found