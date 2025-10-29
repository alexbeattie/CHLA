# Generated migration to add location_name field before fixture loading

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('locations', '0022_alter_providerfundingsource_provider_and_more'),
    ]

    operations = [
        migrations.AddField(
            model_name='regionalcenter',
            name='location_name',
            field=models.CharField(max_length=100, blank=True, null=True),
        ),
    ]
