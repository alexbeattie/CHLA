# LangGraph and LangSmith Chat Guide

This guide explains the new LangGraph sidecar for KiNDD chat and how it relates
to the existing Strands agent. The goal is learning and evaluation first: the
LangGraph endpoint runs beside the existing agent instead of replacing it.

## What Was Added

- `maplocation/llm/langgraph_agent.py` contains the LangGraph sidecar.
- `POST /api/llm/langgraph-agent/` exposes the sidecar through Django.
- `POST /api/llm/langgraph-agent-stream/` exposes the SSE-compatible LangGraph
  streaming path.
- `POST /api/llm/langgraph-supervisor/` exposes the first multi-agent
  supervisor graph.
- `maplocation/llm/langsmith_evals.py` contains LangSmith dataset,
  experiment, and side-by-side comparison helpers.
- `locations/tests/test_langgraph_agent.py` verifies the sidecar without calling
  Bedrock.
- `maplocation/maplocation/env.py` loads local development settings from
  `maplocation/.env`.
- `maplocation/requirements.txt` now includes LangGraph, LangChain, LangChain
  AWS, LangSmith, and `python-dotenv` packages.

## Accounts and Setup

LangGraph does not require an account. It is a Python library for building
agent workflows as graphs.

LangSmith is optional. You need a LangSmith account only when you want hosted
traces, experiments, datasets, and eval results.

```bash
cd maplocation
pip install -r requirements.txt

# Optional: add these to maplocation/.env for LangSmith tracing
LANGSMITH_TRACING=true
LANGSMITH_API_KEY="your-langsmith-key"
LANGSMITH_PROJECT="kindd-langgraph-dev"
```

Do not commit LangSmith keys or any other credentials.

## Local `.env` Loading

Django now loads `maplocation/.env` automatically during settings import.

The loader uses `override=False`, which means already-exported shell, CI, or
deployment environment variables take precedence over `.env` values. This keeps
local setup convenient without making production depend on checked-out files.

For LangSmith, that means either of these work:

```bash
# Option A: put values in maplocation/.env
LANGSMITH_TRACING=true
LANGSMITH_API_KEY="your-langsmith-key"
LANGSMITH_PROJECT="kindd-langgraph-dev"
```

```bash
# Option B: export values in the shell before Django starts
export LANGSMITH_TRACING=true
export LANGSMITH_API_KEY="your-langsmith-key"
export LANGSMITH_PROJECT="kindd-langgraph-dev"
```

## Strands to LangGraph Translation

### Agent Runtime

In Strands, KiNDD uses:

```python
agent = Agent(model=model, system_prompt=system_prompt, tools=[...])
response = agent(enhanced_message)
```

In LangGraph, KiNDD uses:

```python
graph = create_kindd_langgraph()
result = graph.invoke({"messages": messages}, config=config)
```

The main difference is that Strands hides the agent loop inside `Agent`.
LangGraph makes the loop explicit as nodes and edges.

### Model

In Strands, the model is:

```python
BedrockModel(model_id="...", region_name="us-west-2")
```

In LangGraph, the model is a LangChain chat model:

```python
ChatBedrockConverse(model=CHAT_MODEL_ID, region_name="us-west-2")
```

LangGraph does not call Bedrock directly. LangChain provides the Bedrock chat
model wrapper, then LangGraph decides when that model runs.

### Tools

In Strands, tools are decorated with `@tool` from `strands` and passed to
`Agent`.

In LangGraph, tools are decorated with `@tool` from `langchain_core.tools`,
then passed to both:

- the chat model via `model.bind_tools(...)`
- the graph's `ToolNode(...)`

Binding tools tells the model what tools exist. `ToolNode` actually runs the
tool call the model requested.

The LangGraph sidecar now has parity with the Strands agent tool list:

| Existing Strands tool    | LangGraph tool           | Purpose                                                                                           |
| ------------------------ | ------------------------ | ------------------------------------------------------------------------------------------------- |
| `search_providers`       | `search_providers`       | Search provider records by user need, therapy type, and location.                                 |
| `get_regional_center`    | `get_regional_center`    | Resolve the Regional Center for a ZIP code.                                                       |
| `get_provider_details`   | `get_provider_details`   | Return fuller details for a named provider.                                                       |
| `find_provider_location` | `find_provider_location` | Return address, coordinates, and map action data for a named provider.                            |
| `check_eligibility`      | `check_eligibility`      | Explain likely Regional Center or therapy-service eligibility.                                    |
| `list_therapy_types`     | `list_therapy_types`     | Explain available therapy categories.                                                             |
| `clinical_search`        | `clinical_search`        | Search authoritative pediatric clinical sources through the existing Tavily path.                 |
| `autism_research`        | `autism_research`        | Query the Autism Research RAG client for literature, trials, gene evidence, and dataset metadata. |
| `web_search`             | `web_search`             | Search current web facts through the existing Tavily path.                                        |

The provider detail and location tools also handle the current `ProviderV2`
many-to-many Regional Center relationship while still tolerating the legacy
single `regional_center` attribute shape used in older agent code.

### Prompt And Messages

In Strands, the user turn is mostly one enhanced string:

```python
enhanced_message = build_agent_message(user_message, user_context)
```

In LangGraph, the input is an explicit list of messages:

```python
[
    SystemMessage(content=system_prompt),
    AIMessage(content="previous assistant turn"),
    HumanMessage(content="User context: ZIP: 90001\n\nFind providers"),
]
```

This is closer to how LangChain and LangSmith represent chat traces.

### Control Flow

In Strands, the tool loop is internal:

```text
Agent decides whether to call a tool, runs it, then continues.
```

In LangGraph, the loop is visible:

```text
START -> agent -> END
              -> tools -> agent -> ...
```

The `agent` node calls the model. The conditional edge checks whether the model
requested a tool. If it did, the graph goes to `tools`; otherwise it ends.

### Streaming

The current Strands streaming endpoint emits the completed agent answer as one
SSE chunk:

```text
data: {"type": "chunk", "content": "..."}
data: {"type": "done", "regional_center": "..."}
```

The LangGraph streaming endpoint mirrors that shape:

```text
data: {"type": "chunk", "content": "..."}
data: {"type": "done", "runtime": "langgraph"}
```

This gives the frontend an SSE-compatible path now while leaving room to switch
`stream_chat_with_langgraph_agent()` to token-level graph streaming later.

### Supervisor Graph

The supervisor graph is the first multi-agent version:

```text
START -> supervisor -> provider_agent  -> END
                    -> clinical_agent  -> END
                    -> research_agent  -> END
                    -> web_agent       -> END
```

The supervisor is deterministic for now. It routes based on the user's message:

- provider/resource navigation -> `provider_agent`
- medical symptoms, treatment, urgency -> `clinical_agent`
- studies, trials, genes, SFARI, evidence -> `research_agent`
- current people, policies, dates, leadership -> `web_agent`

Each specialist receives a short role instruction and emits its specialist name
into LangSmith metadata. This makes traces easy to read before introducing an
LLM-based router.

### Tracing

In the current Strands path, observability is mostly handled through the
existing Langfuse/OpenTelemetry helper.

In the LangGraph path, LangSmith tracing is mostly configuration:

```python
config = {
    "run_name": "kindd_langgraph_agent",
    "tags": ["kindd-langgraph", "agent", "sync"],
    "metadata": {"locale": locale},
}
graph.invoke(..., config=config)
```

When `LANGSMITH_TRACING=true` and `LANGSMITH_API_KEY` are set, LangSmith can
show the graph run, model call, tool calls, timing, inputs, and outputs.

## How To Read The New Code

Start in `maplocation/llm/langgraph_agent.py`.

1. `KiNDDGraphState` defines what moves through the graph. Right now it is just
   `messages`.
2. `LANGGRAPH_TOOLS` lists the tools the model can call.
3. `build_langgraph_messages()` turns the API request into explicit chat
   messages.
4. `create_kindd_langgraph()` builds the graph:
   - `agent` calls the model
   - `tools` runs tool calls
   - the conditional edge decides whether to end or loop
5. `chat_with_langgraph_agent()` is the public wrapper used by the Django view.
6. `create_kindd_supervisor_graph()` builds the first multi-agent graph.
7. `chat_with_langgraph_supervisor()` runs a turn through the supervisor graph.

## Testing The Sidecar

The focused test command is:

```bash
cd maplocation
python3 -m pytest locations/tests/test_langgraph_agent.py locations/tests/test_local_env.py -q
```

The LangGraph tests use a fake chat model. That is intentional: they verify
graph wiring, message construction, endpoint shape, and LangSmith metadata
without making a real Bedrock call. The local env test verifies `.env` values
load without overriding existing shell variables. The parity tests also verify
that all nine Strands tools are present in `LANGGRAPH_TOOLS` and that the
external-search tools route through existing backend clients rather than
calling external services during tests.

The eval harness tests are:

```bash
cd maplocation
python3 -m pytest locations/tests/test_langsmith_evals.py -q
```

## Trying The Endpoint

After starting Django, call the single-agent endpoint:

```bash
curl -X POST http://localhost:8000/api/llm/langgraph-agent/ \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What Regional Center serves 90001?",
    "context": {"zip_code": "90001"},
    "locale": "en"
  }'
```

The response shape mirrors the Strands endpoint:

```json
{
  "query": "What Regional Center serves 90001?",
  "answer": "...",
  "tools_used": ["get_regional_center"],
  "regional_center": null,
  "runtime": "langgraph"
}
```

The streaming-compatible endpoint is:

```bash
curl -N -X POST http://localhost:8000/api/llm/langgraph-agent-stream/ \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Find ABA providers near 90001",
    "context": {"zip_code": "90001"},
    "locale": "en"
  }'
```

The supervisor endpoint is:

```bash
curl -X POST http://localhost:8000/api/llm/langgraph-supervisor/ \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What does research say about early autism intervention?",
    "context": {},
    "locale": "en"
  }'
```

It returns the selected specialist:

```json
{
  "query": "What does research say about early autism intervention?",
  "answer": "...",
  "tools_used": [],
  "regional_center": null,
  "runtime": "langgraph-supervisor",
  "specialist": "research_agent"
}
```

## LangSmith Evals And Comparison

`maplocation/llm/langsmith_evals.py` defines a small starter regression set:

- urgent symptom escalation
- provider search grounding
- autism research citations
- current facts web search

Before running live experiments, start the Autism RAG API in a separate terminal:

```bash
cd /path/to/repo
source autism_rag/.venv/bin/activate
uvicorn autism_rag.api.server:app --host 127.0.0.1 --port 8000
```

Then verify Django can reach it:

```bash
cd maplocation
python3 manage.py shell -c "from llm.autism_research import check_autism_research_health; print(check_autism_research_health()['status'])"
```

For current-facts and clinical web-search cases, set `TAVILY_API_KEY` in your
shell or `maplocation/.env`. Without it, those tools intentionally return a
`web_search_unavailable` or `clinical_search_unavailable` result.

To build or update the LangSmith dataset:

```bash
cd maplocation
python3 manage.py shell -c "from llm.langsmith_evals import ensure_langsmith_dataset; ensure_langsmith_dataset()"
```

To run a LangSmith experiment for the LangGraph sidecar:

```bash
python3 manage.py shell -c "from llm.langsmith_evals import run_langsmith_experiment; run_langsmith_experiment(runtime='langgraph')"
```

To run the existing Strands path as a separate experiment:

```bash
python3 manage.py shell -c "from llm.langsmith_evals import run_langsmith_experiment; run_langsmith_experiment(runtime='strands')"
```

For local side-by-side comparison without creating LangSmith runs:

```bash
python3 manage.py shell -c "from llm.langsmith_evals import run_side_by_side_comparison; print(run_side_by_side_comparison())"
```

## Path To Multi-Agent Graphs

The codebase now has both a single-agent graph and a first supervisor graph.
The next version can make each specialist more independent:

- **Provider navigator node:** uses provider and Regional Center tools.
- **Clinical safety node:** handles medical caution and urgent symptoms.
- **Research node:** handles autism research and source-heavy answers.
- **Response editor node:** enforces iOS-safe Markdown before returning.

That would move from the current first supervisor graph to a more production
multi-agent system where each specialist has narrower tools and a response
editor enforces the final iOS Markdown contract.
