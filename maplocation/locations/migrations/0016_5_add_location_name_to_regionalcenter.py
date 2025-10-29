# Generated migration to add location_name field before fixture loading

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('locations', '0016_fix_la_regional_center_flags'),
    ]

    operations = [
        migrations.AddField(
            model_name='regionalcenter',
            name='location_name',
            field=models.CharField(max_length=100, blank=True, null=True),
        ),
    ]
