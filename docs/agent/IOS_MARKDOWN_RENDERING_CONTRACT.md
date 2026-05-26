# iOS Markdown Rendering Contract

## Purpose

The iOS app should receive predictable Markdown that renders cleanly on small screens.

## Supported Markdown

### Paragraphs

Use short paragraphs.

### Bold Labels

```md
**Main point:** Explanation here.
```

### Bullets

```md
- **Memory:** Short explanation.
- **Movement:** Short explanation.
- **Mood:** Short explanation.
```

### Numbered Steps

Use only for ordered instructions.

```md
1. Track the symptom.
2. Note when it started.
3. Contact the clinician if it persists or worsens.
```

### Links

Use standard Markdown links only.

```md
[Source name](https://example.com)
```

## Avoid

Do not emit:

```html
<br>
<div>
<span>
```

Do not rely on:

- tables
- footnotes
- raw URLs
- deeply nested bullets
- Markdown inside Markdown links
