# Pinecone CLI Guide

> **Prerequisites**: See [PINECONE.md](./PINECONE.md) for universal concepts and CLI vs SDK guidance.
>
> **Getting Started?** See [Quickstart Guide](./PINECONE-quickstart.md) for step-by-step tutorials.

This guide provides comprehensive CLI setup, authentication, and command reference for Pinecone.

## Installation

> **⚠️ Before installing**: ALWAYS check if the CLI is already installed by running `pc version`. Only prompt for installation if the command fails or CLI is not found.

### macOS (Homebrew)

```bash
brew tap pinecone-io/tap
brew install pinecone-io/tap/pinecone

# Upgrade later
brew update && brew upgrade pinecone
```

### Other Platforms

Download from [GitHub Releases](https://github.com/pinecone-io/cli/releases) (Linux, Windows, macOS)

## Authentication

> **⚠️ Before configuring authentication**: ALWAYS check if the CLI is already authenticated by running `pc auth status`. Only prompt for authentication setup if the command fails or shows `UNSET` values.
>
> **⚠️ After verifying authentication**: If CLI is authenticated, check the target organization and project by running `pc target --show`. Ask the user to confirm this is where indexes should be created before proceeding with index creation. If those are missing ask the user to Double check the configured key belongs to the correct organization and project. If user wants to change the target, use `pc target -o "org-name" -p "project-name"` to help them set it.

Choose one method:

### Option 1: User Login (Recommended for Development)

Only use if you can confirm the user has an interactive terminal with browser access

```bash
pc auth login
```

**⚠️ Important for agents**: This command prints a login URL and prompts the user to press Enter to open the browser. **This requires an interactive terminal with browser access**.

**If running in a non-interactive environment** (headless server, CI/CD, remote terminal, or agent environment without browser access):

- **Do NOT use `pc auth login`** - it will not work
- **Use Option 2 (API Key)** or **Option 3 (Service Account)** instead
- These methods work in all environments and are better suited for automation

### Option 2a: Environment Variable (Recommended for Quickstarts)

Simplest method - CLI reads `PINECONE_API_KEY` from your environment:

1. Create `.env` file:

```bash
PINECONE_API_KEY=your-api-key-here
```

2. Load into shell:

```bash
source .env
```

3. Export for CLI use (required for quickstart):

```bash
export PINECONE_API_KEY
```

4. CLI commands now work automatically.

**Benefits:**
- Same API key for CLI and SDK
- No separate configuration step
- Perfect for development and quickstarts

**Note:** Only persists for current shell session. For persistent auth, use Option 2 below.

### Option 2: API Key

Use for most automated scenarios, CI/CD, or when browser access is unavailable. Scoped to a specific project.

```bash
pc auth configure --api-key your-api-key
```

**Note**: For application code, use `.env` files (see [PINECONE.md](./PINECONE.md#-environment-variables--security-best-practices)).

### Option 3: Service Account

Use when you need access to multiple projects within an organization. Scoped at the organization level (see details below).

```bash
pc auth configure --client-id your-client-id --client-secret your-client-secret
```

**Key difference from API Key**: Service accounts are scoped at the **organization level** (not project level), providing access to **all projects within the organization**. This makes them ideal for:

- Managing multiple projects across an organization
- Cross-project automation and deployment pipelines
- Service-to-service authentication where access to multiple projects is needed
- Centralized authentication for organization-wide operations

## Index Management

### Creating Indexes

**Create index with integrated embeddings (preferred):**

```bash
pc index create -n my-index -m cosine -c aws -r us-east-1 \
  --model llama-text-embed-v2 \
  --field_map text=content
```

**Create serverless index without integrated embeddings:**

```bash
pc index create-serverless -n my-index -m cosine -c aws -r us-east-1 \
  --dimension 1536
```

### Available Embedding Models

- `llama-text-embed-v2`: High-performance, configurable dimensions, recommended for most use cases
- `multilingual-e5-large`: For multilingual content, 1024 dimensions
- `pinecone-sparse-english-v0`: For keyword/hybrid search scenarios

### Other Index Operations

```bash
# List indexes
pc index list

# Describe index
pc index describe --name my-index

# Configure index (replicas, deletion protection)
pc index configure --name my-index --replicas 3

# Delete index
pc index delete --name my-index
```

## API Key Management

```bash
# Create API key
pc api-key create

# List API keys
pc api-key list

# Delete API key
pc api-key delete --key-id <key-id>
```

## Common CLI Patterns

### Development Setup

```bash
# 1. Install CLI (check first: pc version)
brew tap pinecone-io/tap && brew install pinecone-io/tap/pinecone

# 2. Authenticate (choose one method):

# Option A: Environment variable (recommended for quickstarts)
# Create .env file with PINECONE_API_KEY=your-key, then:
source .env

# Option B: Interactive login (requires browser)
pc auth login

# Option C: Direct API key configuration (persists across sessions)
pc auth configure --api-key your-api-key

# 3. Verify target (if already authenticated) and set it if needed
pc target --show
pc target -o "my-org" -p "my-project"

# 4. Create index
pc index create -n my-dev-index -m cosine -c aws -r us-east-1 \
  --model llama-text-embed-v2 --field_map text=content

# 5. Verify setup
pc index list
pc index describe --name my-dev-index
```

### Production Deployment

```bash
# Create production index with higher replicas
pc index create -n my-prod-index -m cosine -c aws -r us-east-1 \
  --model llama-text-embed-v2 --field_map text=content \
  --replicas 3

# Configure deletion protection
pc index configure --name my-prod-index --deletion-protection true
```

### Multi-Environment Setup

```bash
# Development
pc index create -n my-app-dev -m cosine -c aws -r us-east-1 \
  --model llama-text-embed-v2 --field_map text=content

# Staging
pc index create -n my-app-staging -m cosine -c aws -r us-east-1 \
  --model llama-text-embed-v2 --field_map text=content

# Production
pc index create -n my-app-prod -m cosine -c aws -r us-east-1 \
  --model llama-text-embed-v2 --field_map text=content \
  --replicas 3 --deletion-protection true
```

## CLI Troubleshooting

> **⚠️ For agents**: If a CLI command is not recognized or returns an "Unknown command" error:
>
> 1. First check the CLI version with `pc version`
> 2. Compare with the latest version from [GitHub Releases](https://github.com/pinecone-io/cli/releases)
> 3. If outdated, ask the user to update: `brew update && brew upgrade pinecone` (macOS) or download the latest release
> 4. Verify the command syntax matches the current CLI version documentation

### Common Issues

| Issue                                     | Solution                                                                         |
| ----------------------------------------- | -------------------------------------------------------------------------------- |
| `pc: command not found`                   | Install CLI: `brew tap pinecone-io/tap && brew install pinecone-io/tap/pinecone` |
| `Unknown command` or unrecognized command | See troubleshooting steps above (check version, update if needed)                |
| `Authentication failed`                   | Run `source .env` (if using .env file), `pc auth login`, or `pc auth configure --api-key` |
| `Index already exists`                    | Use different name or delete existing: `pc index delete --name <name>`           |
| `Permission denied`                       | Check API key permissions or organization access                                 |

### Verification Commands

```bash
# Check CLI version
pc version

# Check authentication status and method
pc auth status

# Check target organization and project
pc target --show

# Verify authentication (test with API call)
pc index list

# Check index status
pc index describe --name my-index

```

## CLI vs SDK Decision Matrix

| Task                       | Use CLI | Use SDK |
| -------------------------- | ------- | ------- |
| One-time index creation    | ✅      | ❌      |
| Development setup          | ✅      | ❌      |
| Automated deployment       | ✅      | ✅      |
| Application startup checks | ❌      | ✅      |
| Dynamic index creation     | ❌      | ✅      |
| Data operations            | ❌      | ✅      |
| Runtime index management   | ❌      | ✅      |

## Best Practices

### For Development

- Use CLI for initial setup and testing
- Create indexes with descriptive names (e.g., `my-app-dev`)
- Use integrated embeddings for simplicity

### For Production

- Use CLI in deployment pipelines
- Configure appropriate replicas and deletion protection
- Use environment-specific index names
- Document all CLI commands used in deployment

### For Teams

- Share CLI setup instructions in README
- Use consistent naming conventions
- Document authentication method used
- Version control deployment scripts

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Deploy Pinecone Index
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Install Pinecone CLI
        run: |
          curl -L https://github.com/pinecone-io/cli/releases/latest/download/pc_Linux_x86_64.tar.gz | tar xz
          sudo mv pinecone /usr/local/bin/

      - name: Create Production Index
        run: |
          pc auth configure --api-key ${{ secrets.PINECONE_API_KEY }}
          pc index create -n my-app-prod -m cosine -c aws -r us-east-1 \
            --model llama-text-embed-v2 --field_map text=content \
            --replicas 3 --deletion-protection true
        env:
          PINECONE_API_KEY: ${{ secrets.PINECONE_API_KEY }}
```

### Docker Integration

```dockerfile
# Install Pinecone CLI in Docker image
FROM alpine:latest
RUN apk add --no-cache curl
RUN curl -L https://github.com/pinecone-io/cli/releases/latest/download/pc_Linux_x86_64.tar.gz | tar xz -C /usr/local/bin/
COPY deploy-index.sh /scripts/
RUN chmod +x /scripts/deploy-index.sh
```

## Advanced Usage

```bash
# Create multiple indexes
for env in dev staging prod; do
  pc index create -n my-app-$env -m cosine -c aws -r us-east-1 \
    --model llama-text-embed-v2 --field_map text=content
done
```

```bash
# Delete multiple indexes by pattern (e.g., test indexes)
pc index list | grep "^test-" | awk '{print $1}' | xargs -I {} pc index delete --name {}
```

```bash
# Export configurations for documentation
pc index list | awk '{print $1}' | while read index; do
  pc index describe --json --name "$index" > "${index}-config.json"
done
```

```bash
# Check the status of all indexes in a project
pc index list | awk '{print $1}' | while read index; do
  pc index describe --json --name "$index" | jq -r '"\(.name): \(.status.ready)"';
done
```

```bash
# Configure multiple indexes at once
for index in prod-index staging-index; do
  pc index configure --name "$index" --deletion_protection "enabled"
done
```

## Resources

- **Official CLI Documentation**: [https://docs.pinecone.io/reference/cli/command-reference](https://docs.pinecone.io/reference/cli/command-reference)
- **CLI GitHub Repository**: [https://github.com/pinecone-io/cli](https://github.com/pinecone-io/cli)
- **CLI Releases**: [https://github.com/pinecone-io/cli/releases](https://github.com/pinecone-io/cli/releases)
