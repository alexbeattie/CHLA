from django.core.management.base import BaseCommand
from django.conf import settings
from locations.models import ProviderV2, RegionalCenter
from locations.utils.mapbox_geocode import geocode_with_fallback
from decimal import Decimal
import requests
import time
import logging

logger = logging.getLogger(__name__)

class Command(BaseCommand):
    help = 'Geocode addresses for providers and regional centers'

    def add_arguments(self, parser):
        parser.add_argument(
            '--providers',
            action='store_true',
            help='Geocode provider addresses',
        )
        parser.add_argument(
            '--regional-centers',
            action='store_true',
            help='Geocode regional center addresses',
        )
        parser.add_argument(
            '--all',
            action='store_true',
            help='Geocode all addresses',
        )
        parser.add_argument(
            '--force',
            action='store_true',
            help='Force geocoding even if coordinates already exist',
        )
        parser.add_argument(
            '--dry-run',
            action='store_true',
            help='Show what would be geocoded without making changes',
        )

    def handle(self, *args, **options):
        if options['all']:
            options['providers'] = True
            options['regional_centers'] = True

        if not options['providers'] and not options['regional_centers']:
            self.stdout.write(self.style.ERROR('Please specify --providers, --regional-centers, or --all'))
            return

        if options['providers']:
            self.geocode_providers(options['force'], options['dry_run'])

        if options['regional_centers']:
            self.geocode_regional_centers(options['force'], options['dry_run'])

    def geocode_providers(self, force=False, dry_run=False):
        """Geocode provider addresses"""
        self.stdout.write(self.style.SUCCESS('Starting provider geocoding...'))
        
        # Get providers that need geocoding
        if force:
            providers = ProviderV2.objects.all()
        else:
            # Find providers with NULL or 0.00000000 coordinates (both mean not geocoded)
            from django.db.models import Q
            providers = ProviderV2.objects.filter(
                Q(latitude__isnull=True) | Q(longitude__isnull=True) |
                Q(latitude=Decimal('0.00000000')) | Q(longitude=Decimal('0.00000000'))
            )

        total_providers = providers.count()
        self.stdout.write(f'Found {total_providers} providers to geocode')

        if dry_run:
            self.stdout.write(self.style.WARNING('DRY RUN - No changes will be made'))
            for provider in providers[:10]:  # Show first 10
                self.stdout.write(f'Would geocode: {provider.name} - {provider.address}')
            if total_providers > 10:
                self.stdout.write(f'... and {total_providers - 10} more')
            return

        geocoded_count = 0
        failed_count = 0

        for i, provider in enumerate(providers, 1):
            self.stdout.write(f'[{i}/{total_providers}] Geocoding {provider.name}...')
            
            try:
                # Build address from provider data
                address = provider.address
                if isinstance(address, dict):
                    # Handle JSON address format
                    parts = [
                        address.get('street', ''),
                        address.get('city', ''),
                        address.get('state', ''),
                        address.get('zip', '')
                    ]
                    address = ', '.join([p for p in parts if p])
                
                if not address:
                    failed_count += 1
                    self.stdout.write(self.style.WARNING(f'  ⚠ No address available'))
                    continue
                
                # Use Mapbox geocoding with fallback
                coordinates = geocode_with_fallback(address)
                if coordinates:
                    provider.latitude = Decimal(str(coordinates[0]))
                    provider.longitude = Decimal(str(coordinates[1]))
                    provider.save()
                    geocoded_count += 1
                    self.stdout.write(
                        self.style.SUCCESS(f'  ✓ Success: {coordinates[0]}, {coordinates[1]}')
                    )
                else:
                    failed_count += 1
                    self.stdout.write(
                        self.style.ERROR(f'  ✗ Failed to geocode: {address}')
                    )
                
                # Rate limiting
                time.sleep(0.1)  # 10 requests per second max
                
            except Exception as e:
                failed_count += 1
                self.stdout.write(
                    self.style.ERROR(f'  ✗ Error: {str(e)}')
                )

        self.stdout.write(
            self.style.SUCCESS(
                f'Provider geocoding complete: {geocoded_count} success, {failed_count} failed'
            )
        )

    def geocode_regional_centers(self, force=False, dry_run=False):
        """Geocode regional center addresses"""
        self.stdout.write(self.style.SUCCESS('Starting regional center geocoding...'))
        
        # Get regional centers that need geocoding
        if force:
            centers = RegionalCenter.objects.all()
        else:
            # Find centers with NULL or 0.00000000 coordinates
            from django.db.models import Q
            centers = RegionalCenter.objects.filter(
                Q(latitude__isnull=True) | Q(longitude__isnull=True) |
                Q(latitude=Decimal('0.00000000')) | Q(longitude=Decimal('0.00000000'))
            )

        total_centers = centers.count()
        self.stdout.write(f'Found {total_centers} regional centers to geocode')

        if dry_run:
            self.stdout.write(self.style.WARNING('DRY RUN - No changes will be made'))
            for center in centers[:10]:  # Show first 10
                self.stdout.write(f'Would geocode: {center.regional_center} - {center.address}')
            if total_centers > 10:
                self.stdout.write(f'... and {total_centers - 10} more')
            return

        geocoded_count = 0
        failed_count = 0

        for i, center in enumerate(centers, 1):
            self.stdout.write(f'[{i}/{total_centers}] Geocoding {center.regional_center}...')
            
            try:
                # Combine address parts
                address_parts = [center.address]
                if center.suite:
                    address_parts.append(center.suite)
                if center.city:
                    address_parts.append(center.city)
                if center.state:
                    address_parts.append(center.state)
                if center.zip_code:
                    address_parts.append(center.zip_code)
                
                full_address = ', '.join(filter(None, address_parts))
                
                if not full_address:
                    failed_count += 1
                    self.stdout.write(self.style.WARNING(f'  ⚠ No address available'))
                    continue
                
                # Use Mapbox geocoding with fallback
                coordinates = geocode_with_fallback(full_address)
                if coordinates:
                    center.latitude = Decimal(str(coordinates[0]))
                    center.longitude = Decimal(str(coordinates[1]))
                    center.save()
                    geocoded_count += 1
                    self.stdout.write(
                        self.style.SUCCESS(f'  ✓ Success: {coordinates[0]}, {coordinates[1]}')
                    )
                else:
                    failed_count += 1
                    self.stdout.write(
                        self.style.ERROR(f'  ✗ Failed to geocode: {full_address}')
                    )
                
                # Rate limiting
                time.sleep(0.1)  # 10 requests per second max
                
            except Exception as e:
                failed_count += 1
                self.stdout.write(
                    self.style.ERROR(f'  ✗ Error: {str(e)}')
                )

        self.stdout.write(
            self.style.SUCCESS(
                f'Regional center geocoding complete: {geocoded_count} success, {failed_count} failed'
            )
        )

    def geocode_address(self, address):
        """
        Geocode an address using a geocoding service
        This is a basic implementation - in production you'd use a proper geocoding service
        """
        if not address:
            return None

        # Basic geocoding for common California locations
        # In production, you'd use Google Maps, MapBox, or similar service
        california_locations = {
            'los angeles': {'lat': 34.0522, 'lng': -118.2437},
            'san francisco': {'lat': 37.7749, 'lng': -122.4194},
            'san diego': {'lat': 32.7157, 'lng': -117.1611},
            'sacramento': {'lat': 38.5816, 'lng': -121.4944},
            'fresno': {'lat': 36.7468, 'lng': -119.7725},
            'oakland': {'lat': 37.8044, 'lng': -122.2712},
            'long beach': {'lat': 33.7701, 'lng': -118.1937},
            'santa monica': {'lat': 34.0195, 'lng': -118.4912},
            'beverly hills': {'lat': 34.0736, 'lng': -118.4004},
            'pasadena': {'lat': 34.1478, 'lng': -118.1445},
            'anaheim': {'lat': 33.8366, 'lng': -117.9143},
            'riverside': {'lat': 33.9533, 'lng': -117.3962},
            'stockton': {'lat': 37.9577, 'lng': -121.2908},
            'irvine': {'lat': 33.6846, 'lng': -117.8265},
            'chula vista': {'lat': 32.6401, 'lng': -117.0842},
            'fremont': {'lat': 37.5485, 'lng': -121.9886},
            'san bernardino': {'lat': 34.1083, 'lng': -117.2898},
            'modesto': {'lat': 37.6391, 'lng': -120.9969},
            'fontana': {'lat': 34.0922, 'lng': -117.4350},
            'oxnard': {'lat': 34.1975, 'lng': -119.1771},
            'moreno valley': {'lat': 33.9425, 'lng': -117.2297},
            'huntington beach': {'lat': 33.6603, 'lng': -117.9992},
            'glendale': {'lat': 34.1425, 'lng': -118.2551},
            'santa clarita': {'lat': 34.3917, 'lng': -118.5426},
            'garden grove': {'lat': 33.7739, 'lng': -117.9414},
            'santa rosa': {'lat': 38.4404, 'lng': -122.7141},
            'oceanside': {'lat': 33.1959, 'lng': -117.3795},
            'rancho cucamonga': {'lat': 34.1064, 'lng': -117.5931},
            'ontario': {'lat': 34.0633, 'lng': -117.6509},
            'lancaster': {'lat': 34.6868, 'lng': -118.1542},
            'elk grove': {'lat': 38.4088, 'lng': -121.3716},
            'corona': {'lat': 33.8753, 'lng': -117.5664},
            'palmdale': {'lat': 34.5794, 'lng': -118.1165},
            'salinas': {'lat': 36.6777, 'lng': -121.6555},
            'pomona': {'lat': 34.0552, 'lng': -117.7499},
            'torrance': {'lat': 33.8358, 'lng': -118.3406},
            'hayward': {'lat': 37.6688, 'lng': -122.0808},
            'escondido': {'lat': 33.1192, 'lng': -117.0864},
            'sunnyvale': {'lat': 37.3688, 'lng': -122.0363},
            'orange': {'lat': 33.7879, 'lng': -117.8531},
            'fullerton': {'lat': 33.8704, 'lng': -117.9242},
            'pasadena': {'lat': 34.1478, 'lng': -118.1445},
            'thousand oaks': {'lat': 34.1706, 'lng': -118.8376},
            'visalia': {'lat': 36.3302, 'lng': -119.2921},
            'simi valley': {'lat': 34.2694, 'lng': -118.7815},
            'concord': {'lat': 37.9780, 'lng': -122.0311},
            'roseville': {'lat': 38.7521, 'lng': -121.2880},
            'santa clara': {'lat': 37.3541, 'lng': -121.9552},
            'vallejo': {'lat': 38.1041, 'lng': -122.2564},
            'victorville': {'lat': 34.5362, 'lng': -117.2917},
            'el monte': {'lat': 34.0686, 'lng': -118.0276},
            'berkeley': {'lat': 37.8715, 'lng': -122.2730},
            'downey': {'lat': 33.9401, 'lng': -118.1326},
            'costa mesa': {'lat': 33.6411, 'lng': -117.9187},
            'inglewood': {'lat': 33.9617, 'lng': -118.3531},
            'san buenaventura': {'lat': 34.2747, 'lng': -119.2290},
            'west covina': {'lat': 34.0686, 'lng': -117.9390},
            'carlsbad': {'lat': 33.1581, 'lng': -117.3506},
            'fairfield': {'lat': 38.2494, 'lng': -122.0400},
            'richmond': {'lat': 37.9358, 'lng': -122.3477},
            'murrieta': {'lat': 33.5539, 'lng': -117.2139},
            'burbank': {'lat': 34.1808, 'lng': -118.3090},
            'antioch': {'lat': 37.9857, 'lng': -121.8058},
            'temecula': {'lat': 33.4936, 'lng': -117.1484},
            'norwalk': {'lat': 33.9022, 'lng': -118.0817},
            'daly city': {'lat': 37.7058, 'lng': -122.4622},
            'rialto': {'lat': 34.1064, 'lng': -117.3703},
            'el cajon': {'lat': 32.7948, 'lng': -116.9625},
            'san mateo': {'lat': 37.5630, 'lng': -122.3255},
            'compton': {'lat': 33.8958, 'lng': -118.2201},
            'mission viejo': {'lat': 33.6000, 'lng': -117.6720},
            'carson': {'lat': 33.8317, 'lng': -118.2820},
            'santa monica': {'lat': 34.0195, 'lng': -118.4912},
            'redding': {'lat': 40.5865, 'lng': -122.3917},
            'santa barbara': {'lat': 34.4208, 'lng': -119.6982},
            'chico': {'lat': 39.7285, 'lng': -121.8375},
            'whittier': {'lat': 33.9792, 'lng': -118.0328},
            'hawthorne': {'lat': 33.9164, 'lng': -118.3526},
            'citrus heights': {'lat': 38.7071, 'lng': -121.2810},
            'livermore': {'lat': 37.6819, 'lng': -121.7680},
            'tracy': {'lat': 37.7397, 'lng': -121.4252},
            'alhambra': {'lat': 34.0953, 'lng': -118.1270},
            'lakewood': {'lat': 33.8536, 'lng': -118.1339},
            'mountain view': {'lat': 37.3861, 'lng': -122.0839},
            'redondo beach': {'lat': 33.8492, 'lng': -118.3884},
            'san leandro': {'lat': 37.7249, 'lng': -122.1561},
            'santa maria': {'lat': 34.9530, 'lng': -120.4357},
            'merced': {'lat': 37.3022, 'lng': -120.4830},
            'buena park': {'lat': 33.8675, 'lng': -117.9981},
            'chino': {'lat': 34.0122, 'lng': -117.6889},
            'clovis': {'lat': 36.8252, 'lng': -119.7029},
            'alameda': {'lat': 37.7652, 'lng': -122.2416},
            'south gate': {'lat': 33.9548, 'lng': -118.2120},
            'vacaville': {'lat': 38.3566, 'lng': -121.9877},
            'west sacramento': {'lat': 38.5816, 'lng': -121.5300},
            'san rafael': {'lat': 37.9735, 'lng': -122.5311},
            'bellflower': {'lat': 33.8817, 'lng': -118.1170},
            'woodland': {'lat': 38.6785, 'lng': -121.7733},
            'napa': {'lat': 38.2975, 'lng': -122.2869},
            'tustin': {'lat': 33.7458, 'lng': -117.8265},
            'davis': {'lat': 38.5449, 'lng': -121.7405},
            'mountain view': {'lat': 37.3861, 'lng': -122.0839},
            'danville': {'lat': 37.8216, 'lng': -121.9999},
            'san marcos': {'lat': 33.1434, 'lng': -117.1661},
            'petaluma': {'lat': 38.2324, 'lng': -122.6367},
            'upland': {'lat': 34.0975, 'lng': -117.6484},
            'pleasanton': {'lat': 37.6624, 'lng': -121.8747},
            'lakewood': {'lat': 33.8536, 'lng': -118.1339},
            'baldwin park': {'lat': 34.0853, 'lng': -117.9609},
            'santee': {'lat': 32.8384, 'lng': -116.9739},
            'milpitas': {'lat': 37.4323, 'lng': -121.8996},
            'union city': {'lat': 37.5933, 'lng': -122.0438},
            'redwood city': {'lat': 37.4852, 'lng': -122.2364},
            'turlock': {'lat': 37.4947, 'lng': -120.8466},
            'manteca': {'lat': 37.7974, 'lng': -121.2160},
            'national city': {'lat': 32.6781, 'lng': -117.0992},
            'san bruno': {'lat': 37.6305, 'lng': -122.4111},
            'yorba linda': {'lat': 33.8886, 'lng': -117.8131},
            'folsom': {'lat': 38.6779, 'lng': -121.1760},
            'pico rivera': {'lat': 33.9830, 'lng': -118.0967},
            'paramount': {'lat': 33.8894, 'lng': -118.1598},
            'cerritos': {'lat': 33.8583, 'lng': -118.0648},
            'cupertino': {'lat': 37.3230, 'lng': -122.0322},
            'diamond bar': {'lat': 34.0286, 'lng': -117.8103},
            'azusa': {'lat': 34.1336, 'lng': -117.9076},
            'san clemente': {'lat': 33.4270, 'lng': -117.6120},
            'madera': {'lat': 36.9613, 'lng': -120.0607},
            'el segundo': {'lat': 33.9164, 'lng': -118.4148},
            'fountain valley': {'lat': 33.7092, 'lng': -117.9537},
            'covina': {'lat': 34.0900, 'lng': -117.8903},
            'brentwood': {'lat': 37.9318, 'lng': -121.6957},
            'beaumont': {'lat': 33.9294, 'lng': -116.9773},
            'san ramon': {'lat': 37.7799, 'lng': -121.9780},
            'camarillo': {'lat': 34.2164, 'lng': -119.0376},
            'walnut': {'lat': 34.0203, 'lng': -117.8651},
            'cathedral city': {'lat': 33.7792, 'lng': -116.4668},
            'delano': {'lat': 35.7688, 'lng': -119.2471},
            'watsonville': {'lat': 36.9107, 'lng': -121.7568},
            'hanford': {'lat': 36.3274, 'lng': -119.6457},
            'lompoc': {'lat': 34.6391, 'lng': -120.4579},
            'dublin': {'lat': 37.7022, 'lng': -121.9358},
            'castro valley': {'lat': 37.6941, 'lng': -122.0863},
            'hesperia': {'lat': 34.4264, 'lng': -117.3009},
            'santa paula': {'lat': 34.3542, 'lng': -119.0596},
            'lodi': {'lat': 38.1341, 'lng': -121.2728},
            'el cerrito': {'lat': 37.9135, 'lng': -122.3107},
            'azusa': {'lat': 34.1336, 'lng': -117.9076},
            'san gabriel': {'lat': 34.0961, 'lng': -118.1058},
            'colton': {'lat': 34.0739, 'lng': -117.3137},
            'hercules': {'lat': 38.0174, 'lng': -122.2886},
            'orange': {'lat': 33.7879, 'lng': -117.8531},
            'pittsburg': {'lat': 38.0280, 'lng': -121.8846},
            'rosemead': {'lat': 34.0806, 'lng': -118.0728},
            'san pablo': {'lat': 37.9621, 'lng': -122.3455},
            'suisun city': {'lat': 38.2382, 'lng': -122.0402},
            'laguna niguel': {'lat': 33.5225, 'lng': -117.7075},
            'martinez': {'lat': 38.0193, 'lng': -122.1341},
            'newport beach': {'lat': 33.6189, 'lng': -117.9298},
            'la habra': {'lat': 33.9319, 'lng': -117.9462},
            'campbell': {'lat': 37.2872, 'lng': -121.9499},
            'martinez': {'lat': 38.0193, 'lng': -122.1341},
            'pacifica': {'lat': 37.6138, 'lng': -122.4869},
            'montebello': {'lat': 34.0165, 'lng': -118.1137},
            'novato': {'lat': 38.1074, 'lng': -122.5697},
            'la mesa': {'lat': 32.7678, 'lng': -117.0230},
            'rancho palos verdes': {'lat': 33.7447, 'lng': -118.3873},
            'monterey park': {'lat': 34.0625, 'lng': -118.1287},
            'gardena': {'lat': 33.8883, 'lng': -118.3090},
            'palm desert': {'lat': 33.7222, 'lng': -116.3744},
            'rancho santa margarita': {'lat': 33.6406, 'lng': -117.6031},
            'moraga': {'lat': 37.8349, 'lng': -122.1297},
            'yuba city': {'lat': 39.1404, 'lng': -121.6169},
            'san carlos': {'lat': 37.5072, 'lng': -122.2605},
            'santee': {'lat': 32.8384, 'lng': -116.9739},
            'lemon grove': {'lat': 32.7426, 'lng': -117.0317},
            'seaside': {'lat': 36.6177, 'lng': -121.8508},
            'roseville': {'lat': 38.7521, 'lng': -121.2880},
            'la puente': {'lat': 34.0200, 'lng': -117.9445},
            'campbell': {'lat': 37.2872, 'lng': -121.9499},
            'san dimas': {'lat': 34.1067, 'lng': -117.8067},
            'seal beach': {'lat': 33.7414, 'lng': -118.1048},
            'encinitas': {'lat': 33.0370, 'lng': -117.2920},
            'temple city': {'lat': 34.1072, 'lng': -118.0579},
            'davis': {'lat': 38.5449, 'lng': -121.7405},
            'el centro': {'lat': 32.7920, 'lng': -115.5630},
            'fair oaks': {'lat': 38.6743, 'lng': -121.2644},
            'goleta': {'lat': 34.4358, 'lng': -119.8276},
            'hanford': {'lat': 36.3274, 'lng': -119.6457},
            'hollister': {'lat': 36.8524, 'lng': -121.4016},
            'imperial beach': {'lat': 32.5834, 'lng': -117.1133},
            'king city': {'lat': 36.2128, 'lng': -121.1224},
            'la verne': {'lat': 34.1089, 'lng': -117.7681},
            'mammoth lakes': {'lat': 37.6485, 'lng': -118.9721},
            'marina': {'lat': 36.6844, 'lng': -121.8022},
            'menlo park': {'lat': 37.4419, 'lng': -122.1430},
            'mill valley': {'lat': 37.9061, 'lng': -122.5450},
            'millbrae': {'lat': 37.5985, 'lng': -122.3872},
            'monrovia': {'lat': 34.1442, 'lng': -117.9992},
            'morgan hill': {'lat': 37.1305, 'lng': -121.6544},
            'los altos': {'lat': 37.3855, 'lng': -122.1141},
            'los gatos': {'lat': 37.2358, 'lng': -121.9623},
            'lynwood': {'lat': 33.9306, 'lng': -118.2115},
            'san fernando': {'lat': 34.2820, 'lng': -118.4388},
            'san anselmo': {'lat': 37.9746, 'lng': -122.5619},
            'saratoga': {'lat': 37.2638, 'lng': -122.0230},
            'soledad': {'lat': 36.4246, 'lng': -121.3263},
            'south san francisco': {'lat': 37.6547, 'lng': -122.4077},
            'west hollywood': {'lat': 34.0900, 'lng': -118.3617},
            'west sacramento': {'lat': 38.5816, 'lng': -121.5300},
            'westminster': {'lat': 33.7513, 'lng': -117.9940},
            'willows': {'lat': 39.5246, 'lng': -122.1933},
            'woodside': {'lat': 37.4300, 'lng': -122.2538},
            'yountville': {'lat': 38.4013, 'lng': -122.3597},
        }

        address_lower = address.lower()
        
        # Check for exact city matches
        for city, coords in california_locations.items():
            if city in address_lower:
                return coords

        # Check for ZIP code patterns
        import re
        zip_match = re.search(r'\b9\d{4}\b', address)
        if zip_match:
            # Basic ZIP code geocoding (would use proper service in production)
            zip_code = zip_match.group()
            if zip_code.startswith('90'):
                return {'lat': 34.0522, 'lng': -118.2437}  # LA area
            elif zip_code.startswith('94'):
                return {'lat': 37.7749, 'lng': -122.4194}  # SF area
            elif zip_code.startswith('92'):
                return {'lat': 32.7157, 'lng': -117.1611}  # San Diego area
            elif zip_code.startswith('95'):
                return {'lat': 38.5816, 'lng': -121.4944}  # Sacramento area
            elif zip_code.startswith('93'):
                return {'lat': 36.7468, 'lng': -119.7725}  # Fresno area

        # If no match found, return None
        return None