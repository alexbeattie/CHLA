"""
Minimal admin configuration - only models with existing database tables
"""

from django.contrib import admin
from django.forms import (
    TextInput,
    Textarea,
    ModelForm,
    SelectMultiple,
    CheckboxSelectMultiple,
    MultipleChoiceField,
)
from django.db import models
import json
from decimal import Decimal, ROUND_HALF_UP, InvalidOperation

# Only import models we know exist in the database
from .models import ProviderV2, RegionalCenter
from .utils.mapbox_geocode import geocode_with_fallback


class ProviderV2Form(ModelForm):
    # Add a custom field for insurance selection
    insurance_selection = MultipleChoiceField(
        choices=ProviderV2.INSURANCE_CHOICES,
        widget=CheckboxSelectMultiple,
        required=False,
        help_text="Select all insurance types accepted by this provider",
    )

    # Add custom fields for age groups, diagnoses, and therapy types
    age_groups_selection = MultipleChoiceField(
        choices=ProviderV2.AGE_GROUP_CHOICES,
        widget=CheckboxSelectMultiple,
        required=False,
        help_text="Select all age groups this provider serves",
    )

    diagnoses_selection = MultipleChoiceField(
        choices=ProviderV2.DIAGNOSIS_CHOICES,
        widget=CheckboxSelectMultiple,
        required=False,
        help_text="Select all diagnoses/conditions this provider treats",
    )

    therapy_types_selection = MultipleChoiceField(
        choices=ProviderV2.THERAPY_TYPE_CHOICES,
        widget=CheckboxSelectMultiple,
        required=False,
        help_text="Select all therapy types offered by this provider",
    )

    class Meta:
        model = ProviderV2
        fields = "__all__"
        exclude = ["insurance_accepted"]  # Exclude from form validation
        widgets = {
            "insurance_accepted": Textarea(
                attrs={"rows": 3, "cols": 60, "readonly": True}
            ),
        }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        # Always make latitude and longitude optional in the form
        # The clean method will handle geocoding and validation
        self.fields["latitude"].required = False
        self.fields["longitude"].required = False

        # Initialize insurance_selection from insurance_accepted
        if self.instance and self.instance.pk:
            # Parse existing insurance_accepted value
            insurance_text = self.instance.insurance_accepted or ""
            if insurance_text:
                # Split by common delimiters and clean up
                selected_insurances = []
                for item in (
                    insurance_text.replace(",", "|").replace(";", "|").split("|")
                ):
                    item = item.strip().strip("\"'{}[]")
                    if item and item in [
                        choice[0] for choice in ProviderV2.INSURANCE_CHOICES
                    ]:
                        selected_insurances.append(item)
                self.fields["insurance_selection"].initial = selected_insurances

            # Initialize dropdown selections from JSON fields
            json_field_mappings = {
                "age_groups_selection": "age_groups",
                "diagnoses_selection": "diagnoses_treated",
                "therapy_types_selection": "therapy_types",
            }

            for form_field, model_field in json_field_mappings.items():
                field_value = getattr(self.instance, model_field, None)
                if field_value:
                    if isinstance(field_value, str):
                        try:
                            parsed_value = json.loads(field_value)
                            self.fields[form_field].initial = parsed_value
                        except (json.JSONDecodeError, TypeError):
                            self.fields[form_field].initial = (
                                [field_value] if field_value else []
                            )
                    else:
                        self.fields[form_field].initial = field_value
                else:
                    self.fields[form_field].initial = []

            # Initialize other JSON fields from existing data
            other_json_fields = ["funding_sources", "specific_areas_served"]
            for field_name in other_json_fields:
                field_value = getattr(self.instance, field_name, None)
                if field_value:
                    if isinstance(field_value, str):
                        try:
                            self.fields[field_name].initial = json.loads(field_value)
                        except (json.JSONDecodeError, TypeError):
                            self.fields[field_name].initial = field_value
                    else:
                        self.fields[field_name].initial = field_value

    def clean(self):
        cleaned = super().clean()

        # Handle insurance selection
        insurance_selection = cleaned.get("insurance_selection", [])
        if insurance_selection:
            # Convert list to comma-separated string
            cleaned["insurance_accepted"] = ", ".join(insurance_selection)
        else:
            # If no selection, require at least one
            self.add_error(
                "insurance_selection", "Please select at least one insurance type."
            )

        # Handle dropdown selections for JSON fields
        dropdown_mappings = {
            "age_groups_selection": "age_groups",
            "diagnoses_selection": "diagnoses_treated",
            "therapy_types_selection": "therapy_types",
        }

        print(f"DEBUG: Processing dropdown selections...")
        for form_field, model_field in dropdown_mappings.items():
            selection = cleaned.get(form_field, [])
            print(f"DEBUG: {form_field} -> {model_field}: {selection}")
            if selection:
                # Convert list to JSON array
                cleaned[model_field] = selection
                print(f"DEBUG: Set {model_field} to {cleaned[model_field]}")
            else:
                # Set empty list if no selection
                cleaned[model_field] = []
                print(f"DEBUG: Set {model_field} to empty list")

        # Normalize languages_spoken
        def normalize_text_or_json(value):
            if value is None:
                return value
            text = str(value).strip()
            if not text:
                return ""
            # If JSON array provided, convert to comma-separated string
            if text.startswith("[") and text.endswith("]"):
                try:
                    arr = json.loads(text)
                    if isinstance(arr, list):
                        return ", ".join(
                            [str(x).strip() for x in arr if str(x).strip()]
                        )
                except:
                    pass
            return text

        # Handle JSON fields
        json_fields = [
            "hours",
            "age_groups",
            "diagnoses_treated",
            "therapy_types",
            "funding_sources",
            "specific_areas_served",
        ]
        for field_name in json_fields:
            field_value = cleaned.get(field_name)
            if field_value and isinstance(field_value, str):
                try:
                    # Try to parse as JSON if it's a string
                    cleaned[field_name] = json.loads(field_value)
                except (json.JSONDecodeError, TypeError):
                    # If not valid JSON, treat as plain text
                    cleaned[field_name] = field_value

        cleaned["languages_spoken"] = normalize_text_or_json(
            cleaned.get("languages_spoken")
        )

        # Geocode if address provided and coordinates missing
        address = cleaned.get("address")
        lat = cleaned.get("latitude")
        lng = cleaned.get("longitude")

        # Helper function to properly format coordinates
        def format_coordinate(value, field_name):
            if value is None or value == "":
                return None
            try:
                # Convert to Decimal and round to 6 decimal places
                decimal_value = Decimal(str(value)).quantize(
                    Decimal("0.000001"), rounding=ROUND_HALF_UP
                )
                return decimal_value
            except (ValueError, InvalidOperation):
                self.add_error(field_name, f"Invalid coordinate value: {value}")
                return None

        if address and (lat in (None, "") or lng in (None, "")):
            coords = geocode_with_fallback(address)
            if coords:
                cleaned["latitude"] = format_coordinate(coords[0], "latitude")
                cleaned["longitude"] = format_coordinate(coords[1], "longitude")
            else:
                # If geocoding fails, require manual input
                if not lat:
                    self.add_error(
                        "latitude",
                        "Please provide latitude or use a more specific address that can be geocoded.",
                    )
                if not lng:
                    self.add_error(
                        "longitude",
                        "Please provide longitude or use a more specific address that can be geocoded.",
                    )
        else:
            # Format existing coordinates
            if lat is not None and lat != "":
                cleaned["latitude"] = format_coordinate(lat, "latitude")
            if lng is not None and lng != "":
                cleaned["longitude"] = format_coordinate(lng, "longitude")

            # If no address provided, coordinates are required
            if not lat:
                self.add_error("latitude", "This field is required.")
            if not lng:
                self.add_error("longitude", "This field is required.")

        return cleaned

    def save(self, commit=True):
        instance = super().save(commit=False)

        # Debug logging
        print(f"DEBUG: Saving provider {instance.name}")
        print(
            f"DEBUG: insurance_selection: {self.cleaned_data.get('insurance_selection', [])}"
        )
        print(
            f"DEBUG: age_groups_selection: {self.cleaned_data.get('age_groups_selection', [])}"
        )
        print(
            f"DEBUG: diagnoses_selection: {self.cleaned_data.get('diagnoses_selection', [])}"
        )
        print(
            f"DEBUG: therapy_types_selection: {self.cleaned_data.get('therapy_types_selection', [])}"
        )

        # Ensure insurance_accepted is set from insurance_selection
        insurance_selection = self.cleaned_data.get("insurance_selection", [])
        if insurance_selection:
            instance.insurance_accepted = ", ".join(insurance_selection)

        # Ensure JSON fields are set from dropdown selections
        instance.age_groups = self.cleaned_data.get("age_groups", [])
        instance.diagnoses_treated = self.cleaned_data.get("diagnoses_treated", [])
        instance.therapy_types = self.cleaned_data.get("therapy_types", [])

        print(f"DEBUG: Final instance values:")
        print(f"DEBUG: age_groups: {instance.age_groups}")
        print(f"DEBUG: diagnoses_treated: {instance.diagnoses_treated}")
        print(f"DEBUG: therapy_types: {instance.therapy_types}")

        if commit:
            instance.save()
            print(f"DEBUG: Provider saved to database")
        return instance


@admin.register(ProviderV2)
class ProviderV2Admin(admin.ModelAdmin):
    form = ProviderV2Form
    list_display = [
        "name",
        "type",
        "phone",
        "get_age_groups",
        "get_diagnoses",
        "get_therapy_types",
    ]
    list_filter = ["type"]
    search_fields = [
        "name",
        "address",
        "type",
        "insurance_accepted",
    ]
    list_per_page = 50  # Paginate to avoid loading all records at once
    actions = ["import_pasadena_providers", "import_san_gabriel_providers"]

    def get_age_groups(self, obj):
        """Display age groups as comma-separated string"""
        if obj.age_groups and isinstance(obj.age_groups, list):
            return ", ".join(obj.age_groups)
        return "-"

    get_age_groups.short_description = "Age Groups"

    def get_diagnoses(self, obj):
        """Display diagnoses as comma-separated string"""
        if obj.diagnoses_treated and isinstance(obj.diagnoses_treated, list):
            return ", ".join(obj.diagnoses_treated)
        return "-"

    get_diagnoses.short_description = "Diagnoses"

    def get_therapy_types(self, obj):
        """Display therapy types as comma-separated string"""
        if obj.therapy_types and isinstance(obj.therapy_types, list):
            return ", ".join(obj.therapy_types)
        return "-"

    get_therapy_types.short_description = "Therapy Types"

    fieldsets = (
        ("Basic Information", {"fields": ("name", "type", "description")}),
        (
            "Contact Information",
            {"fields": ("phone", "email", "website")},
        ),
        (
            "Address & Location",
            {
                "fields": ("address", "latitude", "longitude"),
                "description": "Coordinates will be automatically geocoded from address if left empty.",
            },
        ),
        (
            "Service Details",
            {
                "fields": ("hours", "insurance_selection", "languages_spoken"),
                "description": "Select insurance types from the checkboxes above.",
            },
        ),
        (
            "Age Groups & Diagnoses",
            {
                "fields": ("age_groups_selection", "diagnoses_selection"),
                "description": "Age groups and diagnoses this provider serves (from onboarding flow)",
            },
        ),
        (
            "Therapy Types",
            {
                "fields": ("therapy_types_selection",),
                "description": "Types of therapy offered (e.g., ABA therapy, Speech therapy, etc.)",
            },
        ),
        (
            "Funding & Payment",
            {
                "fields": (
                    "funding_sources",
                    "accepts_private_pay",
                    "accepts_regional_center",
                    "accepts_insurance",
                ),
                "description": "Funding sources and payment options accepted",
            },
        ),
        (
            "Service Delivery",
            {
                "fields": (
                    "in_person_services",
                    "virtual_services",
                    "home_based_services",
                    "center_based_services",
                ),
                "description": "How services are delivered",
            },
        ),
        (
            "Provider Qualifications",
            {
                "fields": ("license_number", "license_type", "years_experience"),
                "description": "Professional qualifications and experience",
            },
        ),
        (
            "Service Area",
            {
                "fields": (
                    "service_radius_miles",
                    "serves_la_county",
                    "specific_areas_served",
                ),
                "description": "Geographic service area and coverage",
            },
        ),
        (
            "System Information",
            {
                "fields": ("id", "created_at", "updated_at"),
                "classes": ("collapse",),
            },
        ),
    )

    readonly_fields = ["id", "created_at", "updated_at"]

    formfield_overrides = {
        models.CharField: {"widget": TextInput(attrs={"size": "80"})},
        models.TextField: {"widget": Textarea(attrs={"rows": 3, "cols": 80})},
    }

    def save_model(self, request, obj, form, change):
        """Override to add any custom save logic"""
        super().save_model(request, obj, form, change)

    def get_queryset(self, request):
        """Optimize queryset to avoid N+1 queries"""
        qs = super().get_queryset(request)
        # Add any select_related or prefetch_related here if needed
        return qs

    def import_pasadena_providers(self, request, queryset):
        """Import Pasadena providers from Excel file"""
        from django.core.management import call_command
        from django.conf import settings
        from io import StringIO
        import os

        output = StringIO()
        try:
            # Try multiple possible paths
            possible_paths = [
                "/var/app/current/data/Pasadena Provider List.xlsx",  # EB deployment path
                os.path.join(
                    settings.BASE_DIR, "data", "Pasadena Provider List.xlsx"
                ),  # Inside maplocation
                os.path.join(
                    settings.BASE_DIR, "..", "data", "Pasadena Provider List.xlsx"
                ),  # One level up (old location)
                os.path.join(
                    os.path.dirname(settings.BASE_DIR),
                    "data",
                    "Pasadena Provider List.xlsx",
                ),  # Parent dir
            ]

            file_path = None
            for path in possible_paths:
                if os.path.exists(path):
                    file_path = path
                    break

            if not file_path:
                self.message_user(
                    request,
                    f"❌ File not found. Tried: {', '.join(possible_paths)}",
                    level="ERROR",
                )
                return

            call_command(
                "import_regional_center_providers",
                file=file_path,
                area="Pasadena",
                stdout=output,
            )
            self.message_user(
                request,
                f"✅ Pasadena providers import completed!\n\n{output.getvalue()}",
                level="SUCCESS",
            )
        except Exception as e:
            self.message_user(request, f"❌ Import failed: {str(e)}", level="ERROR")

    import_pasadena_providers.short_description = "Import Pasadena providers from Excel"

    def import_san_gabriel_providers(self, request, queryset):
        """Import San Gabriel/Pomona providers from Excel file"""
        from django.core.management import call_command
        from django.conf import settings
        from io import StringIO
        import os

        output = StringIO()
        try:
            # Try multiple possible paths
            possible_paths = [
                "/var/app/current/data/San Gabriel Pomona Provider List.xlsx",  # EB deployment path
                os.path.join(
                    settings.BASE_DIR, "data", "San Gabriel Pomona Provider List.xlsx"
                ),  # Inside maplocation
                os.path.join(
                    settings.BASE_DIR,
                    "..",
                    "data",
                    "San Gabriel Pomona Provider List.xlsx",
                ),  # One level up (old location)
                os.path.join(
                    os.path.dirname(settings.BASE_DIR),
                    "data",
                    "San Gabriel Pomona Provider List.xlsx",
                ),  # Parent dir
            ]

            file_path = None
            for path in possible_paths:
                if os.path.exists(path):
                    file_path = path
                    break

            if not file_path:
                self.message_user(
                    request,
                    f"❌ File not found. Tried: {', '.join(possible_paths)}",
                    level="ERROR",
                )
                return

            call_command(
                "import_regional_center_providers",
                file=file_path,
                regional_center="San Gabriel",
                stdout=output,
            )
            self.message_user(
                request,
                f"✅ San Gabriel/Pomona providers import completed!\n\n{output.getvalue()}",
                level="SUCCESS",
            )
        except Exception as e:
            self.message_user(request, f"❌ Import failed: {str(e)}", level="ERROR")

    import_san_gabriel_providers.short_description = (
        "Import San Gabriel/Pomona providers from Excel"
    )


@admin.register(RegionalCenter)
class RegionalCenterAdmin(admin.ModelAdmin):
    list_display = [
        "regional_center",
        "office_type",
        "city",
        "county_served",
        "telephone",
    ]
    list_filter = ["office_type", "county_served", "city"]
    search_fields = ["regional_center", "address", "city", "county_served"]
    list_per_page = 50

    fieldsets = (
        (
            "Basic Information",
            {
                "fields": (
                    "regional_center",
                    "office_type",
                    "county_served",
                    "los_angeles_health_district",
                )
            },
        ),
        ("Contact Information", {"fields": ("telephone", "website")}),
        (
            "Address",
            {
                "fields": ("address", "suite", "city", "state", "zip_code"),
            },
        ),
        (
            "Location Data",
            {
                "fields": ("latitude", "longitude", "location_coordinates", "location"),
                "classes": ("collapse",),
            },
        ),
        (
            "Service Area",
            {
                "fields": ("service_area", "service_radius_miles"),
                "classes": ("collapse",),
            },
        ),
    )

    readonly_fields = ["location_coordinates"]

    formfield_overrides = {
        models.CharField: {"widget": TextInput(attrs={"size": "60"})},
        models.TextField: {"widget": Textarea(attrs={"rows": 3, "cols": 60})},
    }


# Customize admin site header
admin.site.site_header = "CHLA Provider Portal"
admin.site.site_title = "Provider Portal"
admin.site.index_title = "Provider Management"
