"""
Management command to update service_areas and zip_codes for LA Regional Centers in RDS
"""
from django.core.management.base import BaseCommand
import psycopg2
import json

class Command(BaseCommand):
    help = "Update zip_codes and service_areas JSON fields for LA Regional Centers in RDS"

    def add_arguments(self, parser):
        parser.add_argument(
            '--rds-host',
            type=str,
            default='chla-db.c8dcqnhex8me.us-west-2.rds.amazonaws.com',
            help='RDS hostname'
        )
        parser.add_argument(
            '--rds-db',
            type=str,
            default='postgres',
            help='RDS database name'
        )
        parser.add_argument(
            '--rds-user',
            type=str,
            default='alexbeattie',
            help='RDS username'
        )
        parser.add_argument(
            '--rds-password',
            type=str,
            required=False,
            help='RDS password (or set PGPASSWORD env var)'
        )

    def handle(self, *args, **options):
        # Data from populate_la_regional_centers.py
        la_regional_centers_data = {
            "Eastern Los Angeles Regional Center": {
                "zip_codes": ["91801", "91802", "91803", "91804", "91896", "91899", "90201", "90022", "90023", "90040", "90091", "90270", "90640", "90280", "90058"],
                "service_areas": ["Alhambra", "Bell", "Bell Gardens", "Commerce", "Cudahy", "East Los Angeles", "Huntington Park", "Maywood", "Montebello", "South Gate", "Vernon"],
            },
            "Frank D. Lanterman Regional Center": {
                "zip_codes": ["90004", "90005", "90006", "90010", "90017", "90018", "90019", "90020", "90026", "90027", "90028", "90029", "90036", "90038", "90057", "90068", "90078", "90046", "91201", "91202", "91203", "91204", "91205", "91206", "91207", "91208", "91209", "91210", "91214", "91221", "91222", "91224", "91225", "91226", "91501", "91502", "91503", "91504", "91505", "91506", "91507", "91508", "91510", "91521", "91522", "91523", "91526", "91101", "91102", "91103", "91104", "91105", "91106", "91107", "91108", "91109", "91110", "91114", "91115", "91116", "91117", "91118", "91121", "91123", "91124", "91125", "91126", "91129", "91182", "91184", "91185", "91188", "91189", "91199", "90041", "90042", "90065"],
                "service_areas": ["Central Los Angeles", "Hollywood", "Glendale", "Burbank", "Pasadena", "Eagle Rock", "Highland Park"],
            },
            "Harbor Regional Center": {
                "zip_codes": ["90501", "90502", "90503", "90504", "90505", "90506", "90507", "90508", "90509", "90510", "90802", "90803", "90804", "90805", "90806", "90807", "90808", "90809", "90810", "90813", "90814", "90815", "90822", "90831", "90832", "90833", "90834", "90835", "90840", "90842", "90844", "90846", "90847", "90848", "90853", "90888", "90899", "90731", "90732", "90733", "90734", "90744", "90748", "90745", "90746", "90747", "90749", "90810", "90895", "90710", "90717", "90274", "90275", "90277", "90278", "90266", "90267", "90254", "90245", "90250", "90251", "90247", "90248", "90249", "90260", "90261"],
                "service_areas": ["Torrance", "Long Beach", "San Pedro", "Wilmington", "Carson", "Harbor City", "Lomita", "Palos Verdes", "Redondo Beach", "Manhattan Beach", "Hermosa Beach", "El Segundo", "Hawthorne", "Gardena", "Lawndale"],
            },
            "North Los Angeles County Regional Center": {
                "zip_codes": ["91301", "91302", "91303", "91304", "91305", "91306", "91307", "91308", "91309", "91310", "91311", "91313", "91316", "91324", "91325", "91326", "91327", "91328", "91329", "91330", "91331", "91333", "91334", "91335", "91337", "91340", "91341", "91342", "91343", "91344", "91345", "91346", "91350", "91351", "91352", "91353", "91354", "91355", "91356", "91357", "91364", "91365", "91367", "91371", "91372", "91380", "91381", "91382", "91383", "91384", "91385", "91386", "91387", "91390", "91392", "91393", "91394", "91395", "91396", "91401", "91402", "91403", "91404", "91405", "91406", "91407", "91408", "91409", "91410", "91411", "91412", "91413", "91416", "91423", "91426", "91436", "91470", "91482", "91495", "91496", "91499", "91601", "91602", "91603", "91604", "91605", "91606", "91607", "91608", "91609", "91610", "91611", "91612", "91614", "91615", "91616", "91617", "91618", "91321", "93534", "93535", "93536", "93543", "93544", "93550", "93551", "93552", "93553", "93591"],
                "service_areas": ["San Fernando Valley", "Santa Clarita Valley", "Antelope Valley", "Van Nuys", "North Hollywood", "Sherman Oaks", "Encino", "Tarzana", "Woodland Hills", "Canoga Park", "Winnetka", "Reseda", "Northridge", "Granada Hills", "Mission Hills", "Sylmar", "Pacoima", "Arleta", "Sun Valley", "Valley Village", "Studio City", "Toluca Lake", "Santa Clarita", "Valencia", "Canyon Country", "Newhall", "Castaic", "Lancaster", "Palmdale", "Quartz Hill", "Lake Los Angeles", "Littlerock", "Pearblossom", "Acton"],
            },
            "San Gabriel/Pomona Regional Center": {
                "zip_codes": ["91766", "91767", "91768", "91769", "91765", "91789", "91711", "91750", "91773", "91740", "91741", "91722", "91723", "91724", "91790", "91791", "91792", "91793", "91706", "91731", "91732", "91733", "91734", "91735", "91733", "91780", "91770", "91771", "91772", "91775", "91776", "91778", "91754", "91755", "91756"],
                "service_areas": ["San Gabriel Valley (eastern portion)", "Pomona Valley", "Diamond Bar", "Claremont", "La Verne", "San Dimas", "Glendora", "Covina", "West Covina", "Baldwin Park", "El Monte", "South El Monte", "Temple City", "Rosemead", "San Gabriel", "Monterey Park"],
            },
            "South Central Los Angeles Regional Center": {
                "zip_codes": ["90001", "90002", "90003", "90007", "90008", "90011", "90015", "90016", "90018", "90037", "90043", "90044", "90047", "90056", "90059", "90061", "90062", "90089", "90220", "90221", "90222", "90223", "90224", "90262", "90301", "90302", "90303", "90304", "90305", "90306", "90307", "90308", "90309", "90310", "90311", "90312", "90313", "90397", "90398", "90304", "90043", "90305", "90008", "90016", "90018", "90019", "90043", "90056"],
                "service_areas": ["South Los Angeles", "Watts", "Compton", "Lynwood", "Inglewood", "Lennox", "Hyde Park", "Crenshaw", "Baldwin Hills"],
            },
            "Westside Regional Center": {
                "zip_codes": ["90024", "90025", "90049", "90064", "90066", "90067", "90073", "90077", "90095", "90401", "90402", "90403", "90404", "90405", "90406", "90407", "90408", "90409", "90410", "90411", "90230", "90231", "90232", "90233", "90292", "90295", "90291", "90294", "90066", "90066", "90293", "90094", "90045", "90034", "90035", "90064", "90034", "90035", "90209", "90210", "90211", "90212", "90213", "90046", "90048", "90069", "90049", "90272", "90272", "90263", "90264", "90265"],
                "service_areas": ["West Los Angeles", "Santa Monica", "Culver City", "Marina del Rey", "Venice", "Mar Vista", "Del Rey", "Playa del Rey", "Playa Vista", "Westchester", "Cheviot Hills", "Pico-Robertson", "Beverly Hills", "West Hollywood", "Brentwood", "Pacific Palisades", "Malibu"],
            },
        }

        self.stdout.write("\n" + "="*70)
        self.stdout.write("🔧 UPDATING RDS PRODUCTION DATABASE")
        self.stdout.write("="*70 + "\n")

        # Connection details
        host = options['rds_host']
        database = options['rds_db']
        user = options['rds_user']
        password = options.get('rds_password')

        if not password:
            import os
            password = os.environ.get('PGPASSWORD')
            if not password:
                self.stdout.write(self.style.ERROR("❌ Password required. Use --rds-password or set PGPASSWORD env var"))
                return

        try:
            # Connect to RDS
            self.stdout.write(f"📡 Connecting to RDS: {host}...")
            conn = psycopg2.connect(
                host=host,
                database=database,
                user=user,
                password=password,
                port=5432
            )
            cursor = conn.cursor()
            self.stdout.write(self.style.SUCCESS("✅ Connected to RDS\n"))

            updated_count = 0
            error_count = 0

            for center_name, data in la_regional_centers_data.items():
                try:
                    # Update the regional center
                    cursor.execute(
                        """
                        UPDATE locations_regionalcenter
                        SET zip_codes = %s, service_areas = %s
                        WHERE regional_center = %s
                        """,
                        (json.dumps(data["zip_codes"]), json.dumps(data["service_areas"]), center_name)
                    )
                    
                    if cursor.rowcount > 0:
                        self.stdout.write(self.style.SUCCESS(
                            f"✓ Updated {center_name}: {len(data['zip_codes'])} ZIPs, {len(data['service_areas'])} cities"
                        ))
                        updated_count += 1
                    else:
                        self.stdout.write(self.style.WARNING(
                            f"⚠️  Regional Center '{center_name}' not found in RDS"
                        ))
                        error_count += 1

                except Exception as e:
                    self.stdout.write(self.style.ERROR(f"✗ Error updating {center_name}: {e}"))
                    error_count += 1

            # Commit changes
            conn.commit()
            cursor.close()
            conn.close()

            self.stdout.write("\n" + "="*70)
            self.stdout.write(f"✅ RDS UPDATE COMPLETE!")
            self.stdout.write(f"   Updated: {updated_count} regional centers")
            self.stdout.write(f"   Errors: {error_count}")
            self.stdout.write("="*70 + "\n")

        except psycopg2.Error as e:
            self.stdout.write(self.style.ERROR(f"\n❌ Database error: {e}\n"))
            raise
        except Exception as e:
            self.stdout.write(self.style.ERROR(f"\n❌ Unexpected error: {e}\n"))
            raise

