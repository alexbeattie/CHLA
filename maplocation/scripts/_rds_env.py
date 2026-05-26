"""Bootstrap helper for ops scripts under maplocation/scripts/.

Loads the production RDS connection blob from AWS Secrets Manager
(secret id `kindd/prod/rds`) and exports the values as DB_* env vars
*before* `django.setup()` is called. Designed to replace the older
pattern where each script hardcoded the RDS password.

Usage at the top of an ops script (before any Django imports):

    from pathlib import Path
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))  # maplocation/
    from scripts._rds_env import load_prod_rds_env
    load_prod_rds_env()

Then proceed with the usual `os.environ.setdefault("DJANGO_SETTINGS_MODULE", ...)`
and `django.setup()`.
"""
import json
import os

import boto3


DEFAULT_SECRET_ID = "kindd/prod/rds"


def load_prod_rds_env(secret_id: str = DEFAULT_SECRET_ID) -> None:
    """Populate DB_HOST/PORT/NAME/USER/PASSWORD/SSL_REQUIRE in os.environ
    from AWS Secrets Manager. Existing env vars are not overwritten."""
    region = os.environ.get("AWS_REGION", "us-west-2")
    client = boto3.client("secretsmanager", region_name=region)
    blob = json.loads(client.get_secret_value(SecretId=secret_id)["SecretString"])

    os.environ.setdefault("DB_HOST", blob["host"])
    os.environ.setdefault("DB_PORT", str(blob["port"]))
    os.environ.setdefault("DB_NAME", blob["dbname"])
    os.environ.setdefault("DB_USER", blob["username"])
    os.environ.setdefault("DB_PASSWORD", blob["password"])
    os.environ.setdefault("DB_SSL_REQUIRE", "true" if blob.get("sslmode", "require") == "require" else "false")
