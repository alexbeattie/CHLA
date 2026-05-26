#!/usr/bin/env python3
"""
Script to populate Orange County ZIP codes for Regional Center of Orange County
"""

import os
import sys
import django
import json

# Add the project directory to Python path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# Setup Django
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "maplocation.settings")
django.setup()

from locations.models import RegionalCenter


def populate_orange_county_zip_codes():
    """Populate Orange County ZIP codes for Regional Center of Orange County"""

    # Orange County ZIP code ranges and specific codes
    orange_county_zip_codes = [
        # 92600 series (Huntington Beach, Costa Mesa, Newport Beach, etc.)
        "92600",
        "92601",
        "92602",
        "92603",
        "92604",
        "92605",
        "92606",
        "92607",
        "92608",
        "92609",
        "92610",
        "92611",
        "92612",
        "92613",
        "92614",
        "92615",
        "92616",
        "92617",
        "92618",
        "92619",
        "92620",
        "92621",
        "92622",
        "92623",
        "92624",
        "92625",
        "92626",
        "92627",
        "92628",
        "92629",
        "92630",
        "92631",
        "92632",
        "92633",
        "92634",
        "92635",
        "92636",
        "92637",
        "92638",
        "92639",
        "92640",
        "92641",
        "92642",
        "92643",
        "92644",
        "92645",
        "92646",
        "92647",
        "92648",
        "92649",
        "92650",
        "92651",
        "92652",
        "92653",
        "92654",
        "92655",
        "92656",
        "92657",
        "92658",
        "92659",
        "92660",
        "92661",
        "92662",
        "92663",
        "92664",
        "92665",
        "92666",
        "92667",
        "92668",
        "92669",
        "92670",
        "92671",
        "92672",
        "92673",
        "92674",
        "92675",
        "92676",
        "92677",
        "92678",
        "92679",
        "92680",
        "92681",
        "92682",
        "92683",
        "92684",
        "92685",
        "92686",
        "92687",
        "92688",
        "92689",
        "92690",
        "92691",
        "92692",
        "92693",
        "92694",
        "92695",
        "92696",
        "92697",
        "92698",
        "92699",
        # 92700 series (Santa Ana, Irvine, Tustin, etc.)
        "92700",
        "92701",
        "92702",
        "92703",
        "92704",
        "92705",
        "92706",
        "92707",
        "92708",
        "92709",
        "92710",
        "92711",
        "92712",
        "92713",
        "92714",
        "92715",
        "92716",
        "92717",
        "92718",
        "92719",
        "92720",
        "92721",
        "92722",
        "92723",
        "92724",
        "92725",
        "92726",
        "92727",
        "92728",
        "92729",
        "92730",
        "92731",
        "92732",
        "92733",
        "92734",
        "92735",
        "92736",
        "92737",
        "92738",
        "92739",
        "92740",
        "92741",
        "92742",
        "92743",
        "92744",
        "92745",
        "92746",
        "92747",
        "92748",
        "92749",
        "92750",
        "92751",
        "92752",
        "92753",
        "92754",
        "92755",
        "92756",
        "92757",
        "92758",
        "92759",
        "92760",
        "92761",
        "92762",
        "92763",
        "92764",
        "92765",
        "92766",
        "92767",
        "92768",
        "92769",
        "92770",
        "92771",
        "92772",
        "92773",
        "92774",
        "92775",
        "92776",
        "92777",
        "92778",
        "92779",
        "92780",
        "92781",
        "92782",
        "92783",
        "92784",
        "92785",
        "92786",
        "92787",
        "92788",
        "92789",
        "92790",
        "92791",
        "92792",
        "92793",
        "92794",
        "92795",
        "92796",
        "92797",
        "92798",
        "92799",
        # 92800 series (Anaheim, Fullerton, Orange, etc.)
        "92800",
        "92801",
        "92802",
        "92803",
        "92804",
        "92805",
        "92806",
        "92807",
        "92808",
        "92809",
        "92810",
        "92811",
        "92812",
        "92813",
        "92814",
        "92815",
        "92816",
        "92817",
        "92818",
        "92819",
        "92820",
        "92821",
        "92822",
        "92823",
        "92824",
        "92825",
        "92826",
        "92827",
        "92828",
        "92829",
        "92830",
        "92831",
        "92832",
        "92833",
        "92834",
        "92835",
        "92836",
        "92837",
        "92838",
        "92839",
        "92840",
        "92841",
        "92842",
        "92843",
        "92844",
        "92845",
        "92846",
        "92847",
        "92848",
        "92849",
        "92850",
        "92851",
        "92852",
        "92853",
        "92854",
        "92855",
        "92856",
        "92857",
        "92858",
        "92859",
        "92860",
        "92861",
        "92862",
        "92863",
        "92864",
        "92865",
        "92866",
        "92867",
        "92868",
        "92869",
        "92870",
        "92871",
        "92872",
        "92873",
        "92874",
        "92875",
        "92876",
        "92877",
        "92878",
        "92879",
        "92880",
        "92881",
        "92882",
        "92883",
        "92884",
        "92885",
        "92886",
        "92887",
        "92888",
        "92889",
        "92890",
        "92891",
        "92892",
        "92893",
        "92894",
        "92895",
        "92896",
        "92897",
        "92898",
        "92899",
        # Additional Orange County ZIP codes
        "90742",  # Sunset Beach (PO Box only)
        "90743",  # Sunset Beach
        "90744",  # Sunset Beach
        "90745",  # Sunset Beach
        "90746",  # Sunset Beach
        "90747",  # Sunset Beach
        "90748",  # Sunset Beach
        "90749",  # Sunset Beach
    ]

    # Get the Regional Center of Orange County (Main office)
    try:
        rc_oc = RegionalCenter.objects.get(
            regional_center="Regional Center of Orange County", office_type="Main"
        )
        print(f"Found Regional Center: {rc_oc.regional_center}")

        # Update the ZIP codes
        rc_oc.zip_codes = json.dumps(orange_county_zip_codes)
        rc_oc.save()

        print(
            f"✅ Successfully updated Regional Center of Orange County with {len(orange_county_zip_codes)} ZIP codes"
        )
        print(
            f"ZIP codes include: {orange_county_zip_codes[:10]}... (showing first 10)"
        )

        # Verify the update
        rc_oc.refresh_from_db()
        saved_zip_codes = (
            json.loads(rc_oc.zip_codes)
            if isinstance(rc_oc.zip_codes, str)
            else rc_oc.zip_codes
        )
        print(
            f"✅ Verification: Regional Center now has {len(saved_zip_codes)} ZIP codes"
        )

        # Check if 90742 and 92649 are now included
        if "90742" in saved_zip_codes:
            print("✅ ZIP code 90742 (Sunset Beach PO Box) is now included")
        if "92649" in saved_zip_codes:
            print("✅ ZIP code 92649 (Sunset Beach street addresses) is now included")

    except RegionalCenter.DoesNotExist:
        print("❌ Regional Center of Orange County not found in database")
        print("Available regional centers:")
        for rc in RegionalCenter.objects.all():
            print(f"  - {rc.regional_center}")
    except Exception as e:
        print(f"❌ Error updating Regional Center: {e}")


if __name__ == "__main__":
    populate_orange_county_zip_codes()
