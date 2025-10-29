# Generated migration to drop the old providers table
# The Provider model has been replaced by ProviderV2
# Data was migrated via migration 0010_copy_providers_to_providerv2

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ("locations", "0021_restore_regional_center_zip_codes"),
    ]

    operations = [
        migrations.RunSQL(
            # Drop the old providers table
            sql="DROP TABLE IF EXISTS providers CASCADE;",
            # Reverse migration would need to recreate the table (not recommended)
            reverse_sql="-- Cannot reverse: old providers table has been permanently removed",
        ),
    ]
