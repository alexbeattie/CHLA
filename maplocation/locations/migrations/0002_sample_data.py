from django.db import migrations

def create_sample_data(apps, schema_editor):
    LocationCategory = apps.get_model('locations', 'LocationCategory')
    Location = apps.get_model('locations', 'Location')
    
    # Create categories
    restaurant = LocationCategory.objects.create(
        name='Restaurant',
        description='Places to eat'
    )
    
    shopping = LocationCategory.objects.create(
        name='Shopping',
        description='Retail stores and malls'
    )
    
    entertainment = LocationCategory.objects.create(
        name='Entertainment',
        description='Movie theaters, parks, and other entertainment venues'
    )
    
    hotel = LocationCategory.objects.create(
        name='Hotel',
        description='Places to stay'
    )
    
    # Create sample locations for San Francisco
    Location.objects.create(
        name='Golden Gate Park',
        address='501 Stanyan St',
        city='San Francisco',
        state='CA',
        zip_code='94117',
        latitude='37.7694',
        longitude='-122.4862',
        description='Large urban park with gardens, museums, and recreational areas.',
        phone='(415) 831-2700',
        website='https://goldengatepark.com/',
        is_active=True,
        category=entertainment,
        rating=4.8,
        price_level=1,
        hours_of_operation='Open 24 hours',
        has_parking=True,
        is_accessible=True
    )
    
    Location.objects.create(
        name='Fisherman\'s Wharf',
        address='Beach Street & The Embarcadero',
        city='San Francisco',
        state='CA',
        zip_code='94133',
        latitude='37.8080',
        longitude='-122.4177',
        description='Popular tourist area with seafood restaurants, shops, and sea lion viewing.',
        is_active=True,
        category=entertainment,
        rating=4.2,
        price_level=2,
        has_parking=True,
        is_accessible=True
    )
    
    Location.objects.create(
        name='Ferry Building Marketplace',
        address='1 Ferry Building',
        city='San Francisco',
        state='CA',
        zip_code='94111',
        latitude='37.7955',
        longitude='-122.3937',
        description='Historic ferry terminal with gourmet food shops and restaurants.',
        phone='(415) 983-8000',
        website='https://www.ferrybuildingmarketplace.com/',
        is_active=True,
        category=shopping,
        rating=4.5,
        price_level=3,
        hours_of_operation='Monday-Friday: 10am-7pm\nSaturday: 8am-6pm\nSunday: 11am-5pm',
        has_parking=False,
        is_accessible=True
    )
    
    Location.objects.create(
        name='Tartine Bakery',
        address='600 Guerrero St',
        city='San Francisco',
        state='CA',
        zip_code='94110',
        latitude='37.7614',
        longitude='-122.4241',
        description='Famous bakery known for its bread and pastries.',
        phone='(415) 487-2600',
        website='https://tartinebakery.com/',
        is_active=True,
        category=restaurant,
        rating=4.7,
        price_level=2,
        hours_of_operation='Monday-Sunday: 7:30am-5pm',
        has_parking=False,
        is_accessible=True
    )
    
    Location.objects.create(
        name='Fairmont San Francisco',
        address='950 Mason St',
        city='San Francisco',
        state='CA',
        zip_code='94108',
        latitude='37.7924',
        longitude='-122.4106',
        description='Luxury hotel with stunning city views.',
        phone='(415) 772-5000',
        website='https://www.fairmont.com/san-francisco/',
        is_active=True,
        category=hotel,
        rating=4.6,
        price_level=4,
        hours_of_operation='Open 24 hours',
        has_parking=True,
        is_accessible=True
    )
    
    Location.objects.create(
        name='Union Square',
        address='333 Post St',
        city='San Francisco',
        state='CA',
        zip_code='94108',
        latitude='37.7879',
        longitude='-122.4075',
        description='Major shopping and hotel district with upscale stores.',
        is_active=True,
        category=shopping,
        rating=4.3,
        price_level=3,
        has_parking=True,
        is_accessible=True
    )

class Migration(migrations.Migration):

    dependencies = [
        ('locations', '0001_initial'),
    ]

    operations = [
        migrations.RunPython(create_sample_data),
    ]