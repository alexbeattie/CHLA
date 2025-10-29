"""
Dummy emergency_populate command.

This command was previously used during deployment but is no longer needed.
It's kept as a no-op to prevent deployment failures from legacy .ebextensions configuration.
"""

from django.core.management.base import BaseCommand


class Command(BaseCommand):
    help = "Legacy command - no longer performs any actions"

    def handle(self, *args, **options):
        self.stdout.write(
            self.style.SUCCESS(
                "emergency_populate command invoked (no-op - legacy compatibility)"
            )
        )
