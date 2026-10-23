---
name: github-profile
description: Analyze a GitHub user's profile — activity patterns, repositories, technical expertise, and developer characterization. Use for evaluating candidates, understanding collaborators, or researching potential hires.
---

# GitHub Profile Analysis

Analyze a GitHub user's profile, activity, and repositories to build a comprehensive understanding of their professional role, skills, and coding style.

## Tools

### 1. Activity analysis

Use `.claude/skills/github-profile/github_activity.py`
usage: github_activity.py USERNAME

Fetches contribution activity for all years since account creation. Returns yearly totals, active days, and activity level distribution.

### 2. Repository analysis

Use `.claude/skills/github-profile/github_repos.py`
usage: github_repos.py USERNAME

Fetches all public repositories with metadata: languages, stars, forks, activity dates, and descriptions.

## Workflow

1. Fetch the profile page (`https://github.com/USERNAME`) and extract basic information.
2. Run `.claude/skills/github-profile/github_activity.py USERNAME` to understand activity patterns.
3. Run `.claude/skills/github-profile/github_repos.py USERNAME` to understand repositories.
4. Use information about people and projects available to you to augment the analysis.

## Analysis Framework

Answer the following questions:

1. What is their likely professional role?
2. What are their main technical interests/expertise areas?
3. What kind of projects do they work on (research, tools, learning, etc.)?
4. How would you characterize their coding activity pattern?
5. Developer profile classification — estimate whether they are (in any combination):
   - an active creative developer
   - a student trying to learn basic principles
   - a researcher testing conceptual ideas
   - an open source enthusiast creating broadly useful tools and libraries
   - a genius hacker with exceptional ideas
   - narrow or broad in scope and technical skills

Base your analysis solely on the data and your knowledge about the person. Be specific and cite evidence.

## Output

```markdown
# GitHub Profile Analysis: [USERNAME]

## Basic Information

- Name, location, bio, join date
- Follower/following stats
- Notable affiliations

## Activity Summary

- Overall activity level and patterns
- Contribution timeline observations
- Collaboration indicators

## Technical Profile

- Primary languages (with percentages if available)
- Key technologies and frameworks
- Domain areas

## Repository Analysis

- Total repos (owned vs forked)
- Notable projects with brief descriptions
- Quality indicators (documentation, maintenance)

## Assessment

### 1. Likely Professional Role

[Assessment with specific evidence]

### 2. Technical Interests & Expertise

[Assessment with specific evidence]

### 3. Project Types

[Assessment with specific evidence]

### 4. Activity Pattern Characterization

[Assessment with specific evidence]

### 5. Developer Profile Classification

[Assessment with reasoning and evidence]

## Key Observations

- Strengths
- Notable patterns
- Any concerns or gaps
```

## Guidelines

1. **Evidence-based** — every claim must cite specific data from your analysis
2. **Be specific** — name repositories, languages, patterns you observed
3. **Acknowledge limitations** — note if data is sparse or ambiguous
4. **Use your knowledge** — if you recognize the person or their work, incorporate that context
5. **Avoid assumptions** — distinguish between what the data shows vs inferences
6. **Be balanced** — note both strengths and limitations
7. **Quote metrics** — include specific numbers (commit counts, star counts, dates)
8. **Connections** — note any relationship to the user's organization if found

## Error Handling

- If scripts fail, report the error and do not proceed with partial data
- If the profile is private or doesn't exist, report this clearly
- If activity is minimal, note this and adjust analysis depth accordingly