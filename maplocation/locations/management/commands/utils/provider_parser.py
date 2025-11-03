"""Provider data parsing and normalization utilities."""
from typing import Dict, List, Optional, Any


class TherapyTypeParser:
    """Parses service descriptions into standardized therapy types."""

    # Map keywords to therapy types
    THERAPY_MAPPINGS = {
        "aba": "ABA therapy",
        "applied behavior": "ABA therapy",
        "speech": "Speech therapy",
        "occupational": "Occupational therapy",
        "ot": "Occupational therapy",
        "physical": "Physical therapy",
        "pt": "Physical therapy",
        "feeding": "Feeding therapy",
        "parent": "Parent child interaction therapy/parent training behavior management",
    }

    @classmethod
    def parse(cls, services_text: str) -> Optional[List[str]]:
        """
        Parse services text into standardized therapy types.
        
        Args:
            services_text: Raw services/therapy description text
            
        Returns:
            List of therapy type strings, or None if no matches
        """
        if not services_text:
            return None

        therapy_types = []
        services_lower = services_text.lower()

        for keyword, therapy_type in cls.THERAPY_MAPPINGS.items():
            if keyword in services_lower and therapy_type not in therapy_types:
                therapy_types.append(therapy_type)

        return therapy_types if therapy_types else None


class InsuranceParser:
    """Parses insurance information into standardized format."""

    # Map keywords to insurance names
    INSURANCE_KEYWORDS = {
        "medi-cal": "Medi-Cal",
        "medicaid": "Medicaid",
        "medicare": "Medicare",
        "blue cross": "Blue Cross",
        "blue shield": "Blue Shield",
        "anthem": "Anthem",
        "aetna": "Aetna",
        "cigna": "Cigna",
        "kaiser": "Kaiser Permanente",
        "united": "United Healthcare",
        "health net": "Health Net",
        "molina": "Molina",
        "la care": "L.A. Care",
        "l.a. care": "L.A. Care",
    }

    @classmethod
    def parse(cls, insurance_text: str) -> tuple[List[str], Dict[str, bool]]:
        """
        Parse insurance text into list of accepted insurances and acceptance flags.
        
        Args:
            insurance_text: Raw insurance/payment description text
            
        Returns:
            Tuple of (insurance_list, accepts_flags)
        """
        insurance_list = []
        accepts_flags = {
            "accepts_insurance": False,
            "accepts_regional_center": True,  # Default to True for regional center lists
            "accepts_private_pay": False,
        }

        # Always add Regional Center since these are regional center provider lists
        insurance_list.append("Regional Center")

        if not insurance_text:
            return insurance_list, accepts_flags

        insurance_lower = insurance_text.lower()

        # Check for Private Pay
        if any(term in insurance_lower for term in ["private", "self pay", "cash"]):
            insurance_list.append("Private Pay")
            accepts_flags["accepts_private_pay"] = True

        # Check for insurance types
        for keyword, insurance_name in cls.INSURANCE_KEYWORDS.items():
            if keyword in insurance_lower and insurance_name not in insurance_list:
                insurance_list.append(insurance_name)
                accepts_flags["accepts_insurance"] = True

        return insurance_list, accepts_flags


class ProviderDataParser:
    """Parses raw provider data into normalized format for database."""

    @staticmethod
    def parse(
        row_data: Dict[str, Any],
        column_map: Dict[str, str],
        area_name: str,
        get_value_func
    ) -> Dict[str, Any]:
        """
        Parse provider data from Excel row into normalized database format.
        
        Args:
            row_data: Raw row data from Excel
            column_map: Mapping of field names to column names
            area_name: Geographic area name for the provider (currently not stored)
            get_value_func: Function to safely extract values from row_data
            
        Returns:
            Dictionary of normalized provider data ready for database
            
        Note:
            Only uses fields that exist in ProviderV2 model.
            Many fields were removed in migration 0030_drop_unused_provider_fields.
        """
        data = {
            "name": get_value_func(row_data, column_map.get("name", "")),
            "phone": get_value_func(row_data, column_map.get("phone")),
            "email": get_value_func(row_data, column_map.get("email")),
            "website": get_value_func(row_data, column_map.get("website")),
            "address": get_value_func(row_data, column_map.get("address", "")),
        }

        # Parse services into therapy_types (JSONField)
        services_text = get_value_func(row_data, column_map.get("services"))
        if services_text:
            therapy_types = TherapyTypeParser.parse(services_text)
            if therapy_types:
                data["therapy_types"] = therapy_types

        # Parse insurance into legacy insurance_accepted text field
        insurance_text = get_value_func(row_data, column_map.get("insurance"))
        insurance_list, _ = InsuranceParser.parse(insurance_text or "")
        if insurance_list:
            data["insurance_accepted"] = ", ".join(insurance_list)

        # Parse notes into description
        notes = get_value_func(row_data, column_map.get("notes"))
        if notes:
            data["description"] = notes

        return data

