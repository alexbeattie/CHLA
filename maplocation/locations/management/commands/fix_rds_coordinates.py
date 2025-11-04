"""
Fix coordinates for providers in RDS that have zero coordinates.
"""
from django.core.management.base import BaseCommand
from django.db import connections
from locations.utils.mapbox_geocode import geocode_with_fallback


class Command(BaseCommand):
    help = 'Fix zero coordinates in RDS by geocoding addresses'

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
            'TIME_ZONE': default_db.get('TIME_ZONE'),
            'TEST': default_db.get('TEST', {}),
        }

        connections.databases['rds'] = rds_settings

        try:
            with connections['rds'].cursor() as cursor:
                # Find providers with zero coordinates
                cursor.execute("""
                    SELECT id, name, address
                    FROM providers_v2
                    WHERE latitude = 0.0 AND longitude = 0.0
                    AND address IS NOT NULL AND address != ''
                """)
                
                providers = cursor.fetchall()
                
                self.stdout.write('\n' + '=' * 80)
                self.stdout.write(f'Found {len(providers)} provider(s) to geocode')
                self.stdout.write('=' * 80 + '\n')
                
                fixed = 0
                failed = 0
                
                for provider_id, name, address in providers:
                    self.stdout.write(f'\nGeocoding: {name}')
                    self.stdout.write(f'  Address: {address}')
                    
                    # Clean address
                    clean_address = address.replace('\n', ', ')
                    
                    # Geocode
                    coords = geocode_with_fallback(clean_address)
                    
                    if coords:
                        lat, lng = coords
                        
                        # Update in RDS with PostGIS location
                        cursor.execute("""
                            UPDATE providers_v2
                            SET latitude = %s, 
                                longitude = %s,
                                location = ST_GeomFromEWKT(%s)
                            WHERE id = %s
                        """, [
                            lat, lng, 
                            f'SRID=4326;POINT({lng} {lat})',
                            provider_id
                        ])
                        
                        fixed += 1
                        self.stdout.write(self.style.SUCCESS(f'  ✓ Fixed: {lat}, {lng}'))
                    else:
                        failed += 1
                        self.stdout.write(self.style.ERROR('  ✗ Geocoding failed'))
                
                self.stdout.write('\n' + '=' * 80)
                self.stdout.write(f'Fixed: {fixed}, Failed: {failed}')
                self.stdout.write('=' * 80 + '\n')

        except Exception as e:
            self.stdout.write(self.style.ERROR(f'\n❌ Error: {str(e)}\n'))
            import traceback
            traceback.print_exc()
            raise

        finally:
            if 'rds' in connections.databases:
                connections['rds'].close()

