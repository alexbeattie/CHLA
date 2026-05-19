# Pinecone Java SDK Guide

> **Prerequisites**: See [PINECONE.md](./PINECONE.md) for universal concepts and setup.

This guide provides Java-specific patterns, examples, and best practices for the Pinecone SDK.

## 🚨 MANDATORY RULES - Read First

**⚠️ CRITICAL: These rules MUST be followed. Violations will cause runtime errors or data issues.**

1. **MUST use namespaces** - Every upsert, search, fetch, delete operation MUST specify a namespace parameter
2. **MUST wait 10+ seconds** - After upserting records, MUST wait 10+ seconds before searching
3. **MUST match field_map** - Record field names MUST match the right side of `--field_map` used when creating index
4. **MUST respect batch limits** - Text records: MAX 96 per batch, Vector records: MAX 1000 per batch
5. **MUST use flat metadata** - No nested objects allowed, only flat key-value pairs
6. **MUST handle exceptions** - All operations can throw exceptions, MUST use try-catch
7. **MUST verify before installing** - Check if SDK/CLI already installed before prompting installation

**Before proceeding with any operation, verify these rules are followed. See detailed sections below for implementation.**

## Installation & Setup

> **⚠️ IMPORTANT**: See [PINECONE.md](./PINECONE.md#-mandatory-always-use-latest-version) for the mandatory requirement to always use the latest version when creating projects.

### Finding the Latest Version

**Check latest version on Maven Central:**

- Browse: [https://repo1.maven.org/maven2/io/pinecone/pinecone-client/](https://repo1.maven.org/maven2/io/pinecone/pinecone-client/)
- Or check via CLI: `curl -s https://repo1.maven.org/maven2/io/pinecone/pinecone-client/maven-metadata.xml`

### Maven Dependency

**Install latest version:**

```xml
<dependency>
    <groupId>io.pinecone</groupId>
    <artifactId>pinecone-client</artifactId>
    <version>5.1.0</version>  <!-- Replace with desired version -->
</dependency>
```

### Gradle Dependency

**Install specific version:**

```gradle
implementation 'io.pinecone:pinecone-client:5.1.0'  // Replace with desired version
```

**Using dynamic version (not recommended for production):**

```gradle
implementation 'io.pinecone:pinecone-client:+'  // Gets latest, but can cause unexpected updates
// Or with version range
implementation 'io.pinecone:pinecone-client:[5.1.0,)'  // 5.1.0 or higher
```

**Check versions available (requires `gradle-versions-plugin`):**

```bash
# First add plugin to build.gradle:
# plugins { id 'com.github.ben-manes.versions' version '0.46.0' }

gradle dependencyUpdates
```

**Or check manually:**

```bash
# Check dependency version in your project
gradle dependencies | grep pinecone-client

# Or browse Maven Central directly
```

### Java Imports

```java
import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;
import org.openapitools.db_data.client.model.SearchRecordsRequestQuery;
import org.openapitools.db_data.client.model.SearchRecordsRequestRerank;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import java.util.*;
```

### Environment Configuration

**⚠️ Use `.env` files (see [PINECONE.md](./PINECONE.md#-environment-variables--security-best-practices)).**

**Maven:**
```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>dotenv-java</artifactId>
    <version>3.0.0</version>
</dependency>
```

```java
import io.github.cdimascio.dotenv.Dotenv;
import io.pinecone.clients.Pinecone;

Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
String apiKey = dotenv.get("PINECONE_API_KEY");
if (apiKey == null || apiKey.isEmpty()) {
    throw new IllegalArgumentException("PINECONE_API_KEY required");
}
Pinecone client = new Pinecone.Builder(apiKey).build();
```

### Production Client Class

```java
import io.github.cdimascio.dotenv.Dotenv;
import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;

public class PineconeService {
    private final Pinecone client;
    private final String indexName;
    public PineconeService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String apiKey = dotenv.get("PINECONE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("PINECONE_API_KEY required");
        }
        this.client = new Pinecone.Builder(apiKey).build();
        this.indexName = dotenv.get("PINECONE_INDEX", "default-index");
    }
    public Index getIndex() {
        return client.getIndexConnection(indexName);
    }
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

> **Sample Data**: Use the sample data from [PINECONE-quickstart.md](./PINECONE-quickstart.md#sample-data-use-in-all-languages). Convert JSON format to Java Maps.

```java
import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;
import java.util.*;

public class QuickStartExample {
    public static void main(String[] args) {
        // Initialize Pinecone client
        String apiKey = System.getenv("PINECONE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("PINECONE_API_KEY environment variable not set");
        }

        Pinecone client = new Pinecone.Builder(apiKey).build();

        // Get the index (with integrated embeddings)
        Index index = client.getIndexConnection("agentic-quickstart-test");

        // Sample records (see quickstart guide for full list of 12 records)
        // Records are Maps with _id and text fields that match field_map
        List<Map<String, String>> records = Arrays.asList(
            Map.of(
                "_id", "rec1",
                "content", "The Eiffel Tower was completed in 1889 and stands in Paris, France.",
                "category", "history"
            ),
            Map.of(
                "_id", "rec2",
                "content", "Photosynthesis allows plants to convert sunlight into energy.",
                "category", "science"
            ),
            // ... (use all 12 records from quickstart guide)
        );

        // Upsert the records into a namespace (embeddings generated automatically)
        index.upsertRecords("example-namespace", records);
    }
}
```

3. **Search with reranking:**

```java
import io.pinecone.clients.Index;
import org.openapitools.db_data.client.model.SearchRecordsRequestRerank;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class SearchExample {
    public static void main(String[] args) throws InterruptedException {
        // Wait for the upserted records to be indexed (10+ seconds for eventual consistency)
        TimeUnit.SECONDS.sleep(10);

        // Define the query text (see quickstart guide for test query)
        String queryText = "Famous historical structures and monuments";

        // Configure reranking
        SearchRecordsRequestRerank rerank = new SearchRecordsRequestRerank()
            .model("bge-reranker-v2-m3")
            .topN(10)
            .rankFields(Arrays.asList("content"));

        // Search using text query with integrated inference and reranking
        SearchRecordsResponse results = index.searchRecordsByText(
            queryText,                              // Query text (embedding generated automatically)
            "example-namespace",                    // Namespace
            Arrays.asList("content", "category"),  // Fields to return
            10,                                     // Top K (get more candidates for reranking)
            null,                                   // Filter (null = no filter)
            rerank                                  // Reranking configuration
        );

        // Print the reranked results
        results.getResult().getHits().forEach(hit -> {
            Map<String, Object> fields = (Map<String, Object>) hit.getFields();
            System.out.printf("id: %s | score: %.2f | category: %s | text: %s%n",
                hit.getId(),
                hit.getScore(),
                fields.get("category"),
                fields.get("content"));
        });
    }
}
```

## Data Operations

### Upserting Records

```java
import io.pinecone.clients.Index;
import java.util.*;

public class DataOperations {
    private Index index;

    public void upsertRecords() {
        // Indexes with integrated embeddings - use text records directly
        // Records are Maps with _id and fields that match field_map
        List<Map<String, String>> records = Arrays.asList(
            Map.of(
                "_id", "doc1",
                "content", "Your text content here", // must match field_map
                "category", "documentation",
                "created_at", "2025-01-01",
                "priority", "high"
            )
        );

        // Always use namespaces
        String namespace = "user_123"; // e.g., "knowledge_base", "session_456"

        // Upsert records (embeddings generated automatically)
        index.upsertRecords(namespace, records);
    }
}
```

### Updating Records

```java
public void updateRecords() {
    // Update existing records (use same upsertRecords operation with existing IDs)
    List<Map<String, String>> updatedRecords = Arrays.asList(
        Map.of(
            "_id", "doc1", // existing record ID
            "content", "Updated content here",
            "category", "updated_docs", // can change fields
            "last_modified", "2025-01-15"
        )
    );

    // Partial updates - only changed fields need to be included
    List<Map<String, String>> partialUpdate = Arrays.asList(
        Map.of(
            "_id", "doc1",
            "category", "urgent", // only updating category field
            "priority", "high"    // adding new field
        )
    );

    index.upsertRecords(namespace, updatedRecords);
}
```

### Fetching Records

```java
import io.pinecone.proto.FetchResponse;
import java.util.*;

public void fetchRecords() {
    // Fetch single record
    FetchResponse result = index.fetch(
        Arrays.asList("doc1"),
        namespace
    );

    if (result.getVectors() != null && result.getVectors().containsKey("doc1")) {
        var record = result.getVectors().get("doc1");
        System.out.println("Content: " + record.getMetadata().get("content"));
        System.out.println("Metadata: " + record.getMetadata());
    }

    // Fetch multiple records
    FetchResponse multiResult = index.fetch(
        Arrays.asList("doc1", "doc2", "doc3"),
        namespace
    );

    multiResult.getVectors().forEach((recordId, record) -> {
        Map<String, Object> metadata = record.getMetadata();
        System.out.println("ID: " + recordId + ", Content: " + metadata.get("content"));
    });
}

// Fetch with error handling
public Map<String, ?> safeFetch(String namespace, List<String> ids) {
    try {
        FetchResponse result = index.fetch(ids, namespace);
        return result.getVectors();
    } catch (Exception e) {
        System.err.println("Fetch failed: " + e.getMessage());
        return new HashMap<>();
    }
}
```

### Listing Record IDs

```java
public List<String> listAllIds(String namespace, String prefix) {
    List<String> allIds = new ArrayList<>();
    String paginationToken = null;

    while (true) {
        ListRequest.Builder listRequestBuilder = ListRequest.builder()
            .namespace(namespace)
            .limit(1000);

        if (prefix != null) {
            listRequestBuilder.prefix(prefix);
        }

        if (paginationToken != null) {
            listRequestBuilder.paginationToken(paginationToken);
        }

        ListResponse result = index.list(listRequestBuilder.build());

        result.getVectors().forEach(vector -> allIds.add(vector.getId()));

        if (result.getPagination() == null || result.getPagination().getNext() == null) {
            break;
        }
        paginationToken = result.getPagination().getNext();
    }

    return allIds;
}

// Usage
List<String> allRecordIds = listAllIds("user_123", null);
List<String> docsOnly = listAllIds("user_123", "doc_");
```

## Search Operations

### Semantic Search with Reranking (Best Practice)

**Note**: Reranking is a best practice for production quality results. Quickstarts include reranking to demonstrate usage.

```java
import io.pinecone.clients.Index;
import org.openapitools.db_data.client.model.SearchRecordsRequestRerank;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import java.util.*;

public SearchRecordsResponse searchWithRerank(String namespace, String queryText, int topK) {
    // Best practice: Use reranking for production quality results. This pattern is shown in quickstarts.

    // Configure reranking
    SearchRecordsRequestRerank rerank = new SearchRecordsRequestRerank()
        .model("bge-reranker-v2-m3")
        .topN(topK)
        .rankFields(Arrays.asList("content"));

    // Search using text query with integrated inference and reranking
    // Get more candidates initially (2x for reranking)
    SearchRecordsResponse results = index.searchRecordsByText(
        queryText,                              // Query text (embedding generated automatically)
        namespace,                              // Namespace
        Arrays.asList("content", "category"),  // Fields to return
        topK * 2,                              // Top K (more candidates for reranking)
        null,                                   // Filter (null = no filter)
        rerank                                  // Reranking configuration
    );

    return results;
}
```

### Lexical Search

```java
import io.pinecone.clients.Index;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import java.util.*;

// Basic lexical search
public SearchRecordsResponse lexicalSearch(String namespace, String queryText, int topK) {
    // Keyword-based search using integrated inference
    SearchRecordsResponse results = index.searchRecordsByText(
        queryText,                              // Query text
        namespace,                              // Namespace
        Arrays.asList("content", "category"),  // Fields to return
        topK,                                   // Top K
        null,                                   // Filter (null = no filter)
        null                                    // Rerank (null = no reranking)
    );

    return results;
}

// Lexical search with required terms
public SearchRecordsResponse lexicalSearchWithRequiredTerms(
    String namespace,
    String queryText,
    List<String> requiredTerms,
    int topK
) {
    // Results must contain specific required words
    Map<String, Object> filter = Map.of(
        "$and", Arrays.asList(
            Map.of("content", Map.of("$in", requiredTerms))
        )
    );

    SearchRecordsResponse results = index.searchRecordsByText(
        queryText,                              // Query text
        namespace,                              // Namespace
        Arrays.asList("content", "category"),  // Fields to return
        topK,                                   // Top K
        filter,                                 // Filter for required terms
        null                                    // Rerank (null = no reranking)
    );

    return results;
}

// Lexical search with reranking
public SearchRecordsResponse lexicalSearchWithRerank(
    String namespace,
    String queryText,
    int topK
) {
    // Lexical search with reranking for better relevance
    SearchRecordsRequestRerank rerank = new SearchRecordsRequestRerank()
        .model("bge-reranker-v2-m3")
        .topN(topK)
        .rankFields(Arrays.asList("content"));

    SearchRecordsResponse results = index.searchRecordsByText(
        queryText,                              // Query text
        namespace,                              // Namespace
        Arrays.asList("content", "category"),  // Fields to return
        topK * 2,                              // Get more candidates for reranking
        null,                                   // Filter (null = no filter)
        rerank                                  // Reranking configuration
    );

    return results;
}
```

### Metadata Filtering

```java
import io.pinecone.clients.Index;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import java.util.*;

public SearchRecordsResponse searchWithFilters(String namespace, String queryText) {
    // Simple filters
    Map<String, Object> simpleFilter = Map.of("category", "documentation");

    // Complex filters
    Map<String, Object> complexFilter = Map.of(
        "$and", Arrays.asList(
            Map.of("category", Map.of("$in", Arrays.asList("docs", "tutorial"))),
            Map.of("priority", Map.of("$ne", "low")),
            Map.of("created_at", Map.of("$gte", "2025-01-01"))
        )
    );

    SearchRecordsResponse results = index.searchRecordsByText(
        queryText,                              // Query text (embedding generated automatically)
        namespace,                              // Namespace
        Arrays.asList("content", "category"),  // Fields to return
        10,                                     // Top K
        complexFilter,                          // Filter
        null                                    // Rerank (null = no reranking)
    );

    return results;
}

// Search without filters - pass null for filter
public SearchRecordsResponse searchWithoutFilters(String namespace, String queryText) {
    SearchRecordsResponse results = index.searchRecordsByText(
        queryText,                              // Query text
        namespace,                              // Namespace
        Arrays.asList("content", "category"),  // Fields to return
        10,                                     // Top K
        null,                                   // No filter
        null                                    // No reranking
    );

    return results;
}

// Dynamic filter pattern - conditionally add filter
public SearchRecordsResponse searchWithDynamicFilter(String namespace, String queryText, boolean hasFilters) {
    Map<String, Object> filter = null;
    if (hasFilters) {
        filter = Map.of("category", Map.of("$eq", "docs"));
    }

    SearchRecordsResponse results = index.searchRecordsByText(
        queryText,                              // Query text
        namespace,                              // Namespace
        Arrays.asList("content", "category"),  // Fields to return
        10,                                     // Top K
        filter,                                 // Conditional filter
        null                                    // No reranking
    );

    return results;
}
```

## Error Handling (Production)

### Retry Pattern

```java
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RetryUtils {
    public static <T> T exponentialBackoffRetry(Supplier<T> func, int maxRetries) throws Exception {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return func.get();
            } catch (Exception e) {
                // Extract status code from exception
                int statusCode = extractStatusCode(e);

                // Only retry transient errors
                if (statusCode >= 500 || statusCode == 429) {
                    if (attempt < maxRetries - 1) {
                        long delay = Math.min((long) Math.pow(2, attempt), 60) * 1000; // Exponential backoff, cap at 60s
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } else {
                        throw e;
                    }
                } else {
                    throw e; // Don't retry client errors (4xx except 429)
                }
            }
        }
        throw new RuntimeException("Max retries exceeded");
    }

    private static int extractStatusCode(Exception e) {
        // This would need to be implemented based on the actual exception structure
        // For now, return a default value
        return 500;
    }
}

// Usage
RetryUtils.exponentialBackoffRetry(() -> {
    index.upsertRecords(namespace, records);
    return null;
}, 5);
```

## Batch Processing

```java
import io.pinecone.clients.Index;
import java.util.*;
import java.util.concurrent.TimeUnit;

public void batchUpsert(String namespace, List<Map<String, String>> records, int batchSize) throws Exception {
    // Text records: MAX 96 per batch, 2MB total
    // Adjust batchSize accordingly (typically 96 for text records)

    for (int i = 0; i < records.size(); i += batchSize) {
        int endIndex = Math.min(i + batchSize, records.size());
        List<Map<String, String>> batch = records.subList(i, endIndex);

        RetryUtils.exponentialBackoffRetry(() -> {
            index.upsertRecords(namespace, batch);
            return null;
        }, 5);

        TimeUnit.MILLISECONDS.sleep(100); // Rate limiting
    }
}
```

## Common Operations

### Index Management

```java
import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;
import io.pinecone.proto.DescribeIndexStatsResponse;

public class IndexManagement {
    private Pinecone client;

    // Check if index exists (in application startup)
    public boolean indexExists(String indexName) {
        try {
            var indexes = client.listIndexes();
            return indexes.getIndexes().stream()
                .anyMatch(index -> index.getName().equals(indexName));
        } catch (Exception e) {
            return false;
        }
    }

    // Get stats (for monitoring/metrics)
    public void printStats(String indexName) {
        Index index = client.getIndexConnection(indexName);
        DescribeIndexStatsResponse stats = index.describeIndexStats();

        System.out.println("Total vectors: " + stats.getTotalVectorCount());
        System.out.println("Namespaces: " + stats.getNamespaces().keySet());
    }
}
```

### Data Operations

```java
import io.pinecone.clients.Index;
import io.pinecone.proto.FetchResponse;
import java.util.*;

public class DataOperations {
    private Index index;

    // Fetch records
    public void fetchRecords(String namespace, List<String> ids) {
        FetchResponse result = index.fetch(ids, namespace);

        result.getVectors().forEach((recordId, record) -> {
            Map<String, Object> metadata = record.getMetadata();
            System.out.println(recordId + ": " + metadata.get("content"));
        });
    }

    // List all IDs (paginated)
    public List<String> listAllIds(String namespace) {
        List<String> allIds = new ArrayList<>();
        String paginationToken = null;

        while (true) {
            ListRequest.Builder listRequestBuilder = ListRequest.builder()
                .namespace(namespace)
                .limit(1000);

            if (paginationToken != null) {
                listRequestBuilder.paginationToken(paginationToken);
            }

            ListResponse result = index.list(listRequestBuilder.build());

            result.getVectors().forEach(vector -> allIds.add(vector.getId()));

            if (result.getPagination() == null || result.getPagination().getNext() == null) {
                break;
            }
            paginationToken = result.getPagination().getNext();
        }

        return allIds;
    }

    // Delete records
    public void deleteRecords(String namespace, List<String> ids) {
        index.delete(ids, true, namespace, null);
    }

    // Delete entire namespace
    public void deleteNamespace(String namespace) {
        index.delete(null, true, namespace, null);
    }
}
```

## Java-Specific Patterns

### Namespace Strategy

```java
public class NamespaceUtils {
    // Multi-user apps
    public static String userNamespace(String userId) {
        return "user_" + userId;
    }

    // Session-based
    public static String sessionNamespace(String sessionId) {
        return "session_" + sessionId;
    }

    // Content-based
    public static String knowledgeBaseNamespace() {
        return "knowledge_base";
    }

    public static String chatHistoryNamespace() {
        return "chat_history";
    }
}
```

### Configuration Management

```java
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class PineconeConfigManager {
    private Properties properties;

    public PineconeConfigManager(String configFile) throws IOException {
        properties = new Properties();
        properties.load(new FileInputStream(configFile));
    }

    public PineconeClient createClient() {
        String apiKey = properties.getProperty("pinecone.api.key");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("PINECONE_API_KEY required");
        }

        PineconeClientConfig config = PineconeClientConfig.builder()
            .apiKey(apiKey)
            .build();

        return new PineconeClient(config);
    }

    public String getIndexName() {
        return properties.getProperty("pinecone.index.name", "default-index");
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

```java
import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;
import org.openapitools.db_data.client.model.SearchRecordsRequestRerank;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import java.util.*;

public class SemanticSearchSystem {
    private Index index;

    public SemanticSearchSystem() {
        String apiKey = System.getenv("PINECONE_API_KEY");
        Pinecone client = new Pinecone.Builder(apiKey).build();
        this.index = client.getIndexConnection("agentic-quickstart-search");
    }

    public SearchRecordsResponse searchKnowledgeBase(String query, String categoryFilter, int topK) {
        // Configure reranking
        SearchRecordsRequestRerank rerank = new SearchRecordsRequestRerank()
            .model("bge-reranker-v2-m3")
            .topN(topK)
            .rankFields(Arrays.asList("content"));

        // Build filter if specified
        Map<String, Object> filter = null;
        if (categoryFilter != null) {
            filter = Map.of("category", Map.of("$eq", categoryFilter));
        }

        // Search using text query with integrated inference and reranking
        SearchRecordsResponse results = index.searchRecordsByText(
            query,                                  // Query text (embedding generated automatically)
            "knowledge_base",                       // Namespace
            Arrays.asList("content", "category"),  // Fields to return
            topK * 2,                              // Get more candidates for reranking
            filter,                                 // Filter (null = no filter)
            rerank                                  // Reranking configuration
        );

        return results;
    }
}
```

### Multi-Tenant RAG System

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-rag -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```java
import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;
import org.openapitools.db_data.client.model.SearchRecordsRequestRerank;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import java.util.*;

public class RagSystem {
    private Index index;

    public RagSystem() {
        String apiKey = System.getenv("PINECONE_API_KEY");
        Pinecone client = new Pinecone.Builder(apiKey).build();
        this.index = client.getIndexConnection("agentic-quickstart-rag");
    }

    public String ragQuery(String userId, String query, int topK) {
        // Ensure namespace isolation
        String namespace = "user_" + userId;

        // Configure reranking
        SearchRecordsRequestRerank rerank = new SearchRecordsRequestRerank()
            .model("bge-reranker-v2-m3")
            .topN(topK)
            .rankFields(Arrays.asList("content"));

        // Search only user's namespace with integrated inference and reranking
        SearchRecordsResponse results = index.searchRecordsByText(
            query,                                  // Query text (embedding generated automatically)
            namespace,                              // Namespace (user isolation)
            Arrays.asList("content", "category"),  // Fields to return
            topK * 2,                              // Get more candidates for reranking
            null,                                   // Filter (null = no filter)
            rerank                                  // Reranking configuration
        );

        // Construct context for LLM
        StringBuilder context = new StringBuilder();
        results.getResult().getHits().forEach(hit -> {
            Map<String, Object> fields = (Map<String, Object>) hit.getFields();
            context.append("Document ").append(hit.getId())
                   .append(": ").append(fields.get("content"))
                   .append("\n");
        });

        return context.toString();
    }
}
```

### Recommendation Engine

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-recommendations -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```java
import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;
import org.openapitools.db_data.client.model.SearchRecordsRequestRerank;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import java.util.*;

public class RecommendationEngine {
    private Index index;

    public RecommendationEngine() {
        String apiKey = System.getenv("PINECONE_API_KEY");
        Pinecone client = new Pinecone.Builder(apiKey).build();
        this.index = client.getIndexConnection("agentic-quickstart-recommendations");
    }

    public SearchRecordsResponse getRecommendations(String productId, String categoryFilter, int topK) {
        // Configure reranking
        SearchRecordsRequestRerank rerank = new SearchRecordsRequestRerank()
            .model("bge-reranker-v2-m3")
            .topN(topK)
            .rankFields(Arrays.asList("content"));

        // Build query text and filter
        String queryText = "product description for " + productId;
        Map<String, Object> filter = null;
        if (categoryFilter != null) {
            filter = Map.of("category", Map.of("$eq", categoryFilter));
        }

        // Get similar products with integrated inference and reranking
        SearchRecordsResponse results = index.searchRecordsByText(
            queryText,                              // Query text (embedding generated automatically)
            "products",                             // Namespace
            Arrays.asList("content", "category"),  // Fields to return
            topK * 2,                              // Get more candidates for reranking
            filter,                                 // Filter (null = no filter)
            rerank                                  // Reranking configuration
        );

        return results;
    }
}
```

## 🚨 Common Mistakes (Must Avoid)

> **For universal common mistakes**, see [PINECONE.md](./PINECONE.md#-common-mistakes-must-avoid). Below are Java-specific examples.

### 1. **Nested Metadata** (will cause API errors)

```java
// ❌ WRONG - nested objects not allowed
Map<String, Object> badMetadata = Map.of(
    "user", Map.of("name", "John", "id", 123), // Nested
    "tags", Arrays.asList(Map.of("type", "urgent")) // Nested in list
);

// ✅ CORRECT - flat structure only
Map<String, Object> goodMetadata = Map.of(
    "user_name", "John",
    "user_id", 123,
    "tags", Arrays.asList("urgent", "important") // String lists OK
);
```

### 2. **Batch Size Limits** (will cause API errors)

```java
// Text records: MAX 96 per batch, 2MB total
// Vector records: MAX 1000 per batch, 2MB total

// ✅ CORRECT - respect limits
public void batchUpsert(String namespace, List<Map<String, String>> records, int batchSize) {
    for (int i = 0; i < records.size(); i += batchSize) {
        int endIndex = Math.min(i + batchSize, records.size());
        List<Map<String, String>> batch = records.subList(i, endIndex);

        index.upsertRecords(namespace, batch);
    }
}
```

### 3. **Missing Namespaces** (causes data isolation issues)

```java
// ❌ WRONG - no namespace (would need to use default namespace)
// index.upsertRecords("", records); // Not recommended

// ✅ CORRECT - always use namespaces
index.upsertRecords("user_123", records);
```

### 4. **Skipping Reranking** (reduces search quality)

```java
// ⚠️ OK but not optimal
SearchRecordsResponse basicResults = index.searchRecordsByText(
    queryText, namespace, Arrays.asList("content"), 5, null, null
);

// ✅ BETTER - use reranking for best results (best practice)
SearchRecordsRequestRerank rerank = new SearchRecordsRequestRerank()
    .model("bge-reranker-v2-m3")
    .topN(5)
    .rankFields(Arrays.asList("content"));

SearchRecordsResponse rerankedResults = index.searchRecordsByText(
    queryText, namespace, Arrays.asList("content"), 10, null, rerank
);
```

### 5. **Hardcoded API Keys**

```java
// ❌ WRONG
Pinecone client = new Pinecone.Builder("pc-abc123...").build();

// ✅ CORRECT
String apiKey = System.getenv("PINECONE_API_KEY");
if (apiKey == null || apiKey.isEmpty()) {
    throw new IllegalArgumentException("PINECONE_API_KEY required");
}
Pinecone client = new Pinecone.Builder(apiKey).build();
```

### 6. **Not Waiting for Indexing** (eventual consistency)

> **⚠️ IMPORTANT FOR AGENTS**: Include wait logic in generated code (e.g., `TimeUnit.SECONDS.sleep(10)`). **DO NOT** execute `sleep` commands in the terminal yourself.

```java
// ❌ WRONG - search immediately after upsert
index.upsertRecords(namespace, records);
SearchRecordsResponse results = index.searchRecordsByText(...); // May return no results!

// ✅ CORRECT - wait for indexing (10+ seconds) - include in generated code
index.upsertRecords(namespace, records);
TimeUnit.SECONDS.sleep(10); // Wait for eventual consistency
SearchRecordsResponse results = index.searchRecordsByText(...);
```

## ⏳ Indexing Delays & Eventual Consistency

> **⚠️ IMPORTANT FOR AGENTS**: The wait instructions below apply **ONLY to generated code**, not to the agent's own behavior. Include wait logic in the code you generate. **DO NOT** execute `sleep` commands in the terminal yourself.

> **For complete information on eventual consistency**, see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md#indexing-delays--eventual-consistency).

**Key Points:**

- Records become searchable 5-10 seconds after upsert
- Stats update 10-20 seconds after upsert
- Always wait 10+ seconds before searching after upserting (include wait logic in generated code)

**Production Pattern (Java):**

```java
public void waitForRecords(String namespace, int expectedCount, int maxWaitSeconds)
        throws InterruptedException {
    long startTime = System.currentTimeMillis();

    while ((System.currentTimeMillis() - startTime) < maxWaitSeconds * 1000) {
        DescribeIndexStatsRequest statsRequest = DescribeIndexStatsRequest.builder().build();
        DescribeIndexStatsResponse stats = index.describeIndexStats(statsRequest);

        int count = stats.getNamespaces() != null &&
                   stats.getNamespaces().containsKey(namespace) ?
                   stats.getNamespaces().get(namespace).getVectorCount() : 0;

        if (count >= expectedCount) {
            System.out.println("✓ All " + count + " records indexed");
            return;
        }

        System.out.println("⏳ Indexed " + count + "/" + expectedCount + " records, waiting...");
        TimeUnit.SECONDS.sleep(5); // Check every 5 seconds
    }

    throw new RuntimeException(
        "Timeout: Records not fully indexed after " + maxWaitSeconds + "s"
    );
}

// Usage
index.upsertRecords("example-namespace", records);
waitForRecords("example-namespace", records.size(), 300);
```

## 🆘 Troubleshooting

> **For general troubleshooting issues** (search returns no results, rate limits, metadata errors, etc.), see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md).

### Problem: Search returns no results after upserting

**Cause**: Eventual consistency - records not yet indexed

**Solution**: Wait 10+ seconds after upsert before searching (include wait logic in generated code)

```java
// ✅ CORRECT - include wait in generated code
index.upsertRecords(namespace, records);
TimeUnit.SECONDS.sleep(10);
SearchRecordsResponse results = index.searchRecordsByText(queryText, namespace, ...);
```

### Problem: Metadata too large error

**Cause**: Exceeding 40KB limit per record or nested objects

**Solution**: Check metadata size and flatten nested structures

```java
// ❌ WRONG - nested objects
Map<String, Object> badMetadata = Map.of(
    "user", Map.of("name", "John") // Nested
);

// ✅ CORRECT - flat structure
Map<String, Object> goodMetadata = Map.of(
    "user_name", "John" // Flat
);
```

### Problem: Batch too large error

**Cause**: Exceeding batch size limits (96 for text, 1000 for vectors)

**Solution**: Reduce batch size

```java
// ✅ CORRECT - respect batch limits
int batchSize = 96; // For text records
for (int i = 0; i < records.size(); i += batchSize) {
    List<Vector> batch = records.subList(i, Math.min(i + batchSize, records.size()));
    // ... upsert batch
}
```

For other troubleshooting issues, see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md).

## Spring Boot Integration

```java
import io.pinecone.clients.Pinecone;
import io.pinecone.clients.Index;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PineconeService {
    private final Pinecone client;
    private final String indexName;

    public PineconeService(@Value("${pinecone.api.key}") String apiKey,
                          @Value("${pinecone.index.name:default-index}") String indexName) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("PINECONE_API_KEY required");
        }

        this.client = new Pinecone.Builder(apiKey).build();
        this.indexName = indexName;
    }

    public Index getIndex() {
        return client.getIndexConnection(indexName);
    }
}
```

## Application Properties

```properties
# application.properties
pinecone.api.key=${PINECONE_API_KEY}
pinecone.index.name=${PINECONE_INDEX:default-index}
```
