from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("locations", "0005_modify_provider_areas_field"),
    ]

    operations = [
        # Drop existing provider tables if they exist
        migrations.RunSQL("DROP TABLE IF EXISTS provider_coverage_areas CASCADE;", ""),
        migrations.RunSQL("DROP TABLE IF EXISTS provider_services CASCADE;", ""),
        migrations.RunSQL("DROP TABLE IF EXISTS provider_specializations CASCADE;", ""),
        migrations.RunSQL("DROP TABLE IF EXISTS provider_insurance CASCADE;", ""),
        migrations.RunSQL("DROP TABLE IF EXISTS providers CASCADE;", ""),
        # Create provider model in Django ORM
        migrations.CreateModel(
            name="Provider",
            fields=[
                (
                    "id",
                    models.AutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("name", models.CharField(max_length=255)),
                ("phone", models.CharField(blank=True, max_length=100, null=True)),
                ("coverage_areas", models.TextField(blank=True, null=True)),
                ("center_based_services", models.TextField(blank=True, null=True)),
                ("areas", models.TextField(blank=True, null=True)),
            ],
            options={
                "db_table": "providers",
            },
        ),
    ]
