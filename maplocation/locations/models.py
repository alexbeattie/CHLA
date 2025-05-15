from django.db import models
from django.contrib.gis.db import models as gis_models
from django.contrib.gis.geos import Point

class LocationCategory(models.Model):
    name = models.CharField(max_length=100)
    description = models.TextField(blank=True, null=True)
    
    def __str__(self):
        return self.name
    
    class Meta:
        verbose_name_plural = "Location Categories"

class Location(models.Model):
    name = models.CharField(max_length=200)
    address = models.CharField(max_length=255)
    city = models.CharField(max_length=100)
    state = models.CharField(max_length=50)
    zip_code = models.CharField(max_length=20)
    latitude = models.DecimalField(max_digits=10, decimal_places=7)
    longitude = models.DecimalField(max_digits=10, decimal_places=7)
    description = models.TextField(blank=True, null=True)
    phone = models.CharField(max_length=20, blank=True, null=True)
    website = models.URLField(blank=True, null=True)
    email = models.EmailField(blank=True, null=True)
    is_active = models.BooleanField(default=True)
    category = models.ForeignKey(LocationCategory, on_delete=models.CASCADE, related_name='locations')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    # Additional fields for filter options
    rating = models.DecimalField(max_digits=3, decimal_places=1, default=0.0)
    price_level = models.IntegerField(choices=[(1, '$'), (2, '$$'), (3, '$$$'), (4, '$$$$')], default=1)
    hours_of_operation = models.TextField(blank=True, null=True)
    has_parking = models.BooleanField(default=False)
    is_accessible = models.BooleanField(default=False)
    
    def __str__(self):
        return self.name

class RegionalCenter(models.Model):
    regional_center = models.CharField(max_length=200)
    office_type = models.CharField(max_length=50, blank=True, null=True)
    address = models.CharField(max_length=255)
    suite = models.CharField(max_length=50, blank=True, null=True)
    city = models.CharField(max_length=100)
    state = models.CharField(max_length=50)
    zip_code = models.CharField(max_length=20)
    telephone = models.CharField(max_length=20, blank=True, null=True)
    website = models.URLField(blank=True, null=True)
    county_served = models.CharField(max_length=100, blank=True, null=True)
    los_angeles_health_district = models.CharField(max_length=100, blank=True, null=True)
    location_coordinates = models.CharField(max_length=255, blank=True, null=True)
    latitude = models.FloatField(blank=True, null=True)
    longitude = models.FloatField(blank=True, null=True)
    location = models.CharField(max_length=100, blank=True, null=True)
    
    def __str__(self):
        return self.regional_center
    
    class Meta:
        verbose_name_plural = "Regional Centers"
        db_table = 'regional_centers'  # Match the existing table name

class Provider(models.Model):
    name = models.CharField(max_length=255)
    phone = models.CharField(max_length=100, blank=True, null=True)
    coverage_areas = models.TextField(blank=True, null=True)
    center_based_services = models.TextField(blank=True, null=True)
    areas = models.TextField(blank=True, null=True)  # Changed from JSONField to TextField
    
    # Now these are actual database fields, not virtual properties
    address = models.CharField(max_length=255, blank=True, null=True)
    city = models.CharField(max_length=100, blank=True, null=True)
    state = models.CharField(max_length=50, blank=True, null=True)
    zip_code = models.CharField(max_length=20, blank=True, null=True)
    latitude = models.DecimalField(max_digits=10, decimal_places=7, blank=True, null=True)
    longitude = models.DecimalField(max_digits=10, decimal_places=7, blank=True, null=True)
    location = gis_models.PointField(geography=True, blank=True, null=True, srid=4326)
    website = models.URLField(blank=True, null=True)
    age_groups_served = models.CharField(max_length=100, blank=True, null=True)
    diagnoses_served = models.TextField(blank=True, null=True)
    
    # New fields for funding and services
    accepts_insurance = models.BooleanField(default=False)
    accepts_private_pay = models.BooleanField(default=False)
    accepts_regional_center = models.BooleanField(default=False)
    accepts_school_funding = models.BooleanField(default=False)
    regional_centers_served = models.TextField(blank=True, null=True)
    telehealth_available = models.BooleanField(default=False)
    application_process = models.TextField(blank=True, null=True)
    eligibility_requirements = models.TextField(blank=True, null=True)
    waiting_list = models.BooleanField(default=False)
    waiting_list_time = models.CharField(max_length=100, blank=True, null=True)
    
    def save(self, *args, **kwargs):
        # Update location field based on latitude and longitude
        if self.latitude and self.longitude:
            self.location = Point(float(self.longitude), float(self.latitude), srid=4326)
        
        # Update accepts_* fields based on the relationships
        self.accepts_insurance = self.insurance_carriers.exists() if hasattr(self, 'insurance_carriers') else False
        self.accepts_private_pay = self.funding_sources.filter(
            funding_source__name='Private Pay').exists() if hasattr(self, 'funding_sources') else False
        self.accepts_regional_center = self.funding_sources.filter(
            funding_source__name='Regional Center').exists() if hasattr(self, 'funding_sources') else False
        self.accepts_school_funding = self.funding_sources.filter(
            funding_source__name='School/IEP').exists() if hasattr(self, 'funding_sources') else False
        
        super().save(*args, **kwargs)
    
    def __str__(self):
        return self.name
    
    class Meta:
        db_table = 'providers'  # Match the existing table name

class LocationImage(models.Model):
    location = models.ForeignKey(Location, on_delete=models.CASCADE, related_name='images')
    image = models.ImageField(upload_to='location_images/')
    caption = models.CharField(max_length=255, blank=True, null=True)
    is_primary = models.BooleanField(default=False)
    uploaded_at = models.DateTimeField(auto_now_add=True)
    
    def __str__(self):
        return f"Image for {self.location.name}"

class LocationReview(models.Model):
    location = models.ForeignKey(Location, on_delete=models.CASCADE, related_name='reviews')
    name = models.CharField(max_length=100)
    email = models.EmailField()
    rating = models.IntegerField(choices=[(1, '1'), (2, '2'), (3, '3'), (4, '4'), (5, '5')])
    comment = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)
    
    def __str__(self):
        return f"Review for {self.location.name} by {self.name}"


# New models for provider funding and service types
class FundingSource(models.Model):
    name = models.CharField(max_length=100, unique=True)
    description = models.TextField(blank=True, null=True)
    
    def __str__(self):
        return self.name


class InsuranceCarrier(models.Model):
    name = models.CharField(max_length=100, unique=True)
    description = models.TextField(blank=True, null=True)
    
    def __str__(self):
        return self.name


class ServiceDeliveryModel(models.Model):
    name = models.CharField(max_length=100, unique=True)
    description = models.TextField(blank=True, null=True)
    
    def __str__(self):
        return self.name


class ProviderFundingSource(models.Model):
    provider = models.ForeignKey(Provider, on_delete=models.CASCADE, related_name='funding_sources')
    funding_source = models.ForeignKey(FundingSource, on_delete=models.CASCADE, related_name='providers')
    
    class Meta:
        unique_together = ('provider', 'funding_source')
        
    def __str__(self):
        return f"{self.provider.name} - {self.funding_source.name}"


class ProviderInsuranceCarrier(models.Model):
    provider = models.ForeignKey(Provider, on_delete=models.CASCADE, related_name='insurance_carriers')
    insurance_carrier = models.ForeignKey(InsuranceCarrier, on_delete=models.CASCADE, related_name='providers')
    
    class Meta:
        unique_together = ('provider', 'insurance_carrier')
        
    def __str__(self):
        return f"{self.provider.name} - {self.insurance_carrier.name}"


class ProviderServiceModel(models.Model):
    provider = models.ForeignKey(Provider, on_delete=models.CASCADE, related_name='service_models')
    service_model = models.ForeignKey(ServiceDeliveryModel, on_delete=models.CASCADE, related_name='providers')
    
    class Meta:
        unique_together = ('provider', 'service_model')
        
    def __str__(self):
        return f"{self.provider.name} - {self.service_model.name}"
