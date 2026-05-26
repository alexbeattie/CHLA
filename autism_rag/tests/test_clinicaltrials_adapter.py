from autism_rag.ingestion.chunker import chunk_document
from autism_rag.sources.adapters.clinicaltrials import ClinicalTrialsAdapter


def test_clinical_trials_adapter_indexes_locations_as_text_and_flat_metadata():
    study = {
        "protocolSection": {
            "identificationModule": {
                "nctId": "NCT123",
                "briefTitle": "Autism trial near Los Angeles",
            },
            "conditionsModule": {"conditions": ["Autism Spectrum Disorder"]},
            "armsInterventionsModule": {
                "interventions": [{"name": "Care navigation"}]
            },
            "descriptionModule": {"briefSummary": "Testing a support intervention."},
            "eligibilityModule": {"eligibilityCriteria": "Ages 6 to 17."},
            "statusModule": {
                "overallStatus": "RECRUITING",
                "startDateStruct": {"date": "2025-01"},
            },
            "contactsLocationsModule": {
                "locations": [
                    {
                        "facility": "Children's Hospital Los Angeles",
                        "city": "Los Angeles",
                        "state": "California",
                        "zip": "90027",
                        "country": "United States",
                        "status": "RECRUITING",
                        "geoPoint": {"lat": 34.098, "lon": -118.29},
                    }
                ]
            },
        }
    }

    docs = list(ClinicalTrialsAdapter()._normalize([study]))

    assert len(docs) == 1
    doc = docs[0]
    assert "Locations: Children's Hospital Los Angeles - Los Angeles, California, United States - RECRUITING" in doc.text
    assert doc.extra["location_facilities"] == ["Children's Hospital Los Angeles"]
    assert doc.extra["location_cities"] == ["Los Angeles"]
    assert doc.extra["location_states"] == ["California"]
    assert doc.extra["location_zips"] == ["90027"]
    assert doc.extra["location_coordinates"] == ["34.098,-118.29"]

    chunk = chunk_document(doc)[0]
    assert chunk.metadata["extra_location_cities"] == ["Los Angeles"]
    assert chunk.metadata["extra_location_zips"] == ["90027"]
    assert all(not isinstance(value, dict) for value in chunk.metadata.values())
