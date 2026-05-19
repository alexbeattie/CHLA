# Pinecone Go SDK Guide

> **Prerequisites**: See [PINECONE.md](./PINECONE.md) for universal concepts and setup.

This guide provides Go-specific patterns, examples, and best practices for the Pinecone SDK.

## 🚨 MANDATORY RULES - Read First

**⚠️ CRITICAL: These rules MUST be followed. Violations will cause runtime errors or data issues.**

1. **MUST use namespaces** - Every operation MUST use `.WithNamespace()` method
2. **MUST wait 10+ seconds** - After upserting records, MUST wait 10+ seconds before searching
3. **MUST match field_map** - Record field names MUST match the right side of `--field_map` used when creating index
4. **MUST respect batch limits** - Text records: MAX 96 per batch, Vector records: MAX 1000 per batch
5. **MUST use flat metadata** - No nested objects allowed, only flat key-value pairs
6. **MUST use safe type assertions** - Use `ok` check: `val, ok := field.(string)`
7. **MUST pass context** - All operations require `context.Context` parameter
8. **MUST verify before installing** - Check if SDK/CLI already installed before prompting installation

**Before proceeding with any operation, verify these rules are followed. See detailed sections below for implementation.**

## Installation & Setup

> **⚠️ IMPORTANT**: See [PINECONE.md](./PINECONE.md#-mandatory-always-use-latest-version) for the mandatory requirement to always use the latest version when creating projects.

### Go Module Installation

```bash
go get github.com/pinecone-io/go-pinecone/pinecone
```

### Finding the Latest Version

**Check latest version on GitHub:**

- Releases: [https://github.com/pinecone-io/go-pinecone/releases](https://github.com/pinecone-io/go-pinecone/releases)
- Or via Go: `go list -m -versions github.com/pinecone-io/go-pinecone/pinecone`

**Install latest version:**

```bash
go get github.com/pinecone-io/go-pinecone/pinecone@latest
```

**Install specific version:**

```bash
go get github.com/pinecone-io/go-pinecone/pinecone@v4.1.4  # Replace with desired version
```

**Check installed version:**

```bash
go list -m github.com/pinecone-io/go-pinecone/pinecone
```

**Update dependencies:**

```bash
go get -u github.com/pinecone-io/go-pinecone/pinecone
go mod tidy
```

### Go Imports

```go
import (
    "context"
    "fmt"
    "os"
    "time"

    "github.com/pinecone-io/go-pinecone/pinecone"
)
```

### Environment Configuration

**⚠️ Use `.env` files (see [PINECONE.md](./PINECONE.md#-environment-variables--security-best-practices)).**

```bash
go get github.com/joho/godotenv
```

```go
import (
    "fmt"
    "log"
    "os"
    "github.com/joho/godotenv"
    "github.com/pinecone-io/go-pinecone/pinecone"
)

godotenv.Load()  // Loads .env file
apiKey := os.Getenv("PINECONE_API_KEY")
if apiKey == "" {
    return nil, fmt.Errorf("PINECONE_API_KEY required")
}
client, err := pinecone.NewClient(pinecone.NewClientParams{ApiKey: apiKey})
```

### Production Client Struct

```go
import (
    "context"
    "fmt"
    "log"
    "os"
    "github.com/joho/godotenv"
    "github.com/pinecone-io/go-pinecone/pinecone"
)

type PineconeService struct {
    client    *pinecone.Client
    indexName string
}

func NewPineconeService() (*PineconeService, error) {
    godotenv.Load()
    apiKey := os.Getenv("PINECONE_API_KEY")
    if apiKey == "" {
        return nil, fmt.Errorf("PINECONE_API_KEY required")
    }
    client, err := pinecone.NewClient(pinecone.NewClientParams{ApiKey: apiKey})
    if err != nil {
        return nil, fmt.Errorf("failed to create client: %w", err)
    }
    indexName := os.Getenv("PINECONE_INDEX")
    if indexName == "" {
        indexName = "default-index"
    }
    return &PineconeService{client: client, indexName: indexName}, nil
}

func (ps *PineconeService) GetIndexConnection() (*pinecone.IndexConnection, error) {
    ctx := context.Background()
    indexes, err := ps.client.ListIndexes(ctx)
    if err != nil {
        return nil, fmt.Errorf("failed to list indexes: %w", err)
    }

    var idx *pinecone.Index
    for _, i := range indexes {
        if i.Name == ps.indexName {
            idx = i
            break
        }
    }

    if idx == nil {
        return nil, fmt.Errorf("index %s not found", ps.indexName)
    }

    return ps.client.Index(pinecone.NewIndexConnParams{
        Host: idx.Host,
    }), nil
}
```

## Quickstarts

### Quick Test

Complete setup prerequisites first, then:

1. **Create index with CLI:**

```bash
pc index create -n agentic-quickstart-test -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

2. **Upsert sample data:**

> **Sample Data**: Use the sample data from [PINECONE-quickstart.md](./PINECONE-quickstart.md#sample-data-use-in-all-languages). Convert JSON format to Go structs.

```go
package main

import (
    "context"
    "fmt"
    "os"

    "github.com/pinecone-io/go-pinecone/pinecone"
)

func main() {
    // Initialize Pinecone client
    apiKey := os.Getenv("PINECONE_API_KEY")
    if apiKey == "" {
        panic("PINECONE_API_KEY environment variable not set")
    }

    client, err := pinecone.NewClient(pinecone.NewClientParams{
        ApiKey: apiKey,
    })
    if err != nil {
        panic(fmt.Sprintf("Failed to create client: %v", err))
    }

    // Get index connection
    ctx := context.Background()
    indexes, err := client.ListIndexes(ctx)
    if err != nil {
        panic(fmt.Sprintf("Failed to list indexes: %v", err))
    }

    var idx *pinecone.Index
    for _, i := range indexes {
        if i.Name == "agentic-quickstart-test" {
            idx = i
            break
        }
    }

    if idx == nil {
        panic("Index agentic-quickstart-test not found")
    }

    indexConn := client.Index(pinecone.NewIndexConnParams{
        Host: idx.Host,
    })

    // Sample records (see quickstart guide for full list of 12 records)
    records := []*pinecone.IntegratedRecord{
        {
            Id:       pinecone.StringPtr("rec1"),
            Content:  pinecone.StringPtr("The Eiffel Tower was completed in 1889 and stands in Paris, France."),
            Category: pinecone.StringPtr("history"),
        },
        {
            Id:       pinecone.StringPtr("rec2"),
            Content:  pinecone.StringPtr("Photosynthesis allows plants to convert sunlight into energy."),
            Category: pinecone.StringPtr("science"),
        },
        // ... (use all 12 records from quickstart guide)
    }

    // Upsert the records into a namespace (embeddings generated automatically)
    err = indexConn.WithNamespace("example-namespace").UpsertRecords(ctx, records)
    if err != nil {
        panic(fmt.Sprintf("Failed to upsert: %v", err))
    }
}
```

3. **Search with reranking:**

```go
import (
    "context"
    "fmt"
    "time"

    "github.com/pinecone-io/go-pinecone/pinecone"
)

func searchExample(indexConn *pinecone.IndexConnection) {
    // Wait for the upserted records to be indexed (eventual consistency)
    time.Sleep(10 * time.Second)

    // Define the query text (see quickstart guide for test query)
    queryText := "Famous historical structures and monuments"

    // Search using text query with integrated inference and reranking
    ctx := context.Background()
    results, err := indexConn.WithNamespace("example-namespace").SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: &pinecone.SearchRecordsRequestQuery{
            TopK: pinecone.Int32Ptr(10),
            Inputs: map[string]interface{}{
                "text": queryText,
            },
        },
        Rerank: &pinecone.SearchRecordsRequestRerank{
            Model:     pinecone.StringPtr("bge-reranker-v2-m3"),
            TopN:      pinecone.Int32Ptr(10),
            RankFields: []string{"content"},
        },
    })

    if err != nil {
        panic(fmt.Sprintf("Failed to search: %v", err))
    }

    // Print the reranked results
    if results.Result != nil {
        for _, hit := range results.Result.Hits {
            id := ""
            if hit.Id != nil {
                id = *hit.Id
            }
            score := 0.0
            if hit.Score != nil {
                score = *hit.Score
            }

            category := "unknown"
            content := ""
            if hit.Fields != nil {
                if cat, ok := hit.Fields["category"].(string); ok {
                    category = cat
                }
                if cont, ok := hit.Fields["content"].(string); ok {
                    content = cont
                }
            }

            fmt.Printf("id: %s | score: %.2f | category: %s | text: %s\n",
                id, score, category, content)
        }
    }
}
```

## Data Operations

### Upserting Records

```go
func upsertRecords(indexConn *pinecone.IndexConnection, namespace string) error {
    // Indexes with integrated embeddings - use text records directly
    records := []*pinecone.IntegratedRecord{
        {
            Id:       pinecone.StringPtr("doc1"),
            Content:  pinecone.StringPtr("Your text content here"), // must match field_map
            Category: pinecone.StringPtr("documentation"),
            CreatedAt: pinecone.StringPtr("2025-01-01"),
            Priority: pinecone.StringPtr("high"),
        },
    }

    // Always use namespaces
    // namespace := "user_123" // e.g., "knowledge_base", "session_456"

    ctx := context.Background()
    err := indexConn.WithNamespace(namespace).UpsertRecords(ctx, records)
    return err
}
```

### Updating Records

```go
func updateRecords(indexConn *pinecone.IndexConnection, namespace string) error {
    // Update existing records (use same UpsertRecords operation with existing IDs)
    updatedRecords := []*pinecone.IntegratedRecord{
        {
            Id:       pinecone.StringPtr("doc1"), // existing record ID
            Content:  pinecone.StringPtr("Updated content here"),
            Category: pinecone.StringPtr("updated_docs"), // can change fields
            LastModified: pinecone.StringPtr("2025-01-15"),
        },
    }

    // Partial updates - only changed fields need to be included
    partialUpdate := []*pinecone.IntegratedRecord{
        {
            Id:       pinecone.StringPtr("doc1"),
            Category: pinecone.StringPtr("urgent"), // only updating category field
            Priority: pinecone.StringPtr("high"),  // adding new field
        },
    }

    ctx := context.Background()
    err := indexConn.WithNamespace(namespace).UpsertRecords(ctx, updatedRecords)
    return err
}
```

### Fetching Records

```go
func fetchRecords(indexConn *pinecone.IndexConnection, namespace string, ids []string) error {
    ctx := context.Background()

    // Fetch records
    result, err := indexConn.WithNamespace(namespace).Fetch(ctx, ids)
    if err != nil {
        return err
    }

    if result.Records != nil {
        for recordId, record := range result.Records {
            if record != nil && record.Fields != nil {
                content := ""
                if cont, ok := record.Fields["content"].(string); ok {
                    content = cont
                }
                fmt.Printf("ID: %s, Content: %s\n", recordId, content)
            }
        }
    }

    return nil
}

// Fetch with error handling
func safeFetch(indexConn *pinecone.IndexConnection, namespace string, ids []string) (map[string]*pinecone.Record, error) {
    ctx := context.Background()
    result, err := indexConn.WithNamespace(namespace).Fetch(ctx, ids)

    if err != nil {
        fmt.Printf("Fetch failed: %v\n", err)
        return nil, err
    }

    if result.Records != nil {
        return result.Records, nil
    }
    return make(map[string]*pinecone.Record), nil
}
```

### Listing Record IDs

```go
func listAllIds(indexConn *pinecone.IndexConnection, namespace string, prefix string) ([]string, error) {
    var allIds []string
    var paginationToken *string

    for {
        params := &pinecone.ListPaginatedRequest{
            Limit: pinecone.Int32Ptr(1000),
        }

        if prefix != "" {
            params.Prefix = pinecone.StringPtr(prefix)
        }

        if paginationToken != nil {
            params.PaginationToken = paginationToken
        }

        ctx := context.Background()
        result, err := indexConn.WithNamespace(namespace).ListPaginated(ctx, params)
        if err != nil {
            return nil, err
        }

        if result.Vectors != nil {
            for _, vector := range result.Vectors {
                if vector.Id != nil {
                    allIds = append(allIds, *vector.Id)
                }
            }
        }

        if result.Pagination == nil || result.Pagination.Next == nil {
            break
        }
        paginationToken = result.Pagination.Next
    }

    return allIds, nil
}

// Usage
allRecordIds, err := listAllIds(indexConn, "user_123", "")
if err != nil {
    panic(err)
}

docsOnly, err := listAllIds(indexConn, "user_123", "doc_")
if err != nil {
    panic(err)
}
```

## Search Operations

### Semantic Search with Reranking (Best Practice)

**Note**: Reranking is a best practice for production quality results. Quickstarts include reranking to demonstrate usage.

```go
func searchWithRerank(indexConn *pinecone.IndexConnection, namespace string, queryText string, topK int) (*pinecone.SearchRecordsResponse, error) {
    // Best practice: Use reranking for production quality results. This pattern is shown in quickstarts.
    ctx := context.Background()
    results, err := indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: &pinecone.SearchRecordsRequestQuery{
            TopK: pinecone.Int32Ptr(int32(topK * 2)), // more candidates for reranking
            Inputs: map[string]interface{}{
                "text": queryText, // must match index config
            },
        },
        Rerank: &pinecone.SearchRecordsRequestRerank{
            Model:      pinecone.StringPtr("bge-reranker-v2-m3"),
            TopN:       pinecone.Int32Ptr(int32(topK)),
            RankFields: []string{"content"},
        },
    })

    return results, err
}
```

### Lexical Search

```go
// Basic lexical search
func lexicalSearch(indexConn *pinecone.IndexConnection, namespace string, queryText string, topK int) (*pinecone.SearchRecordsResponse, error) {
    // Keyword-based search using integrated inference
    ctx := context.Background()
    results, err := indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: &pinecone.SearchRecordsRequestQuery{
            Inputs: map[string]interface{}{
                "text": queryText,
            },
            TopK: pinecone.Int32Ptr(int32(topK)),
        },
    })

    return results, err
}

// Lexical search with required terms
func lexicalSearchWithRequiredTerms(indexConn *pinecone.IndexConnection, namespace string, queryText string, requiredTerms []string, topK int) (*pinecone.SearchRecordsResponse, error) {
    // Results must contain specific required words
    ctx := context.Background()
    results, err := indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: &pinecone.SearchRecordsRequestQuery{
            Inputs: map[string]interface{}{
                "text": queryText,
            },
            TopK:      pinecone.Int32Ptr(int32(topK)),
            MatchTerms: requiredTerms, // results must contain these terms
        },
    })

    return results, err
}

// Lexical search with reranking
func lexicalSearchWithRerank(indexConn *pinecone.IndexConnection, namespace string, queryText string, topK int) (*pinecone.SearchRecordsResponse, error) {
    // Lexical search with reranking for better relevance
    ctx := context.Background()
    results, err := indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: &pinecone.SearchRecordsRequestQuery{
            Inputs: map[string]interface{}{
                "text": queryText,
            },
            TopK: pinecone.Int32Ptr(int32(topK * 2)), // get more candidates for reranking
        },
        Rerank: &pinecone.SearchRecordsRequestRerank{
            Model:      pinecone.StringPtr("bge-reranker-v2-m3"),
            TopN:       pinecone.Int32Ptr(int32(topK)),
            RankFields: []string{"content"},
        },
    })

    return results, err
}
```

### Metadata Filtering

```go
func searchWithFilters(indexConn *pinecone.IndexConnection, namespace string, queryText string) (*pinecone.SearchRecordsResponse, error) {
    // Simple filters
    simpleFilter := map[string]interface{}{
        "category": "documentation",
    }

    // Complex filters
    complexFilter := map[string]interface{}{
        "$and": []map[string]interface{}{
            {
                "category": map[string]interface{}{
                    "$in": []string{"docs", "tutorial"},
                },
            },
            {
                "priority": map[string]interface{}{
                    "$ne": "low",
                },
            },
            {
                "created_at": map[string]interface{}{
                    "$gte": "2025-01-01",
                },
            },
        },
    }

    ctx := context.Background()
    results, err := indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: &pinecone.SearchRecordsRequestQuery{
            TopK: pinecone.Int32Ptr(10),
            Inputs: map[string]interface{}{
                "text": queryText,
            },
            Filter: complexFilter, // Filter goes inside query object
        },
    })

    return results, err
}

// Search without filters - omit the filter property
func searchWithoutFilters(indexConn *pinecone.IndexConnection, namespace string, queryText string) (*pinecone.SearchRecordsResponse, error) {
    ctx := context.Background()
    results, err := indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: &pinecone.SearchRecordsRequestQuery{
            TopK: pinecone.Int32Ptr(10),
            Inputs: map[string]interface{}{
                "text": queryText,
            },
            // No filter key at all
        },
    })

    return results, err
}

// Dynamic filter pattern - conditionally add filter
func searchWithDynamicFilter(indexConn *pinecone.IndexConnection, namespace string, queryText string, hasFilters bool) (*pinecone.SearchRecordsResponse, error) {
    query := &pinecone.SearchRecordsRequestQuery{
        TopK: pinecone.Int32Ptr(10),
        Inputs: map[string]interface{}{
            "text": queryText,
        },
    }

    if hasFilters {
        query.Filter = map[string]interface{}{
            "category": map[string]interface{}{
                "$eq": "docs",
            },
        }
    }

    ctx := context.Background()
    return indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: query,
    })
}
```

## Error Handling (Production)

### Retry Pattern

```go
import (
    "context"
    "fmt"
    "math"
    "time"
)

func exponentialBackoffRetry(ctx context.Context, fn func() error, maxRetries int) error {
    for attempt := 0; attempt < maxRetries; attempt++ {
        err := fn()
        if err == nil {
            return nil
        }

        // Extract status code from error
        statusCode := extractStatusCode(err)

        // Only retry transient errors
        if statusCode >= 500 || statusCode == 429 {
            if attempt < maxRetries-1 {
                delay := time.Duration(math.Min(math.Pow(2, float64(attempt)), 60)) * time.Second
                select {
                case <-ctx.Done():
                    return ctx.Err()
                case <-time.After(delay):
                    continue
                }
            }
        }

        return err // Don't retry client errors (4xx except 429)
    }

    return fmt.Errorf("max retries exceeded")
}

func extractStatusCode(err error) int {
    // This would need to be implemented based on the actual error structure
    // For now, return a default value
    return 500
}

// Usage
ctx := context.Background()
err := exponentialBackoffRetry(ctx, func() error {
    return indexConn.WithNamespace(namespace).UpsertRecords(ctx, records)
}, 5)
```

## Batch Processing

```go
func batchUpsert(indexConn *pinecone.IndexConnection, namespace string, records []*pinecone.IntegratedRecord, batchSize int) error {
    ctx := context.Background()

    // Text records: MAX 96 per batch, 2MB total
    // Adjust batchSize accordingly (typically 96 for text records)

    for i := 0; i < len(records); i += batchSize {
        end := i + batchSize
        if end > len(records) {
            end = len(records)
        }

        batch := records[i:end]

        err := exponentialBackoffRetry(ctx, func() error {
            return indexConn.WithNamespace(namespace).UpsertRecords(ctx, batch)
        }, 5)

        if err != nil {
            return err
        }

        time.Sleep(100 * time.Millisecond) // Rate limiting
    }

    return nil
}
```

## Common Operations

### Index Management

```go
func (ps *PineconeService) indexExists(indexName string) bool {
    ctx := context.Background()
    indexes, err := ps.client.ListIndexes(ctx)
    if err != nil {
        return false
    }

    for _, idx := range indexes {
        if idx.Name == indexName {
            return true
        }
    }

    return false
}

// Get stats (for monitoring/metrics)
func (ps *PineconeService) printStats() error {
    indexConn, err := ps.GetIndexConnection()
    if err != nil {
        return err
    }

    ctx := context.Background()
    stats, err := indexConn.DescribeIndexStats(ctx)
    if err != nil {
        return err
    }

    if stats.TotalRecordCount != nil {
        fmt.Printf("Total records: %d\n", *stats.TotalRecordCount)
    }
    if stats.Namespaces != nil {
        fmt.Printf("Namespaces: %v\n", stats.Namespaces)
    }

    return nil
}
```

### Data Operations

```go
func (ps *PineconeService) fetchRecords(namespace string, ids []string) error {
    indexConn, err := ps.GetIndexConnection()
    if err != nil {
        return err
    }

    ctx := context.Background()
    result, err := indexConn.WithNamespace(namespace).Fetch(ctx, ids)
    if err != nil {
        return err
    }

    if result.Records != nil {
        for recordId, record := range result.Records {
            if record != nil && record.Fields != nil {
                content := ""
                if cont, ok := record.Fields["content"].(string); ok {
                    content = cont
                }
                fmt.Printf("%s: %s\n", recordId, content)
            }
        }
    }

    return nil
}

// List all IDs (paginated)
func (ps *PineconeService) listAllIds(namespace string) ([]string, error) {
    indexConn, err := ps.GetIndexConnection()
    if err != nil {
        return nil, err
    }

    var allIds []string
    var paginationToken *string

    for {
        params := &pinecone.ListPaginatedRequest{
            Limit: pinecone.Int32Ptr(1000),
        }

        if paginationToken != nil {
            params.PaginationToken = paginationToken
        }

        ctx := context.Background()
        result, err := indexConn.WithNamespace(namespace).ListPaginated(ctx, params)
        if err != nil {
            return nil, err
        }

        if result.Vectors != nil {
            for _, vector := range result.Vectors {
                if vector.Id != nil {
                    allIds = append(allIds, *vector.Id)
                }
            }
        }

        if result.Pagination == nil || result.Pagination.Next == nil {
            break
        }
        paginationToken = result.Pagination.Next
    }

    return allIds, nil
}

// Delete records
func (ps *PineconeService) deleteRecords(namespace string, ids []string) error {
    indexConn, err := ps.GetIndexConnection()
    if err != nil {
        return err
    }

    ctx := context.Background()
    err = indexConn.WithNamespace(namespace).DeleteMany(ctx, ids)
    return err
}

// Delete entire namespace
func (ps *PineconeService) deleteNamespace(namespace string) error {
    indexConn, err := ps.GetIndexConnection()
    if err != nil {
        return err
    }

    ctx := context.Background()
    err = indexConn.WithNamespace(namespace).DeleteAll(ctx)
    return err
}
```

## Go-Specific Patterns

### Namespace Strategy

```go
// Multi-user apps
func userNamespace(userID string) string {
    return fmt.Sprintf("user_%s", userID)
}

// Session-based
func sessionNamespace(sessionID string) string {
    return fmt.Sprintf("session_%s", sessionID)
}

// Content-based
func knowledgeBaseNamespace() string {
    return "knowledge_base"
}

func chatHistoryNamespace() string {
    return "chat_history"
}
```

### Configuration Management

```go
import (
    "encoding/json"
    "os"
)

type Config struct {
    PineconeAPIKey  string `json:"pinecone_api_key"`
    PineconeIndex   string `json:"pinecone_index"`
}

func loadConfig(configFile string) (*Config, error) {
    data, err := os.ReadFile(configFile)
    if err != nil {
        return nil, err
    }

    var config Config
    err = json.Unmarshal(data, &config)
    if err != nil {
        return nil, err
    }

    // Override with environment variables if present
    if apiKey := os.Getenv("PINECONE_API_KEY"); apiKey != "" {
        config.PineconeAPIKey = apiKey
    }

    if indexName := os.Getenv("PINECONE_INDEX"); indexName != "" {
        config.PineconeIndex = indexName
    }

    return &config, nil
}
```

## 🚨 Common Mistakes (Must Avoid)

> **For universal common mistakes**, see [PINECONE.md](./PINECONE.md#-common-mistakes-must-avoid). Below are Go-specific examples.

### 1. **Nested Metadata** (will cause API errors)

```go
// ❌ WRONG - nested objects not allowed
badMetadata := map[string]interface{}{
    "user": map[string]interface{}{
        "name": "John",
        "id":   123,
    }, // Nested
    "tags": []interface{}{
        map[string]interface{}{"type": "urgent"}, // Nested in list
    },
}

// ✅ CORRECT - flat structure only
goodMetadata := map[string]interface{}{
    "user_name": "John",
    "user_id":   123,
    "tags":      []string{"urgent", "important"}, // String lists OK
}
```

### 2. **Batch Size Limits** (will cause API errors)

```go
// Vector records: MAX 1000 per batch, 2MB total
// Adjust batchSize accordingly (typically 1000 for vector records)

// ✅ CORRECT - respect limits
func batchUpsert(namespace string, records []pinecone.Vector, batchSize int) error {
    ctx := context.Background()
    for i := 0; i < len(records); i += batchSize {
        end := i + batchSize
        if end > len(records) {
            end = len(records)
        }
        batch := records[i:end]

        _, err := index.Upsert(ctx, pinecone.UpsertParams{
            Vectors:   batch,
            Namespace: namespace,
        })
        if err != nil {
            return err
        }
    }
    return nil
}
```

### 3. **Missing Namespaces** (causes data isolation issues)

```go
// ❌ WRONG - no namespace (default namespace not recommended)
// index.Upsert(ctx, pinecone.UpsertParams{Vectors: records})

// ✅ CORRECT - always use namespaces
index.Upsert(ctx, pinecone.UpsertParams{
    Vectors:   records,
    Namespace: "user_123",
})
```

### 4. **Hardcoded API Keys**

```go
// ❌ WRONG
client, err := pinecone.NewClient(pinecone.NewClientParams{
    ApiKey: "pc-abc123...",
})

// ✅ CORRECT
apiKey := os.Getenv("PINECONE_API_KEY")
if apiKey == "" {
    return nil, fmt.Errorf("PINECONE_API_KEY required")
}
client, err := pinecone.NewClient(pinecone.NewClientParams{
    ApiKey: apiKey,
})
```

### 5. **Not Waiting for Indexing** (eventual consistency)

> **⚠️ IMPORTANT FOR AGENTS**: Include wait logic in generated code (e.g., `time.Sleep(10 * time.Second)`). **DO NOT** execute `sleep` commands in the terminal yourself.

```go
// ❌ WRONG - search immediately after upsert
ctx := context.Background()
indexConn.WithNamespace(namespace).UpsertRecords(ctx, records)
results, err := indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{...}) // May return no results!

// ✅ CORRECT - wait for indexing (10+ seconds) - include in generated code
ctx := context.Background()
indexConn.WithNamespace(namespace).UpsertRecords(ctx, records)
time.Sleep(10 * time.Second) // Wait for eventual consistency
results, err := indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{...})
```

### 6. **Type Assertion Errors**

```go
// ❌ WRONG - unsafe type assertion
category := hit.Metadata["category"].(string) // May panic if not string

// ✅ CORRECT - safe type assertion with ok check
category, ok := hit.Metadata["category"].(string)
if !ok {
    category = "unknown"
}
```

## ⏳ Indexing Delays & Eventual Consistency

> **⚠️ IMPORTANT FOR AGENTS**: The wait instructions below apply **ONLY to generated code**, not to the agent's own behavior. Include wait logic in the code you generate. **DO NOT** execute `sleep` commands in the terminal yourself.

> **For complete information on eventual consistency**, see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md#indexing-delays--eventual-consistency).

**Key Points:**
- Records become searchable 5-10 seconds after upsert
- Stats update 10-20 seconds after upsert
- Always wait 10+ seconds before searching after upserting (include wait logic in generated code)

**Production Pattern (Go):**

```go
func waitForRecords(indexConn *pinecone.IndexConnection, namespace string, expectedCount int, maxWaitSeconds int) error {
    startTime := time.Now()
    ctx := context.Background()

    for time.Since(startTime) < time.Duration(maxWaitSeconds)*time.Second {
        stats, err := indexConn.DescribeIndexStats(ctx)
        if err != nil {
            return err
        }

        count := 0
        if stats.Namespaces != nil {
            if nsStats, exists := stats.Namespaces[namespace]; exists {
                if nsStats.RecordCount != nil {
                    count = int(*nsStats.RecordCount)
                }
            }
        }

        if count >= expectedCount {
            fmt.Printf("✓ All %d records indexed\n", count)
            return nil
        }

        fmt.Printf("⏳ Indexed %d/%d records, waiting...\n", count, expectedCount)
        time.Sleep(5 * time.Second) // Check every 5 seconds
    }

    return fmt.Errorf("timeout: records not fully indexed after %ds", maxWaitSeconds)
}

// Usage
ctx := context.Background()
err := indexConn.WithNamespace("example-namespace").UpsertRecords(ctx, records)
if err != nil {
    return err
}

err = waitForRecords(indexConn, "example-namespace", len(records), 300)
```

## 🆘 Troubleshooting

> **For general troubleshooting issues** (search returns no results, rate limits, metadata errors, etc.), see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md).

### Problem: Search returns no results after upserting

**Cause**: Eventual consistency - records not yet indexed

**Solution**: Wait 10+ seconds after upsert before searching (include wait logic in generated code)

```go
// ✅ CORRECT - include wait in generated code
ctx := context.Background()
err := indexConn.WithNamespace(namespace).UpsertRecords(ctx, records)
if err != nil {
    return err
}

time.Sleep(10 * time.Second)

results, err := indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{
    Query: &pinecone.SearchRecordsRequestQuery{
        TopK: pinecone.Int32Ptr(10),
        Inputs: map[string]interface{}{
            "text": queryText,
        },
    },
})
```

### Problem: Metadata too large error

**Cause**: Exceeding 40KB limit per record or nested objects

**Solution**: Check metadata size and flatten nested structures

```go
// ❌ WRONG - nested objects
badMetadata := map[string]interface{}{
    "user": map[string]interface{}{
        "name": "John",
    }, // Nested
}

// ✅ CORRECT - flat structure
goodMetadata := map[string]interface{}{
    "user_name": "John", // Flat
}
```

### Problem: Batch too large error

**Cause**: Exceeding batch size limits (1000 for vectors)

**Solution**: Reduce batch size

```go
// ✅ CORRECT - respect batch limits
batchSize := 96 // For text records (MAX 96 per batch)
ctx := context.Background()
for i := 0; i < len(records); i += batchSize {
    end := i + batchSize
    if end > len(records) {
        end = len(records)
    }
    batch := records[i:end]

    err := indexConn.WithNamespace(namespace).UpsertRecords(ctx, batch)
    if err != nil {
        return err
    }
}
```

### Problem: Type assertion panics

**Cause**: Metadata values may not be the expected type

**Solution**: Use safe type assertions with ok checks

```go
// ❌ WRONG - unsafe assertion (if using old API)
// category := hit.Metadata["category"].(string) // May panic

// ✅ CORRECT - safe assertion with new API (Fields instead of Metadata)
category := "unknown"
if hit.Fields != nil {
    if cat, ok := hit.Fields["category"].(string); ok {
        category = cat
    }
}

// ✅ BETTER - helper function
func getStringFromFields(fields map[string]interface{}, key string, defaultValue string) string {
    if fields == nil {
        return defaultValue
    }
    if val, ok := fields[key].(string); ok {
        return val
    }
    return defaultValue
}

category := getStringFromFields(hit.Fields, "category", "unknown")
```

For other troubleshooting issues, see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md).

## Use Case Examples

### Semantic Search System

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-search -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```go
package main

import (
    "context"
    "fmt"
    "os"

    "github.com/pinecone-io/go-pinecone/pinecone"
)

type SemanticSearchSystem struct {
    indexConn *pinecone.IndexConnection
}

func NewSemanticSearchSystem(client *pinecone.Client, indexName string) (*SemanticSearchSystem, error) {
    ctx := context.Background()
    indexes, err := client.ListIndexes(ctx)
    if err != nil {
        return nil, fmt.Errorf("failed to list indexes: %w", err)
    }

    var idx *pinecone.Index
    for _, i := range indexes {
        if i.Name == indexName {
            idx = i
            break
        }
    }

    if idx == nil {
        return nil, fmt.Errorf("index %s not found", indexName)
    }

    indexConn := client.Index(pinecone.NewIndexConnParams{
        Host: idx.Host,
    })

    return &SemanticSearchSystem{
        indexConn: indexConn,
    }, nil
}

func (s *SemanticSearchSystem) SearchKnowledgeBase(query string, categoryFilter string, topK int) (*pinecone.SearchRecordsResponse, error) {
    queryObj := &pinecone.SearchRecordsRequestQuery{
        TopK: pinecone.Int32Ptr(int32(topK * 2)), // Get more candidates for reranking
        Inputs: map[string]interface{}{
            "text": query,
        },
    }

    if categoryFilter != "" {
        queryObj.Filter = map[string]interface{}{
            "category": map[string]interface{}{
                "$eq": categoryFilter,
            },
        }
    }

    ctx := context.Background()
    return s.indexConn.WithNamespace("knowledge_base").SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: queryObj,
        Rerank: &pinecone.SearchRecordsRequestRerank{
            Model:      pinecone.StringPtr("bge-reranker-v2-m3"),
            TopN:       pinecone.Int32Ptr(int32(topK)),
            RankFields: []string{"content"},
        },
    })
}
```

### Multi-Tenant RAG System

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-rag -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```go
package main

import (
    "context"
    "fmt"
    "os"
    "strings"

    "github.com/pinecone-io/go-pinecone/pinecone"
)

type RagSystem struct {
    indexConn *pinecone.IndexConnection
}

func NewRagSystem(client *pinecone.Client, indexName string) (*RagSystem, error) {
    ctx := context.Background()
    indexes, err := client.ListIndexes(ctx)
    if err != nil {
        return nil, fmt.Errorf("failed to list indexes: %w", err)
    }

    var idx *pinecone.Index
    for _, i := range indexes {
        if i.Name == indexName {
            idx = i
            break
        }
    }

    if idx == nil {
        return nil, fmt.Errorf("index %s not found", indexName)
    }

    indexConn := client.Index(pinecone.NewIndexConnParams{
        Host: idx.Host,
    })

    return &RagSystem{
        indexConn: indexConn,
    }, nil
}

func (r *RagSystem) RagQuery(userID string, query string, topK int) (string, error) {
    // Ensure namespace isolation
    namespace := fmt.Sprintf("user_%s", userID)

    // Search only user's namespace with integrated inference and reranking
    ctx := context.Background()
    results, err := r.indexConn.WithNamespace(namespace).SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: &pinecone.SearchRecordsRequestQuery{
            TopK: pinecone.Int32Ptr(int32(topK * 2)), // Get more candidates for reranking
            Inputs: map[string]interface{}{
                "text": query,
            },
        },
        Rerank: &pinecone.SearchRecordsRequestRerank{
            Model:      pinecone.StringPtr("bge-reranker-v2-m3"),
            TopN:       pinecone.Int32Ptr(int32(topK)),
            RankFields: []string{"content"},
        },
    })

    if err != nil {
        return "", err
    }

    // Construct context for LLM with safe type assertions
    var context strings.Builder
    if results.Result != nil && results.Result.Hits != nil {
        for _, hit := range results.Result.Hits {
            id := ""
            if hit.Id != nil {
                id = *hit.Id
            }
            content := ""
            if hit.Fields != nil {
                if cont, ok := hit.Fields["content"].(string); ok {
                    content = cont
                }
            }
            context.WriteString(fmt.Sprintf("Document %s: %s\n", id, content))
        }
    }

    return context.String(), nil
}
```

### Recommendation Engine

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-recommendations -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```go
package main

import (
    "context"
    "fmt"
    "os"

    "github.com/pinecone-io/go-pinecone/pinecone"
)

type RecommendationEngine struct {
    indexConn *pinecone.IndexConnection
}

func NewRecommendationEngine(client *pinecone.Client, indexName string) (*RecommendationEngine, error) {
    ctx := context.Background()
    indexes, err := client.ListIndexes(ctx)
    if err != nil {
        return nil, fmt.Errorf("failed to list indexes: %w", err)
    }

    var idx *pinecone.Index
    for _, i := range indexes {
        if i.Name == indexName {
            idx = i
            break
        }
    }

    if idx == nil {
        return nil, fmt.Errorf("index %s not found", indexName)
    }

    indexConn := client.Index(pinecone.NewIndexConnParams{
        Host: idx.Host,
    })

    return &RecommendationEngine{
        indexConn: indexConn,
    }, nil
}

func (r *RecommendationEngine) GetRecommendations(productID string, categoryFilter string, topK int) ([]*pinecone.SearchResultHit, error) {
    queryText := fmt.Sprintf("product description for %s", productID)

    queryObj := &pinecone.SearchRecordsRequestQuery{
        TopK: pinecone.Int32Ptr(int32(topK * 2)), // Get more candidates for reranking
        Inputs: map[string]interface{}{
            "text": queryText,
        },
    }

    // Apply category filtering if specified
    if categoryFilter != "" {
        queryObj.Filter = map[string]interface{}{
            "category": map[string]interface{}{
                "$eq": categoryFilter,
            },
        }
    }

    ctx := context.Background()
    results, err := r.indexConn.WithNamespace("products").SearchRecords(ctx, &pinecone.SearchRecordsRequest{
        Query: queryObj,
        Rerank: &pinecone.SearchRecordsRequestRerank{
            Model:      pinecone.StringPtr("bge-reranker-v2-m3"),
            TopN:       pinecone.Int32Ptr(int32(topK)),
            RankFields: []string{"content"},
        },
    })

    if err != nil {
        return nil, err
    }

    // Return top K results
    if results.Result != nil && results.Result.Hits != nil {
        if len(results.Result.Hits) > topK {
            return results.Result.Hits[:topK], nil
        }
        return results.Result.Hits, nil
    }

    return []*pinecone.SearchResultHit{}, nil
}
```

## Go Ecosystem Integration

### Gin Framework Integration

```go
package main

import (
    "net/http"
    "os"

    "github.com/gin-gonic/gin"
    "github.com/pinecone-io/go-pinecone/pinecone"
)

type SearchRequest struct {
    Query    string `json:"query" binding:"required"`
    Category string `json:"category"`
    TopK     int    `json:"top_k"`
}

func setupSearchHandler(router *gin.Engine, searchSystem *SemanticSearchSystem) {
    router.POST("/search", func(c *gin.Context) {
        var req SearchRequest
        if err := c.ShouldBindJSON(&req); err != nil {
            c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
            return
        }

        if req.TopK == 0 {
            req.TopK = 5 // Default
        }

        results, err := searchSystem.SearchKnowledgeBase(req.Query, req.Category, req.TopK)
        if err != nil {
            c.JSON(http.StatusInternalServerError, gin.H{"error": "Search failed"})
            return
        }

        c.JSON(http.StatusOK, results)
    })
}

func main() {
    apiKey := os.Getenv("PINECONE_API_KEY")
    if apiKey == "" {
        panic("PINECONE_API_KEY required")
    }

    client, err := pinecone.NewClient(pinecone.NewClientParams{ApiKey: apiKey})
    if err != nil {
        panic(err)
    }

    searchSystem := NewSemanticSearchSystem(client, "my-index")

    router := gin.Default()
    setupSearchHandler(router, searchSystem)
    router.Run(":8080")
}
```

### Echo Framework Integration

```go
package main

import (
    "net/http"
    "os"

    "github.com/labstack/echo/v4"
    "github.com/pinecone-io/go-pinecone/pinecone"
)

type SearchRequest struct {
    Query    string `json:"query"`
    Category string `json:"category"`
    TopK     int    `json:"top_k"`
}

func setupSearchHandler(e *echo.Echo, searchSystem *SemanticSearchSystem) {
    e.POST("/search", func(c echo.Context) error {
        var req SearchRequest
        if err := c.Bind(&req); err != nil {
            return c.JSON(http.StatusBadRequest, map[string]string{"error": err.Error()})
        }

        if req.TopK == 0 {
            req.TopK = 5 // Default
        }

        results, err := searchSystem.SearchKnowledgeBase(req.Query, req.Category, req.TopK)
        if err != nil {
            return c.JSON(http.StatusInternalServerError, map[string]string{"error": "Search failed"})
        }

        return c.JSON(http.StatusOK, results)
    })
}

func main() {
    apiKey := os.Getenv("PINECONE_API_KEY")
    if apiKey == "" {
        panic("PINECONE_API_KEY required")
    }

    client, err := pinecone.NewClient(pinecone.NewClientParams{ApiKey: apiKey})
    if err != nil {
        panic(err)
    }

    searchSystem, err := NewSemanticSearchSystem(client, "my-index")
    if err != nil {
        panic(err)
    }

    e := echo.New()
    setupSearchHandler(e, searchSystem)
    e.Start(":8080")
}
```

### Standard HTTP Server Integration

```go
package main

import (
    "encoding/json"
    "net/http"
    "os"

    "github.com/pinecone-io/go-pinecone/pinecone"
)

type SearchRequest struct {
    Query    string `json:"query"`
    Category string `json:"category"`
    TopK     int    `json:"top_k"`
}

type SearchHandler struct {
    searchSystem *SemanticSearchSystem
}

func NewSearchHandler(searchSystem *SemanticSearchSystem) *SearchHandler {
    return &SearchHandler{
        searchSystem: searchSystem,
    }
}

func (h *SearchHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    var req SearchRequest
    if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
        http.Error(w, "Invalid JSON", http.StatusBadRequest)
        return
    }

    if req.TopK == 0 {
        req.TopK = 5 // Default
    }

    results, err := h.searchSystem.SearchKnowledgeBase(req.Query, req.Category, req.TopK)
    if err != nil {
        http.Error(w, "Search failed", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(results)
}

func main() {
    apiKey := os.Getenv("PINECONE_API_KEY")
    if apiKey == "" {
        panic("PINECONE_API_KEY required")
    }

    client, err := pinecone.NewClient(pinecone.NewClientParams{ApiKey: apiKey})
    if err != nil {
        panic(err)
    }

    searchSystem, err := NewSemanticSearchSystem(client, "my-index")
    if err != nil {
        panic(err)
    }
    http.Handle("/search", NewSearchHandler(searchSystem))
    http.ListenAndServe(":8080", nil)
}
```
