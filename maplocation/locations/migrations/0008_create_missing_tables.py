from django.db import migrations


class Migration(migrations.Migration):
    dependencies = [
        ("locations", "0001_initial"),
    ]

    operations = [
        migrations.RunSQL(
            """
            -- Create insurance carriers table
            CREATE TABLE IF NOT EXISTS locations_insurancecarrier (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) UNIQUE NOT NULL,
                description TEXT NULL
            );
            
            -- Create funding sources table
            CREATE TABLE IF NOT EXISTS locations_fundingsource (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) UNIQUE NOT NULL,
                description TEXT NULL
            );
            
            -- Create service delivery models table
            CREATE TABLE IF NOT EXISTS locations_servicedeliverymodel (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) UNIQUE NOT NULL,
                description TEXT NULL
            );
            
            -- Create provider funding source table
            CREATE TABLE IF NOT EXISTS locations_providerfundingsource (
                id SERIAL PRIMARY KEY,
                provider_id INTEGER REFERENCES providers(id) ON DELETE CASCADE,
                funding_source_id INTEGER REFERENCES locations_fundingsource(id) ON DELETE CASCADE,
                UNIQUE(provider_id, funding_source_id)
            );
            
            -- Create provider insurance carrier table
            CREATE TABLE IF NOT EXISTS locations_providerinsurancecarrier (
                id SERIAL PRIMARY KEY,
                provider_id INTEGER REFERENCES providers(id) ON DELETE CASCADE,
                insurance_carrier_id INTEGER REFERENCES locations_insurancecarrier(id) ON DELETE CASCADE,
                UNIQUE(provider_id, insurance_carrier_id)
            );
            
            -- Create provider service model table
            CREATE TABLE IF NOT EXISTS locations_providerservicemodel (
                id SERIAL PRIMARY KEY,
                provider_id INTEGER REFERENCES providers(id) ON DELETE CASCADE,
                service_model_id INTEGER REFERENCES locations_servicedeliverymodel(id) ON DELETE CASCADE,
                UNIQUE(provider_id, service_model_id)
            );
            
            -- Insert initial data
            INSERT INTO locations_fundingsource (name, description) VALUES
            ('Private Pay', 'Services paid directly by families'),
            ('Regional Center', 'Services funded through California Regional Centers'),
            ('School/IEP', 'Services provided through school district and IEP'),
            ('Insurance', 'Services covered by health insurance plans')
            ON CONFLICT (name) DO NOTHING;
            
            INSERT INTO locations_insurancecarrier (name) VALUES
            ('Anthem Blue Cross'),
            ('Blue Shield of California'),
            ('Cigna'),
            ('Aetna'),
            ('Magellan Health Services'),
            ('United Behavioral Health'),
            ('LifeSynch'),
            ('Humana')
            ON CONFLICT (name) DO NOTHING;
            
            INSERT INTO locations_servicedeliverymodel (name, description) VALUES
            ('Center-Based', 'Services provided at the provider''s facility'),
            ('Home-Based', 'Services provided in the client''s home'),
            ('School-Based', 'Services provided at the client''s school'),
            ('Telehealth', 'Services provided remotely via video conferencing')
            ON CONFLICT (name) DO NOTHING;
            """
        ),
    ]
