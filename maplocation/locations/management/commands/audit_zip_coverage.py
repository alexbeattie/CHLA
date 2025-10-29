"""
Management command to audit LA County ZIP code coverage in regional centers
"""

from django.core.management.base import BaseCommand
from locations.models import RegionalCenter
import requests


class Command(BaseCommand):
    help = "Audit LA County ZIP code coverage and identify gaps"

    def add_arguments(self, parser):
        parser.add_argument(
            "--fix",
            action="store_true",
            help="Automatically add missing ZIPs to nearest regional center",
        )
        parser.add_argument(
            "--verbose",
            action="store_true",
            help="Show detailed output including all ZIPs",
        )

    def handle(self, *args, **options):
        verbose = options["verbose"]
        fix = options["fix"]

        self.stdout.write(self.style.HTTP_INFO("\n" + "=" * 70))
        self.stdout.write(self.style.HTTP_INFO("LA COUNTY ZIP CODE COVERAGE AUDIT"))
        self.stdout.write(self.style.HTTP_INFO("=" * 70 + "\n"))

        # Get all regional centers
        regional_centers = RegionalCenter.objects.all()

        if not regional_centers.exists():
            self.stdout.write(
                self.style.ERROR("No regional centers found in database!")
            )
            return

        # Collect all ZIPs currently in database
        all_assigned_zips = set()
        rc_data = {}

        for rc in regional_centers:
            zip_codes = set(rc.zip_codes or [])
            all_assigned_zips.update(zip_codes)
            rc_data[rc.id] = {
                "name": rc.regional_center,
                "zips": zip_codes,
                "count": len(zip_codes),
            }

        # Known LA County ZIP code ranges (90xxx-93xxx)
        # We'll use a curated list of known active LA County ZIPs
        known_la_zips = self._get_known_la_zips()

        # Calculate coverage
        missing_zips = known_la_zips - all_assigned_zips
        duplicate_zips = self._find_duplicates(rc_data)

        # Print summary
        self.stdout.write(self.style.SUCCESS(f"\n📊 SUMMARY:"))
        self.stdout.write(f"  Total Regional Centers: {regional_centers.count()}")
        self.stdout.write(f"  Total ZIPs assigned: {len(all_assigned_zips)}")
        self.stdout.write(f"  Known LA County ZIPs: {len(known_la_zips)}")
        self.stdout.write(self.style.WARNING(f"  Missing ZIPs: {len(missing_zips)}"))
        if duplicate_zips:
            self.stdout.write(
                self.style.ERROR(f"  Duplicate ZIPs: {len(duplicate_zips)}")
            )

        # Print regional center breakdown
        self.stdout.write(self.style.SUCCESS(f"\n🏢 REGIONAL CENTER BREAKDOWN:"))
        for rc_id, data in sorted(rc_data.items(), key=lambda x: x[1]["name"]):
            self.stdout.write(f"\n  {data['name']}")
            self.stdout.write(f"    ZIP count: {data['count']}")
            if verbose and data["zips"]:
                sorted_zips = sorted(data["zips"])
                zip_ranges = self._format_zip_ranges(sorted_zips)
                self.stdout.write(f"    Ranges: {zip_ranges}")

        # Print missing ZIPs
        if missing_zips:
            self.stdout.write(
                self.style.WARNING(f"\n⚠️  MISSING ZIP CODES ({len(missing_zips)}):")
            )
            sorted_missing = sorted(missing_zips)

            # Group by prefix for readability
            by_prefix = {}
            for zip_code in sorted_missing:
                prefix = zip_code[:3]
                if prefix not in by_prefix:
                    by_prefix[prefix] = []
                by_prefix[prefix].append(zip_code)

            for prefix in sorted(by_prefix.keys()):
                zips = by_prefix[prefix]
                self.stdout.write(f"  {prefix}xx: {', '.join(zips)} ({len(zips)} ZIPs)")

                # Show geographic context
                context = self._get_zip_context(zips[0])
                if context:
                    self.stdout.write(f"        → {context}")

        # Print duplicates
        if duplicate_zips:
            self.stdout.write(self.style.ERROR(f"\n❌ DUPLICATE ZIP CODES:"))
            for zip_code, centers in sorted(duplicate_zips.items()):
                self.stdout.write(f"  {zip_code} appears in:")
                for center in centers:
                    self.stdout.write(f"    - {center}")

        # Recommendations
        self.stdout.write(self.style.HTTP_INFO(f"\n💡 RECOMMENDATIONS:"))
        if missing_zips:
            self.stdout.write(
                f"  • Add {len(missing_zips)} missing ZIP codes to appropriate regional centers"
            )
            self.stdout.write(f"  • Run: python3 manage.py audit_zip_coverage --fix")
        if duplicate_zips:
            self.stdout.write(
                f"  • Remove {len(duplicate_zips)} duplicate ZIP code assignments"
            )
        if not missing_zips and not duplicate_zips:
            self.stdout.write(self.style.SUCCESS("  ✅ ZIP code coverage looks good!"))

        # Fix mode
        if fix and missing_zips:
            self.stdout.write(
                self.style.WARNING(f"\n🔧 FIX MODE: Adding missing ZIPs...")
            )
            self._fix_missing_zips(sorted_missing, regional_centers)

        self.stdout.write(self.style.HTTP_INFO("\n" + "=" * 70 + "\n"))

    def _get_known_la_zips(self):
        """Return set of known LA County ZIP codes"""
        # Comprehensive list of LA County ZIP codes by area
        # Source: USPS and LA County Regional Centers
        la_zips = set()

        # Central LA (900xx)
        la_zips.update(
            [
                "90001",
                "90002",
                "90003",
                "90004",
                "90005",
                "90006",
                "90007",
                "90008",
                "90010",
                "90011",
                "90012",
                "90013",
                "90014",
                "90015",
                "90016",
                "90017",
                "90018",
                "90019",
                "90020",
                "90021",
                "90022",
                "90023",
                "90024",
                "90025",
                "90026",
                "90027",
                "90028",
                "90029",
                "90031",
                "90032",
                "90033",
                "90034",
                "90035",
                "90036",
                "90037",
                "90038",
                "90039",
                "90040",
                "90041",
                "90042",
                "90043",
                "90044",
                "90045",
                "90046",
                "90047",
                "90048",
                "90049",
                "90056",
                "90057",
                "90058",
                "90059",
                "90061",
                "90062",
                "90063",
                "90064",
                "90065",
                "90066",
                "90067",
                "90068",
                "90069",
                "90071",
                "90077",
                "90089",
                "90094",
            ]
        )

        # South Bay (902xx-905xx)
        la_zips.update(
            [
                "90201",
                "90220",
                "90221",
                "90222",
                "90223",
                "90224",
                "90230",
                "90240",
                "90241",
                "90242",
                "90245",
                "90247",
                "90248",
                "90249",
                "90250",
                "90254",
                "90255",
                "90260",
                "90261",
                "90262",
                "90265",
                "90266",
                "90267",
                "90270",
                "90272",
                "90274",
                "90275",
                "90277",
                "90278",
                "90280",
                "90290",
                "90291",
                "90292",
                "90293",
                "90301",
                "90302",
                "90303",
                "90304",
                "90305",
                "90401",
                "90402",
                "90403",
                "90404",
                "90405",
                "90501",
                "90502",
                "90503",
                "90504",
                "90505",
                "90506",
                "90650",
                "90660",
                "90670",
                "90701",
                "90703",
                "90706",
                "90710",
                "90712",
                "90713",
                "90715",
                "90716",
                "90717",
                "90723",
                "90731",
                "90732",
                "90740",
                "90744",
                "90745",
                "90746",
                "90755",
                "90802",
                "90803",
                "90804",
                "90805",
                "90806",
                "90807",
                "90808",
                "90810",
                "90813",
                "90814",
                "90815",
                "90822",
                "90831",
                "90840",
            ]
        )

        # San Gabriel Valley (910xx-918xx)
        la_zips.update(
            [
                "91001",
                "91006",
                "91007",
                "91008",
                "91010",
                "91011",
                "91016",
                "91020",
                "91024",
                "91030",
                "91040",
                "91041",
                "91042",
                "91043",
                "91046",
                "91101",
                "91103",
                "91104",
                "91105",
                "91106",
                "91107",
                "91108",
                "91201",
                "91202",
                "91203",
                "91204",
                "91205",
                "91206",
                "91207",
                "91208",
                "91210",
                "91214",
                "91301",
                "91302",
                "91303",
                "91304",
                "91306",
                "91307",
                "91311",
                "91316",
                "91321",
                "91324",
                "91325",
                "91326",
                "91330",
                "91331",
                "91335",
                "91340",
                "91342",
                "91343",
                "91344",
                "91345",
                "91350",
                "91351",
                "91352",
                "91354",
                "91355",
                "91356",
                "91361",
                "91362",
                "91364",
                "91367",
                "91381",
                "91384",
                "91387",
                "91390",
                "91401",
                "91402",
                "91403",
                "91404",
                "91405",
                "91406",
                "91411",
                "91423",
                "91436",
                "91501",
                "91502",
                "91504",
                "91505",
                "91506",
                "91601",
                "91602",
                "91604",
                "91605",
                "91606",
                "91607",
                "91608",
                "91702",
                "91706",
                "91711",
                "91722",
                "91723",
                "91724",
                "91731",
                "91732",
                "91733",
                "91740",
                "91741",
                "91744",
                "91745",
                "91746",
                "91747",
                "91748",
                "91750",
                "91754",
                "91755",
                "91765",
                "91766",
                "91767",
                "91768",
                "91770",
                "91773",
                "91775",
                "91776",
                "91780",
                "91789",
                "91790",
                "91791",
                "91792",
                "91801",
                "91803",
            ]
        )

        # Antelope Valley (935xx)
        la_zips.update(
            [
                "93510",
                "93532",
                "93534",
                "93535",
                "93536",
                "93543",
                "93550",
                "93551",
                "93552",
                "93553",
                "93591",
            ]
        )

        return la_zips

    def _find_duplicates(self, rc_data):
        """Find ZIP codes assigned to multiple regional centers"""
        zip_to_centers = {}

        for rc_id, data in rc_data.items():
            for zip_code in data["zips"]:
                if zip_code not in zip_to_centers:
                    zip_to_centers[zip_code] = []
                zip_to_centers[zip_code].append(data["name"])

        # Return only duplicates
        return {
            zip_code: centers
            for zip_code, centers in zip_to_centers.items()
            if len(centers) > 1
        }

    def _format_zip_ranges(self, sorted_zips):
        """Format ZIP codes as ranges for compact display"""
        if not sorted_zips:
            return "None"

        ranges = []
        start = sorted_zips[0]
        prev = start

        for zip_code in sorted_zips[1:]:
            if int(zip_code) != int(prev) + 1:
                # End of range
                if start == prev:
                    ranges.append(start)
                else:
                    ranges.append(f"{start}-{prev}")
                start = zip_code
            prev = zip_code

        # Add last range
        if start == prev:
            ranges.append(start)
        else:
            ranges.append(f"{start}-{prev}")

        return ", ".join(ranges)

    def _get_zip_context(self, zip_code):
        """Get geographic context for a ZIP code"""
        # Map ZIP prefixes to areas
        contexts = {
            "900": "Central/Downtown LA",
            "901": "South Bay/Torrance",
            "902": "Compton/Inglewood/South Bay",
            "903": "Santa Monica/Malibu",
            "904": "Long Beach",
            "905": "Torrance/South Bay",
            "906": "Whittier/Norwalk",
            "907": "Long Beach/San Pedro",
            "910": "Pasadena/San Gabriel Valley",
            "911": "Burbank/Glendale/La Cañada",
            "912": "Burbank/Glendale",
            "913": "San Fernando Valley (West)",
            "914": "Sherman Oaks/Van Nuys",
            "915": "North Hollywood/Van Nuys",
            "916": "Encino/Tarzana",
            "917": "Monrovia/Duarte/SGV",
            "918": "Covina/West Covina/SGV",
            "935": "Antelope Valley (Lancaster/Palmdale)",
        }

        prefix = zip_code[:3]
        return contexts.get(prefix, "Unknown area")

    def _fix_missing_zips(self, missing_zips, regional_centers):
        """Automatically add missing ZIPs to appropriate regional centers"""
        # Create mapping of area codes to regional centers
        area_to_rc = {
            "Sherman Oaks/Van Nuys/Encino": ["914", "915", "916"],
            "North LA County": ["913", "935"],
            "San Gabriel Valley": ["910", "917", "918"],
            "South Bay": ["901", "902", "905"],
        }

        # Find appropriate regional centers
        north_la = RegionalCenter.objects.filter(
            regional_center__icontains="North Los Angeles County"
        ).first()

        sgv = RegionalCenter.objects.filter(
            regional_center__icontains="San Gabriel"
        ).first()

        # Add missing ZIPs based on prefix
        added_count = 0
        for zip_code in missing_zips:
            prefix = zip_code[:3]

            # Determine which RC should have it
            target_rc = None
            if prefix in ["914", "915", "916", "913", "935"]:
                target_rc = north_la
            elif prefix in ["910", "917", "918"]:
                target_rc = sgv

            if target_rc:
                current_zips = set(target_rc.zip_codes or [])
                current_zips.add(zip_code)
                target_rc.zip_codes = sorted(list(current_zips))
                target_rc.save()
                added_count += 1
                self.stdout.write(
                    self.style.SUCCESS(
                        f"  ✅ Added {zip_code} to {target_rc.regional_center}"
                    )
                )

        if added_count > 0:
            self.stdout.write(
                self.style.SUCCESS(f"\n✅ Added {added_count} ZIP codes!")
            )
