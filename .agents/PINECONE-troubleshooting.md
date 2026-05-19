# Pinecone Troubleshooting Guide

> **Prerequisites**: See [PINECONE.md](./PINECONE.md) for universal concepts and setup.

This guide covers common troubleshooting issues that apply across all programming languages. For language-specific troubleshooting and code examples, see the appropriate language guide:

- **Python**: [PINECONE-python.md](./PINECONE-python.md#troubleshooting)
- **TypeScript/Node.js**: [PINECONE-typescript.md](./PINECONE-typescript.md#troubleshooting)
- **Go**: [PINECONE-go.md](./PINECONE-go.md#troubleshooting)
- **Java**: [PINECONE-java.md](./PINECONE-java.md#troubleshooting)

## Common Problems & Solutions

### Problem: `describeIndexStats()` returns 0 records after upsert

**Cause**: Eventual consistency - records haven't indexed yet

**Solution**:

1. Wait 10+ seconds minimum after upserting records (include wait logic in generated code, not in terminal commands)
2. Check if records were actually upserted (no errors thrown)
3. Use a polling pattern for production code (see language-specific guides for examples)

**How to verify**:

- Wait 10-20 seconds after upserting
- Check stats again with `describeIndexStats()`
- Records should appear in stats within 10-20 seconds

### Problem: Search returns no results

**Cause**: Usually one of these:

1. Field name mismatch (using wrong field in `--field_map`)
2. Records not indexed yet (eventual consistency)
3. Empty namespace or wrong namespace name
4. Filtering too aggressively

**Solutions**:

1. **Verify field_map matches your records**:

   - If you created index with `--field_map text=content`
   - Make sure records have `content` field, not `text`
   - The field name in records must match the right side of `field_map`

2. **Check if records exist**:

   - List records using `listPaginated()` or equivalent
   - Verify records are in the correct namespace
   - See language-specific guides for code examples

3. **Try simple search without filters**:

   - Start with a basic search query without any filters
   - Gradually add filters to isolate the issue
   - See language-specific guides for code examples

4. **Wait for indexing**:
   - Records become searchable 5-10 seconds after upsert
   - Include wait logic in generated code (not terminal commands)
   - Use polling pattern in production code
   - See [Indexing Delays & Eventual Consistency](#indexing-delays--eventual-consistency) below

### Problem: Metadata too large error

**Cause**: Metadata exceeds 40KB limit per record

**Solution**:

- Check metadata size (must be under 40KB)
- Remove unnecessary metadata fields
- Flatten nested objects (nested structures not allowed)
- Store large data elsewhere and reference it in metadata

**Best practices**:

- Keep metadata minimal (IDs, categories, dates, etc.)
- Store large content in the record fields, not metadata
- Use flat structure only (no nested objects)

### Problem: Batch too large error

**Cause**: Exceeding batch size limits

**Solution**:

- **Text records**: MAX 96 records per batch, 2MB total
- **Vector records**: MAX 1000 records per batch, 2MB total
- Split large batches into smaller chunks
- See language-specific guides for batch processing examples

### Problem: Nested metadata error

**Cause**: Metadata contains nested objects or arrays of objects

**Solution**:

- Flatten all metadata to a single level
- Use string lists instead of object arrays
- Convert nested structures to flat key-value pairs

**Example**:

```typescript
// ❌ WRONG - nested objects not allowed
{
  user: { name: "John", id: 123 },
  tags: [{ type: "urgent" }]
}

// ✅ CORRECT - flat structure only
{
  user_name: "John",
  user_id: 123,
  tags: ["urgent", "important"]  // String lists OK
}
```

### Problem: Rate limit (429) errors

**Cause**: Too many requests too quickly

**Solution**:

- Implement exponential backoff retry logic
- Reduce request rate
- Add delays between batch operations
- See language-specific guides for retry pattern examples

**Best practices**:

- Only retry on transient errors (429, 5xx)
- Don't retry on client errors (4xx except 429)
- Use exponential backoff with max retries
- Cap delay at reasonable maximum (e.g., 60 seconds)

### Problem: Search results are not relevant

**Cause**: Usually one of these:

1. Not using reranking
2. Query doesn't match field_map configuration
3. Insufficient or poor quality data

**Solution**:

1. **Always use reranking in production**:

   - Use `bge-reranker-v2-m3` model
   - Rerank top 2x candidates for better results
   - See language-specific guides for reranking examples

2. **Verify query format**:

   - Query text should match the field type in `field_map`
   - For `--field_map text=content`, use text queries
   - Ensure embedding model matches your use case

3. **Improve data quality**:
   - Ensure records have sufficient context
   - Add more relevant records to the index
   - Consider data preprocessing and cleaning

### Problem: Index not found (404 errors)

**Cause**: Index doesn't exist or wrong index name

**Solution**:

1. Verify index exists: `pc index list` (CLI)
2. Check index name spelling (case-sensitive)
3. Verify you're in the correct project/organization
4. Create index if it doesn't exist (use CLI, not SDK)

### Problem: API key authentication failed

**Cause**: Invalid or expired API key

**Solution**:

1. Verify API key is set correctly in environment variable
2. Check API key hasn't expired or been revoked
3. Ensure API key has proper permissions
4. Generate new API key if needed: [Pinecone Console](https://app.pinecone.io/)

**Best practices**:

- Never hardcode API keys in source code
- Use environment variables for API keys
- Add `.env` files to `.gitignore`
- Rotate keys if exposed

### Problem: Namespace isolation issues

**Cause**: Not using namespaces or wrong namespace name

**Solution**:

- Always use namespaces for data isolation
- Verify namespace names match exactly (case-sensitive)
- Use consistent namespace naming strategy
- See [PINECONE.md](./PINECONE.md#data-operations) for namespace best practices

## Indexing Delays & Eventual Consistency

> **⚠️ IMPORTANT FOR AGENTS**: The wait instructions below apply **ONLY to generated code**, not to the agent's own behavior. When generating code that upserts records and then queries them, include wait logic in the generated code. **DO NOT** execute `sleep` commands in the terminal yourself.

Pinecone uses **eventual consistency**. This means records don't immediately appear in searches or stats after upserting.

### Realistic Timing Expectations

| Operation          | Time          | Notes                                       |
| ------------------ | ------------- | ------------------------------------------- |
| Record stored      | 1-3 seconds   | Data is persisted                           |
| Records searchable | 5-10 seconds  | Can find via `searchRecords()`              |
| Stats updated      | 10-20 seconds | `describeIndexStats()` shows accurate count |
| Indexes ready      | 30-60 seconds | New indexes enter "Ready" state             |

### Correct Wait Pattern

After upserting records:

1. **Minimum wait**: 10 seconds for records to become searchable
2. **Stats wait**: 10-20 seconds for stats to update
3. **Production pattern**: Use polling with `describeIndexStats()` to verify readiness

**Note**: Include wait logic in generated code (e.g., `time.sleep(10)` in Python, `TimeUnit.SECONDS.sleep(10)` in Java). Do not execute sleep commands in the terminal.

See language-specific guides for polling pattern code examples.

### Production Pattern: Polling for Readiness

For production code, implement a polling function that:

1. Checks `describeIndexStats()` periodically (every 5 seconds)
2. Compares record count with expected count
3. Times out after reasonable maximum (e.g., 5 minutes)
4. Returns when records are fully indexed

See language-specific guides for complete polling implementation examples.

## Quick Reference Table

| Issue                            | Solution                                              | Section                                         |
| -------------------------------- | ----------------------------------------------------- | ----------------------------------------------- |
| `describeIndexStats()` returns 0 | Wait 10+ seconds, use polling pattern                 | [Above](#indexing-delays--eventual-consistency) |
| Search returns no results        | Check field_map, namespace, wait for indexing         | [Above](#problem-search-returns-no-results)     |
| Metadata too large               | Reduce to <40KB, flatten nested objects               | [Above](#problem-metadata-too-large-error)      |
| Batch too large                  | Split to 96 (text) or 1000 (vector) per batch         | [Above](#problem-batch-too-large-error)         |
| Rate limit (429) errors          | Implement exponential backoff retry                   | [Above](#problem-rate-limit-429-errors)         |
| Nested metadata error            | Flatten all metadata - no nested objects              | [Above](#problem-nested-metadata-error)         |
| Index not found                  | Verify index exists with `pc index list`              | [Above](#problem-index-not-found-404-errors)    |
| API key authentication failed    | Verify key in environment variable, check permissions | [Above](#problem-api-key-authentication-failed) |

## Getting More Help

If you're still experiencing issues:

1. **Check language-specific guides** for code examples and patterns
2. **Review official documentation**: [https://docs.pinecone.io/](https://docs.pinecone.io/)
3. **Search documentation**: Use the search feature on [docs.pinecone.io](https://docs.pinecone.io/) to find relevant guides
4. **Error Handling**: See error handling patterns in language-specific guides and the [Production guides](https://docs.pinecone.io/guides/production/) section

---

**Remember**: Always use namespaces, use reranking for best results (shown in quickstarts), always handle errors with retry logic, and account for eventual consistency delays.
