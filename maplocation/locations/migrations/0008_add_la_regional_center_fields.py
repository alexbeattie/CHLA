# Generated manually to add LA Regional Center fields

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("locations", "0003_providerv2"),
    ]

    operations = [
        migrations.AddField(
            model_name="regionalcenter",
            name="zip_codes",
            field=models.JSONField(
                blank=True,
                help_text="List of ZIP codes served by this regional center (LA-specific)",
                null=True,
            ),
        ),
        migrations.AddField(
            model_name="regionalcenter",
            name="service_areas",
            field=models.JSONField(
                blank=True,
                help_text="List of service area names (LA-specific)",
                null=True,
            ),
        ),
        migrations.AddField(
            model_name="regionalcenter",
            name="is_la_regional_center",
            field=models.BooleanField(
                default=False,
                help_text="Whether this is a Los Angeles County regional center",
            ),
        ),
    ]
