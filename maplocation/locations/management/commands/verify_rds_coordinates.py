"""
Django management command to verify provider coordinates in RDS.
"""
from django.core.management.base import BaseCommand
from django.db import connections


class Command(BaseCommand):
    help = 'Verify provider coordinates in RDS production database'

    def handle(self, *args, **options):
        # RDS connection settings
        from django.conf import settings
        default_db = connections['default'].settings_dict

        rds_settings = {
            'ENGINE': 'django.contrib.gis.db.backends.postgis',
            'NAME': 'postgres',
            'USER': 'chla_admin',
            'PASSWORD': 'CHLASecure2024',
            'HOST': 'chla-postgres-db.cpkvcu4f59w6.us-west-2.rds.amazonaws.com',
            'PORT': '5432',
            'ATOMIC_REQUESTS': default_db.get('ATOMIC_REQUESTS', False),
            'AUTOCOMMIT': default_db.get('AUTOCOMMIT', True),
            'CONN_MAX_AGE': default_db.get('CONN_MAX_AGE', 0),
            'CONN_HEALTH_CHECKS': default_db.get('CONN_HEALTH_CHECKS', False),
            'OPTIONS': default_db.get('OPTIONS', {}),
            'TIME_ZONE': default_db.get('TIME_ZONE', None),
            'TEST': default_db.get('TEST', {}),
        }

        self.stdout.write('\n' + '=' * 80)
        self.stdout.write(self.style.HTTP_INFO('RDS PROVIDER COORDINATES VERIFICATION'))
        self.stdout.write('=' * 80 + '\n')

        # Add RDS connection to Django
        connections.databases['rds'] = rds_settings

        try:
            # Test RDS connection
            self.stdout.write('Testing RDS connection...')
            rds_conn = connections['rds']
            
            with rds_conn.cursor() as cursor:
                # Get total count
                cursor.execute('SELECT COUNT(*) FROM providers_v2')
                total = cursor.fetchone()[0]
                self.stdout.write(f'Total providers in RDS: {total}\n')
                
                # Check for providers with PostGIS location
                cursor.execute("""
                    SELECT COUNT(*) 
                    FROM providers_v2 
                    WHERE location IS NOT NULL
                """)
                with_location = cursor.fetchone()[0]
                
                # Check for providers with zero coordinates
                cursor.execute("""
                    SELECT COUNT(*) 
                    FROM providers_v2 
                    WHERE latitude = 0.0 AND longitude = 0.0
                """)
                zero_coords = cursor.fetchone()[0]
                
                # Check for providers with valid lat/lng (non-zero)
                cursor.execute("""
                    SELECT COUNT(*) 
                    FROM providers_v2 
                    WHERE latitude != 0.0 OR longitude != 0.0
                """)
                valid_coords = cursor.fetchone()[0]
                
                # Check coordinate ranges (LA County)
                cursor.execute("""
                    SELECT COUNT(*) 
                    FROM providers_v2 
                    WHERE latitude BETWEEN 33.7 AND 34.8 
                    AND longitude BETWEEN -118.7 AND -117.6
                """)
                in_la_range = cursor.fetchone()[0]
                
                # Get sample providers
                cursor.execute("""
                    SELECT name, latitude, longitude, 
                           ST_X(location::geometry) as loc_lng, 
                           ST_Y(location::geometry) as loc_lat
                    FROM providers_v2 
                    WHERE location IS NOT NULL
                    ORDER BY RANDOM()
                    LIMIT 5
                """)
                samples = cursor.fetchall()
                
                # Results
                self.stdout.write(self.style.SUCCESS('✓ RDS connection successful\n'))
                
                self.stdout.write('=' * 80)
                self.stdout.write(self.style.HTTP_INFO('COORDINATE STATISTICS'))
                self.stdout.write('=' * 80)
                self.stdout.write(f'Total Providers:              {total}')
                self.stdout.write(f'With PostGIS Location:        {with_location} ({with_location/total*100:.1f}%)')
                self.stdout.write(f'With Valid Coordinates:       {valid_coords} ({valid_coords/total*100:.1f}%)')
                self.stdout.write(f'Zero Coordinates (0, 0):      {zero_coords}')
                self.stdout.write(f'Within LA County Range:       {in_la_range} ({in_la_range/total*100:.1f}%)\n')
                
                if with_location == total and zero_coords == 0:
                    self.stdout.write(self.style.SUCCESS('✅ ALL PROVIDERS HAVE VALID COORDINATES!\n'))
                else:
                    self.stdout.write(self.style.WARNING(f'⚠️  {total - with_location} providers missing coordinates\n'))
                
                # Sample data
                self.stdout.write('=' * 80)
                self.stdout.write(self.style.HTTP_INFO('SAMPLE PROVIDERS'))
                self.stdout.write('=' * 80)
                for name, lat, lng, loc_lng, loc_lat in samples:
                    name_short = name[:40]
                    self.stdout.write(
                        f'{name_short:40} | '
                        f'Lat: {float(lat):9.5f} | '
                        f'Lng: {float(lng):9.5f} | '
                        f'PostGIS: ({float(loc_lat):9.5f}, {float(loc_lng):9.5f})'
                    )
                
                self.stdout.write('\n' + '=' * 80)
                self.stdout.write(self.style.SUCCESS('VERIFICATION COMPLETE'))
                self.stdout.write('=' * 80 + '\n')

        except Exception as e:
            self.stdout.write(self.style.ERROR(f'\n❌ Error: {str(e)}\n'))
            import traceback
            traceback.print_exc()
            raise

        finally:
            # Clean up connection
            if 'rds' in connections.databases:
                connections['rds'].close()

