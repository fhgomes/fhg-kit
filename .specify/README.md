# Speckit Directory Structure

This directory contains the specification-driven development artifacts for the **Live Resume & Career Copilot** project.

---

## Structure

- **`memory/`** - Project governance and long-term reference documents
  - `constitution.md` - Project constitution defining core principles and governance (v2.0.0)
  - `project-overview.md` - High-level project goals, architecture, and roadmap (v1.0.0)
  - `tech-context.md` - Technology stack tracking and architectural decisions (v1.0.0)
  - `version-tracking.md` - Version history and release protocol (v1.0.0)
  - `liquibase-guidelines.md` - Database schema evolution best practices (v1.0.0)

- **`templates/`** - Templates for specifications, plans, and tasks
  - `plan-template.md` - Design planning template
  - `spec-template.md` - Feature specification template
  - `tasks-template.md` - Task breakdown template
  - `commands/` - Workflow command definitions (slash commands)

- **`scripts/bash/`** - Utility scripts for workflow automation
  - `check-prerequisites.sh` - Validate feature development prerequisites
  - `create-new-feature.sh` - Scaffold new feature directories
  - `common.sh` - Shared script utilities

---

## Constitution

The project constitution (v2.1.0) establishes **14 core principles**:

1. **Simplicity First** - React SPA + Spring Boot REST API; standard patterns
2. **Containerization as First-Class Citizen** - Docker-first design
3. **Modern Java Standards** - Leverage Java 25 features (virtual threads, records)
4. **Data Sovereignty and Multi-Tenant Isolation** - PostgreSQL with tenant filtering
5. **AI Integration and LLM Management** - Backend-controlled LLM with cost tracking
6. **Observability and Debugging** - Comprehensive logging and monitoring
7. **Security, Privacy, and Credential Management** - JWT auth, BCrypt passwords, public/private content separation
8. **Multi-Tenant Architecture** - Soft multi-tenancy from day one
9. **Freemium Model and Coin-Based Monetization** - Sustainable pricing with virtual currency
10. **Full-Stack Modularity and Separation of Concerns** - Backend services max 500 lines, React components max 300 lines
11. **Database Schema Evolution** - Liquibase timestamp-based migrations
12. **Async Job Processing and Background Tasks** - Spring `@Async` or job queue library
13. **LinkedIn Compliance and ToS Respect** - Draft-only, user-initiated features
14. **Event-Driven Architecture and Reactive Patterns** - Domain events, loosely-coupled components

All specifications, plans, and tasks should reference applicable constitutional principles.

---

## Usage

### For New Features (Recommended Workflow):

1. **/speckit.specify** - Write a specification using the spec template
2. **/speckit.design** - Generate both plan.md and tasks.md in one step (streamlined)
   - _Alternative:_ **/speckit.plan** then **/speckit.tasks** for explicit control
3. Review plan and tasks
4. **/speckit.implement** - Execute tasks automatically

### For Small Changes/Bugs:

1. Run `.specify/scripts/bash/create-new-feature.sh --skip-branch "description"`
2. Write minimal `spec.md`, `plan.md`, `tasks.md` in `specs/[N]-[name]/`
3. Implement and document changes

---

## Project Context

**Project:** Live Resume & Career Copilot
**Type:** Career Management SaaS Platform
**Current Version:** v0.1.0 (Initial Planning)

**What is it?**
A modern career management platform combining:
- Live resume/portfolio site (always up-to-date)
- Structured skills & stories graph (STAR format)
- AI career copilot:
  - Recruiter chat on profile site
  - LinkedIn inbox reply drafting
  - LinkedIn profile score analyzer

**Tech Stack:**
- **Frontend:** React 18+ with TypeScript, React Query, MUI/Chakra UI
- **Backend:** Spring Boot 4.x with Java 25, Spring Security (JWT), Spring Data JPA
- **Database:** PostgreSQL 18 with JSONB and pgvector extension
- **AI/LLM:** OpenAI, Gemini, Claude APIs (backend-controlled)
- **Infrastructure:** Docker, Gradle, Logback + Logstash Encoder

---

## Key Documents

### Memory Files (Project Context)
- **constitution.md** - Core principles and governance model (v2.1.0 - 14 principles)
- **project-overview.md** - Goals, architecture, personas, roadmap (v2.0.0)
- **tech-context.md** - Technology stack and decisions (v1.0.2)
- **version-tracking.md** - Version history and release protocol (v1.0.0)
- **liquibase-guidelines.md** - Database migration best practices (v1.0.2)

### Templates
- **spec-template.md** - Feature specification structure
- **plan-template.md** - Design planning structure
- **tasks-template.md** - Task breakdown structure

---

## Governance

Constitution amendments require:
- Pull request with rationale
- Semantic version increment
- Updated Sync Impact Report
- Review of dependent templates

---

## Slash Commands

Available workflow commands (via `/speckit.*`):
- **/speckit.specify** - Create or update feature specification
- **/speckit.plan** - Execute implementation planning workflow
- **/speckit.tasks** - Generate actionable task breakdown
- **/speckit.design** - Generate both plan.md and tasks.md (streamlined)
- **/speckit.implement** - Execute implementation plan from tasks.md
- **/speckit.constitution** - Create or update project constitution
- **/speckit.clarify** - Identify underspecified areas in spec
- **/speckit.analyze** - Cross-artifact consistency and quality analysis
- **/speckit.checklist** - Generate custom checklist for feature
- **/speckit.taskstoissues** - Convert tasks to GitHub issues
- **/speckit.simple.task** - Simplified workflow for simple tasks
- **/speckit.simplest** - Minimal overhead for very simple tasks
- **/speckit.specifypd** - Unified spec-driven development workflow

---

## How This Differs from Travian Bot

This `.specify/` folder was adapted from the **Travian Bot** project (a game automation platform) to the **Live Resume & Career Copilot** project (a career management SaaS).

**What was preserved:**
- Speckit workflow framework (specify → plan → tasks → implement)
- Liquibase best practices (timestamp-based migrations, PostgreSQL patterns)
- Constitution structure (principles, governance, versioning)
- Spring Boot + Gradle + Docker patterns
- Java 25 best practices (virtual threads, records, pattern matching)

**What was changed from Travian Bot template:**
- **Project context:** Game automation platform → Career management SaaS
- **Domain model:** Villages/heroes/farm lists → Profiles/stories/skills/packages
- **Database:** ~~SQLite (embedded)~~ → **PostgreSQL 18** (production-grade relational DB with JSONB and pgvector for RAG)
- **Frontend:** ~~Vanilla JavaScript~~ → **React 18+ with TypeScript**
- **AI integration:** ~~Selenium browser automation~~ → **LLM/RAG APIs** (OpenAI, Gemini, Claude)
- **Security model:** Game credentials → **Multi-tenant SaaS** with JWT, RBAC, LinkedIn ToS compliance
- **File storage:** Local filesystem only → **Cloud storage** (S3/GCS) with local volume for development
- **Deployment:** Single container with embedded DB → **Multi-container architecture** (backend + frontend + PostgreSQL in separate containers)

---

## External References

**Confluence Documentation:**
- Project Space: https://techfernandogomes.atlassian.net/wiki/spaces/INTME/overview?homepageId=310149424

---

**Last Updated:** 2026-02-19
**AI Agents:** Use `.specify/memory/project-overview.md` for project context before implementing features.
