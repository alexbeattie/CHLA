# Provider Database - Admin Data Entry Guide

## 📊 Overview

**File:** `providers_complete_export.csv`  
**Total Providers:** 376  
**File Size:** 150KB  
**Critical Issue:** 99.5% of providers are missing diagnosis and age group data

---

## ⚠️ Missing Data Summary

| Field | Missing | Percentage |
|-------|---------|------------|
| **diagnoses_treated** | 374 providers | **99.5%** |
| **age_groups** | 374 providers | **99.5%** |

**Impact:** Users cannot effectively filter by diagnosis or age group, making the search less useful.

---

## 📝 Fields That NEED Data

### 1. `diagnoses_treated` (JSON Array)

**Format:** `["Diagnosis 1", "Diagnosis 2", "Diagnosis 3"]`

**Example:**
```json
["Autism Spectrum Disorder", "ADHD", "Speech and Language Disorder"]
```

**Valid Options:**
- `"Autism Spectrum Disorder"`
- `"Global Development Delay"`
- `"Intellectual Disability"`
- `"Speech and Language Disorder"`
- `"ADHD"`
- `"Sensory Processing Disorder"`
- `"Down Syndrome"`
- `"Cerebral Palsy"`
- `"Other"`

**Tips:**
- Use exact spelling from the list above
- Multiple diagnoses per provider are common
- If provider treats "all diagnoses", include all 9 options
- If unknown, use `["Other"]` as a placeholder

---

### 2. `age_groups` (JSON Array)

**Format:** `["0-5", "6-12"]`

**Example:**
```json
["0-5", "6-12", "13-18"]
```

**Valid Options:**
- `"0-5"` (0-5 years)
- `"6-12"` (6-12 years)
- `"13-18"` (13-18 years)
- `"19+"` (19+ years / adults)
- `"All Ages"` (serves all age groups)

**Tips:**
- Most ABA providers serve `["0-5", "6-12"]`
- Many also extend to `["0-5", "6-12", "13-18"]`
- Speech therapy often covers `["All Ages"]`
- If provider serves everyone, use `["All Ages"]`

---

## 📋 CSV Editing Instructions

### Option 1: Excel / Google Sheets (Recommended)

1. Open `providers_complete_export.csv` in Excel or Google Sheets
2. Find the `diagnoses_treated` column (Column J)
3. Find the `age_groups` column (Column K)
4. For each provider (row):
   - Fill in `diagnoses_treated` with appropriate JSON array
   - Fill in `age_groups` with appropriate JSON array
5. **Important:** Keep it as valid JSON format!
6. Save as CSV (UTF-8)

### Option 2: Bulk Pattern Fill

If many providers have similar patterns:

**Example 1 - Most ABA Therapy Providers:**
```
diagnoses_treated: ["Autism Spectrum Disorder", "Global Development Delay", "ADHD"]
age_groups: ["0-5", "6-12"]
```

**Example 2 - Speech Therapy Providers:**
```
diagnoses_treated: ["Speech and Language Disorder", "Autism Spectrum Disorder", "Global Development Delay"]
age_groups: ["0-5", "6-12", "13-18"]
```

**Example 3 - Multi-Service Providers:**
```
diagnoses_treated: ["Autism Spectrum Disorder", "ADHD", "Speech and Language Disorder", "Sensory Processing Disorder"]
age_groups: ["All Ages"]
```

### Option 3: Ask Provider Directly

For each provider, you can:
1. Visit their website (see `website` column)
2. Call them (see `phone` column)
3. Ask: "What diagnoses do you treat?" and "What age groups do you serve?"

---

## ✅ Data Entry Checklist

For each provider, ensure:

- [ ] `diagnoses_treated` is filled with JSON array
- [ ] At least 1 diagnosis is specified
- [ ] All diagnosis values match the valid options exactly
- [ ] `age_groups` is filled with JSON array
- [ ] At least 1 age group is specified
- [ ] All age group values match the valid options exactly
- [ ] JSON syntax is correct (double quotes, brackets, commas)

---

## 🔄 Re-Import Process

After filling in the data:

```bash
cd /Users/alexbeattie/Developer/CHLA/maplocation
source ../venv/bin/activate
python3 manage.py import_csv_providers ../providers_complete_export_filled.csv
```

This will update the database with your changes.

---

## 💡 Quick Examples

### Provider 1: Pure ABA Therapy
```csv
diagnoses_treated: ["Autism Spectrum Disorder"]
age_groups: ["0-5", "6-12"]
```

### Provider 2: Speech + OT Clinic
```csv
diagnoses_treated: ["Speech and Language Disorder", "Autism Spectrum Disorder", "Global Development Delay", "Sensory Processing Disorder"]
age_groups: ["0-5", "6-12", "13-18"]
```

### Provider 3: Multi-Therapy Center
```csv
diagnoses_treated: ["Autism Spectrum Disorder", "ADHD", "Speech and Language Disorder", "Intellectual Disability", "Sensory Processing Disorder"]
age_groups: ["All Ages"]
```

### Provider 4: Physical Therapy
```csv
diagnoses_treated: ["Cerebral Palsy", "Down Syndrome", "Global Development Delay", "Other"]
age_groups: ["0-5", "6-12", "13-18", "19+"]
```

---

## ❓ Common Questions

**Q: What if I don't know what diagnoses a provider treats?**  
A: Look at their `therapy_types` column for clues:
- ABA therapy → likely Autism Spectrum Disorder
- Speech therapy → likely Speech and Language Disorder
- If truly unknown, use `["Other"]`

**Q: Can a provider have multiple diagnoses?**  
A: Yes! Most providers serve multiple diagnoses. Just include all that apply.

**Q: What if a provider serves all age groups?**  
A: Use `["All Ages"]` as a single option.

**Q: What's the difference between `"0-5"` and `"All Ages"`?**  
A: `"0-5"` is specific to early childhood. `"All Ages"` means they serve everyone from infants to adults.

**Q: Should I include `"Other"` for every provider?**  
A: No. Only use `"Other"` if the provider treats diagnoses not listed in the options, or as a placeholder if unknown.

---

## 🚨 Important Notes

1. **Use EXACT spelling** from the valid options lists
2. **Keep JSON syntax correct** - double quotes, brackets, commas
3. **Don't leave fields empty** - use `[]` if truly no data available
4. **Test with a few providers first** before doing all 376
5. **Save backup** of original CSV before editing

---

## 📞 Need Help?

If you encounter any issues or have questions:
- Check the examples above
- Verify JSON syntax at [jsonlint.com](https://jsonlint.com/)
- Reach out to the development team

---

**Good luck! This data will dramatically improve the user search experience! 🎯**

