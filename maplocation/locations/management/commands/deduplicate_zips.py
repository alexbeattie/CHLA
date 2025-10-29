"""
Management command to remove duplicate ZIP code assignments across regional centers
Uses official CA Regional Center service area boundaries to determine correct assignments
"""

from django.core.management.base import BaseCommand
from locations.models import RegionalCenter


class Command(BaseCommand):
    help = "Remove duplicate ZIP code assignments based on official service area boundaries"

    def add_arguments(self, parser):
        parser.add_argument(
            "--dry-run",
            action="store_true",
            help="Show what would be changed without making changes",
        )

    def handle(self, *args, **options):
        dry_run = options["dry_run"]

        if dry_run:
            self.stdout.write(
                self.style.WARNING("\n🔍 DRY RUN MODE - No changes will be made\n")
            )

        self.stdout.write(self.style.HTTP_INFO("=" * 70))
        self.stdout.write(
            self.style.HTTP_INFO("DEDUPLICATING LA COUNTY REGIONAL CENTER ZIP CODES")
        )
        self.stdout.write(self.style.HTTP_INFO("=" * 70 + "\n"))

        # Step 1: Clear all LA County ZIPs from ALL regional centers first
        self.stdout.write(
            self.style.WARNING("\nStep 1: Clearing LA County ZIPs from all centers...")
        )
        la_county_zips = set()
        for zips in self._get_la_centers().values():
            la_county_zips.update(zips)

        cleared_count = 0
        for rc in RegionalCenter.objects.all():
            if rc.zip_codes:
                current_zips = set(rc.zip_codes)
                non_la_zips = current_zips - la_county_zips
                if current_zips != non_la_zips:
                    if not dry_run:
                        rc.zip_codes = sorted(list(non_la_zips))
                        rc.save()
                    self.stdout.write(
                        f"  Cleared {len(current_zips - non_la_zips)} LA ZIPs from {rc.regional_center}"
                    )
                    cleared_count += 1

        if cleared_count == 0:
            self.stdout.write("  ✓ No LA ZIPs found in other centers")

        self.stdout.write(
            self.style.SUCCESS(
                f"\nStep 2: Assigning correct ZIPs to LA County centers..."
            )
        )

        # Get LA County regional centers only
        la_centers = self._get_la_centers()

        changes_made = 0

        for center_name, correct_zips in la_centers.items():
            try:
                rc = RegionalCenter.objects.get(regional_center=center_name)

                current_zips = set(rc.zip_codes or [])
                correct_zips_set = set(correct_zips)

                # Calculate changes
                to_remove = current_zips - correct_zips_set
                to_add = correct_zips_set - current_zips

                if to_remove or to_add:
                    self.stdout.write(f"\n{center_name}:")

                    if to_remove:
                        self.stdout.write(
                            self.style.WARNING(
                                f"  Remove {len(to_remove)} ZIPs: {', '.join(sorted(list(to_remove))[:10])}"
                                + ("..." if len(to_remove) > 10 else "")
                            )
                        )

                    if to_add:
                        self.stdout.write(
                            self.style.SUCCESS(
                                f"  Add {len(to_add)} ZIPs: {', '.join(sorted(list(to_add))[:10])}"
                                + ("..." if len(to_add) > 10 else "")
                            )
                        )

                    if not dry_run:
                        rc.zip_codes = sorted(list(correct_zips_set))
                        rc.save()
                        changes_made += 1
                        self.stdout.write(self.style.SUCCESS(f"  ✅ Updated!"))
                else:
                    self.stdout.write(f"\n{center_name}: ✓ Already correct")

            except RegionalCenter.DoesNotExist:
                self.stdout.write(
                    self.style.ERROR(f"\n❌ {center_name} not found in database")
                )

        # Summary
        self.stdout.write(self.style.HTTP_INFO("\n" + "=" * 70))
        if dry_run:
            self.stdout.write(
                self.style.WARNING(
                    f"\nDRY RUN: Would update {len([k for k, v in la_centers.items()])} regional centers"
                )
            )
            self.stdout.write(
                self.style.WARNING("Run without --dry-run to apply changes")
            )
        else:
            if changes_made > 0:
                self.stdout.write(
                    self.style.SUCCESS(
                        f"\n✅ Successfully updated {changes_made} regional centers!"
                    )
                )
                self.stdout.write("\nNext steps:")
                self.stdout.write("  1. Run: python3 manage.py audit_zip_coverage")
                self.stdout.write("  2. Verify duplicates are gone")
                self.stdout.write("  3. Test ZIP search: 91403")
            else:
                self.stdout.write(
                    self.style.SUCCESS(
                        "\n✅ All regional centers already have correct ZIP assignments!"
                    )
                )

        self.stdout.write(self.style.HTTP_INFO("=" * 70 + "\n"))

    def _get_la_centers(self):
        """Return dictionary of LA County regional centers and their correct ZIP codes"""
        return {
            "North Los Angeles County Regional Center": [
                # San Fernando Valley, Antelope Valley
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
                # Antelope Valley
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
            ],
            "Frank D. Lanterman Regional Center": [
                # Hollywood, Central LA, parts of West LA
                "90004",
                "90005",
                "90010",
                "90012",
                "90013",
                "90014",
                "90015",
                "90017",
                "90020",
                "90021",
                "90026",
                "90027",
                "90028",
                "90029",
                "90031",
                "90036",
                "90038",
                "90039",
                "90046",
                "90048",
                "90068",
                "90069",
                "90071",
                "91011",
                "91020",
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
            ],
            "Westside Regional Center": [
                # West LA, Santa Monica, Beverly Hills, Malibu
                "90024",
                "90025",
                "90034",
                "90035",
                "90049",
                "90064",
                "90066",
                "90067",
                "90077",
                "90094",
                "90210",
                "90211",
                "90212",
                "90230",
                "90245",
                "90265",
                "90272",
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
            ],
            "South Central Los Angeles Regional Center": [
                # South Central LA
                "90001",
                "90002",
                "90003",
                "90007",
                "90008",
                "90011",
                "90016",
                "90018",
                "90019",
                "90037",
                "90043",
                "90044",
                "90047",
                "90056",
                "90057",
                "90059",
                "90062",
            ],
            "Eastern Los Angeles Regional Center": [
                # East LA, Commerce, Montebello
                "90022",
                "90023",
                "90032",
                "90033",
                "90040",
                "90063",
                "90201",
                "90220",
                "90221",
                "90222",
                "90223",
                "90224",
                "90240",
                "90241",
                "90242",
                "90255",
                "90601",
                "90602",
                "90603",
                "90604",
                "90605",
                "90606",
                "90640",
                "90650",
                "90660",
                "90670",
            ],
            "San Gabriel/Pomona Regional Center": [
                # San Gabriel Valley
                "91006",
                "91007",
                "91008",
                "91010",
                "91016",
                "91024",
                "91030",
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
            ],
            "Harbor Regional Center": [
                # South Bay, San Pedro, Harbor area
                "90247",
                "90248",
                "90249",
                "90250",
                "90254",
                "90260",
                "90261",
                "90262",
                "90266",
                "90267",
                "90270",
                "90274",
                "90275",
                "90277",
                "90278",
                "90280",
                "90501",
                "90502",
                "90503",
                "90504",
                "90505",
                "90506",
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
            ],
        }
