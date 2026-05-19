# Pinecone Python SDK Guide

> **Prerequisites**: See [PINECONE.md](./PINECONE.md) for universal concepts and setup.

This guide provides Python-specific patterns, examples, and best practices for the Pinecone SDK.

## 🚨 MANDATORY RULES - Read First

**⚠️ CRITICAL: These rules MUST be followed. Violations will cause runtime errors or data issues.**

1. **MUST use namespaces** - Every upsert, search, fetch, delete operation MUST specify a namespace
2. **MUST wait 10+ seconds** - After upserting records, MUST wait 10+ seconds before searching
3. **MUST match field_map** - Record field names MUST match the right side of `--field_map` used when creating index
4. **MUST respect batch limits** - Text records: MAX 96 per batch, Vector records: MAX 1000 per batch
5. **MUST use flat metadata** - No nested objects allowed, only flat key-value pairs
6. **MUST use `pinecone` package** - NOT `pinecone-client` (deprecated, causes errors)
7. **MUST verify before installing** - Check if SDK/CLI already installed before prompting installation

**Before proceeding with any operation, verify these rules are followed. See detailed sections below for implementation.**

## Installation & Setup

> **⚠️ IMPORTANT**: See [PINECONE.md](./PINECONE.md#-mandatory-always-use-latest-version) for the mandatory requirement to always use the latest version when creating projects.

### Virtual Environment Setup

> **⚠️ MANDATORY**: Before installing any dependencies, you MUST check if a virtual environment is already configured for the project. If no virtual environment is detected, you MUST create one.

**Virtual Environment Requirements:**

1. **Check for existing virtual environment:**

   - Look for activation scripts: `venv/bin/activate`, `.venv/bin/activate`, `env/bin/activate`, `virtualenv/bin/activate`
   - Check for `conda` environment files: `environment.yml`, `.conda/`
   - Check for `pipenv` indicators: `Pipfile`, `.venv/`
   - Check for `poetry` indicators: `poetry.lock`, `pyproject.toml` with `[tool.poetry]` section

2. **If no virtual environment is found:**

   - **Ask the user** which Python virtual environment framework they prefer:
     - `venv` (default, recommended) - Built into Python 3.3+
     - `virtualenv` - Third-party alternative
     - `conda` - For data science projects
     - `pipenv` - For Pipfile-based projects
     - `poetry` - For modern dependency management
   - **Default to `venv`** if the user doesn't specify a preference
   - **Create the virtual environment** using the chosen framework
   - **Provide activation instructions** for the user's shell

3. **Virtual environment creation examples:**

   **venv (default):**

   ```bash
   python3 -m venv venv
   # Activate: source venv/bin/activate (Unix/Mac) or venv\Scripts\activate (Windows)
   ```

   **virtualenv:**

   ```bash
   virtualenv venv
   # Activate: source venv/bin/activate (Unix/Mac) or venv\Scripts\activate (Windows)
   ```

   **conda:**

   ```bash
   conda create -n project-name python=3.11
   conda activate project-name
   ```

   **pipenv:**

   ```bash
   pipenv install
   pipenv shell
   ```

   **poetry:**

   ```bash
   poetry install
   poetry shell
   ```

4. **After creating/activating the virtual environment:**
   - Install dependencies within the activated virtual environment
   - Ensure all `pip install` commands run in the virtual environment context
   - Update `.gitignore` to exclude the virtual environment directory (e.g., `venv/`, `.venv/`, `env/`)

**Never install Python packages globally unless explicitly requested by the user.**

### Current API (2025)

```python
from pinecone import Pinecone
```

### Finding the Latest Version

**Check latest version on PyPI:**

- Browse: [https://pypi.org/project/pinecone/](https://pypi.org/project/pinecone/)
- Or check programmatically: `pip index versions pinecone`

**Install latest version:**

```bash
pip install pinecone
```

**Install specific version:**

```bash
pip install pinecone==7.3.0  # Replace with desired version
```

**Check installed version:**

```bash
pip show pinecone
```

**Upgrade to latest:**

```bash
pip install --upgrade pinecone
```

### Dependency Management

**⚠️ CRITICAL - Package Name**: Always use `pinecone` (NOT `pinecone-client`). The `pinecone-client` package is deprecated and will cause runtime errors.

**requirements.txt:**

```txt
pinecone
```

**pyproject.toml (PEP 621):**

```toml
[project]
dependencies = ["pinecone"]
```

**pyproject.toml (Poetry):**

```toml
[tool.poetry.dependencies]
pinecone = "^7.0.0"
```

**setup.py:**

```python
install_requires=["pinecone"]
```

**Pipfile (pipenv):**

```toml
[packages]
pinecone = "*"
```

### Environment Configuration

**⚠️ Use `.env` files (see [PINECONE.md](./PINECONE.md#-environment-variables--security-best-practices)).**

```bash
pip install python-dotenv
```

```python
import os
from dotenv import load_dotenv
from pinecone import Pinecone

load_dotenv()  # Loads .env file
api_key = os.getenv("PINECONE_API_KEY")
if not api_key:
    raise ValueError("PINECONE_API_KEY required")
pc = Pinecone(api_key=api_key)
```

### Production Client Class

```python
import os
from dotenv import load_dotenv
from pinecone import Pinecone

load_dotenv()

class PineconeClient:
    def __init__(self):
        self.api_key = os.getenv("PINECONE_API_KEY")
        if not self.api_key:
            raise ValueError("PINECONE_API_KEY required")
        self.pc = Pinecone(api_key=self.api_key)
        self.index_name = os.getenv("PINECONE_INDEX", "default-index")
    def get_index(self):
        return self.pc.Index(self.index_name)
```

## Quickstarts

### Quick Test

Complete setup prerequisites first, then:

**Before running commands, ensure environment is ready:**

```bash
# Load API key into environment (CLI will use this)
source .env

# Export for CLI use (required for quickstart)
export PINECONE_API_KEY

# Activate virtual environment
source venv/bin/activate
```

1. **Create index with CLI:**

```bash
pc index create -n agentic-quickstart-test -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content

# Wait for index to be ready
sleep 5
```

2. **Upsert sample data:**

> **Sample Data**: Use the sample data from [PINECONE-quickstart.md](./PINECONE-quickstart.md#sample-data-use-in-all-languages). Convert JSON format to Python dictionaries.

```python
from pinecone import Pinecone
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Initialize Pinecone client
api_key = os.getenv("PINECONE_API_KEY")
if not api_key:
    raise ValueError("PINECONE_API_KEY environment variable not set")

pc = Pinecone(api_key=api_key)

# Sample data (see quickstart guide for full list)
records = [
    { "_id": "rec1", "content": "The Eiffel Tower was completed in 1889 and stands in Paris, France.", "category": "history" },
    { "_id": "rec2", "content": "Photosynthesis allows plants to convert sunlight into energy.", "category": "science" },
    # ... (use all 12 records from quickstart guide)
]

# Target the index
dense_index = pc.Index("agentic-quickstart-test")

# Upsert the records into a namespace
dense_index.upsert_records("example-namespace", records)
```

3. **Search with reranking:**

```python
import time

# Wait for the upserted vectors to be indexed
time.sleep(10)

# View stats for the index
stats = dense_index.describe_index_stats()
print(stats)

# Define the query (see quickstart guide for test query)
query = "Famous historical structures and monuments"

# Search the dense index and rerank results
reranked_results = dense_index.search(
    namespace="example-namespace",
    query={
        "top_k": 10,
        "inputs": {
            'text': query
        }
    },
    rerank={
        "model": "bge-reranker-v2-m3",
        "top_n": 10,
        "rank_fields": ["content"]
    }
)

# Print the reranked results
for hit in reranked_results['result']['hits']:
    print(f"id: {hit['_id']}, score: {round(hit['_score'], 2)}, text: {hit['fields']['content']}, category: {hit['fields']['category']}")

# Access search results
# IMPORTANT: With reranking, use dict-style access for hit object
for hit in reranked_results.result.hits:
    doc_id = hit["_id"]              # Dict access for id
    score = hit["_score"]            # Dict access for score
    content = hit.fields["content"]  # hit.fields is also a dict
    metadata = hit.fields.get("metadata_field", "")  # Use .get() for optional fields
```

## Data Operations

### Upserting Records

**⚠️ Before upserting, verify:**

1. Namespace is specified (MANDATORY)
2. Field names match `--field_map` used when creating index (MANDATORY)
3. Batch size ≤ 96 records for text, ≤ 1000 for vectors (MANDATORY)
4. Metadata is flat (no nested objects) (MANDATORY)

```python
# Indexes with integrated embeddings
records = [
    {
        "_id": "doc1",
        "content": "Your text content here",  # must match field_map
        "category": "documentation",
        "created_at": "2025-01-01",
        "priority": "high"
    }
]

# Always use namespaces
namespace = "user_123"  # e.g., "knowledge_base", "session_456"
index.upsert_records(namespace, records)
```

### Updating Records

```python
# Update existing records (use same upsert operation with existing IDs)
updated_records = [
    {
        "_id": "doc1",  # existing record ID
        "content": "Updated content here",
        "category": "updated_docs",  # can change metadata
        "last_modified": "2025-01-15"
    }
]

# Partial updates - only changed fields need to be included
partial_update = [
    {
        "_id": "doc1",
        "category": "urgent",  # only updating category field
        "priority": "high"     # adding new field
    }
]

index.upsert_records(namespace, updated_records)
```

### Fetching Records

```python
# Fetch single record
result = index.fetch(namespace=namespace, ids=["doc1"])
if result.records:
    record = result.records["doc1"]
    print(f"Content: {record.fields.content}")
    print(f"Metadata: {record.metadata}")

# Fetch multiple records
result = index.fetch(namespace=namespace, ids=["doc1", "doc2", "doc3"])
for record_id, record in result.records.items():
    print(f"ID: {record_id}, Content: {record.fields.content}")

# Fetch with error handling
def safe_fetch(index, namespace, ids):
    try:
        result = index.fetch(namespace=namespace, ids=ids)
        return result.records
    except Exception as e:
        print(f"Fetch failed: {e}")
        return {}
```

### Listing Record IDs

```python
# List all record IDs (paginated)
def list_all_ids(index, namespace, prefix=None):
    """List all record IDs with optional prefix filter"""
    all_ids = []
    pagination_token = None

    while True:
        result = index.list(
            namespace=namespace,
            prefix=prefix,  # filter by ID prefix
            limit=1000,
            pagination_token=pagination_token
        )

        all_ids.extend([record.id for record in result.records])

        if not result.pagination or not result.pagination.next:
            break
        pagination_token = result.pagination.next

    return all_ids

# Usage
all_record_ids = list_all_ids(index, "user_123")
docs_only = list_all_ids(index, "user_123", prefix="doc_")
```

## Search Operations

### Semantic Search with Reranking (Best Practice)

**⚠️ Before searching, verify:**

1. Namespace is specified (MANDATORY)
2. Wait 10+ seconds after upserting before searching (MANDATORY)
3. Field name in query matches `--field_map` (MANDATORY)

**Note**: Reranking is a best practice for production quality results. Quickstarts include reranking to demonstrate usage.

```python
def search_with_rerank(index, namespace, query_text, top_k=5):
    """Best practice: Use reranking for production quality results. This pattern is shown in quickstarts."""
    results = index.search(
        namespace=namespace,
        query={
            "top_k": top_k * 2,  # more candidates for reranking
            "inputs": {
                "text": query_text  # must match index config
            }
        },
        rerank={
            "model": "bge-reranker-v2-m3",
            "top_n": top_k,
            "rank_fields": ["content"]
        }
    )
    return results
```

### Lexical Search

```python
# Basic lexical search
def lexical_search(index, namespace, query_text, top_k=5):
    """Keyword-based search using sparse embeddings"""
    results = index.search(
        namespace=namespace,
        query={
            "inputs": {"text": query_text},
            "top_k": top_k
        }
    )
    return results

# Lexical search with required terms
def lexical_search_with_required_terms(index, namespace, query_text, required_terms, top_k=5):
    """Results must contain specific required words"""
    results = index.search(
        namespace=namespace,
        query={
            "inputs": {"text": query_text},
            "top_k": top_k,
            "match_terms": required_terms  # results must contain these terms
        }
    )
    return results

# Lexical search with reranking
def lexical_search_with_rerank(index, namespace, query_text, top_k=5):
    """Lexical search with reranking for better relevance"""
    results = index.search(
        namespace=namespace,
        query={
            "inputs": {"text": query_text},
            "top_k": top_k * 2  # get more candidates for reranking
        },
        rerank={
            "model": "bge-reranker-v2-m3",
            "top_n": top_k,
            "rank_fields": ["content"]
        }
    )
    return results

# Example usage
search_results = lexical_search_with_required_terms(
    index,
    "knowledge_base",
    "machine learning algorithms neural networks",
    required_terms=["algorithms"]  # must contain "algorithms"
)
```

### Metadata Filtering

```python
# Simple filters
filter_criteria = {"category": "documentation"}

# Complex filters
filter_criteria = {
    "$and": [
        {"category": {"$in": ["docs", "tutorial"]}},
        {"priority": {"$ne": "low"}},
        {"created_at": {"$gte": "2025-01-01"}}
    ]
}

results = index.search(
    namespace=namespace,
    query={
        "top_k": 10,
        "inputs": {"text": query_text},
        "filter": filter_criteria  # Filter goes inside query object
    }
)

# Search without filters - omit the "filter" key
results = index.search(
    namespace=namespace,
    query={
        "top_k": 10,
        "inputs": {"text": query_text}
        # No filter key at all
    }
)

# Dynamic filter pattern - conditionally add filter to query dict
query_dict = {
    "top_k": 10,
    "inputs": {"text": "query"}
}
if has_filters:  # Only add filter if it exists
    query_dict["filter"] = {"category": {"$eq": "docs"}}

results = index.search(namespace=namespace, query=query_dict, rerank={...})
```

## Error Handling (Production)

### Retry Pattern

```python
import time
from pinecone.exceptions import PineconeException

def exponential_backoff_retry(func, max_retries=5):
    for attempt in range(max_retries):
        try:
            return func()
        except PineconeException as e:
            status_code = getattr(e, 'status', None)

            # Only retry transient errors
            if status_code and (status_code >= 500 or status_code == 429):
                if attempt < max_retries - 1:
                    delay = min(2 ** attempt, 60)  # Exponential backoff, cap at 60s
                    time.sleep(delay)
                else:
                    raise
            else:
                raise  # Don't retry client errors (4xx except 429)

# Usage
exponential_backoff_retry(lambda: index.upsert_records(namespace, records))
```

## Batch Processing

```python
def batch_upsert(index, namespace, records, batch_size=96):
    for i in range(0, len(records), batch_size):
        batch = records[i:i + batch_size]
        exponential_backoff_retry(
            lambda: index.upsert_records(namespace, batch)
        )
        time.sleep(0.1)  # Rate limiting
```

## Common Operations

### Index Management

```python
# Check if index exists (in application startup)
if pc.has_index("my-index"):
    index = pc.Index("my-index")

# Get stats (for monitoring/metrics)
stats = index.describe_index_stats()
print(f"Total vectors: {stats.total_vector_count}")
print(f"Namespaces: {list(stats.namespaces.keys())}")
```

### Data Operations

```python
# Fetch records
result = index.fetch(namespace="ns", ids=["doc1", "doc2"])
for record_id, record in result.records.items():
    print(f"{record_id}: {record.fields.content}")

# List all IDs (paginated)
all_ids = []
pagination_token = None
while True:
    result = index.list(namespace="ns", limit=1000, pagination_token=pagination_token)
    all_ids.extend([record.id for record in result.records])
    if not result.pagination or not result.pagination.next:
        break
    pagination_token = result.pagination.next

# Delete records
index.delete(namespace="ns", ids=["doc1", "doc2"])

# Delete entire namespace
index.delete(namespace="ns", delete_all=True)
```

## Python-Specific Patterns

### Namespace Strategy

```python
# Multi-user apps
namespace = f"user_{user_id}"

# Session-based
namespace = f"session_{session_id}"

# Content-based
namespace = "knowledge_base"
namespace = "chat_history"
```

### Environment Configuration

```python
import os
from pinecone import Pinecone

class PineconeClient:
    def __init__(self):
        self.api_key = os.getenv("PINECONE_API_KEY")
        if not self.api_key:
            raise ValueError("PINECONE_API_KEY required")
        self.pc = Pinecone(api_key=self.api_key)
        self.index_name = os.getenv("PINECONE_INDEX", "default-index")

    def get_index(self):
        return self.pc.Index(self.index_name)
```

## Use Case Examples

### Semantic Search System

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-search -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```python
from pinecone import Pinecone
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Initialize Pinecone client
pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
index = pc.Index("agentic-quickstart-search")

def build_semantic_search_system():
    """Build a semantic search system with reranking and filtering"""

    # Create search function
    def search_knowledge_base(query, category_filter=None, top_k=5):
        query_dict = {
            "top_k": top_k * 2,
            "inputs": {"text": query}
        }

        if category_filter:
            query_dict["filter"] = {"category": {"$eq": category_filter}}

        results = index.search(
            namespace="knowledge_base",
            query=query_dict,
            rerank={
                "model": "bge-reranker-v2-m3",
                "top_n": top_k,
                "rank_fields": ["content"]
            }
        )

        return results

    return search_knowledge_base
```

### Multi-Tenant RAG System

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-rag -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```python
from pinecone import Pinecone
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Initialize Pinecone client
pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
index = pc.Index("agentic-quickstart-rag")

def build_rag_system():
    """Build a multi-tenant RAG system with namespace isolation"""

    def rag_query(user_id, query, top_k=5):
        # Ensure namespace isolation
        namespace = f"user_{user_id}"

        # Search only user's namespace
        results = index.search(
            namespace=namespace,
            query={
                "top_k": top_k * 2,
                "inputs": {"text": query}
            },
            rerank={
                "model": "bge-reranker-v2-m3",
                "top_n": top_k,
                "rank_fields": ["content"]
            }
        )

        # Construct context for LLM
        context = "\n".join([
            f"Document {hit['_id']}: {hit.fields['content']}"
            for hit in results.result.hits
        ])

        return context

    return rag_query
```

### Recommendation Engine

**Create index with CLI:**

```bash
pc index create -n agentic-quickstart-recommendations -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

**Implementation:**

```python
from pinecone import Pinecone
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Initialize Pinecone client
pc = Pinecone(api_key=os.getenv("PINECONE_API_KEY"))
index = pc.Index("agentic-quickstart-recommendations")

def build_recommendation_engine():
    """Build a recommendation engine with filtering and diversity"""

    def get_recommendations(product_id, category_filter=None, top_k=10):
        # Get similar products
        results = index.search(
            namespace="products",
            query={
                "top_k": top_k * 2,
                "inputs": {"text": f"product_{product_id}"}
            },
            rerank={
                "model": "bge-reranker-v2-m3",
                "top_n": top_k,
                "rank_fields": ["content"]
            }
        )

        # Apply category filtering if specified
        if category_filter:
            filtered_results = [
                hit for hit in results.result.hits
                if hit.fields.get("category") == category_filter
            ]
            return filtered_results[:top_k]

        return results.result.hits

    return get_recommendations
```
