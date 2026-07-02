#!/bin/bash
set -e

# KINDD_MCP_AUTH_TOKEN can be provided two ways:
#   1. Directly as an Elastic Beanstalk environment property (simplest).
#   2. Via AWS Secrets Manager at kindd/prod/mcp-auth-token (matches the
#      kindd/prod/* convention used by maplocation's docker-entrypoint.sh),
#      fetched here at container startup if the env var isn't already set.
if [ -z "$KINDD_MCP_AUTH_TOKEN" ]; then
    echo "KINDD_MCP_AUTH_TOKEN not set directly; fetching from AWS Secrets Manager..."
    export KINDD_MCP_AUTH_TOKEN=$(python3 -c "
import boto3
client = boto3.client('secretsmanager', region_name='${AWS_REGION:-us-west-2}')
print(client.get_secret_value(SecretId='kindd/prod/mcp-auth-token')['SecretString'])
")
fi

echo "Starting kindd_mcp server on port ${PORT:-8800}..."
exec uvicorn kindd_mcp.server:app --host 0.0.0.0 --port "${PORT:-8800}"
