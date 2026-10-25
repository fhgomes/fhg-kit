---
description: Conduct technical research on technologies, patterns, libraries, or best practices to inform implementation decisions.
handoffs:
  - label: Create Plan
    agent: speckit.plan
    prompt: Use this research to create an implementation plan
    send: false
  - label: Update Constitution
    agent: speckit.constitution
    prompt: Update constitution based on research findings
    send: false
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Outline

This command performs focused technical research to answer specific questions about:
- Technology choices (libraries, frameworks, tools)
- Implementation patterns and best practices
- Performance, security, or scalability considerations
- Integration approaches with existing systems
- Trade-offs between alternative solutions

**Research is technology-focused and evidence-based.** It produces actionable recommendations with clear rationale.

---

## Execution Flow

### Step 1: Parse Research Request

**Extract from user input:**
1. **Research topic** - What needs to be researched?
2. **Context** - What feature/system is this for?
3. **Constraints** - Project-specific limitations (from constitution)
4. **Decision criteria** - What factors matter most?

**Examples:**
- "Research task scheduling libraries for Java Spring Boot"
  - Topic: Task scheduling libraries
  - Context: Java 21, Spring Boot 4.x
  - Constraints: Simplicity First, Containerization, Modern Java
  - Criteria: Ease of use, Spring integration, Docker compatibility

- "Best practices for Selenium retry logic with exponential backoff"
  - Topic: Retry patterns
  - Context: Selenium WebDriver, Travian automation
  - Constraints: Browser Automation Reliability
  - Criteria: Robustness, error handling, configurability

**If user input is vague:**
- Ask clarifying questions (max 3)
- Suggest specific research angles
- Provide research scope options

---

### Step 2: Load Project Context

**Read constitutional constraints:**
1. `.specify/memory/constitution.md` - All 9 principles
2. `.specify/memory/project-overview.md` - Architecture, tech stack
3. `.specify/memory/tech-context.md` - Current dependencies

**Extract applicable constraints:**
- **Principle 1 (Simplicity):** Prefer simple solutions over complex ones
- **Principle 2 (Containerization):** Must work in Docker
- **Principle 3 (Modern Java):** Java 21, Spring Boot 4.x, records, virtual threads
- **Principle 4 (Data Sovereignty):** Embedded database (H2/SQLite)
- **Principle 5 (Selenium Reliability):** Explicit waits, retries, error handling
- **Principle 6 (Observability):** Logging, monitoring, debugging support
- **Principle 7 (Security):** Credential management, encryption
- **Principle 8 (Reference Implementation):** Check TravianBotSharp for patterns
- **Principle 9 (Modularity):** Services <500 lines, modules <400 lines

**Identify relevant constraints for this research topic.**

---

### Step 3: Conduct Research

#### 3.1 Web Search Strategy

**For each research topic, use multi-angle search:**

1. **Official Documentation**
   - Search: "[library/framework] official documentation [version]"
   - Priority: Official docs are source of truth
   - Example: "Spring @Scheduled official documentation Spring Boot 3"

2. **Best Practices & Guides**
   - Search: "[topic] best practices [context] [year]"
   - Priority: Industry standards, authoritative sources (Spring.io, Baeldung, DZone)
   - Example: "task scheduling best practices Spring Boot 2024"

3. **Comparisons & Alternatives**
   - Search: "[option A] vs [option B] [context]"
   - Priority: Benchmark articles, Stack Overflow comparisons
   - Example: "Spring @Scheduled vs Quartz scheduler Spring Boot"

4. **Real-World Examples**
   - Search: "[topic] example [context] GitHub"
   - Priority: Production code examples, popular repositories
   - Example: "Selenium retry logic exponential backoff Java example GitHub"

5. **Known Issues & Gotchas**
   - Search: "[library] common issues [context]"
   - Priority: Stack Overflow, GitHub issues, blog posts about pitfalls
   - Example: "Quartz scheduler Docker container issues"

**Search Guidelines:**
- Include version numbers (Java 21, Spring Boot 4.x)
- Prefer recent content (2023-2024) unless researching mature libraries
- Cross-reference multiple sources (minimum 3 sources per finding)
- Prioritize: Official Docs > Spring Guides > Baeldung > Stack Overflow > Medium

#### 3.2 Reference Implementation Check

**If applicable, check TravianBotSharp:**

1. Search `.reference/TravianBotSharp/` for similar functionality
2. Identify C# patterns that could translate to Java
3. Note improvements over reference implementation
4. Document feature parity considerations

**Example:**
```bash
# Search for task scheduling in TravianBotSharp
grep -r "scheduler\|scheduling\|timer" .reference/TravianBotSharp/ --include="*.cs" | head -20
```

#### 3.3 Constitutional Validation

**For each solution candidate, validate against principles:**

| Principle | Question                                           | Pass/Fail |
|-----------|----------------------------------------------------|-----------|
| 1. Simplicity | Is this the simplest solution that works?          | ✅/❌ |
| 2. Containerization | Does it work in Docker without modification?       | ✅/❌ |
| 3. Modern Java | Does it leverage Java 21/Spring Boot 4.x features? | ✅/❌ |
| 4. Data Sovereignty | Does it support embedded database?                 | ✅/N/A |
| 5. Selenium Reliability | Does it handle retries/errors gracefully?          | ✅/N/A |
| 6. Observability | Can we log/monitor its execution?                  | ✅/❌ |
| 7. Security | Does it handle credentials securely?               | ✅/N/A |
| 8. Reference Compatibility | Does TBS use similar approach?                     | ✅/N/A |
| 9. Modularity | Can it be implemented in <500 lines?               | ✅/❌ |

**Mark N/A for non-applicable principles.**

---

### Step 4: Analyze Findings

#### 4.1 Option Comparison

For each viable solution, document:

**Option [N]: [Solution Name]**

**Overview:**
- [1-2 sentence description]
- [Link to official documentation]

**Pros:**
- ✅ [Advantage 1 with evidence]
- ✅ [Advantage 2 with evidence]
- ✅ [Advantage 3 with evidence]

**Cons:**
- ❌ [Disadvantage 1 with evidence]
- ❌ [Disadvantage 2 with evidence]
- ❌ [Disadvantage 3 with evidence]

**Constitutional Compliance:**
- [List which principles it satisfies/violates]
- [Overall compliance: PASSED/FAILED/CONDITIONAL]

**Integration Effort:**
- Dependencies: [List Gradle dependencies]
- Configuration: [Low/Medium/High complexity]
- Code changes: [Estimate lines of code]

**Maturity & Support:**
- First release: [Year]
- Latest stable version: [X.Y.Z]
- Community: [GitHub stars, Stack Overflow questions]
- Maintenance: [Active/Stable/Declining]

**Performance Characteristics:**
- Overhead: [Minimal/Moderate/High]
- Scalability: [How it scales]
- Resource usage: [Memory, CPU, threads]

#### 4.2 Decision Matrix

Create comparison table:

| Criteria | Weight | Option 1 | Option 2 | Option 3 |
|----------|--------|----------|----------|----------|
| Simplicity | High | 9/10 | 6/10 | 7/10 |
| Spring Integration | High | 10/10 | 5/10 | 8/10 |
| Docker Compatibility | High | 9/10 | 9/10 | 9/10 |
| Observability | Medium | 8/10 | 9/10 | 7/10 |
| Community Support | Medium | 10/10 | 8/10 | 6/10 |
| Performance | Low | 7/10 | 9/10 | 8/10 |
| **Weighted Score** | - | **8.7** | **7.2** | **7.6** |

**Weight Guidelines:**
- **High:** Constitutional principles, core requirements
- **Medium:** Nice-to-have features, developer experience
- **Low:** Minor optimizations, aesthetic preferences

---

### Step 5: Make Recommendation

#### 5.1 Primary Recommendation

**Recommended Solution: [Option Name]**

**Rationale:**
1. [Primary reason - e.g., "Best alignment with Simplicity First principle"]
2. [Secondary reason - e.g., "Native Spring Boot integration requires minimal code"]
3. [Tertiary reason - e.g., "Proven in production, 10k+ GitHub stars"]

**Constitutional Compliance:**
- ✅ Satisfies principles: [List numbers and names]
- ⚠ Conditional on: [Any caveats]
- ❌ Violates: [None / list exceptions with mitigation]

**Implementation Summary:**
- **Dependencies:** [Gradle snippet]
- **Estimated LOC:** [X lines]
- **Complexity:** [Low/Medium/High]
- **Risk Level:** [Low/Medium/High]

**Example Code:**
```java
// Minimal working example demonstrating recommended approach
@Configuration
public class SchedulerConfig {
    // Example implementation
}
```

#### 5.2 Alternative Recommendation (Optional)

**Alternative: [Option Name]**

**When to use this instead:**
- [Specific scenario where alternative is better]
- [Example: "If we need distributed scheduling across multiple containers"]

**Trade-offs:**
- [What you gain vs what you lose]

---

### Step 6: Document Research

#### 6.1 Create research.md

**File location:**
- If part of feature spec: `specs/[N]-[feature-name]/research/research.md`
- If standalone research: `.specify/research/[topic-slug]/research.md`

**File structure:**

```markdown
# Research: [Topic]

**Research Date:** [YYYY-MM-DD]
**Researcher:** [AI Agent Name]
**Context:** [Feature or general research]

---

## Research Question

[What specific question(s) did this research aim to answer?]

---

## Background & Context

[Why is this research needed? What problem are we solving?]

**Project Constraints:**
- [Relevant constitutional principles]
- [Existing tech stack limitations]
- [Performance/security requirements]

---

## Options Evaluated

### Option 1: [Name]

[Use format from Step 4.1]

### Option 2: [Name]

[Use format from Step 4.1]

### Option 3: [Name]

[Use format from Step 4.1]

---

## Decision Matrix

[Use table from Step 4.2]

---

## Recommendation

[Use format from Step 5.1]

---

## Implementation Guidance

### Dependencies

```gradle
dependencies {
    implementation 'group:artifact:version' // Purpose
}
```

### Configuration

```properties
# application.properties
key=value
```

### Code Example

```java
// Minimal working implementation
```

### Testing Strategy

- [How to test this implementation]
- [Key test cases to cover]

---

## References

- [Official Documentation](URL)
- [Best Practices Guide](URL)
- [Example Implementation](URL)
- [Stack Overflow Discussion](URL)

---

## Open Questions

[Any unresolved questions or areas needing further research]

---

**Document Version:** 1.0.0
**Status:** Complete | Pending Clarification | Requires Update
```

#### 6.2 Update Tech Context

**If recommendation is accepted, update `.specify/memory/tech-context.md`:**

Add to appropriate feature section:
```markdown
### Feature [N]: [Feature Name]

**Added Technologies:**
- `[artifact-id]:[version]` - [Purpose, based on research findings]

**Key Patterns:**
- [Pattern name]: [Description from research]

**Research Reference:** `specs/[N]-[feature]/research/research.md`
```

---

### Step 7: Report Findings

**Output summary:**

```
✅ Research Complete: [Topic]

Recommendation: [Recommended Solution Name]
Rationale: [1-sentence summary]

Options Evaluated: [N options]
Constitutional Compliance: [PASSED/CONDITIONAL/FAILED]

Research Document: [Path to research.md]

Next Steps:
1. Review research findings
2. Approve/reject recommendation
3. Update technical plan with chosen solution
4. Implement with guidance from research document

References:
- [Key reference 1]
- [Key reference 2]
```

---

## Research Best Practices

### Information Quality Hierarchy

**Tier 1 (Most Reliable):**
1. Official documentation (Spring.io, Oracle Java docs)
2. Framework maintainer blogs (Spring team, Baeldung with Spring team input)
3. GitHub repositories from framework maintainers

**Tier 2 (Reliable):**
1. Authoritative tutorial sites (Baeldung, DZone, JetBrains guides)
2. Spring Boot guides and samples
3. High-reputation Stack Overflow answers (>100 upvotes, accepted answer)

**Tier 3 (Useful but verify):**
1. Technical blogs from industry practitioners
2. Medium articles with code examples
3. GitHub repositories from experienced developers (>500 stars)

**Tier 4 (Use with caution):**
1. Random blog posts without attribution
2. Outdated Stack Overflow answers (>3 years old for fast-moving tech)
3. Unverified code snippets

**Red Flags (Avoid):**
- Information about different major versions (e.g., Spring Boot 4.x when using 4.x)
- Conflicting information without explanation
- Code examples that violate project principles
- Sources promoting specific commercial products without disclosure

### Constitutional Compliance Check

**Every recommendation MUST validate against:**

1. **Principle 1 (Simplicity):**
   - Is there a simpler solution?
   - Does it avoid unnecessary abstraction?
   - Can a junior developer understand it?

2. **Principle 2 (Containerization):**
   - Test Docker compatibility explicitly
   - Check for filesystem/network assumptions
   - Verify environment variable configuration

3. **Principle 3 (Modern Java):**
   - Leverages Java 21 features when beneficial
   - Compatible with Spring Boot 4.x
   - Uses modern patterns (records, streams, optionals)

4. **Principle 9 (Modularity):**
   - Implementation fits in <500 lines (services)
   - Clear separation of concerns
   - Single Responsibility Principle

### Evidence Standards

**Each claim must be supported by:**
- **Primary source:** Official documentation link
- **Secondary source:** Authoritative tutorial or guide
- **Validation:** Code example or benchmark data

**Example of well-supported claim:**
> "Spring's `@Scheduled` annotation provides simpler scheduling than Quartz for basic use cases [Official Docs](link). Baeldung's comparison shows 80% less configuration code for cron-based tasks [Guide](link). Spring Boot starter-scheduler includes it by default, requiring zero additional dependencies [Starter Docs](link)."

**Example of poorly-supported claim:**
> "Spring @Scheduled is better than Quartz." ❌ (No evidence, no context)

---

## Error Handling

**If research question is too broad:**
- Break into sub-questions
- Research each independently
- Synthesize findings

**If no clear recommendation emerges:**
- Document trade-offs honestly
- Provide decision criteria for project team
- Suggest prototype/spike to gather data

**If constitutional principles conflict:**
- Document the conflict explicitly
- Propose compromise or exception
- Escalate to project stakeholder

**If information is contradictory:**
- Investigate source reliability
- Test claims with code examples
- Document uncertainty and recommend validation

---

## Template References

- Research output: Use structure from Step 6.1
- Constitution: `.specify/memory/constitution.md`
- Tech context: `.specify/memory/tech-context.md`
- Project overview: `.specify/memory/project-overview.md`

---

**Command Version:** 1.0.0
**Last Updated:** 2026-01-24
**Best Practices:** Evidence-based, constitutional compliance, multi-source validation
