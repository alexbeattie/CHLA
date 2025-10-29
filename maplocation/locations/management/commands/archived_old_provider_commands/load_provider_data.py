import json
import os
from django.core.management.base import BaseCommand
from django.core import serializers
from locations.models import Provider

class Command(BaseCommand):
    help = 'Load provider data from JSON fixture'

    def handle(self, *args, **options):
        # Get the path to provider_data.json
        json_file = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'provider_data.json')
        
        self.stdout.write(f'Loading data from: {json_file}')
        
        try:
            with open(json_file, 'r') as f:
                data = json.load(f)
                
            self.stdout.write(f'Found {len(data)} provider records')
            
            # Clear existing data
            Provider.objects.all().delete()
            
            # Load data
            for record in data:
                Provider.objects.create(**record['fields'])
                
            self.stdout.write(
                self.style.SUCCESS(f'Successfully loaded {len(data)} providers into database')
            )
            
        except Exception as e:
            self.stdout.write(
                self.style.ERROR(f'Error loading data: {str(e)}')
            )
