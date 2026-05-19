"""Print the source registry as a readable table."""

from __future__ import annotations

from ..sources.registry import SOURCE_REGISTRY


def main() -> int:
    rows = [
        ("key", "access_class", "evidence_type", "namespace", "access_method"),
    ]
    for key, source in SOURCE_REGISTRY.items():
        rows.append(
            (
                key,
                source.access_class.value,
                source.evidence_type.value,
                source.pinecone_namespace,
                source.access_method,
            )
        )
    widths = [max(len(str(r[i])) for r in rows) for i in range(len(rows[0]))]
    for index, row in enumerate(rows):
        line = "  ".join(str(cell).ljust(widths[i]) for i, cell in enumerate(row))
        print(line)
        if index == 0:
            print("  ".join("-" * w for w in widths))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
