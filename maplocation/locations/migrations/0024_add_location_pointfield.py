# Generated migration for adding PostGIS PointField to Location model

from django.contrib.gis.db import models as gis_models
from django.contrib.gis.geos import Point
from django.db import migrations


def populate_location_points(apps, schema_editor):
    """Populate the location PostGIS field from existing latitude/longitude"""
    Location = apps.get_model('locations', 'Location')
    
    for location in Location.objects.all():
        if location.latitude and location.longitude:
            try:
                location.location = Point(
                    float(location.longitude),
                    float(location.latitude),
                    srid=4326
                )
                location.save(update_fields=['location'])
            except (ValueError, TypeError) as e:
                print(f"Warning: Could not create point for location {location.id}: {e}")


def reverse_populate(apps, schema_editor):
    """Reverse migration - clear location field"""
    Location = apps.get_model('locations', 'Location')
    Location.objects.all().update(location=None)


class Migration(migrations.Migration):

    dependencies = [
        ('locations', '0023_alter_providerfundingsource_provider_and_more'),
    ]

    operations = [
        migrations.AddField(
            model_name='location',
            name='location',
            field=gis_models.PointField(
                blank=True,
                geography=True,
                null=True,
                srid=4326,
                help_text='PostGIS spatial field for efficient geographic queries'
            ),
        ),
        migrations.RunPython(populate_location_points, reverse_populate),
    ]

