from django.core.management.base import BaseCommand
from locations.models import Provider

class Command(BaseCommand):
    help = 'Emergency populate database with sample providers'

    def handle(self, *args, **options):
        # Sample provider data
        providers_data = [
            {
                "name": "A & H BEHAVIORAL THERAPY",
                "phone": "909-665-7070, 818-823-1515", 
                "address": "16000 Ventura Blvd #1103, Encino, CA 91436, United States",
                "website_domain": "http://www.ahappyfam.com/",
                "latitude": "34.155931",
                "longitude": "-118.481844",
                "areas": "SAN FERNANDO VALLEY,SAN GABRIEL VALLEY,LONG BEACH,INGLEWOOD,COMPTON",
                "specializations": "Autism",
                "insurance_accepted": "Health Net, MHN, Magellan, Blue Shield, Anthem/Blue Cross, Regional center",
                "services": "In Home ABA Therapy, Autism Behavioral Intervention, Autism Training for Parents",
                "coverage_areas": "SAN FERNANDO VALLEY, SAN GABRIEL VALLEY, LONG BEACH, INGLEWOOD, COMPTON"
            },
            {
                "name": "CHLA PEDIATRIC THERAPY",
                "phone": "323-361-2300",
                "address": "4650 Sunset Blvd, Los Angeles, CA 90027",
                "website_domain": "http://www.chla.org/",
                "latitude": "34.0975",
                "longitude": "-118.2903",
                "areas": "LOS ANGELES,HOLLYWOOD,SILVER LAKE",
                "specializations": "Pediatric Therapy, Autism, Developmental Delays",
                "insurance_accepted": "Most major insurance plans, Medi-Cal, Kaiser",
                "services": "Physical Therapy, Occupational Therapy, Speech Therapy, ABA Therapy",
                "coverage_areas": "LOS ANGELES COUNTY"
            },
            {
                "name": "WESTSIDE ABA SERVICES",
                "phone": "310-555-0123",
                "address": "1234 Wilshire Blvd, Santa Monica, CA 90401",
                "website_domain": "http://www.westsideaba.com/",
                "latitude": "34.0259",
                "longitude": "-118.4954",
                "areas": "SANTA MONICA,WESTSIDE,VENICE",
                "specializations": "Applied Behavior Analysis, Autism Spectrum Disorders",
                "insurance_accepted": "Blue Cross, Aetna, Cigna, Regional Center funding",
                "services": "1:1 ABA Therapy, Group Therapy, Parent Training, School Consultation",
                "coverage_areas": "WESTSIDE LOS ANGELES"
            }
        ]
        
        self.stdout.write("🚨 EMERGENCY: Populating database...")
        
        # Clear existing providers
        Provider.objects.all().delete()
        self.stdout.write("Cleared existing providers")
        
        # Create new providers
        for provider_data in providers_data:
            provider = Provider.objects.create(**provider_data)
            self.stdout.write(f"✅ Created: {provider.name}")
        
        total = Provider.objects.count()
        self.stdout.write(
            self.style.SUCCESS(f"🎉 SUCCESS: {total} providers loaded! Database is ready!")
        )
