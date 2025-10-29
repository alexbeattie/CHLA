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
                    -- Only run if the providers table exists (it was dropped in migration 0021)
                    IF EXISTS (
                        SELECT 1 FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name = 'providers'
                    ) THEN
                        -- Try to set up the sequence, but ignore errors if it's an identity column
                        BEGIN
                            -- Ensure a sequence exists
                            IF NOT EXISTS (
                                SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                                WHERE c.relkind = 'S' AND c.relname = 'providers_id_seq'
                            ) THEN
                                CREATE SEQUENCE providers_id_seq;
                            END IF;

                            -- Try to own the sequence (will fail if it's an identity sequence)
                            ALTER SEQUENCE providers_id_seq OWNED BY providers.id;
                        EXCEPTION
                            WHEN feature_not_supported THEN
                                -- Identity sequences can't be altered, skip this migration
                                RAISE NOTICE 'Skipping sequence alteration - providers.id is an identity column';
                        END;

                        -- Set the default if not already set (will be skipped for identity columns)
                        BEGIN
                            ALTER TABLE providers ALTER COLUMN id SET DEFAULT nextval('providers_id_seq');
                        EXCEPTION
                            WHEN others THEN
                                RAISE NOTICE 'Skipping default setting - column may be an identity column';
                        END;

                        -- Align the sequence with the current max(id) if it exists
                        IF EXISTS (
                            SELECT 1 FROM pg_class c WHERE c.relkind = 'S' AND c.relname = 'providers_id_seq'
                        ) THEN
                            PERFORM setval('providers_id_seq', COALESCE((SELECT MAX(id) FROM providers), 0) + 1, false);
                        END IF;
                    END IF;
                END $$;
                """
            ),
            reverse_sql=(
                """
                DO $$
                BEGIN
                    -- Only run if the providers table exists
                    IF EXISTS (
                        SELECT 1 FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name = 'providers'
                    ) THEN
                        -- Try to remove default, but ignore errors
                        BEGIN
                            ALTER TABLE providers ALTER COLUMN id DROP DEFAULT;
                        EXCEPTION
                            WHEN others THEN
                                RAISE NOTICE 'Could not drop default - column may be an identity column';
                        END;
                    END IF;
                END $$;
                """
            ),
        ),
    ]
