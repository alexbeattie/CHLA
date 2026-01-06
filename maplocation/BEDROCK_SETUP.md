# AWS Bedrock Setup for KiNDD

This guide walks you through enabling AWS Bedrock in your Personal AWS account for the KiNDD LLM features.

## Prerequisites

- AWS CLI configured with your Personal account (`aws configure --profile personal`)
- Python environment with boto3 installed
- Access to AWS Console

---

## Step 1: Enable Bedrock Model Access

**In AWS Console:**

1. Go to **Amazon Bedrock** in us-west-2 (Oregon)
2. Click **Model access** in the left sidebar
3. Click **Manage model access**
4. Enable these models:
   - ✅ **Amazon** → Titan Text Embeddings V2
   - ✅ **Anthropic** → Claude 3.5 Sonnet v2
5. Click **Save changes**
6. Wait for access to be granted (usually instant)

**Or via CLI:**

```bash
aws bedrock put-model-access-configuration \
    --region us-west-2 \
    --model-id amazon.titan-embed-text-v2:0 \
    --access-configuration enabled

aws bedrock put-model-access-configuration \
    --region us-west-2 \
    --model-id anthropic.claude-3-5-sonnet-20241022-v2:0 \
    --access-configuration enabled
```

---

## Step 2: Set Up IAM Permissions

Your EC2/EB instance needs permissions to call Bedrock. Add this policy:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "BedrockAccess",
            "Effect": "Allow",
            "Action": [
                "bedrock:InvokeModel",
                "bedrock:InvokeModelWithResponseStream",
                "bedrock:ListFoundationModels"
            ],
            "Resource": [
                "arn:aws:bedrock:us-west-2::foundation-model/amazon.titan-embed-text-v2:0",
                "arn:aws:bedrock:us-west-2::foundation-model/anthropic.claude-3-5-sonnet-20241022-v2:0"
            ]
        }
    ]
}
```

**To attach to your EB instance role:**

1. Go to IAM → Roles
2. Find your EB instance role (usually `aws-elasticbeanstalk-ec2-role`)
3. Click **Add permissions** → **Attach policies**
4. Create a new policy with the JSON above, or attach `AmazonBedrockFullAccess` (broader)

---

## Step 3: Install pgvector on RDS

pgvector enables semantic search with embeddings.

**Connect to RDS:**

```bash
psql -h chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com -U chla_admin -d kindd_db
```

**Enable extension:**

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

**Add embedding column to providers:**

```sql
ALTER TABLE locations_providerv2 
ADD COLUMN IF NOT EXISTS embedding vector(1024);

CREATE INDEX IF NOT EXISTS provider_embedding_idx 
ON locations_providerv2 
USING hnsw (embedding vector_cosine_ops);
```

---

## Step 4: Test Locally

```bash
cd maplocation
source ../venv/bin/activate
pip install -r requirements.txt

# Set AWS profile
export AWS_PROFILE=personal

# Test Bedrock connection
python manage.py shell
```

```python
from llm.bedrock import test_connection
test_connection()
```

Expected output:

```
✅ Titan Embeddings working - 1024 dimensions
✅ Claude 3.5 Sonnet working - Response: Hello from KiNDD!...
```

---

## Step 5: Generate Provider Embeddings

```python
from llm.embeddings import embed_all_providers
embed_all_providers()
```

This takes ~2-3 minutes for 370 providers.

---

## Step 6: Test the API

**Start server:**

```bash
python manage.py runserver
```

**Test endpoint:**

```bash
curl -X POST http://localhost:8000/api/llm/ask/ \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What ABA providers near 90210 accept Medi-Cal?",
    "context": {
      "zip_code": "90210",
      "child_age": 4,
      "diagnosis": "autism",
      "insurance": "Medi-Cal"
    }
  }'
```

---

## Costs

| Service           | Usage                      | Monthly Cost      |
| ----------------- | -------------------------- | ----------------- |
| Titan Embeddings  | 370 providers × 500 tokens | ~$0.01            |
| Claude 3.5 Sonnet | 1000 queries × 2K tokens   | ~$30-60           |
| **Total**         |                            | **~$30-60/month** |

Claude pricing: $3/million input tokens, $15/million output tokens

---

## Production Deployment

1. Ensure EB instance role has Bedrock permissions
2. Deploy with `eb deploy`
3. Run embeddings on production database:

```bash
eb ssh
source /var/app/venv/*/bin/activate
cd /var/app/current
python manage.py shell
>>> from llm.embeddings import embed_all_providers
>>> embed_all_providers()
```

---

## Endpoints

| Endpoint                | Method | Description             |
| ----------------------- | ------ | ----------------------- |
| `/api/llm/ask/`         | POST   | Natural language query  |
| `/api/llm/eligibility/` | POST   | Check eligibility       |
| `/api/llm/search/`      | GET    | Smart structured search |
| `/api/llm/health/`      | GET    | Bedrock health check    |

---

## Troubleshooting

**"AccessDeniedException"**

- Model access not enabled in Bedrock console
- IAM role missing permissions

**"ThrottlingException"**

- Request quotas exceeded, add retry logic

**"ModelNotReadyException"**

- Model still provisioning, wait a few minutes

**Slow embeddings**

- Batch requests or use async
- Consider caching embeddings
