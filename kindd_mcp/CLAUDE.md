# kindd_mcp

Standalone MCP (Model Context Protocol) server exposing read-only KiNDD provider / Regional Center data over Streamable HTTP, backed by the public `api.kinddhelp.com` REST API (no DB access, no writes).

## Stack

- Python, FastMCP (Streamable HTTP transport), `requests`, `boto3` (Secrets Manager token fetch at container startup), official `mcp` client for smoke testing
- Deploys as a Docker/ASGI app (`kindd_mcp.server:app`) on Elastic Beanstalk

## Commands

| Task | Command |
| --- | --- |
| Setup | `cd kindd_mcp && python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt` |
| Run (dev) | `python3 -m kindd_mcp.server` (from repo root, venv active) |
| Run (ASGI) | `uvicorn kindd_mcp.server:app --host 0.0.0.0 --port 8800 --workers 4` |
| Test | `python3 -m pytest kindd_mcp/tests/ -q` (from repo root) |

Defer to the repo root `AGENTS.md` for working agreement; log changes in root `FIXES.md`.
