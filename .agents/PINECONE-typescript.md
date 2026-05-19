# Pinecone TypeScript/Node.js SDK Guide

> **Prerequisites**: See [PINECONE.md](./PINECONE.md) for universal concepts and setup.

This guide provides TypeScript/Node.js-specific patterns, examples, and best practices for the Pinecone SDK.

## 🚨 MANDATORY RULES - Read First

**⚠️ CRITICAL: These rules MUST be followed. Violations will cause runtime errors or data issues.**

1. **MUST use namespaces** - Every upsert, search, fetch, delete operation MUST use `.namespace()` method
2. **MUST wait 10+ seconds** - After upserting records, MUST wait 10+ seconds before searching
3. **MUST match field_map** - Record field names MUST match the right side of `--field_map` used when creating index
4. **MUST respect batch limits** - Text records: MAX 96 per batch, Vector records: MAX 1000 per batch
5. **MUST use flat metadata** - No nested objects allowed, only flat key-value pairs
6. **MUST cast `hit.fields`** - TypeScript requires explicit casting: `hit.fields as Record<string, any>`
7. **MUST use `await`** - All SDK operations are async, MUST use `await` keyword
8. **MUST verify before installing** - Check if SDK/CLI already installed before prompting installation

**Before proceeding with any operation, verify these rules are followed. See detailed sections below for implementation.**

## Installation & Setup

> **⚠️ IMPORTANT**: See [PINECONE.md](./PINECONE.md#-mandatory-always-use-latest-version) for the mandatory requirement to always use the latest version when creating projects.

### Package Installation

```bash
npm install @pinecone-database/pinecone
# or
yarn add @pinecone-database/pinecone
```

### Finding the Latest Version

**Check latest version on npm:**

- Browse: [https://www.npmjs.com/package/@pinecone-database/pinecone](https://www.npmjs.com/package/@pinecone-database/pinecone)
- Or check via CLI: `npm view @pinecone-database/pinecone version`

**Install latest version:**

```bash
npm install @pinecone-database/pinecone@latest
# or
yarn add @pinecone-database/pinecone@latest
```

**Install specific version:**

```bash
npm install @pinecone-database/pinecone@6.1.2
# or
yarn add @pinecone-database/pinecone@6.1.2
```

**Check installed version:**

```bash
npm list @pinecone-database/pinecone
# or
yarn list --pattern @pinecone-database/pinecone
```

**Update to latest:**

```bash
npm update @pinecone-database/pinecone
# or
yarn upgrade @pinecone-database/pinecone
```

### TypeScript Imports

```typescript
import { Pinecone } from "@pinecone-database/pinecone";
```

### Environment Configuration

**⚠️ Use `.env` files (see [PINECONE.md](./PINECONE.md#-environment-variables--security-best-practices)).**

```bash
npm install dotenv
```

```typescript
import { config } from "dotenv";
import { Pinecone } from "@pinecone-database/pinecone";

config(); // Loads .env file
const apiKey = process.env.PINECONE_API_KEY;
if (!apiKey) throw new Error("PINECONE_API_KEY required");
const pc = new Pinecone({ apiKey });
```

**Note**: Next.js auto-loads `.env` files - no `config()` needed.

### Production Client Class

```typescript
import { config } from "dotenv";
import { Pinecone } from "@pinecone-database/pinecone";

config();

class PineconeClient {
  private pc: Pinecone;
  private indexName: string;
  constructor() {
    const apiKey = process.env.PINECONE_API_KEY;
    if (!apiKey) throw new Error("PINECONE_API_KEY required");
    this.pc = new Pinecone({ apiKey });
    this.indexName = process.env.PINECONE_INDEX || "default-index";
  }
  getIndex() {
    return this.pc.index(this.indexName);
  }
}
```

## 🛡️ TypeScript Types & Type Safety

When working with the Pinecone SDK, proper type handling prevents runtime errors:

### Search Result Field Typing

Search results return `hit.fields` as a generic object. Always cast to a typed structure:

```typescript
// ❌ WRONG - TypeScript error: Property 'content' does not exist on type 'object'
for (const hit of results.result.hits) {
  console.log(hit.fields.content); // Compile error!
}

// ✅ CORRECT - Use type casting with Record<string, any>
for (const hit of results.result.hits) {
  const fields = hit.fields as Record<string, any>;
  const content = String(fields?.content ?? "");
  const category = String(fields?.category ?? "unknown");
}

// ✅ BETTER - Define an interface for your records
interface Document {
  content: string;
  category: string;
}

for (const hit of results.result.hits) {
  const doc = hit.fields as unknown as Document;
  console.log(doc.content, doc.category);
}
```

### Complete Search Hit Interface

```typescript
interface SearchHit {
  _id: string; // Record ID
  _score: number; // Relevance score (0-1 range)
  fields: Record<string, any>; // Your custom fields
  metadata?: Record<string, any>; // Optional metadata
}

interface SearchResults {
  result: {
    hits: SearchHit[];
    matches?: number;
  };
}
```

### Best Practices for Type Safety

1. **Always cast `hit.fields`**: Use `as Record<string, any>` or define a proper interface
2. **Use optional chaining**: `fields?.fieldName ?? defaultValue`
3. **Convert to strings**: `String(value)` when building output
4. **Define record interfaces**: Match your actual record structure for IDE autocomplete

## Quickstarts

### Quick Test

Complete setup prerequisites first, then:

1. **Create index with CLI:**

```bash
pc index create -n agentic-quickstart-test -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

2. **Upsert sample data:**

> **Sample Data**: Use the sample data from [PINECONE-quickstart.md](./PINECONE-quickstart.md#sample-data-use-in-all-languages). Convert JSON format to TypeScript objects.

```typescript
import { config } from "dotenv";
import { Pinecone } from "@pinecone-database/pinecone";

// Load environment variables from .env file
config();

// Initialize Pinecone client
const apiKey = process.env.PINECONE_API_KEY;
if (!apiKey) {
  throw new Error("PINECONE_API_KEY environment variable not set");
}

const pc = new Pinecone({ apiKey });

// Sample data (see quickstart guide for full list)
const records = [
  {
    _id: "rec1",
    content:
      "The Eiffel Tower was completed in 1889 and stands in Paris, France.",
    category: "history",
  },
  {
    _id: "rec2",
    content: "Photosynthesis allows plants to convert sunlight into energy.",
    category: "science",
  },
  // ... (use all 12 records from quickstart guide)
];

// Target the index
const denseIndex = pc.index("agentic-quickstart-test");

// Upsert the records into a namespace
await denseIndex.namespace("example-namespace").upsertRecords(records);
```

3. **Search with reranking:**

```typescript
// Wait for the upserted vectors to be indexed
await new Promise((resolve) => setTimeout(resolve, 10000));

// View stats for the index
const stats = await denseIndex.describeIndexStats();
console.log(stats);

// Define the query (see quickstart guide for test query)
const query = "Famous historical structures and monuments";

// Search the dense index and rerank results
const rerankedResults = await denseIndex
  .namespace("example-namespace")
  .searchRecords({
    query: {
      topK: 10,
      inputs: {
        text: query,
      },
    },
    rerank: {
      model: "bge-reranker-v2-m3",
      topN: 10,
      rankFields: ["content"],
    },
  });

// Print the reranked results with proper type casting
for (const hit of rerankedResults.result.hits) {
  const fields = hit.fields as Record<string, any>;
  const category = String(fields?.category ?? "unknown");
  const content = String(fields?.content ?? "");
  console.log(
    `id: ${hit._id.padEnd(5)} | score: ${hit._score
      .toFixed(2)
      .padEnd(5)} | category: ${category.padEnd(
      10
    )} | text: ${content.substring(0, 50)}`
  );
}
```

## Data Operations

### Upserting Records

```typescript
// Indexes with integrated embeddings - use text records directly
const records = [
  {
    _id: "doc1",
    content: "Your text content here", // must match field_map
    category: "documentation",
    created_at: "2025-01-01",
    priority: "high",
  },
];

// Always use namespaces
const namespace = "user_123"; // e.g., "knowledge_base", "session_456"
await index.namespace(namespace).upsertRecords(records);
```

### Updating Records

```typescript
// Update existing records (use same upsertRecords operation with existing IDs)
const updatedRecords = [
  {
    _id: "doc1", // existing record ID
    content: "Updated content here",
    category: "updated_docs", // can change fields
    last_modified: "2025-01-15",
  },
];

// Partial updates - only changed fields need to be included
const partialUpdate = [
  {
    _id: "doc1",
    category: "urgent", // only updating category field
    priority: "high", // adding new field
  },
];

await index.namespace(namespace).upsertRecords(updatedRecords);
```

### Fetching Records

```typescript
// Fetch single record
const result = await index.namespace(namespace).fetch(["doc1"]);
if (result.records && result.records["doc1"]) {
  const record = result.records["doc1"];
  const fields = record.fields as Record<string, any>;
  console.log(`Content: ${fields?.content}`);
  console.log(`Category: ${fields?.category}`);
}

// Fetch multiple records
const multiResult = await index
  .namespace(namespace)
  .fetch(["doc1", "doc2", "doc3"]);
for (const [recordId, record] of Object.entries(multiResult.records || {})) {
  const fields = record.fields as Record<string, any>;
  console.log(`ID: ${recordId}, Content: ${fields?.content}`);
}

// Fetch with error handling
async function safeFetch(
  index: any,
  namespace: string,
  ids: string[]
): Promise<Record<string, any>> {
  try {
    const result = await index.namespace(namespace).fetch(ids);
    return result.records || {};
  } catch (error) {
    console.error("Fetch failed:", error);
    return {};
  }
}
```

### Listing Record IDs

```typescript
// List all record IDs (paginated)
async function listAllIds(
  index: any,
  namespace: string,
  prefix?: string
): Promise<string[]> {
  const allIds: string[] = [];
  let paginationToken: string | undefined;

  while (true) {
    const result = await index.namespace(namespace).listPaginated({
      prefix: prefix, // filter by ID prefix
      limit: 1000,
      paginationToken: paginationToken,
    });

    allIds.push(...result.vectors.map((record: any) => record.id));

    if (!result.pagination || !result.pagination.next) {
      break;
    }
    paginationToken = result.pagination.next;
  }

  return allIds;
}

// Usage
const allRecordIds = await listAllIds(index, "user_123");
const docsOnly = await listAllIds(index, "user_123", "doc_");
```

## Search Operations

### Semantic Search with Reranking (Best Practice)

**Note**: Reranking is a best practice for production quality results. Quickstarts include reranking to demonstrate usage.

```typescript
async function searchWithRerank(
  index: any,
  namespace: string,
  queryText: string,
  topK: number = 5
) {
  // Best practice: Use reranking for production quality results. This pattern is shown in quickstarts.
  const results = await index.namespace(namespace).searchRecords({
    query: {
      topK: topK * 2, // more candidates for reranking
      inputs: {
        text: queryText, // must match index config
      },
    },
    rerank: {
      model: "bge-reranker-v2-m3",
      topN: topK,
      rankFields: ["content"],
    },
  });
  return results;
}
```

### Lexical Search

```typescript
// Basic lexical search
async function lexicalSearch(
  index: any,
  namespace: string,
  queryText: string,
  topK: number = 5
) {
  // Keyword-based search using sparse embeddings
  const results = await index.namespace(namespace).searchRecords({
    query: {
      inputs: { text: queryText },
      topK: topK,
    },
  });
  return results;
}

// Lexical search with required terms
async function lexicalSearchWithRequiredTerms(
  index: any,
  namespace: string,
  queryText: string,
  requiredTerms: string[],
  topK: number = 5
) {
  // Results must contain specific required words
  const results = await index.namespace(namespace).searchRecords({
    query: {
      inputs: { text: queryText },
      topK: topK,
      matchTerms: requiredTerms, // results must contain these terms
    },
  });
  return results;
}

// Lexical search with reranking
async function lexicalSearchWithRerank(
  index: any,
  namespace: string,
  queryText: string,
  topK: number = 5
) {
  // Lexical search with reranking for better relevance
  const results = await index.namespace(namespace).searchRecords({
    query: {
      inputs: { text: queryText },
      topK: topK * 2, // get more candidates for reranking
    },
    rerank: {
      model: "bge-reranker-v2-m3",
      topN: topK,
      rankFields: ["content"],
    },
  });
  return results;
}
```

### Metadata Filtering

```typescript
// Simple filters
const filterCriteria = { category: "documentation" };

// Complex filters
const complexFilter = {
  $and: [
    { category: { $in: ["docs", "tutorial"] } },
    { priority: { $ne: "low" } },
    { created_at: { $gte: "2025-01-01" } },
  ],
};

const results = await index.namespace(namespace).searchRecords({
  query: {
    topK: 10,
    inputs: { text: queryText },
    filter: filterCriteria, // Filter goes inside query object
  },
});

// Search without filters - omit the filter key entirely
const simpleResults = await index.namespace(namespace).searchRecords({
  query: {
    topK: 10,
    inputs: { text: queryText },
    // No filter key at all
  },
});

// Dynamic filter pattern - conditionally add filter to query object
const queryOptions: any = {
  topK: 10,
  inputs: { text: queryText },
};
if (hasFilters) {
  // Only add filter if it exists
  queryOptions.filter = { category: { $eq: "docs" } };
}

const dynamicResults = await index.namespace(namespace).searchRecords({
  query: queryOptions,
});

// Filter operators: $eq, $ne, $gt, $gte, $lt, $lte, $in, $nin, $exists, $and, $or
```

## Error Handling (Production)

### Retry Pattern

```typescript
async function exponentialBackoffRetry<T>(
  func: () => Promise<T>,
  maxRetries: number = 5
): Promise<T> {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await func();
    } catch (error: any) {
      const statusCode = error.status || error.statusCode;

      // Only retry transient errors
      if (statusCode && (statusCode >= 500 || statusCode === 429)) {
        if (attempt < maxRetries - 1) {
          const delay = Math.min(2 ** attempt, 60) * 1000; // Exponential backoff, cap at 60s
          await new Promise((resolve) => setTimeout(resolve, delay));
        } else {
          throw error;
        }
      } else {
        throw error; // Don't retry client errors (4xx except 429)
      }
    }
  }
  throw new Error("Max retries exceeded");
}

// Usage
await exponentialBackoffRetry(() =>
  index.namespace(namespace).upsertRecords(records)
);
```

## Batch Processing

```typescript
async function batchUpsert(
  index: any,
  namespace: string,
  records: any[],
  batchSize: number = 96
) {
  for (let i = 0; i < records.length; i += batchSize) {
    const batch = records.slice(i, i + batchSize);
    await exponentialBackoffRetry(() =>
      index.namespace(namespace).upsertRecords(batch)
    );
    await new Promise((resolve) => setTimeout(resolve, 100)); // Rate limiting
  }
}
```

## Common Operations

### Index Management

```typescript
// Check if index exists (in application startup)
const indexExists = await pc
  .listIndexes()
  .then((indexes) => indexes.indexes?.some((idx) => idx.name === "my-index"));

if (indexExists) {
  const index = pc.index("my-index");
}

// Get stats (for monitoring/metrics)
const stats = await index.describeIndexStats();
console.log(`Total records: ${stats.totalRecordCount}`);
console.log(`Namespaces: ${Object.keys(stats.namespaces || {})}`);
```

### Data Operations

```typescript
// Fetch records
const result = await index.namespace("ns").fetch(["doc1", "doc2"]);
for (const [recordId, record] of Object.entries(result.records || {})) {
  const fields = record.fields as Record<string, any>;
  console.log(`${recordId}: ${fields?.content}`);
}

// List all IDs (paginated)
const allIds: string[] = [];
let paginationToken: string | undefined;

while (true) {
  const result = await index.namespace("ns").listPaginated({
    limit: 1000,
    paginationToken,
  });

  allIds.push(...result.vectors.map((record: any) => record.id));

  if (!result.pagination || !result.pagination.next) {
    break;
  }
  paginationToken = result.pagination.next;
}

// Delete records
await index.namespace("ns").deleteMany(["doc1", "doc2"]);

// Delete entire namespace
await index.namespace("ns").deleteAll();
```

## TypeScript-Specific Patterns

### Namespace Strategy

```typescript
// Multi-user apps
const namespace = `user_${userId}`;

// Session-based
const namespace = `session_${sessionId}`;

// Content-based
const namespace = "knowledge_base";
const namespace = "chat_history";
```

### Type Definitions

```typescript
// Record structure for integrated embeddings
interface PineconeRecord {
  _id: string;
  [key: string]: any; // Your custom fields (e.g., content, category)
}

// Search result structure
interface SearchHit {
  _id: string; // Record ID
  _score: number; // Relevance score (0-1 range)
  fields: Record<string, any>; // Your custom fields
  metadata?: Record<string, any>; // Optional metadata
}

interface SearchResults {
  result: {
    hits: SearchHit[];
    matches?: number;
  };
}

// Filter criteria
interface FilterCriteria {
  [key: string]: any;
  $and?: FilterCriteria[];
  $or?: FilterCriteria[];
  $in?: { [key: string]: any[] };
  $nin?: { [key: string]: any[] };
  $eq?: any;
  $ne?: any;
  $gt?: any;
  $gte?: any;
  $lt?: any;
  $lte?: any;
  $exists?: boolean;
}
```

### Environment Configuration

```typescript
import { Pinecone } from "@pinecone-database/pinecone";

class PineconeClient {
  private pc: Pinecone;
  private indexName: string;

  constructor() {
    const apiKey = process.env.PINECONE_API_KEY;
    if (!apiKey) {
      throw new Error("PINECONE_API_KEY required");
    }
    this.pc = new Pinecone({ apiKey });
    this.indexName = process.env.PINECONE_INDEX || "default-index";
  }

  getIndex() {
    return this.pc.index(this.indexName);
  }
}
```

## Use Case Examples

### Semantic Search System

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-search -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```typescript
import { config } from "dotenv";
import { Pinecone } from "@pinecone-database/pinecone";

// Load environment variables from .env file
config();

// Initialize Pinecone client
const pc = new Pinecone({ apiKey: process.env.PINECONE_API_KEY });
const index = pc.index("agentic-quickstart-search");

function buildSemanticSearchSystem() {
  // Build a semantic search system with reranking and filtering
  return async function searchKnowledgeBase(
    query: string,
    categoryFilter?: string,
    topK: number = 5
  ) {
    const queryOptions: any = {
      topK: topK * 2,
      inputs: { text: query },
    };

    if (categoryFilter) {
      queryOptions.filter = { category: { $eq: categoryFilter } };
    }

    const results = await index.namespace("knowledge_base").searchRecords({
      query: queryOptions,
      rerank: {
        model: "bge-reranker-v2-m3",
        topN: topK,
        rankFields: ["content"],
      },
    });

    return results;
  };
}
```

### Multi-Tenant RAG System

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-rag -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```typescript
import { config } from "dotenv";
import { Pinecone } from "@pinecone-database/pinecone";

// Load environment variables from .env file
config();

// Initialize Pinecone client
const pc = new Pinecone({ apiKey: process.env.PINECONE_API_KEY });
const index = pc.index("agentic-quickstart-rag");

function buildRagSystem() {
  // Build a multi-tenant RAG system with namespace isolation
  return async function ragQuery(
    userId: string,
    query: string,
    topK: number = 5
  ) {
    // Ensure namespace isolation
    const namespace = `user_${userId}`;

    // Search only user's namespace with reranking
    const results = await index.namespace(namespace).searchRecords({
      query: {
        topK: topK * 2,
        inputs: { text: query },
      },
      rerank: {
        model: "bge-reranker-v2-m3",
        topN: topK,
        rankFields: ["content"],
      },
    });

    // Construct context for LLM with proper type casting
    const context = results.result.hits
      .map((hit) => {
        const fields = hit.fields as Record<string, any>;
        return `Document ${hit._id}: ${String(fields?.content ?? "")}`;
      })
      .join("\n");

    return context;
  };
}
```

### Recommendation Engine

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-recommendations -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```typescript
import { config } from "dotenv";
import { Pinecone } from "@pinecone-database/pinecone";

// Load environment variables from .env file
config();

// Initialize Pinecone client
const pc = new Pinecone({ apiKey: process.env.PINECONE_API_KEY });
const index = pc.index("agentic-quickstart-recommendations");

function buildRecommendationEngine() {
  // Build a recommendation engine with filtering and diversity
  return async function getRecommendations(
    productId: string,
    categoryFilter?: string,
    topK: number = 10
  ) {
    const queryOptions: any = {
      topK: topK * 2,
      inputs: { text: `product description for ${productId}` },
    };

    // Apply category filtering if specified
    if (categoryFilter) {
      queryOptions.filter = { category: { $eq: categoryFilter } };
    }

    // Get similar products with reranking
    const results = await index.namespace("products").searchRecords({
      query: queryOptions,
      rerank: {
        model: "bge-reranker-v2-m3",
        topN: topK,
        rankFields: ["content"],
      },
    });

    return results.result.hits;
  };
}
```

## 🚨 Common Mistakes (Must Avoid)

> **For universal common mistakes**, see [PINECONE.md](./PINECONE.md#-common-mistakes-must-avoid). Below are TypeScript-specific examples.

### 1. **Nested Metadata** (will cause API errors)

```typescript
// ❌ WRONG - nested objects not allowed
const badRecord = {
  _id: "doc1",
  user: { name: "John", id: 123 }, // Nested
  tags: [{ type: "urgent" }], // Nested in list
};

// ✅ CORRECT - flat structure only
const goodRecord = {
  _id: "doc1",
  user_name: "John",
  user_id: 123,
  tags: ["urgent", "important"], // String lists OK
};
```

### 2. **Batch Size Limits** (will cause API errors)

```typescript
// Text records: MAX 96 per batch, 2MB total
// Vector records: MAX 1000 per batch, 2MB total

// ✅ CORRECT - respect limits
for (let i = 0; i < records.length; i += 96) {
  const batch = records.slice(i, i + 96);
  await index.namespace(namespace).upsertRecords(batch);
}
```

### 3. **Missing Namespaces** (causes data isolation issues)

```typescript
// ❌ WRONG - no namespace
await index.upsertRecords(records); // Old API pattern

// ✅ CORRECT - always use namespaces
await index.namespace("user_123").upsertRecords(records);
await index.namespace("user_123").searchRecords(params);
await index.namespace("user_123").deleteMany(["doc1"]);
```

### 4. **Skipping Reranking** (reduces search quality)

```typescript
// ⚠️ OK but not optimal
const results = await index.namespace("ns").searchRecords({
  query: { topK: 5, inputs: { text: "query" } },
});

// ✅ BETTER - use reranking for best results (best practice)
const rerankedResults = await index.namespace("ns").searchRecords({
  query: {
    topK: 10,
    inputs: { text: "query" },
  },
  rerank: {
    model: "bge-reranker-v2-m3",
    topN: 5,
    rankFields: ["content"],
  },
});
```

### 5. **Hardcoded API Keys**

```typescript
// ❌ WRONG
const pc = new Pinecone({ apiKey: "pc-abc123..." });

// ✅ CORRECT
const pc = new Pinecone({ apiKey: process.env.PINECONE_API_KEY });
```

### 6. **Missing Async/Await** (TypeScript-specific)

```typescript
// ❌ WRONG - forgetting await
const results = index.namespace("ns").searchRecords({
  query: { topK: 5, inputs: { text: "query" } },
});
console.log(results); // Will log a Promise, not results

// ✅ CORRECT - use await
const results = await index.namespace("ns").searchRecords({
  query: { topK: 5, inputs: { text: "query" } },
});
console.log(results);
```

## ⏳ Indexing Delays & Eventual Consistency

> **⚠️ IMPORTANT FOR AGENTS**: The wait instructions below apply **ONLY to generated code**, not to the agent's own behavior. Include wait logic in the code you generate (e.g., `await new Promise(r => setTimeout(r, 10000))`). **DO NOT** execute `sleep` commands in the terminal yourself.

> **For complete information on eventual consistency**, see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md#indexing-delays--eventual-consistency).

**Key Points:**

- Records become searchable 5-10 seconds after upsert
- Stats update 10-20 seconds after upsert
- Always wait 10+ seconds before searching after upserting (include wait logic in generated code)

**Production Pattern (TypeScript):**

```typescript
async function waitForRecords(
  index: any,
  namespace: string,
  expectedCount: number,
  maxWaitSeconds = 300
): Promise<void> {
  const startTime = Date.now();

  while (Date.now() - startTime < maxWaitSeconds * 1000) {
    const stats = await index.describeIndexStats();
    const count = stats.namespaces?.[namespace]?.recordCount ?? 0;

    if (count >= expectedCount) {
      console.log(`✓ All ${count} records indexed`);
      return;
    }

    console.log(`⏳ Indexed ${count}/${expectedCount} records, waiting...`);
    await new Promise((r) => setTimeout(r, 5000)); // Check every 5 seconds
  }

  throw new Error(
    `Timeout: Records not fully indexed after ${maxWaitSeconds}s`
  );
}

// Usage
await index.namespace("ns").upsertRecords(records);
await waitForRecords(index, "ns", records.length);
```

## 🆘 Troubleshooting

> **For general troubleshooting issues** (search returns no results, rate limits, metadata errors, etc.), see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md).

### Problem: TypeScript errors accessing `hit.fields`

**Cause**: SDK returns generic object, TypeScript doesn't know your field names

**Solution**: Use type casting

```typescript
// ❌ WRONG - TypeScript error: Property 'content' does not exist on type 'object'
for (const hit of results.result.hits) {
  console.log(hit.fields.content); // Compile error!
}

// ✅ CORRECT - Use type casting
for (const hit of results.result.hits) {
  const fields = hit.fields as Record<string, any>;
  const content = String(fields?.content ?? "");
  console.log(content);
}
```

**Best practice**: Define an interface for your records:

```typescript
interface Document {
  content: string;
  category: string;
}

for (const hit of results.result.hits) {
  const doc = hit.fields as unknown as Document;
  console.log(doc.content, doc.category);
}
```

### Problem: `Cannot find module '@pinecone-database/pinecone'`

**Cause**: Wrong package name or not installed

**Solution**:

```bash
# ✅ CORRECT
npm install @pinecone-database/pinecone

# ❌ WRONG - deprecated package
npm install pinecone-client

# If already installed, verify:
npm list @pinecone-database/pinecone
```

### Problem: TypeScript compilation errors with Promise types

**Cause**: Missing `await` keyword or incorrect async/await usage

**Solution**:

```typescript
// ❌ WRONG - forgetting await
const results = index.namespace("ns").searchRecords({
  query: { topK: 5, inputs: { text: "query" } },
});
console.log(results); // Will log a Promise, not results

// ✅ CORRECT - use await
const results = await index.namespace("ns").searchRecords({
  query: { topK: 5, inputs: { text: "query" } },
});
console.log(results);

// ✅ CORRECT - async function
async function searchData() {
  const results = await index.namespace("ns").searchRecords({
    query: { topK: 5, inputs: { text: "query" } },
  });
  return results;
}
```

### Problem: TypeScript errors with index namespace operations

**Cause**: Type inference issues or incorrect index reference

**Solution**:

```typescript
// ✅ CORRECT - properly typed index
const index = pc.index("my-index"); // Type is inferred
await index.namespace("ns").upsertRecords(records);

// ✅ CORRECT - explicit typing if needed
import { Index } from "@pinecone-database/pinecone";
const index: Index = pc.index("my-index");
```

For other troubleshooting issues, see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md).

## Node.js Ecosystem Integration

### Express.js Integration

```typescript
import express from "express";
import { Pinecone } from "@pinecone-database/pinecone";

const app = express();
const pc = new Pinecone({ apiKey: process.env.PINECONE_API_KEY! });

app.get("/search", async (req, res) => {
  try {
    const { query, namespace } = req.query;
    const index = pc.index("my-index");

    const results = await index.namespace(namespace as string).searchRecords({
      query: {
        topK: 10,
        inputs: { text: query as string },
      },
    });

    res.json(results);
  } catch (error) {
    res.status(500).json({ error: "Search failed" });
  }
});
```

### Next.js Integration

```typescript
// pages/api/search.ts
import { Pinecone } from "@pinecone-database/pinecone";

export default async function handler(req: any, res: any) {
  const pc = new Pinecone({ apiKey: process.env.PINECONE_API_KEY! });

  try {
    const { query, namespace } = req.body;
    const index = pc.index("my-index");

    const results = await index.namespace(namespace).searchRecords({
      query: {
        topK: 10,
        inputs: { text: query },
      },
    });

    res.status(200).json(results);
  } catch (error) {
    res.status(500).json({ error: "Search failed" });
  }
}
```
