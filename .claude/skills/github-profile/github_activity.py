#!/usr/bin/env python3
"""Fetch GitHub contribution activity for all years since account creation."""

import re
import sys
from datetime import datetime
from urllib.request import Request, urlopen

def fetch(url):
    req = Request(url, headers={"X-Requested-With": "XMLHttpRequest"})
    return urlopen(req).read().decode()

def get_user_info(username):
    data = fetch(f"https://api.github.com/users/{username}")
    created = re.search(r'"created_at":"(\d{4})', data)
    return int(created.group(1)) if created else None

def get_year_contributions(username, year):
    html = fetch(f"https://github.com/{username}?tab=contributions&from={year}-01-01")
    counts = re.findall(r'>(\d+) contributions? on', html)
    levels = re.findall(r'<td[^>]*data-level="(\d)"', html)  # only <td>, not legend <div>
    return {
        "total": sum(int(c) for c in counts),
        "active_days": sum(1 for l in levels if l != "0"),
        "tracked_days": len(levels),
        "levels": {i: levels.count(str(i)) for i in range(5)}
    }

def main(username):
    start_year = get_user_info(username)
    current_year = datetime.now().year

    print(f"GitHub Activity: {username} ({start_year}-{current_year})\n")
    print(f"{'Year':<6} {'Total':>6} {'Active':>7} {'Rate':>7}  Distribution (0/1/2/3/4)")
    print("-" * 60)

    for year in range(current_year, start_year - 1, -1):
        stats = get_year_contributions(username, year)
        rate = f"{100*stats['active_days']/max(1,stats['tracked_days']):.0f}%"
        dist = "/".join(str(stats['levels'][i]) for i in range(5))
        print(f"{year:<6} {stats['total']:>6} {stats['active_days']:>7} {rate:>7}  {dist}")

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "jbarrow")
