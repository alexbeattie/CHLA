from django.db import migrations

class Migration(migrations.Migration):

    dependencies = [
        ("locations", "0001_initial"),
    ]

    operations = [
        migrations.RunSQL(
            sql=(
                """
                DO $$
                BEGIN
                    -- Ensure a sequence exists and is owned by providers.id
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE c.relkind = 'S' AND c.relname = 'providers_id_seq'
                    ) THEN
                        CREATE SEQUENCE providers_id_seq;
                    END IF;

                    -- Own the sequence by the providers.id column
                    ALTER SEQUENCE providers_id_seq OWNED BY providers.id;

                    -- Set the default on providers.id to use the sequence
                    ALTER TABLE providers ALTER COLUMN id SET DEFAULT nextval('providers_id_seq');

                    -- Align the sequence with the current max(id)
                    PERFORM setval('providers_id_seq', COALESCE((SELECT MAX(id) FROM providers), 0) + 1, false);
                END $$;
                """
            ),
            reverse_sql=(
                """
                DO $$
                BEGIN
                    -- Remove default to revert
                    ALTER TABLE providers ALTER COLUMN id DROP DEFAULT;
                    -- Keep sequence in case other rows depend on it; do not drop in reverse
                END $$;
                """
            ),
        ),
    ]
