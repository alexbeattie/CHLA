"""
Find providers with missing coordinates in RDS.
"""
from django.core.management.base import BaseCommand
from django.db import connections


class Command(BaseCommand):
    help = 'Find providers with missing coordinates in RDS'

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

        # Add RDS connection to Django
        connections.databases['rds'] = rds_settings

        try:
            with connections['rds'].cursor() as cursor:
                # Find providers with zero coordinates
                cursor.execute("""
                    SELECT id, name, address
                    FROM providers_v2
                    WHERE latitude = 0.0 AND longitude = 0.0
                """)
                
                results = cursor.fetchall()
                
                self.stdout.write('\n' + '=' * 80)
                self.stdout.write(f'Found {len(results)} provider(s) with zero coordinates in RDS')
                self.stdout.write('=' * 80 + '\n')
                
                for provider_id, name, address in results:
                    self.stdout.write(f'ID: {provider_id}')
                    self.stdout.write(f'Name: {name}')
                    self.stdout.write(f'Address: {address or "(no address)"}')
                    self.stdout.write('-' * 80)

        except Exception as e:
            self.stdout.write(self.style.ERROR(f'\n❌ Error: {str(e)}\n'))
            raise

        finally:
            if 'rds' in connections.databases:
                connections['rds'].close()

