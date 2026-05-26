#!/usr/bin/env python3
import os
import sys
from pathlib import Path as _Path
sys.path.insert(0, str(_Path(__file__).resolve().parents[2]))  # maplocation/
from scripts._rds_env import load_prod_rds_env

# Configure for RDS
load_prod_rds_env()
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "maplocation.settings")

import django

django.setup()

from locations.models import ProviderV2, ProviderRegionalCenter, RegionalCenter

print("RDS Database Status:")
print("=" * 80)
print(f"Total ProviderV2 records: {ProviderV2.objects.count()}")
print(f"Total RegionalCenter records: {RegionalCenter.objects.count()}")
print(
    f"Total ProviderRegionalCenter relationships: {ProviderRegionalCenter.objects.count()}"
)
print()

# Check if any providers have regional centers
providers_with_rcs = (
    ProviderV2.objects.filter(regional_centers__isnull=False).distinct().count()
)
print(f"Providers with regional center relationships: {providers_with_rcs}")
print()

# Sample regional center
rc = RegionalCenter.objects.first()
if rc:
    print(f"Sample RC: {rc.name}")
    print(
        f"Providers for this RC: {ProviderRegionalCenter.objects.filter(regional_center=rc).count()}"
    )
