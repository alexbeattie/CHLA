# Import Providers via Django Admin (One-Click Solution)

## Quick Steps

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

### 4. Import Pasadena Providers

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
- Search by ZIP code (91101 for Pasadena)
- Filter by therapy type
- Filter by insurance
- Check that new providers appear

---

## Summary

1. ⏳ Wait for deployment (~15-20 min)
2. 🔐 Login to https://api.kinddhelp.com/client-portal/
3. 📋 Go to "Providers V2"
4. ✅ Run "Import Pasadena providers" action
5. ✅ Run "Import San Gabriel providers" action
6. 🎉 Done! 78 new providers now in production

**Total time: ~2 minutes of actual work once deployment is done**
