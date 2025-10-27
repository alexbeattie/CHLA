# Import Providers via Django Admin

## Automatic Import (Default)

**Good News!** Provider imports now happen automatically on every deployment via `.ebextensions` configuration.

When you push to `main`, the deployment will automatically:
1. ✅ Populate regional center ZIP codes (San Gabriel, Pasadena/Eastern LA)
2. ✅ Import Pasadena providers (39 providers)
3. ✅ Import San Gabriel/Pomona providers (39 providers)

**Total: 78 providers imported automatically!**

---

## Manual Import (If Needed)

If automatic import fails or you need to re-import:

### 1. Wait for Deployment (~15-20 minutes)

Check: https://github.com/alexbeattie/CHLA/actions

Wait until you see all green checkmarks.

---

### 2. Login to Django Admin

Visit: **https://api.kinddhelp.com/client-portal/**

Login with your superuser credentials.

---

### 3. Navigate to Providers

Click on **"Providers V2"** in the admin sidebar.

---

### 4. Import Pasadena Providers (Manual)

1. Select **any one provider** from the list (just check any checkbox - the selection doesn't matter, this is just how Django admin actions work)
2. From the **"Action"** dropdown at the top, select:
   - **"Import Pasadena providers from Excel"**
3. Click **"Go"**
4. Wait ~30-60 seconds
5. You'll see a success message:
   ```
   ✅ Pasadena providers import completed!

   Import complete!
     Created: 39
     Updated: 0
     Errors:  0
   ```

---

### 5. Import San Gabriel Providers

1. Again, select **any one provider** from the list
2. From the **"Action"** dropdown, select:
   - **"Import San Gabriel/Pomona providers from Excel"**
3. Click **"Go"**
4. Wait ~30-60 seconds
5. You'll see a success message:
   ```
   ✅ San Gabriel/Pomona providers import completed!

   Import complete!
     Created: 39
     Updated: 0
     Errors:  0
   ```

---

### 6. Verify the Import

**Option 1: In Admin**
- Scroll through the Providers V2 list
- Search for "Pasadena" or "Autism Learning Partners"
- You should see the new providers

**Option 2: Via API**
```bash
curl "https://api.kinddhelp.com/api/providers-v2/" | grep -o '"count":[0-9]*'
```
Should show: `"count":299` (221 old + 78 new)

**Option 3: In Frontend**
- Visit: https://kinddhelp.com
- Enter ZIP: 91101 (Pasadena)
- Filter for providers
- Verify new providers appear

---

## Screenshots/Visual Guide

### Step 1: Select a Provider
![Select any provider checkbox](select-provider.png)

### Step 2: Choose Action
![Select import action from dropdown](choose-action.png)

### Step 3: Success Message
![Import success confirmation](success-message.png)

---

## Troubleshooting

### "File not found" Error

**Problem:** Excel files aren't on the server.

**Solution:** The files are in the git repo and should deploy automatically. If this error appears, wait for the deployment to fully complete (may take 20-25 minutes).

### "openpyxl not found" Error

**Problem:** openpyxl library not installed.

**Solution:** This shouldn't happen as it's in requirements.txt. If it does, the deployment didn't complete successfully. Check GitHub Actions for errors.

### "Regional center not found" Error

**Problem:** Regional center lookup failed.

**Solution:** This is expected for Pasadena (it uses `--area` flag instead). The San Gabriel import might show this if the lookup fails. The imports should still work.

### Import Shows "Updated: 78, Created: 0"

**This is normal!** If you run the import multiple times, it will UPDATE existing providers instead of creating duplicates. This is the expected behavior.

### Can't Login to Admin

**Problem:** Forgot superuser password or no superuser exists.

**Solution:**
1. You'll need SSH access (or use EB CLI commands)
2. Create new superuser: `python manage.py createsuperuser`

For now, you should have a superuser from when you set up the project locally.

---

## What This Does Behind the Scenes

When you click "Go", Django runs:

```python
call_command('import_regional_center_providers',
             file='data/Pasadena Provider List.xlsx',
             area='Pasadena')
```

This:
1. Opens the Excel file from the data/ directory
2. Reads all rows
3. Parses services, insurance, etc.
4. Creates or updates providers using `update_or_create()`
5. Shows you a success/error message

---

## Benefits of This Approach

- ✅ **No SSH required** - Works through web browser
- ✅ **One-click import** - Just select action and click Go
- ✅ **Safe to run multiple times** - Uses update_or_create()
- ✅ **Immediate feedback** - See results right away
- ✅ **Error handling** - Shows clear error messages if something fails

---

## After Import

### Update Provider Details (Optional)

You can now edit any provider directly in the admin:
1. Click on provider name
2. Edit fields
3. Click "Save"

### Verify in Frontend

Visit https://kinddhelp.com and test:

#### **Test ZIP Codes**
- **Pasadena ZIP codes**: 91101, 91103, 91104, 91105, 91106, 91107
- **Pomona ZIP codes**: 91766, 91767, 91768, 91769
- **San Gabriel ZIP codes**: 91775, 91776, 91778

#### **What You Should See**

When you search any of these ZIP codes, the map should display:

1. **Regional Center Boundary** (polygon overlay)
   - Pomona ZIPs → San Gabriel/Pomona Regional Center boundary
   - Pasadena ZIPs → Eastern Los Angeles Regional Center boundary
   - San Gabriel ZIPs → San Gabriel/Pomona Regional Center boundary

2. **Provider Markers** within 25-mile radius
   - All providers imported from the Excel files
   - Additional providers from the original 221 in database
   - Markers are clickable with provider details

3. **Working Filters**
   - Regional Center funding filter works (text-based search)
   - Therapy type filters work
   - Insurance filters work

#### **Provider Data Quality**

All 78 imported providers will have:
- ✅ `insurance_accepted` contains "Regional Center" text (required for filtering)
- ✅ Geocoded addresses with map markers
- ✅ Parsed therapy types from Services column
- ✅ Parsed insurance information

---

## Summary

1. ⏳ Wait for deployment (~15-20 min)
2. 🔐 Login to https://api.kinddhelp.com/client-portal/
3. 📋 Go to "Providers V2"
4. ✅ Run "Import Pasadena providers" action
5. ✅ Run "Import San Gabriel providers" action
6. 🎉 Done! 78 new providers now in production

**Total time: ~2 minutes of actual work once deployment is done**

---

## Troubleshooting ZIP Code Searches

### No Regional Center Boundary Shows

**Problem:** Searching a ZIP code doesn't show the regional center boundary polygon.

**Possible Causes:**
1. **ZIP not assigned to regional center** - Check if the ZIP code is in the regional center's `zip_codes` field
2. **Regional center has no geometry** - Some RCs don't have `service_area` geometry defined

**Solution:**
- For San Gabriel/Pomona: Run `python manage.py populate_san_gabriel_zips`
- For Pasadena/Eastern LA: Run `python manage.py populate_pasadena_zips`
- These run automatically on deployment, but can be run manually if needed

### No Providers Show on Map

**Problem:** Searching a ZIP code shows the boundary but no provider markers.

**Possible Causes:**
1. **No providers in that area** - Check provider count in admin
2. **Providers outside CA bounds** - Coordinate validation skips providers outside 32-42°N, -125--114°W
3. **Providers lack coordinates** - Some providers may not have been geocoded

**Solution:**
- Check total provider count: `curl "https://api.kinddhelp.com/api/providers-v2/" | grep count`
- Search broader radius (default is 25 miles)
- Verify providers have valid coordinates in admin

### Regional Center Filter Doesn't Work

**Problem:** Toggling "Regional Center" filter doesn't show providers.

**Root Cause:** The filter is TEXT-BASED, not boolean-based. It searches for "regional center" text in the `insurance_accepted` field.

**Solution:**
1. Check that providers have "Regional Center" in their `insurance_accepted` field
2. Run imports with latest code (commit 92a4842 or later) which adds "Regional Center" text automatically
3. For existing providers, manually add "Regional Center" to the insurance_accepted field

### Providers in Wrong Location

**Problem:** Provider markers appear in the wrong place on the map.

**Possible Causes:**
1. **Geocoding error** - Address was misinterpreted by geocoding service
2. **Bad address data** - Address in Excel file is incomplete or incorrect

**Solution:**
1. Edit provider in admin
2. Verify address is correct
3. Update latitude/longitude manually or clear them to trigger re-geocoding
4. Save and verify on map

---

## Technical Notes

### How ZIP Code Search Works

1. **User enters ZIP** → Frontend geocodes to lat/lng
2. **Regional Center Lookup** → Backend finds RC by ZIP using `zip_codes` JSONField
3. **Provider Query** → Backend finds all providers within radius using Haversine distance
4. **Filtering** → Sequential filters applied (text search, insurance, specialization, age groups, location)
5. **Display** → Frontend creates markers for providers with valid coordinates within CA bounds

### Known Limitations

1. **Coordinate Validation is Strict** - Providers outside CA bounds (32-42°N, -125--114°W) are silently skipped
2. **Text-Based Insurance Filtering** - Uses `insurance_accepted` text field, not boolean `accepts_regional_center`
3. **Sequential Filter Pipeline** - Filters run in order, earlier filters reduce set for later filters
4. **No Service Area Polygon Filtering** - Providers shown if within radius, regardless of RC service area boundaries

### Future Improvements Needed

See `/Users/alexbeattie/Developer/CHLA/docs/TECHNICAL_DEBT.md` for:
- Coordinate validation improvements
- Filter optimization and query performance
- MapView.vue refactoring (currently 6000+ lines)
