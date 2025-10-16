from django.core.management.base import BaseCommand
from locations.models import RegionalCenter

class Command(BaseCommand):
    help = 'Add ZIP code 90742 to Harbor Regional Center'

    def handle(self, *args, **options):
        try:
            harbor = RegionalCenter.objects.filter(regional_center__icontains='harbor').first()
            if harbor:
                current_zips = harbor.zip_codes or []
                if '90742' not in current_zips:
                    current_zips.append('90742')
                    harbor.zip_codes = current_zips
                    harbor.save()
                    self.stdout.write(
                        self.style.SUCCESS(f'✅ Added ZIP 90742 to Harbor Regional Center')
                    )
                    self.stdout.write(f'Current ZIP codes: {harbor.zip_codes[:10]}...')
                else:
                    self.stdout.write('ZIP 90742 already exists')
            else:
                self.stdout.write(self.style.ERROR('Harbor Regional Center not found'))
        except Exception as e:
            self.stdout.write(self.style.ERROR(f'Error: {e}'))
