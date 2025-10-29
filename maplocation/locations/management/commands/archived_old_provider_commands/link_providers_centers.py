from django.core.management.base import BaseCommand
from locations.models import Provider, RegionalCenter, ProviderRegionalCenter
from django.db import transaction
import math


class Command(BaseCommand):
    help = "Link providers with their appropriate regional centers based on location and service areas"

    def add_arguments(self, parser):
        parser.add_argument(
            "--max-distance",
            type=float,
            default=25.0,
            help="Maximum distance in miles to link provider to regional center (default: 25)",
        )
        parser.add_argument(
            "--clear-existing",
            action="store_true",
            help="Clear existing provider-regional center relationships",
        )

    def handle(self, *args, **options):
        max_distance = options["max_distance"]

        if options["clear_existing"]:
            self.stdout.write(
                "Clearing existing provider-regional center relationships..."
            )
            ProviderRegionalCenter.objects.all().delete()
            self.stdout.write(self.style.SUCCESS("Existing relationships cleared."))

        self.stdout.write(
            f"Linking providers to regional centers (max distance: {max_distance} miles)..."
        )

        linked_count = 0
        skipped_count = 0

        # Get all providers with coordinates
        providers = Provider.objects.filter(
            latitude__isnull=False, longitude__isnull=False
        )

        with transaction.atomic():
            for provider in providers:
                try:
                    # Find nearby regional centers
                    nearby_centers = RegionalCenter.find_nearest(
                        float(provider.latitude),
                        float(provider.longitude),
                        max_distance,
                        limit=5,  # Max 5 centers per provider
                    )

                    if nearby_centers:
                        # Link with the closest center as primary
                        for i, center in enumerate(nearby_centers):
                            is_primary = i == 0  # First (closest) is primary

                            # Check if relationship already exists
                            relationship, created = (
                                ProviderRegionalCenter.objects.get_or_create(
                                    provider=provider,
                                    regional_center=center,
                                    defaults={
                                        "is_primary": is_primary,
                                        "notes": f"Auto-linked: {center.distance:.1f} miles away",
                                    },
                                )
                            )

                            if created:
                                linked_count += 1
                                self.stdout.write(
                                    f"Linked {provider.name} to {center.regional_center} "
                                    f'({center.distance:.1f} miles) {"[PRIMARY]" if is_primary else ""}'
                                )
                    else:
                        skipped_count += 1
                        self.stdout.write(
                            self.style.WARNING(
                                f"No regional centers found within {max_distance} miles of {provider.name}"
                            )
                        )

                except Exception as e:
                    skipped_count += 1
                    self.stdout.write(
                        self.style.ERROR(f"Error linking {provider.name}: {str(e)}")
                    )

        self.stdout.write(
            self.style.SUCCESS(
                f"Linking complete: {linked_count} relationships created, {skipped_count} providers skipped"
            )
        )

        # Show summary statistics
        total_providers = Provider.objects.count()
        providers_with_centers = (
            Provider.objects.filter(regional_centers__isnull=False).distinct().count()
        )

        self.stdout.write(f"\nSummary:")
        self.stdout.write(f"  Total providers: {total_providers}")
        self.stdout.write(
            f"  Providers with regional centers: {providers_with_centers}"
        )
        self.stdout.write(
            f"  Coverage: {providers_with_centers/total_providers*100:.1f}%"
        )
