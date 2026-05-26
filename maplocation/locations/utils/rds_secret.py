"""Fetch the production RDS connection blob from AWS Secrets Manager.

The secret is stored as JSON under `kindd/prod/rds` with keys:
host, port, dbname, username, password, sslmode.

Used by Django management commands that need to open a second connection
to the production database from a developer laptop.
"""
import json
import os
from functools import lru_cache

import boto3


DEFAULT_SECRET_ID = "kindd/prod/rds"


@lru_cache(maxsize=4)
def _fetch(secret_id: str, region: str) -> dict:
    client = boto3.client("secretsmanager", region_name=region)
    return json.loads(client.get_secret_value(SecretId=secret_id)["SecretString"])


def get_rds_settings(secret_id: str = DEFAULT_SECRET_ID, *, use_postgis: bool = True) -> dict:
    """Return a Django DATABASES-style settings dict for production RDS.

    Pass `use_postgis=False` for plain postgresql (e.g. raw SQL admin tasks).
    """
    region = os.environ.get("AWS_REGION", "us-west-2")
    blob = _fetch(secret_id, region)
    engine = (
        "django.contrib.gis.db.backends.postgis"
        if use_postgis
        else "django.db.backends.postgresql"
    )
    return {
        "ENGINE": engine,
        "NAME": blob["dbname"],
        "USER": blob["username"],
        "PASSWORD": blob["password"],
        "HOST": blob["host"],
        "PORT": str(blob["port"]),
        "OPTIONS": {"sslmode": blob.get("sslmode", "require")},
    }
