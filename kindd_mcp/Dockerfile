# Build context is this directory (kindd_mcp/), matching how Elastic
# Beanstalk deploys it (eb deploy is run from inside kindd_mcp/, exactly
# like maplocation/Dockerfile + maplocation's EB setup).
FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Preserve the kindd_mcp/ package layout under /app so relative imports
# (`from .kindd_client import ...`) and `kindd_mcp.server:app` resolve the
# same way locally and in the container.
COPY . ./kindd_mcp

COPY docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8800

ENTRYPOINT ["/app/docker-entrypoint.sh"]
