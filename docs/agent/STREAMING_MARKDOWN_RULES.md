# Streaming Markdown Rules

## Purpose

Streaming responses should remain readable while partial text is arriving and should settle into valid, iOS-safe Markdown when complete.

## Rules

- Keep paragraphs short so partial responses do not create long blocks on iPhone.
- Prefer plain section labels such as `**What this may mean**` and `**What to do next**`.
- Do not split Markdown links across chunks when avoidable.
- Do not split bold markers across chunks when avoidable.
- Do not stream raw HTML.
- Do not rely on tables, footnotes, or deeply nested lists.
- Keep urgent safety language direct and early in the response.

## Safe Streaming Shape

```md
Short direct answer.

**What this may mean**
- **First point:** One concise sentence.
- **Second point:** One concise sentence.

**What to do next**
- Practical next step.
- Clinician follow-up when appropriate.
```

## Unsafe Streaming Shape

```md
<div>
| Symptom | Meaning | Action |
| --- | --- | --- |
...
```
