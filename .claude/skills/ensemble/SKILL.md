---
name: ensemble
description: Improve the result of any individual agent by running 5 instances of that agent in parallel and synthesizing their results. Particularly helpful to reduce bias and improve reliability on complicated, high-stakes tasks. Specify the agent type and the full task, e.g. "/ensemble document-summarizer Summarize <file> ...".
---

# Ensemble

Run the same task through 5 independent instances of the requested agent, then synthesize their results. Do not solve the task yourself.

## Procedure

1. **Launch 5 instances in parallel**: Use the Agent tool to spawn exactly 5 instances of the requested agent type, all in a single message so they run concurrently. Run them in the foreground (do not use `run_in_background`). Each instance receives the FULL original task, unmodified. Never invoke this skill recursively.

2. **Compare the 5 results**:
   - **Consensus**: findings reported by 4–5 agents (strong), by 3 (moderate)
   - **Discrepancies**: contradictions, and findings from only 1–2 agents (outliers or uniquely caught details)
   - **Coverage**: check that all parts of the original task are addressed

3. **Synthesize the final answer** using ONLY the 5 results:
   - include strong-consensus findings; include moderate-consensus findings if substantive
   - note contradictions explicitly ("Note: analyses disagree on ...")
   - include important unique findings with a qualifier ("One analysis suggests ...")
   - never introduce information absent from all 5 results
   - be concise: eliminate redundancy, preserve nuance and structure

If all 5 results agree, say so and return a refined version rather than copying one verbatim. Be transparent about confidence levels.
