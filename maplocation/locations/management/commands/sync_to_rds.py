"""
Django management command to sync provider data from local DB to RDS.
"""
from django.core.management.base import BaseCommand
from django.db import connections
from locations.models import ProviderV2, InsuranceCarrier, ProviderInsuranceCarrier
import os


class Command(BaseCommand):
    help = 'Sync provider data from local database to RDS production database'

    def handle(self, *args, **options):
        # RDS connection settings (copy all required fields from default connection)
        from django.conf import settings
        default_db = connections['default'].settings_dict

        rds_settings = {
            'ENGINE': 'django.db.backends.postgresql',
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

        self.stdout.write('=' * 50)
        self.stdout.write('🔄 Sync FROM Local TO RDS Database')
        self.stdout.write('=' * 50)
        self.stdout.write('')
        self.stdout.write(f'Source: local database ({connections["default"].settings_dict["NAME"]})')
        self.stdout.write(f'Target: RDS ({rds_settings["HOST"]})')
        self.stdout.write('')

        # Add RDS connection to Django
        connections.databases['rds'] = rds_settings

        try:
            # Test RDS connection and check tables
            self.stdout.write('📡 Testing RDS connection...')
            rds_conn = connections['rds']
            with rds_conn.cursor() as cursor:
                cursor.execute('SELECT 1')

                # Check providers_v2 schema to see what columns RDS has
                cursor.execute("""
                    SELECT column_name, data_type, is_nullable
                    FROM information_schema.columns
                    WHERE table_name = 'providers_v2'
                    ORDER BY ordinal_position
                """)
                rds_columns = {row[0]: {'type': row[1], 'nullable': row[2]} for row in cursor.fetchall()}

            self.stdout.write('✅ RDS connection successful')
            self.stdout.write(f'  RDS providers_v2 columns: {len(rds_columns)} columns')
            self.stdout.write('')

            # Get all local data
            self.stdout.write('📥 Reading local data...')
            local_providers = list(ProviderV2.objects.all())
            local_carriers = list(InsuranceCarrier.objects.all())
            local_relationships = list(ProviderInsuranceCarrier.objects.all())

            self.stdout.write(f'  Found {len(local_providers)} providers')
            self.stdout.write(f'  Found {len(local_carriers)} insurance carriers')
            self.stdout.write(f'  Found {len(local_relationships)} provider-insurance relationships')
            self.stdout.write('')

            # Sync to RDS
            self.stdout.write('📤 Syncing to RDS...')

            with connections['rds'].cursor() as cursor:
                # Sync InsuranceCarriers first (referenced by providers)
                self.stdout.write('  Syncing insurance carriers...')
                synced_carriers = 0
                for carrier in local_carriers:
                    cursor.execute('''
                        INSERT INTO locations_insurancecarrier (id, name, description)
                        VALUES (%s, %s, %s)
                        ON CONFLICT (id) DO UPDATE SET
                            name = EXCLUDED.name,
                            description = EXCLUDED.description
                    ''', [carrier.id, carrier.name, carrier.description])
                    synced_carriers += 1
                self.stdout.write(f'    ✅ Synced {synced_carriers} insurance carriers')

                # Sync Providers
                self.stdout.write('  Syncing providers...')
                synced_providers = 0
                for provider in local_providers:
                    import json

                    # Add default values for RDS-only fields
                    insurance_accepted = getattr(provider, 'insurance_accepted', 'Unknown')

                    # Convert therapy_types array to JSONB
                    therapy_types_json = json.dumps(provider.therapy_types) if provider.therapy_types else json.dumps([])

                    cursor.execute('''
                        INSERT INTO providers_v2 (
                            id, name, address, latitude, longitude,
                            phone, email, website, therapy_types,
                            insurance_accepted,
                            created_at, updated_at
                        )
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s::jsonb, %s, %s, %s)
                        ON CONFLICT (id) DO UPDATE SET
                            name = EXCLUDED.name,
                            address = EXCLUDED.address,
                            latitude = EXCLUDED.latitude,
                            longitude = EXCLUDED.longitude,
                            phone = EXCLUDED.phone,
                            email = EXCLUDED.email,
                            website = EXCLUDED.website,
                            therapy_types = EXCLUDED.therapy_types,
                            insurance_accepted = EXCLUDED.insurance_accepted,
                            updated_at = EXCLUDED.updated_at
                    ''', [
                        provider.id, provider.name, provider.address,
                        provider.latitude, provider.longitude,
                        provider.phone, provider.email, provider.website,
                        therapy_types_json,
                        insurance_accepted,
                        provider.created_at, provider.updated_at
                    ])
                    synced_providers += 1
                self.stdout.write(f'    ✅ Synced {synced_providers} providers')

                # Sync Provider-Insurance relationships
                self.stdout.write('  Syncing provider-insurance relationships...')
                synced_relationships = 0
                for rel in local_relationships:
                    cursor.execute('''
                        INSERT INTO locations_providerinsurancecarrier (
                            id, provider_id, insurance_carrier_id
                        )
                        VALUES (%s, %s, %s)
                        ON CONFLICT (id) DO UPDATE SET
                            provider_id = EXCLUDED.provider_id,
                            insurance_carrier_id = EXCLUDED.insurance_carrier_id
                    ''', [rel.id, rel.provider_id, rel.insurance_carrier_id])
                    synced_relationships += 1
                self.stdout.write(f'    ✅ Synced {synced_relationships} relationships')

            self.stdout.write('')
            self.stdout.write('=' * 50)
            self.stdout.write('✅ SYNC TO RDS COMPLETE!')
            self.stdout.write('=' * 50)
            self.stdout.write('')
            self.stdout.write('Your RDS database now has the newly imported providers')
            self.stdout.write('')

        except Exception as e:
            self.stdout.write('')
            self.stdout.write(self.style.ERROR(f'❌ Error: {str(e)}'))
            raise

        finally:
            # Clean up connection
            if 'rds' in connections.databases:
                connections['rds'].close()
