# Architecture — fhg-kit

## Overview
fhg-kit — framework-free shared library for the fhgomes constellation of micro-SaaS products (LLM usage ledger and cost estimation first; credentials, flags, audit and integration contracts to follow)

## Architecture Pattern
Ports & adapters: framework-free core modules expose interfaces, the starter wires them for Spring Boot

## Project Structure
```
fhg-kit/
├── fhg-kit-bom/              # Platform (BOM)
├── fhg-kit-llm/
├── fhg-kit-spring-boot-starter/
├── gradle.properties                # libVersion + pinned versions
└── CHANGELOG.md
```

## Key Design Decisions
- TODO: Document key architectural decisions here

## Dependencies
- TODO: Document key external dependencies

## Data Flow
- TODO: Document the main data flow
