# Pinecone Quickstart Guide

> **Prerequisites**: See [PINECONE.md](./PINECONE.md) for universal concepts and setup.

> **Important for all quickstarts**: Execute all steps completely. Keep setup minimal (directories, virtual environments, dependencies only). Do not expect the user to satisfy any prerequisites except providing API keys. For summaries, use only README.md and SUMMARY.md.

This guide provides step-by-step instructions for building your first Pinecone applications. Choose your programming language below to see examples.

## Choosing Your Quickstart

⚠️ **MANDATORY: When you are asked to help get started with Pinecone:**

1. **Always ask: What do you want to build?**

   - Ask the user to choose one of these options:
     - **Quick Test**: Create an index, upsert data, and perform semantic search. (Best for first-time users)
     - **Search**: Build a semantic search system that returns ranked results from your knowledge base. This pattern is ideal for search interfaces where users need a list of relevant documents with confidence scores.
     - **RAG**: Build a multi-tenant RAG (Retrieval-Augmented Generation) system that retrieves relevant context per tenant and feeds it to an LLM to generate answers. Each tenant (organization, workspace, or user) has isolated data stored in separate Pinecone namespaces. This pattern is ideal for knowledge bases, customer support platforms, and collaborative workspaces.
     - **Recommendations**: Build a recommendation engine that suggests similar items based on semantic similarity. This pattern is ideal for e-commerce, content platforms, and user personalization systems.

2. **Determine programming language:** Follow the language selection instructions in [PINECONE.md](./PINECONE.md#language-selection).

3. **Proceed with the appropriate language-specific pattern** based on the detected/confirmed language and the selected use case.

## Language-Specific Quickstarts

- **Python**: See [PINECONE-python.md](./PINECONE-python.md#quickstarts)
- **TypeScript/Node.js**: See [PINECONE-typescript.md](./PINECONE-typescript.md#quickstarts)
- **Go**: See [PINECONE-go.md](./PINECONE-go.md#quickstarts)
- **Java**: See [PINECONE-java.md](./PINECONE-java.md#quickstarts)

---

## Quickstart Options

### 1. Quick Test

**Create an index, upsert data, and perform semantic search.**

- Best for: First-time users, learning the basics
- Time: 10-15 minutes
- Skills: Basic familiarity with your chosen language

### 2. Semantic Search System

**Build a search system that returns ranked results from your knowledge base.**

- Best for: Search interfaces, document retrieval
- Use case: Users need a list of relevant documents with confidence scores
- Skills: Intermediate programming

### 3. Multi-Tenant RAG System

**Build a RAG system with namespace isolation for multiple tenants.**

- Best for: Knowledge bases, customer support, collaborative workspaces
- Use case: Each tenant needs isolated access to their data
- Skills: Intermediate programming, familiarity with LLMs
- Requirements: LLM API key or access (OpenAI, Anthropic, Groq, or local via Ollama)

### 4. Recommendation Engine

**Build a recommendation engine using semantic similarity.**

- Best for: E-commerce, content platforms, personalization
- Use case: Suggest similar items to users
- Skills: Intermediate programming

---

## Setup Prerequisites (All Quickstarts)

Before starting any quickstart, complete these steps:

> **⚠️ Important**: Check if prerequisites are already installed before prompting users to install them:
>
> - **CLI**: Run `pc version` to check if installed
> - **SDK**: Check if package files exist or run language-specific commands to verify

### 1. Install Pinecone CLI

**First, check if already installed:**

```bash
pc version
```

Only proceed with installation if the above command fails.

**macOS:**

```bash
brew tap pinecone-io/tap
brew install pinecone-io/tap/pinecone
brew update && brew upgrade pinecone
```

**Other platforms:**
Download from [GitHub Releases](https://github.com/pinecone-io/cli/releases)

**Verify installation:**

```bash
pc version
```

### 2. Install SDK (Choose Your Language)

**First, check if SDK is already installed:**

**Python:**

```bash
pip show pinecone
```

**TypeScript/Node.js:**

```bash
npm list @pinecone-database/pinecone
# or
yarn list --pattern @pinecone-database/pinecone
```

**Go:**

```bash
go list -m github.com/pinecone-io/go-pinecone/pinecone
```

**Java:**

```bash
mvn dependency:tree | grep pinecone-client
# or
gradle dependencies | grep pinecone-client
```

**If not installed, install with:**

**Python:**

```bash
pip install pinecone
```

**TypeScript/Node.js:**

```bash
npm install @pinecone-database/pinecone
# or
yarn add @pinecone-database/pinecone
```

**Go:**

```bash
go get github.com/pinecone-io/go-pinecone/pinecone@latest
```

**Java (Maven):**

```xml
<dependency>
    <groupId>io.pinecone</groupId>
    <artifactId>pinecone-client</artifactId>
    <version>5.1.0</version>
</dependency>
```

**Java (Gradle):**

```gradle
implementation 'io.pinecone:pinecone-client:5.1.0'
```

### 3. Configure API Key

**⚠️ Use `.env` files (see [PINECONE.md](./PINECONE.md#-environment-variables--security-best-practices) for details).**

**Recommended Workflow (CLI + SDK):**

1. **Create `.env` file** in your project root:

```bash
PINECONE_API_KEY=your-api-key-here
```

2. **Load into your shell:**

```bash
source .env
```

3. **Export for CLI use** (required for quickstart):

```bash
export PINECONE_API_KEY
```

4. **Activate virtual environment** (if using Python):

```bash
source venv/bin/activate
```

Now both CLI commands (`pc index create ...`) and Python scripts (using `python-dotenv`) will use the same API key.

**Alternative: Persistent CLI Authentication**

If you prefer CLI authentication that persists across shell sessions:

```bash
pc auth configure --api-key your-api-key-here
```

Get your API key from: [https://app.pinecone.io/](https://app.pinecone.io/)

### 4. Optional: LLM API Key (For RAG Quickstart Only)

Add to `.env`: `OPENAI_API_KEY=...`, `ANTHROPIC_API_KEY=...`, or `GROQ_API_KEY=...`. Or use local LLM (Ollama) - no key needed.

**Note**: Any LLM can be used, including local models. For local models, you may need to adapt the API calls in the examples.

---

## Quick Test

**Objective**: Create an index, upsert data, and perform semantic search.

### Steps

1. **Create an index** with integrated embeddings using CLI
2. **Wait for index** to be ready (sleep 5 seconds)
3. **Prepare sample data** from different domains (history, science, art, etc.)
4. **Upsert data** into the index
5. **Search** for semantically similar documents
6. **Rerank results** for better accuracy

### Sample Data (Use in All Languages)

The following sample data should be used for the Quick Test across all languages:

```json
[
  {
    "_id": "rec1",
    "content": "The Eiffel Tower was completed in 1889 and stands in Paris, France.",
    "category": "history"
  },
  {
    "_id": "rec2",
    "content": "Photosynthesis allows plants to convert sunlight into energy.",
    "category": "science"
  },
  {
    "_id": "rec5",
    "content": "Shakespeare wrote many famous plays, including Hamlet and Macbeth.",
    "category": "literature"
  },
  {
    "_id": "rec7",
    "content": "The Great Wall of China was built to protect against invasions.",
    "category": "history"
  },
  {
    "_id": "rec15",
    "content": "Leonardo da Vinci painted the Mona Lisa.",
    "category": "art"
  },
  {
    "_id": "rec17",
    "content": "The Pyramids of Giza are among the Seven Wonders of the Ancient World.",
    "category": "history"
  },
  {
    "_id": "rec21",
    "content": "The Statue of Liberty was a gift from France to the United States.",
    "category": "history"
  },
  {
    "_id": "rec26",
    "content": "Rome was once the center of a vast empire.",
    "category": "history"
  },
  {
    "_id": "rec33",
    "content": "The violin is a string instrument commonly used in orchestras.",
    "category": "music"
  },
  {
    "_id": "rec38",
    "content": "The Taj Mahal is a mausoleum built by Emperor Shah Jahan.",
    "category": "history"
  },
  {
    "_id": "rec48",
    "content": "Vincent van Gogh painted Starry Night.",
    "category": "art"
  },
  {
    "_id": "rec50",
    "content": "Renewable energy sources include wind, solar, and hydroelectric power.",
    "category": "energy"
  }
]
```

**Test Query**: "Famous historical structures and monuments"

### Language Examples

- **Python**: See [PINECONE-python.md - Quick Test](./PINECONE-python.md#quick-test)
- **TypeScript**: See [PINECONE-typescript.md - Quick Test](./PINECONE-typescript.md#quick-test)
- **Go**: See [PINECONE-go.md - Quick Test](./PINECONE-go.md#quick-test)
- **Java**: See [PINECONE-java.md - Quick Test](./PINECONE-java.md#quick-test)

### Key Concepts Learned

- Index creation with integrated embeddings
- Namespace usage for data isolation
- Semantic search basics
- Reranking for improved results

---

## Build a Semantic Search System

**Objective**: Build a production-ready search system for your knowledge base.

### Steps

1. **Create an index** with integrated embeddings using CLI:

```bash
pc index create -n agentic-quickstart-search -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

2. **Create documents** with rich metadata (20+ documents recommended)
3. **Store documents** in Pinecone using proper namespaces
4. **Build search function** with:
   - Semantic search
   - Reranking with `bge-reranker-v2-m3` model
   - Metadata filtering
   - Error handling
5. **Test with sample queries**
6. **Review results** and iterate

### Production Patterns

- Always use shielded namespaces
- Use reranking for best results (included in quickstart examples)
- Implement exponential backoff retry
- Handle edge cases gracefully

### Language Examples

See the "Use Case Examples" section in your language guide:

- **Python**: [PINECONE-python.md - Semantic Search](./PINECONE-python.md#semantic-search-system)
- **TypeScript**: [PINECONE-typescript.md - Semantic Search](./PINECONE-typescript.md#semantic-search-system)
- **Go**: [PINECONE-go.md - Semantic Search](./PINECONE-go.md#semantic-search-system)
- **Java**: [PINECONE-java.md - Semantic Search](./PINECONE-java.md#semantic-search-system)

---

## Build a Multi-Tenant RAG System

**Objective**: Build a RAG system with namespace isolation for multiple tenants.

### Steps

1. **Create an index** with integrated embeddings using CLI:

```bash
pc index create -n agentic-quickstart-rag -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

2. **Create tenant data** (emails, documents, etc.) with metadata
3. **Store data per tenant** using separate namespaces
4. **Build RAG function** that:
   - Enforces namespace isolation
   - Searches only specified user's namespace
   - Retrieves and reranks results
   - Constructs LLM prompt
   - Returns answer with citations
5. **Test with multi-tenant queries**
6. **Verify data isolation**

### Key Features

- **Namespace isolation**: Each tenant's data is completely isolated
- **LLM integration**: Works with any LLM (OpenAI, Anthropic, models provided by Groq, local models via Ollama, etc.)
- **Metadata citation**: Includes source references
- **Context window management**: Handles large result sets intelligently

### Language Examples

See the "Use Case Examples" section in your language guide:

- **Python**: [PINECONE-python.md - RAG System](./PINECONE-python.md#multi-tenant-rag-system)
- **TypeScript**: [PINECONE-typescript.md - RAG System](./PINECONE-typescript.md#multi-tenant-rag-system)
- **Go**: [PINECONE-go.md - RAG System](./PINECONE-go.md#multi-tenant-rag-system)
- **Java**: [PINECONE-java.md - RAG System](./PINECONE-java.md#multi-tenant-rag-system)

---

## Build a Recommendation Engine

**Objective**: Build a recommendation engine using semantic similarity.

### Steps

1. **Create an index** with integrated embeddings using CLI:

```bash
pc index create -n agentic-quickstart-recommendations -m cosine -c aws -r us-east-1 --model llama-text-embed-v2 --field_map text=content
```

2. **Create product listings** with rich metadata (20+ products)
3. **Store products** in Pinecone
4. **Build recommendation function** that:
   - Finds similar items using vector similarity
   - Filters by category, price, etc.
   - Implements diversity strategies
   - Returns formatted recommendations
5. **Test appearance recommendations**
6. **Review accuracy**

### Key Features

- **Semantic similarity**: Finds similar products based on meaning
- **Metadata filtering**: Can filter by any attribute
- **Diversity strategies**: Prevents category clustering
- **Multi-item support**: Can aggregate preferences

### Language Examples

See the "Use Case Examples" section in your language guide:

- **Python**: [PINECONE-python.md - Recommendations](./PINECONE-python.md#recommendation-engine)
- **TypeScript**: [PINECONE-typescript.md - Recommendations](./PINECONE-typescript.md#recommendation-engine)
- **Go**: [PINECONE-go.md - Recommendations](./PINECONE-go.md#recommendation-engine)
- **Java**: [PINECONE-java.md - Recommendations](./PINECONE-java.md#recommendation-engine)

---

## Common CLI Commands

All quickstarts use these CLI commands:

### Create Index

```bash
pc index create -n <index-name> -m cosine -c aws -r us-east-1 \
  --model llama-text-embed-v2 \
  --field_map text=content
```

### List Indexes

```bash
pc index list
```

### Describe Index

```bash
pc index describe --name <index-name>
```

### Delete Index

```bash
pc index delete --name <index-name>
```

See [PINECONE-cli.md](./PINECONE-cli.md) for complete CLI reference.

---

## Next Steps

After completing a quickstart:

1. **Explore advanced features**: [PINECONE.md](./PINECONE.md#official-documentation-resources)
2. **Learn data operations**: See language-specific sections on upsert, fetch, delete
3. **Master search patterns**: Understand filtering, reranking, hybrid search
4. **Production best practices**: Error handling, retry logic, monitoring

## Troubleshooting

For comprehensive troubleshooting guidance, see [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md).

### Quick Reference

| Issue                            | Solution                                 | See Guide                                                    |
| -------------------------------- | ---------------------------------------- | ------------------------------------------------------------ |
| `ModuleNotFoundError` or similar | Reinstall SDK with latest version        | [Language-specific guides](#language-specific-quickstarts)   |
| Search returns no results        | Check namespace, wait ~5s after upsert   | [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md) |
| 404 on index operations          | Verify index exists with `pc index list` | [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md) |
| Rate limit errors                | Implement exponential backoff retry      | [PINECONE-troubleshooting.md](./PINECONE-troubleshooting.md) |

---

**Remember**: Always use namespaces, use reranking for best results (shown in examples), always handle errors with retry logic.
