# KiNDD MCP Server

A standalone [Model Context Protocol](https://modelcontextprotocol.io/) server
that exposes read-only KiNDD Resource Navigator data over Streamable HTTP, so
AI clients (Claude Desktop, Cursor, ChatGPT, or KiNDD's own agent) can query
providers and Regional Centers directly.

This service is intentionally decoupled from the Django backend
(`maplocation/`): it is a thin wrapper around the existing **public** REST
API at `https://api.kinddhelp.com/api`, so it has no database access and no
write capability.

## Tools

- `search_providers` — search KiNDD healthcare providers by ZIP/location,
  insurance, diagnosis, age, and specialization. Returns a compact, trimmed
  result (`total_matches`, `returned`, `providers`).
- `find_regional_center_by_zip` — find the California Regional Center that
  serves a given ZIP code.

Both tools are public/read-only, matching the existing public KiNDD API.

## Local Setup

```bash
cd kindd_mcp
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

## Run (development)

Run from the repository root so the relative imports resolve correctly:

```bash
source kindd_mcp/.venv/bin/activate
python3 -m kindd_mcp.server
```

The MCP endpoint is now available at `http://localhost:8800/mcp`, with a
plain health check at `http://localhost:8800/health`.

## Run (production / ASGI)

```bash
source kindd_mcp/.venv/bin/activate
uvicorn kindd_mcp.server:app --host 0.0.0.0 --port 8800 --workers 4
```

## Pointing At A Different KiNDD API

By default the server calls the production API. To point at a local Django
backend instead, set in `.env`:

```bash
KINDD_API_BASE_URL=http://127.0.0.1:8000/api
```

## Testing

```bash
cd ..  # repo root
source kindd_mcp/.venv/bin/activate
python3 -m pytest kindd_mcp/tests/ -q
```

Tests are split into:

- `tests/test_kindd_client.py` — the thin HTTP client, with a fake
  `requests`-like session (no network calls).
- `tests/test_tools.py` — the LLM-facing formatting/compaction layer, with a
  fake client (no HTTP, no FastMCP).

## Manual Smoke Test

With the server running locally, use the official MCP client or the
[MCP Inspector](https://github.com/modelcontextprotocol/inspector). If
`KINDD_MCP_AUTH_TOKEN` is set, the Inspector's UI has a field for the
bearer token (or pass `Authorization: Bearer <token>` as a custom header).

```bash
npx @modelcontextprotocol/inspector http://localhost:8800/mcp
```

Or call a tool directly with the Python MCP client:

```python
import asyncio
from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client


async def main():
    async with streamablehttp_client("http://127.0.0.1:8800/mcp") as (read, write, _):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool(
                "find_regional_center_by_zip", {"zip_code": "90001"}
            )
            print(result.structuredContent)


asyncio.run(main())
```

## Connecting From Claude Desktop / Cursor

Add a remote MCP server pointing at the deployed URL, including the bearer
token from [Authentication](#authentication):

```json
{
  "mcpServers": {
    "kindd": {
      "url": "https://mcp.kinddhelp.org/mcp",
      "headers": {
        "Authorization": "Bearer <your-token>"
      }
    }
  }
}
```

## Docker Image

This server is built to run as an ASGI app (`kindd_mcp.server:app`). A
`Dockerfile` is included, with the same build-context convention as
`maplocation/Dockerfile`: build from **inside `kindd_mcp/`**, not the repo
root.

```bash
cd kindd_mcp
docker build -t kindd-mcp .
docker run -p 8800:8800 --env-file .env kindd-mcp
```

See [Deployment](#deployment) below for the concrete Elastic Beanstalk
runbook used to actually run this in production.

## Authentication

This server requires a shared-secret bearer token once it's reachable
outside localhost. Clients send `Authorization: Bearer <token>`.

- Set `KINDD_MCP_AUTH_TOKEN` to enable auth. Leave it unset for local
  development only (the server then runs unauthenticated, logged clearly
  by `build_auth_provider` returning `None`).
- This intentionally is **not** FastMCP's `StaticTokenVerifier` (its own
  docs say never use it in production) and **not** full OAuth/JWT (overkill
  for a handful of trusted clients). It's a single secret, compared with
  `hmac.compare_digest`, loaded from the environment — see `auth.py`.
- Generate a strong token:

  ```bash
  python3 -c "import secrets; print(secrets.token_urlsafe(32))"
  ```

- In production, prefer storing it in AWS Secrets Manager
  (`kindd/prod/mcp-auth-token`) over a plain EB environment property — see
  `docker-entrypoint.sh`, which fetches it from there if
  `KINDD_MCP_AUTH_TOKEN` isn't already set.
- Rotate by updating the secret/environment property and restarting the
  environment; no code change needed.

## Deployment

This mirrors `maplocation`'s existing Elastic Beanstalk Docker setup
(same AWS account, region `us-west-2`, profile `personal`), as its own EB
**application** (`kindd-mcp`) so it deploys and scales independently of the
Django API.

Domain: this is new infrastructure, so it uses the canonical `kinddhelp.org`
domain (per `docs/SEO_ORG_DOMAIN_CUTOVER.md`), not the legacy
`api.kinddhelp.com` infra identifier.

**Status: live** at `https://mcp.kinddhelp.org`.

- EB application: `kindd-mcp`, environment: `kindd-mcp-prod`
  (`kindd-mcp-prod.eba-fsy3h9iw.us-west-2.elasticbeanstalk.com`)
- ACM certificate for `mcp.kinddhelp.org`: issued
  (`arn:aws:acm:us-west-2:795519544722:certificate/b202e180-b7c5-4b65-a42b-fdac3ef11f50`)
- `kindd/prod/mcp-auth-token` exists in Secrets Manager; the EB instance
  role (`aws-elasticbeanstalk-ec2-role`) has `secretsmanager:GetSecretValue`
  on it (added to the existing `KinddSecretsManagerRead` inline policy).
- Route 53 `A`/`AAAA` alias records for `mcp.kinddhelp.org` point at the
  environment's ALB.

The steps below are kept as the reproducible runbook for future
environments (e.g. a `kindd-mcp-staging`) or disaster recovery.

### 1. Create the secret (recommended over a plain env var)

```bash
TOKEN=$(python3 -c "import secrets; print(secrets.token_urlsafe(32))")
aws secretsmanager create-secret \
  --name kindd/prod/mcp-auth-token \
  --secret-string "$TOKEN" \
  --region us-west-2 --profile personal
```

Grant the EB instance role `secretsmanager:GetSecretValue` on this secret
(same role already used for `kindd/prod/rds`, `kindd/prod/django-secret-key`).

Save `$TOKEN` somewhere safe (e.g. your password manager) — it's the value
clients configure as their bearer token.

### 2. Request an ACM certificate for `mcp.kinddhelp.org`

The existing `kinddhelp.org` certificate only covers the apex and `www.`,
so a new certificate is needed:

```bash
aws acm request-certificate \
  --domain-name mcp.kinddhelp.org \
  --validation-method DNS \
  --region us-west-2 --profile personal
```

Add the returned DNS validation CNAME to the `kinddhelp.org` Route 53
hosted zone (`Z088350613YTBVXU66CGX`) and wait for `ISSUED` status:

```bash
aws acm describe-certificate --certificate-arn <cert-arn> \
  --region us-west-2 --profile personal --query "Certificate.Status"
```

### 3. Create the EB application and environment

```bash
cd kindd_mcp
eb init kindd-mcp --platform docker --region us-west-2 --profile personal
eb create kindd-mcp-prod \
  --region us-west-2 --profile personal \
  --instance-type t3.small \
  --elb-type application
```

This applies `.ebextensions/01_environment.config` automatically (port,
health check path, ALB idle timeout).

### 4. Attach the HTTPS listener

Once the certificate from step 2 is `ISSUED`, add the listener. This is
already committed as `.ebextensions/02_https_listener.config` with the
cert ARN for `kindd-mcp-prod` filled in; for a new environment, regenerate
it the same way:

```bash
cat > .ebextensions/02_https_listener.config <<EOF
option_settings:
  aws:elbv2:listener:443:
    Protocol: HTTPS
    SSLCertificateArns: <cert-arn-from-step-2>
EOF
eb deploy kindd-mcp-prod --region us-west-2 --profile personal
```

### 5. Point DNS at the environment

```bash
aws elasticbeanstalk describe-environments \
  --environment-names kindd-mcp-prod \
  --region us-west-2 --profile personal \
  --query "Environments[0].CNAME"
```

Create an `A` (alias) record for `mcp.kinddhelp.org` in the
`kinddhelp.org` hosted zone pointing at the environment's load balancer
(EB environments support Route 53 alias records directly to their ALB).

### 6. Verify

```bash
curl -I https://mcp.kinddhelp.org/health
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://mcp.kinddhelp.org/mcp \
  -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
# expect 401 without a token, 200 with the correct bearer token
```

### 7. Configure CI/CD (optional, follow-up)

Mirror `maplocation/.github/workflows/ci-cd.yml`'s `deploy-backend` job in a
new workflow that runs `eb deploy kindd-mcp-prod` from `kindd_mcp/`.

## Security Notes

- Auth is enforced once `KINDD_MCP_AUTH_TOKEN` is set (see
  [Authentication](#authentication) above). Don't deploy step 3+ above
  without first completing step 1, or the environment will come up
  unauthenticated.
- The underlying KiNDD API itself remains public/read-only; this token only
  controls who can reach the MCP tool surface, not who can read provider
  data via the existing public API.
- Rotate the token (update the secret, then restart the environment) if it
  is ever exposed in a client config committed to source control, a log, or
  shared insecurely.
