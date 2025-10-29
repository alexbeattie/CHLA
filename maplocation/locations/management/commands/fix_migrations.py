"""
Fix inconsistent migration history.

This command marks migration 0016_5 as applied without running it,
since the database already has the location_name field from a previous deployment.
"""

from django.core.management.base import BaseCommand
from django.db import connection


class Command(BaseCommand):
    help = "Fix inconsistent migration history by marking 0016_5 as applied"

    def handle(self, *args, **options):
        with connection.cursor() as cursor:
            # Check if the migration record exists
            cursor.execute(
                """
                SELECT COUNT(*) FROM django_migrations
                WHERE app = 'locations' AND name = '0016_5_add_location_name_to_regionalcenter'
                """
            )
            exists = cursor.fetchone()[0] > 0

            if not exists:
                self.stdout.write(
                    "Marking migration 0016_5_add_location_name_to_regionalcenter as applied..."
                )
                cursor.execute(
                    """
                    INSERT INTO django_migrations (app, name, applied)
                    VALUES ('locations', '0016_5_add_location_name_to_regionalcenter', NOW())
                    """
                )
                self.stdout.write(
                    self.style.SUCCESS("✅ Migration 0016_5 marked as applied")
                )
            else:
                self.stdout.write("Migration 0016_5 is already marked as applied")
