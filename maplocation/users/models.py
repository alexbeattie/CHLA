from django.db import models
from django.contrib.auth.models import User
from django.utils.translation import gettext_lazy as _


class UserProfile(models.Model):
    """
    User profile model to store additional information about users
    """

    DIAGNOSIS_CHOICES = [
        ("autism", "Autism"),
        ("dup15q", "dup15q"),
        ("tsc", "TSC"),
        ("adhd", "ADHD"),
        ("angelman", "Angelman Syndrome"),
        ("rhetts", "Rhetts"),
        ("anxiety_depression", "Anxiety/Depression"),
        ("other", "Other"),
    ]

    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name="profile")
    age = models.PositiveIntegerField(null=True, blank=True)
    address = models.CharField(max_length=255, blank=True)
    city = models.CharField(max_length=100, blank=True)
    state = models.CharField(max_length=50, blank=True)
    zip_code = models.CharField(max_length=20, blank=True)
    latitude = models.DecimalField(
        max_digits=10, decimal_places=7, null=True, blank=True
    )
    longitude = models.DecimalField(
        max_digits=10, decimal_places=7, null=True, blank=True
    )
    diagnosis = models.CharField(max_length=50, choices=DIAGNOSIS_CHOICES, blank=True)
    other_diagnosis = models.CharField(max_length=100, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"{self.user.username}'s Profile"

    def get_diagnosis_display_name(self):
        """Get the display name for the diagnosis"""
        if self.diagnosis == "other" and self.other_diagnosis:
            return self.other_diagnosis
        return dict(self.DIAGNOSIS_CHOICES).get(self.diagnosis, "")
