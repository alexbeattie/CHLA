from rest_framework import serializers
from django.contrib.auth.models import User
from .models import UserProfile

class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'first_name', 'last_name']
        read_only_fields = ['username']

class UserProfileSerializer(serializers.ModelSerializer):
    user = UserSerializer(read_only=True)
    diagnosis_display = serializers.SerializerMethodField()
    
    class Meta:
        model = UserProfile
        fields = [
            'id', 'user', 'age', 'address', 'city', 'state', 'zip_code',
            'latitude', 'longitude', 'diagnosis', 'other_diagnosis',
            'diagnosis_display', 'created_at', 'updated_at'
        ]
    
    def get_diagnosis_display(self, obj):
        return obj.get_diagnosis_display_name()