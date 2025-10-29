import graphene
from graphene_django import DjangoObjectType
from django.db import models
from .models import LocationCategory, Location, LocationImage, LocationReview
import math


class LocationCategoryType(DjangoObjectType):
    class Meta:
        model = LocationCategory
        fields = "__all__"


class LocationImageType(DjangoObjectType):
    class Meta:
        model = LocationImage
        fields = ("id", "location", "image", "caption", "is_primary", "uploaded_at")


class LocationReviewType(DjangoObjectType):
    class Meta:
        model = LocationReview
        fields = ("id", "location", "name", "rating", "comment", "created_at")


class LocationType(DjangoObjectType):
    distance = graphene.Float()
    category_name = graphene.String()
    average_rating = graphene.Float()
    images = graphene.List(LocationImageType)
    reviews = graphene.List(LocationReviewType)

    class Meta:
        model = Location
        fields = (
            "id",
            "name",
            "address",
            "city",
            "state",
            "zip_code",
            "latitude",
            "longitude",
            "description",
            "phone",
            "website",
            "email",
            "is_active",
            "category",
            "created_at",
            "updated_at",
            "rating",
            "price_level",
            "hours_of_operation",
            "has_parking",
            "is_accessible",
        )

    def resolve_category_name(self, info):
        return self.category.name if self.category else ""

    def resolve_average_rating(self, info):
        if self.reviews.exists():
            return self.reviews.aggregate(models.Avg("rating"))["rating__avg"]
        return 0

    def resolve_images(self, info):
        return self.images.all()

    def resolve_reviews(self, info):
        return self.reviews.all()


class Query(graphene.ObjectType):
    all_categories = graphene.List(LocationCategoryType)
    all_locations = graphene.List(LocationType)
    location = graphene.Field(LocationType, id=graphene.ID())
    nearby_locations = graphene.List(
        LocationType,
        lat=graphene.Float(required=True),
        lng=graphene.Float(required=True),
        radius=graphene.Float(default_value=5.0),
    )

    def resolve_all_categories(self, info):
        return LocationCategory.objects.all()

    def resolve_all_locations(self, info):
        return Location.objects.filter(is_active=True)

    def resolve_location(self, info, id):
        try:
            return Location.objects.get(pk=id)
        except Location.DoesNotExist:
            return None

    def resolve_nearby_locations(self, info, lat, lng, radius):
        earth_radius = 3959  # Earth's radius in miles (use 3959 for miles, 6371 for kilometers)
        locations = Location.objects.filter(is_active=True)
        nearby_locations = []

        for location in locations:
            try:
                # Convert to radians
                lat1, lng1 = math.radians(lat), math.radians(lng)
                lat2, lng2 = math.radians(float(location.latitude)), math.radians(
                    float(location.longitude)
                )

                # Haversine formula
                dlng = lng2 - lng1
                dlat = lat2 - lat1
                a = (
                    math.sin(dlat / 2) ** 2
                    + math.cos(lat1) * math.cos(lat2) * math.sin(dlng / 2) ** 2
                )
                c = 2 * math.asin(math.sqrt(a))
                distance = earth_radius * c  # in miles

                if distance <= radius:
                    # Add distance to location
                    location.distance = round(distance, 2)
                    nearby_locations.append(location)
            except (ValueError, TypeError):
                continue

        # Sort by distance
        nearby_locations.sort(key=lambda x: x.distance)
        return nearby_locations
