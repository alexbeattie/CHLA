# Regional Centers: Core Concept

**CRITICAL: This is the MAIN PURPOSE of this application.**

## What Regional Centers ARE

Regional Centers are **geographic service areas**, NOT funding sources or insurance types.

### The System

1. **21 Regional Centers** exist statewide in California
2. **7 Regional Centers in LA County**:
   - San Gabriel/Pomona Regional Center
   - Harbor Regional Center
   - North Los Angeles County Regional Center
   - Eastern Los Angeles Regional Center
   - South Central Los Angeles Regional Center
   - Westside Regional Center
   - Frank D. Lanterman Regional Center

3. **ZIP Code Assignment**:
   - EVERY ZIP code in California is assigned to ONE Regional Center
   - This assignment is based on geographic location ONLY
   - It has NOTHING to do with insurance, funding, or payment methods

4. **Providers serve specific Regional Centers**:
   - Example: The 78 providers added yesterday serve "San Gabriel/Pomona Regional Center"
   - Providers operate within specific Regional Center service areas
   - When a user's ZIP code is in San Gabriel/Pomona RC, they see providers that serve that RC

## How It Works

### User Flow

```
User Location → ZIP Code → Regional Center Assignment → Providers in that RC
```

Example:
```
User in ZIP 91789
  → Located in San Gabriel/Pomona Regional Center
  → Show 78 providers that serve San Gabriel/Pomona RC
  → Display San Gabriel/Pomona RC polygon on map
```

### What's NOT How It Works

❌ **WRONG**: User selects "Regional Center" as insurance type
❌ **WRONG**: Provider "accepts" Regional Center funding
❌ **WRONG**: Regional Center is a filter option

✅ **CORRECT**: User's ZIP code determines their Regional Center automatically
✅ **CORRECT**: Providers serve specific Regional Center geographic areas
✅ **CORRECT**: Regional Center is displayed based on user location

## Application Requirements

### Initial Load (PRIMARY REQUIREMENT)

1. Get user's browser geolocation
2. Reverse geocode to get ZIP code
3. Look up which Regional Center serves that ZIP code
4. Load ~25 providers that serve that Regional Center
5. Display the Regional Center polygon overlay for that specific RC

### Search by ZIP Code

1. User enters ZIP code in search bar
2. Look up which Regional Center serves that ZIP
3. Load providers for that Regional Center
4. Display that Regional Center's polygon

### Search by Address/City

1. Geocode address to get coordinates
2. Get ZIP code from coordinates
3. Follow same flow as ZIP code search

## Technical Implementation

### Database Structure

- **RegionalCenter Model**:
  - Stores 21 California Regional Centers
  - Has `zip_codes` JSON field listing all ZIPs in that RC
  - Has `service_area` polygon for map display
  - `is_la_regional_center` flag for the 7 LA County RCs

- **Location Model (Providers)**:
  - Each provider has a category
  - Providers are associated with Regional Centers based on their service area
  - The 78 providers serve "San Gabriel/Pomona Regional Center"

### API Endpoints

- `/api/providers-v2/by_regional_center/?zip_code=91789`
  - Takes user ZIP code
  - Returns providers for that ZIP's Regional Center
  - Returns Regional Center info
  - Returns ~25 providers (not 100+)

- `/api/providers-v2/comprehensive_search/`
  - Fallback for address/city searches
  - Uses lat/lng coordinates

### Frontend Flow

1. **mounted()** hook:
   ```javascript
   const zipCode = await getUserZipCode(); // Get from geolocation
   if (zipCode) {
     await providerStore.searchByZipCode(zipCode); // Uses by_regional_center endpoint
     // This automatically returns the correct Regional Center
   }
   ```

2. **Map display**:
   - Show Regional Center polygon for user's RC
   - Show providers that serve that RC
   - Limit to ~25 providers initially

## Common Misconceptions to Avoid

### ❌ "Accepts Regional Center" Filter

**WRONG CONCEPT**: Checkbox that filters providers who "accept Regional Center payment"

**CORRECT CONCEPT**: All providers in a Regional Center service area serve clients from that RC. There's no "accepts" or "doesn't accept" - they either serve that geographic area or they don't.

### ❌ Regional Center as Insurance Type

**WRONG**: `params.insurance = 'regional center'`

**CORRECT**: Regional Center is determined by ZIP code, not selected as a filter

### ❌ Funding Source Confusion

**WRONG**: "Regional Center" vs "Insurance" vs "Private Pay" as payment options

**CORRECT**: Regional Centers provide services and funding, but that's a consequence of geographic assignment, not a choice. Providers might accept various funding sources, but Regional Center association is about SERVICE AREA.

## Summary

**Regional Center = Where you live (ZIP code)**

NOT:
- How you pay
- What insurance you have
- A filter option
- A choice

The application's PRIMARY purpose is to help users find providers in their Regional Center service area based on their ZIP code.

All other features (insurance filters, therapy type filters, etc.) are SECONDARY to this core ZIP-code-to-Regional-Center-to-Providers flow.
