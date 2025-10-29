-- Migration script to add PostGIS spatial columns to existing RDS database
-- Run this AFTER enabling PostGIS extension on RDS
--
-- To run on RDS:
-- psql -h your-rds-endpoint.rds.amazonaws.com -U chla_admin -d your_db_name -f migrate_to_postgis.sql

-- 1. Enable PostGIS extension (requires rds_superuser role)
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Add location column to providers_v2 if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='providers_v2' AND column_name='location'
    ) THEN
        ALTER TABLE providers_v2 ADD COLUMN location geography(Point, 4326);

        -- Populate from existing lat/lng
        UPDATE providers_v2
        SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

        -- Create spatial index
        CREATE INDEX providers_v2_location_idx ON providers_v2 USING GIST(location);

        RAISE NOTICE 'Added location column to providers_v2 and migrated % rows',
            (SELECT COUNT(*) FROM providers_v2 WHERE location IS NOT NULL);
    ELSE
        RAISE NOTICE 'Location column already exists on providers_v2';
    END IF;
END $$;

-- 3. Add location_name column to regional_centers if needed
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='regional_centers' AND column_name='location_name'
    ) THEN
        ALTER TABLE regional_centers ADD COLUMN location_name varchar(100);
        RAISE NOTICE 'Added location_name column to regional_centers';
    ELSE
        RAISE NOTICE 'location_name column already exists on regional_centers';
    END IF;
END $$;

-- 4. Update regional_centers.location if it exists but is not populated
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='regional_centers' AND column_name='location'
    ) THEN
        -- Check if column type is correct
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name='regional_centers'
            AND column_name='location'
            AND data_type != 'USER-DEFINED'
        ) THEN
            -- Convert from varchar/text to geography
            ALTER TABLE regional_centers RENAME COLUMN location TO location_old;
            ALTER TABLE regional_centers ADD COLUMN location geography(Point, 4326);
            RAISE NOTICE 'Converted location column from text to geography';
        END IF;

        -- Populate location from lat/lng if null
        UPDATE regional_centers
        SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
        WHERE latitude IS NOT NULL
        AND longitude IS NOT NULL
        AND location IS NULL;

        -- Create index if it doesn't exist
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'regional_centers'
            AND indexname = 'idx_regional_centers_location'
        ) THEN
            CREATE INDEX idx_regional_centers_location ON regional_centers USING GIST(location);
            RAISE NOTICE 'Created spatial index on regional_centers.location';
        END IF;

        RAISE NOTICE 'Updated location column on regional_centers';
    END IF;
END $$;

-- 5. Update service_area column type if needed
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='regional_centers'
        AND column_name='service_area'
        AND data_type = 'text'
    ) THEN
        -- Convert from text to geometry
        ALTER TABLE regional_centers RENAME COLUMN service_area TO service_area_old;
        ALTER TABLE regional_centers ADD COLUMN service_area geometry(MultiPolygon, 4326);
        RAISE NOTICE 'Converted service_area from text to geometry';
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='regional_centers'
        AND column_name='service_area'
        AND udt_name = 'geometry'
    ) THEN
        RAISE NOTICE 'service_area column already has correct geometry type';
    END IF;
END $$;

-- 6. Verify migration
SELECT
    'providers_v2' as table_name,
    COUNT(*) as total_rows,
    COUNT(location) as with_location,
    COUNT(*) - COUNT(location) as missing_location
FROM providers_v2
UNION ALL
SELECT
    'regional_centers' as table_name,
    COUNT(*) as total_rows,
    COUNT(location) as with_location,
    COUNT(*) - COUNT(location) as missing_location
FROM regional_centers;

RAISE NOTICE 'PostGIS migration complete!';
