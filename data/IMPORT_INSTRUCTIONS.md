# Importing Regional Center Provider Excel Files

## Quick Start

1. **Place your Excel files** in this directory (`/data/`):
   - `Pasadena_Provider_List.xlsx`
   - `San_Gabriel_Pomona_Provider_List.xlsx`

2. **Run the import commands**:

```bash
cd /Users/alexbeattie/Developer/CHLA/maplocation

# Import Pasadena providers
python manage.py import_regional_center_providers \
  --file ../data/Pasadena_Provider_List.xlsx \
  --regional-center "Pasadena"

# Import San Gabriel/Pomona providers
python manage.py import_regional_center_providers \
  --file ../data/San_Gabriel_Pomona_Provider_List.xlsx \
  --regional-center "San Gabriel"
```

## Command Options

### Basic Usage

```bash
python manage.py import_regional_center_providers --file PATH --regional-center NAME
```

### With Geocoding (Recommended)

Automatically geocode addresses to get latitude/longitude:

```bash
python manage.py import_regional_center_providers \
  --file ../data/Pasadena_Provider_List.xlsx \
  --regional-center "Pasadena" \
  --geocode
```

**Note**: Requires `MAPBOX_ACCESS_TOKEN` environment variable.

### Other Options

```bash
# Clear and reimport
python manage.py import_regional_center_providers \
  --file ../data/Pasadena_Provider_List.xlsx \
  --regional-center "Pasadena" \
  --clear-existing

# Specify sheet name
python manage.py import_regional_center_providers \
  --file ../data/Pasadena_Provider_List.xlsx \
  --regional-center "Pasadena" \
  --sheet "Providers"
```

## Expected Excel Format

The command automatically detects column names. It looks for:

- **Provider Name**: Columns containing "provider", "name", "organization"
- **Address**: Columns containing "address", "location", "street"
- **Phone**: Columns containing "phone", "telephone", "tel"
- **Services**: Columns containing "service", "therapy", "treatment"
- **Insurance**: Columns containing "insurance", "payment", "funding"
- **Notes**: Columns containing "note", "comment", "description"
- **Email**: Columns containing "email", "e-mail"
- **Website**: Columns containing "website", "web", "url"

### Example Excel Structure

| Provider Name | Address | Services | Insurance | Phone | Notes |
|--------------|---------|----------|-----------|-------|-------|
| ABC Therapy | 123 Main St, Pasadena, CA 91101 | ABA, Speech, OT | Regional Center, Medi-Cal | (626) 555-1234 | Accepts ages 3-18 |

## What Gets Imported

The command will:

1. ✅ Create new providers or update existing ones (matched by name + address)
2. ✅ Parse services into therapy types (ABA, Speech, OT, PT, Feeding, etc.)
3. ✅ Parse insurance into accepted types and set acceptance flags
4. ✅ Associate with the specified regional center area
5. ✅ Optionally geocode addresses to get coordinates
6. ✅ Store notes in the description field

## Field Mapping

| Excel Column | Database Field | Notes |
|-------------|----------------|-------|
| Provider Name | `name` | Required |
| Address | `address` | Required for geocoding |
| Phone | `phone` | Formatted as-is |
| Email | `email` | Validated email format |
| Website | `website` | Auto-adds http:// if missing |
| Services | `therapy_types` | Parsed into JSON array |
| Insurance | `insurance_accepted` | Comma-separated list |
| Insurance | `accepts_insurance` | Boolean flag |
| Insurance | `accepts_regional_center` | Boolean flag |
| Insurance | `accepts_private_pay` | Boolean flag |
| Notes | `description` | Full text stored |

## Service Parsing

The command automatically detects these services:

- **ABA** → "ABA therapy"
- **Speech** → "Speech therapy"
- **Occupational/OT** → "Occupational therapy"
- **Physical/PT** → "Physical therapy"
- **Feeding** → "Feeding therapy"
- **Parent** → "Parent child interaction therapy/parent training behavior management"

## Insurance Parsing

Automatically detects:

- Regional Center, Private Pay, Medi-Cal, Medicaid, Medicare
- Blue Cross, Blue Shield, Anthem, Aetna, Cigna
- Kaiser, United Healthcare, Health Net, Molina, L.A. Care

And sets appropriate flags:
- `accepts_insurance = True` if any commercial insurance found
- `accepts_regional_center = True` if "Regional Center" or "RC" found
- `accepts_private_pay = True` if "Private Pay" or "Self Pay" found

## After Import

### Verify in Django Admin

1. Go to: http://127.0.0.1:8000/client-portal/
2. Login with superuser credentials
3. Navigate to "Providers V2"
4. Filter by area or search by name

### Verify via API

```bash
# List all providers
curl http://127.0.0.1:8000/api/providers/

# Filter by regional center area (if implemented)
curl http://127.0.0.1:8000/api/providers/?search=Pasadena

# Filter by therapy type
curl http://127.0.0.1:8000/api/providers/?therapy_types=ABA%20therapy
```

### Check Database Directly

```bash
python manage.py shell
```

```python
from locations.models import ProviderV2

# Count providers
print(ProviderV2.objects.count())

# List recent providers
for p in ProviderV2.objects.order_by('-created_at')[:5]:
    print(f"{p.name} - {p.address}")

# Check therapy types
providers_with_aba = ProviderV2.objects.filter(therapy_types__contains=["ABA therapy"])
print(f"Providers with ABA: {providers_with_aba.count()}")
```

## Troubleshooting

### "Regional center not found"

List available regional centers:

```bash
python manage.py shell
```

```python
from locations.models import RegionalCenter
for rc in RegionalCenter.objects.all():
    print(rc.regional_center)
```

Then use the exact name or partial match.

### "File not found"

Make sure the file path is correct relative to where you run the command:

```bash
# From /maplocation directory
python manage.py import_regional_center_providers --file ../data/YourFile.xlsx ...

# Or use absolute path
python manage.py import_regional_center_providers --file /Users/alexbeattie/Developer/CHLA/data/YourFile.xlsx ...
```

### Geocoding Not Working

Set the Mapbox token:

```bash
export MAPBOX_ACCESS_TOKEN="pk.eyJ1IjoiYmVhdHR5LWFkbWluIiwiYSI6ImNsejFjNGt0YzFqMGMyanF3YW5hdWFmc3UifQ.sn7Uj_gDzzKL6PQq7vO7fw"

# Then run with --geocode flag
python manage.py import_regional_center_providers --file ../data/file.xlsx --regional-center "Pasadena" --geocode
```

### Duplicate Providers

The command uses `update_or_create` with `name` + `address` as the unique key. If you want to reimport and replace:

```bash
# Option 1: Delete manually first
python manage.py shell
>>> from locations.models import ProviderV2
>>> ProviderV2.objects.filter(specific_areas_served__contains=["Pasadena"]).delete()

# Option 2: The command will update existing providers with same name+address
```

## Example Complete Workflow

```bash
# 1. Navigate to maplocation directory
cd /Users/alexbeattie/Developer/CHLA/maplocation

# 2. Activate virtual environment
source ../venv/bin/activate

# 3. Set Mapbox token (for geocoding)
export MAPBOX_ACCESS_TOKEN="your_token_here"

# 4. Import Pasadena providers with geocoding
python manage.py import_regional_center_providers \
  --file ../data/Pasadena_Provider_List.xlsx \
  --regional-center "Pasadena" \
  --geocode

# 5. Import San Gabriel providers
python manage.py import_regional_center_providers \
  --file ../data/San_Gabriel_Pomona_Provider_List.xlsx \
  --regional-center "San Gabriel" \
  --geocode

# 6. Verify in shell
python manage.py shell
>>> from locations.models import ProviderV2
>>> print(f"Total providers: {ProviderV2.objects.count()}")
>>> exit()

# 7. Test API
python manage.py runserver
# Visit: http://127.0.0.1:8000/api/providers/
```

## Next Steps After Import

1. **Update filters** in the frontend to include new providers
2. **Test filtering** by therapy type, insurance, regional center
3. **Verify geocoding** - check that map markers appear correctly
4. **Deploy to production** when ready:
   ```bash
   git add .
   git commit -m "Add Pasadena and San Gabriel regional center providers"
   git push origin main  # Triggers auto-deployment
   ```

## Notes

- Providers are matched by `name` + `address` (case-sensitive)
- Empty rows are automatically skipped
- The command is idempotent - safe to run multiple times
- Geocoding is rate-limited (5 requests per second)
- All providers default to `verified=False`
- Update provider details in Django admin after import if needed

---

**Questions?** Check the command help:

```bash
python manage.py import_regional_center_providers --help
```
