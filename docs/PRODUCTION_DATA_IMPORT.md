# Production Data Import Guide

After deploying the import command code, you need to run the imports on the production database.

## ⚠️ Important Note

**Provider data is stored in the database, NOT in code.** When you push code to production:
- ✅ The import command gets deployed
- ✅ Migrations run automatically
- ❌ Provider data does NOT transfer

You must manually run the import commands on production to add the providers to the RDS database.

---

## Quick Import Steps

### 1. Wait for Deployment to Complete

Monitor deployment: https://github.com/YOUR_USERNAME/CHLAProj/actions

Wait until you see:
- ✅ Backend deployed successfully
- ✅ Migrations completed
- ✅ All checks passed

**Estimated time:** ~15-25 minutes

---

### 2. SSH to Production Server

```bash
eb ssh chla-api-prod --region us-west-2
```

If that doesn't work:
```bash
eb ssh chla-api-prod --profile personal --region us-west-2
```

---

### 3. Navigate to Application Directory

```bash
cd /var/app/current
```

---

### 4. Activate Virtual Environment

```bash
# Find the virtualenv path
ls /var/app/venv/

# Activate it (replace with actual path from ls output)
source /var/app/venv/staging-LQM1lest/bin/activate
```

---

### 5. Verify Import Command Exists

```bash
python manage.py import_regional_center_providers --help
```

You should see the command help output.

---

### 6. Run the Imports

#### Import Pasadena Providers (39 providers)

```bash
python manage.py import_regional_center_providers \
  --file data/Pasadena\ Provider\ List.xlsx \
  --area "Pasadena"
```

Expected output:
```
Importing providers for area: Pasadena
...
==================================================
Import complete!
  Created: 39
  Updated: 0
  Errors:  0
==================================================
```

#### Import San Gabriel/Pomona Providers (39 providers)

```bash
python manage.py import_regional_center_providers \
  --file data/San\ Gabriel\ Pomona\ Provider\ List.xlsx \
  --regional-center "San Gabriel"
```

Expected output:
```
Found regional center: San Gabriel/Pomona Regional Center
...
==================================================
Import complete!
  Created: 39
  Updated: 0
  Errors:  0
==================================================
```

---

### 7. Verify Data in Production Database

```bash
python manage.py shell
```

```python
from locations.models import ProviderV2

# Check total count
print(f"Total providers: {ProviderV2.objects.count()}")

# Check Pasadena providers
pasadena = ProviderV2.objects.filter(specific_areas_served__contains=["Pasadena"])
print(f"Pasadena providers: {pasadena.count()}")

# Check San Gabriel providers
pomona = ProviderV2.objects.filter(specific_areas_served__contains=["Pomona"])
print(f"San Gabriel/Pomona providers: {pomona.count()}")

# Exit shell
exit()
```

Expected output:
```
Total providers: 299 (or 78 if starting fresh)
Pasadena providers: 39
San Gabriel/Pomona providers: 39
```

---

### 8. Test the API

```bash
# Exit SSH
exit

# Test from your local machine
curl https://api.kinddhelp.com/api/providers-v2/?search=Pasadena
```

You should see providers with "Pasadena" in their data.

---

## Alternative: Import with Geocoding (Recommended)

If you want to geocode addresses on production (adds lat/long coordinates):

### 1. Set Mapbox Token on EB

```bash
eb setenv MAPBOX_ACCESS_TOKEN="pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw" --region us-west-2
```

### 2. SSH and Run with --geocode Flag

```bash
eb ssh chla-api-prod --region us-west-2
cd /var/app/current
source /var/app/venv/*/bin/activate

python manage.py import_regional_center_providers \
  --file data/Pasadena\ Provider\ List.xlsx \
  --area "Pasadena" \
  --geocode

python manage.py import_regional_center_providers \
  --file data/San\ Gabriel\ Pomona\ Provider\ List.xlsx \
  --regional-center "San Gabriel" \
  --geocode
```

**Note:** Geocoding adds ~2-3 seconds per provider (rate limiting). For 78 providers, this takes ~3-5 minutes total.

---

## Troubleshooting

### Issue: "File not found"

**Problem:** Excel files aren't on production server.

**Solution 1:** The files are in your git repo, so they should be deployed. Check:
```bash
cd /var/app/current
ls -la data/*.xlsx
```

**Solution 2:** If files are missing, upload them:
```bash
# From your local machine
scp "data/Pasadena Provider List.xlsx" eb-user@your-eb-instance:/tmp/
scp "data/San Gabriel Pomona Provider List.xlsx" eb-user@your-eb-instance:/tmp/

# Then on production:
eb ssh chla-api-prod --region us-west-2
mv /tmp/*.xlsx /var/app/current/data/
```

### Issue: "Regional center not found"

**Problem:** Regional center doesn't exist in production database.

**Solution:** List available regional centers:
```bash
python manage.py shell
>>> from locations.models import RegionalCenter
>>> for rc in RegionalCenter.objects.all().distinct('regional_center'):
...     print(rc.regional_center)
```

Then use the exact name or use `--area` flag instead.

### Issue: "openpyxl not found"

**Problem:** openpyxl wasn't installed.

**Solution:**
```bash
# Should be in requirements.txt, but if not:
pip install openpyxl==3.1.5
```

### Issue: Duplicate Providers

**Problem:** Running import multiple times creates duplicates.

**Solution:** The command uses `update_or_create` with `name` + `address` as unique key. Running multiple times will UPDATE existing providers, not create duplicates. This is safe.

To verify:
```bash
python manage.py shell
>>> from locations.models import ProviderV2
>>> # Check for duplicates
>>> from django.db.models import Count
>>> duplicates = ProviderV2.objects.values('name', 'address').annotate(count=Count('id')).filter(count__gt=1)
>>> print(f"Duplicates: {duplicates.count()}")
```

### Issue: Geocoding Not Working

**Problem:** Addresses not being geocoded.

**Check:**
1. Mapbox token is set: `echo $MAPBOX_ACCESS_TOKEN`
2. Using `--geocode` flag
3. Check rate limits (should pause between requests)

---

## Complete Step-by-Step Checklist

- [ ] 1. Push code to GitHub (done!)
- [ ] 2. Monitor GitHub Actions deployment (~15-25 min)
- [ ] 3. Verify deployment succeeded
- [ ] 4. SSH to production: `eb ssh chla-api-prod --region us-west-2`
- [ ] 5. Navigate: `cd /var/app/current`
- [ ] 6. Activate venv: `source /var/app/venv/*/bin/activate`
- [ ] 7. Verify command: `python manage.py import_regional_center_providers --help`
- [ ] 8. Import Pasadena providers
- [ ] 9. Import San Gabriel/Pomona providers
- [ ] 10. Verify in shell: Check provider counts
- [ ] 11. Test API: `curl https://api.kinddhelp.com/api/providers-v2/`
- [ ] 12. Test frontend: Visit https://kinddhelp.com and filter for new providers
- [ ] 13. Exit SSH: `exit`

---

## Post-Import Verification

### Check in Django Admin

1. Visit: https://api.kinddhelp.com/client-portal/
2. Login with superuser credentials
3. Go to "Providers V2"
4. Filter by:
   - Specific areas served: "Pasadena"
   - Specific areas served: "Pomona"
5. Verify 78 total new providers

### Check in Frontend

1. Visit: https://kinddhelp.com
2. Enter ZIP code: 91101 (Pasadena)
3. Filter for therapy types and insurance
4. Verify new providers appear in results

### Check via API

```bash
# Total count
curl "https://api.kinddhelp.com/api/providers-v2/" | jq '.count'

# Search Pasadena
curl "https://api.kinddhelp.com/api/providers-v2/?search=Pasadena" | jq '.results | length'

# Filter by therapy type
curl "https://api.kinddhelp.com/api/providers-v2/?therapy_types=ABA%20therapy" | jq '.results | length'
```

---

## Re-importing Updated Data

If you need to update provider data later:

1. Update the Excel files locally
2. Commit and push to GitHub
3. SSH to production
4. Run import command again

The command will **UPDATE** existing providers (matched by name + address) instead of creating duplicates.

---

## Notes

- Import is idempotent - safe to run multiple times
- Providers without coordinates (lat/long) will still be imported but won't show on map
- Use `--geocode` flag to add coordinates
- Geocoding requires Mapbox API token
- All providers default to `verified=False`
- Update provider details in Django admin after import if needed

---

## Getting Help

- Check deployment logs: `eb logs --region us-west-2`
- Check application logs: `eb logs --region us-west-2 | grep ERROR`
- Review import command help: `python manage.py import_regional_center_providers --help`
- Check database connection: `python manage.py dbshell`

---

**After completing the import, 78 new providers will be live on production!** 🎉
