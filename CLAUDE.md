# fhg-kit — Claude Code notes

> **`AGENTS.md` is canonical.** Read it first; this file only adds what is specific to
> Claude Code (permissions, skills, hooks). If the two disagree, `AGENTS.md` wins.

## Claude Code layout

- `.claude/settings.json` — allowed commands for this repo.
- `.claude/skills/speckit-*/` — Spec Kit workflow skills (`/speckit-specify`, `/speckit-plan`,
  `/speckit-tasks`, `/speckit-implement`, `/speckit-converge`, …).
- `.claude/commands/` — the remaining slash commands (`/research`, `/speckit.design`, …).
- `.ai/` — architecture, guidelines and testing docs shared with every other agent.

## Working rules for Claude Code here

- Do not build or run `docker compose` under `sudo`.
- Prefer the verified commands in `AGENTS.md` over guessing; if one fails, say so with the output.
- After any non-trivial infrastructure change on a host, write a dated note in the host's
  `infra/notes/` vault.
