"""
Django management command to export all ProviderV2 data to CSV for admin review.
Especially useful for filling in missing diagnoses and age_groups data.
"""
import csv
from django.core.management.base import BaseCommand
from locations.models import ProviderV2
from datetime import datetime


class Command(BaseCommand):
    help = 'Export all provider data to CSV for admin review and data entry'

    def add_arguments(self, parser):
        parser.add_argument(
            '--output',
            type=str,
            default='providers_export.csv',
            help='Output CSV filename (default: providers_export.csv)'
        )

    def handle(self, *args, **options):
        output_file = options['output']
        
        self.stdout.write('=' * 70)
        self.stdout.write('📊 Exporting Provider Data to CSV')
        self.stdout.write('=' * 70)
        self.stdout.write('')

        # Get all providers
        providers = ProviderV2.objects.all().order_by('name')
        total_count = providers.count()
        
        self.stdout.write(f'Found {total_count} providers to export')
        self.stdout.write('')

        # Analyze missing data
        missing_diagnoses = providers.filter(diagnoses_treated__isnull=True).count()
        missing_age_groups = providers.filter(age_groups__isnull=True).count()
        missing_both = providers.filter(diagnoses_treated__isnull=True, age_groups__isnull=True).count()
        
        self.stdout.write('📋 Data Quality Summary:')
        self.stdout.write(f'  - Missing diagnoses: {missing_diagnoses} ({missing_diagnoses/total_count*100:.1f}%)')
        self.stdout.write(f'  - Missing age_groups: {missing_age_groups} ({missing_age_groups/total_count*100:.1f}%)')
        self.stdout.write(f'  - Missing both: {missing_both} ({missing_both/total_count*100:.1f}%)')
        self.stdout.write('')

        # Define CSV columns
        fieldnames = [
            'id',
            'name',
            'address',
            'latitude',
            'longitude',
            'phone',
            'website',
            'therapy_types',       # JSON array
            'insurance_accepted',  # Legacy field
            'diagnoses_treated',   # JSON array - NEEDS FILLING
            'age_groups',          # JSON array - NEEDS FILLING
            'regional_centers',    # JSON array
            'description',
            'type',
            'email',
            'created_at',
            'updated_at'
        ]

        # Write CSV
        with open(output_file, 'w', newline='', encoding='utf-8') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()

            for provider in providers:
                writer.writerow({
                    'id': str(provider.id),
                    'name': provider.name or '',
                    'address': provider.address or '',
                    'latitude': provider.latitude or '',
                    'longitude': provider.longitude or '',
                    'phone': provider.phone or '',
                    'website': provider.website or '',
                    'therapy_types': provider.therapy_types or '[]',
                    'insurance_accepted': provider.insurance_accepted or '',
                    'diagnoses_treated': provider.diagnoses_treated or '[]',  # HIGHLIGHT: Needs data
                    'age_groups': provider.age_groups or '[]',  # HIGHLIGHT: Needs data
                    'regional_centers': provider.regional_centers or '[]',
                    'description': provider.description or '',
                    'type': provider.type or '',
                    'email': provider.email or '',
                    'created_at': provider.created_at.isoformat() if provider.created_at else '',
                    'updated_at': provider.updated_at.isoformat() if provider.updated_at else ''
                })

        self.stdout.write('')
        self.stdout.write(self.style.SUCCESS(f'✅ Export complete: {output_file}'))
        self.stdout.write('')
        
        # Print instructions for admin
        self.stdout.write('=' * 70)
        self.stdout.write('📝 INSTRUCTIONS FOR ADMIN:')
        self.stdout.write('=' * 70)
        self.stdout.write('')
        self.stdout.write('Fields that NEED DATA:')
        self.stdout.write('  1. diagnoses_treated - JSON array format')
        self.stdout.write('     Example: ["Autism Spectrum Disorder", "ADHD", "Speech and Language Disorder"]')
        self.stdout.write('')
        self.stdout.write('  2. age_groups - JSON array format')
        self.stdout.write('     Example: ["0-5", "6-12", "13-18"]')
        self.stdout.write('')
        self.stdout.write('Available Options for diagnoses_treated:')
        for choice in ProviderV2.DIAGNOSIS_CHOICES:
            self.stdout.write(f'     - "{choice[0]}"')
        self.stdout.write('')
        self.stdout.write('Available Options for age_groups:')
        for choice in ProviderV2.AGE_GROUP_CHOICES:
            self.stdout.write(f'     - "{choice[0]}"')
        self.stdout.write('')
        self.stdout.write('After filling in the data, use the import_csv_providers command to re-import.')
        self.stdout.write('=' * 70)

