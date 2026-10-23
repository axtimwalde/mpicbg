#!/usr/bin/env python3
"""Fetch GitHub repository information for a user."""

import json
import sys
from datetime import datetime
from urllib.request import Request, urlopen

def fetch_json(url):
    req = Request(url, headers={"Accept": "application/vnd.github+json"})
    return json.loads(urlopen(req).read().decode())

def get_all_repos(username):
    repos, page = [], 1
    while True:
        batch = fetch_json(f"https://api.github.com/users/{username}/repos?per_page=100&page={page}")
        if not batch: break
        repos.extend(batch)
        page += 1
    return repos

def days_between(d1, d2):
    parse = lambda d: datetime.fromisoformat(d.replace("Z", "+00:00"))
    return (parse(d2) - parse(d1)).days

def main(username):
    repos = get_all_repos(username)
    now = datetime.now().astimezone().isoformat()
    result = []

    for r in repos:
        lifespan = days_between(r["created_at"], r["pushed_at"])
        recency = days_between(r["pushed_at"], now)
        result.append({
            "name": r["name"],
            "description": r["description"],
            "language": r["language"],
            "stars": r["stargazers_count"],
            "forks": r["forks_count"],
            "license": r["license"]["spdx_id"] if r["license"] else None,
            "updated": r["updated_at"][:10],
            "created": r["created_at"][:10],
            "is_fork": r["fork"],
            "lifespan_days": lifespan,
            "days_since_push": recency,
            "activity": "active" if recency < 365 else ("sustained" if lifespan > 90 else "one-off")
        })

    print(json.dumps(result, indent=2))

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "jbarrow")
