from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.contrib.auth.models import User
from django.contrib.auth import get_user_model
from .models import UserProfile
from .serializers import UserSerializer, UserProfileSerializer
import uuid

class IsAuthenticatedOrAnonymousCreateOnly(permissions.BasePermission):
    """
    Custom permission to allow anonymous users to create and update profiles
    but only authenticated users can view or list all profiles.
    """
    
    def has_permission(self, request, view):
        # Anonymous users can use the 'me' or 'update_me' actions
        if view.action in ['me', 'update_me']:
            return True
        
        # Staff can access all actions
        if request.user.is_authenticated and request.user.is_staff:
            return True
            
        # For other actions, require authentication
        return request.user.is_authenticated

    def has_object_permission(self, request, view, obj):
        # Staff can access any object
        if request.user.is_authenticated and request.user.is_staff:
            return True
            
        # Users can access their own profiles
        if request.user.is_authenticated and obj.user == request.user:
            return True
            
        # Otherwise deny access
        return False

class UserProfileViewSet(viewsets.ModelViewSet):
    """
    API endpoint for user profiles
    """
    queryset = UserProfile.objects.all()
    serializer_class = UserProfileSerializer
    permission_classes = [IsAuthenticatedOrAnonymousCreateOnly]
    
    def get_queryset(self):
        """Return only the current user's profile unless staff"""
        if self.request.user.is_authenticated:
            if self.request.user.is_staff:
                return UserProfile.objects.all()
            return UserProfile.objects.filter(user=self.request.user)
        # For anonymous users with session id
        session_key = self.request.session.session_key
        if session_key:
            # Try to find or create anonymous user with session key as username
            User = get_user_model()
            anon_user, created = User.objects.get_or_create(
                username=f"anon_{session_key}",
                defaults={'email': f"anon_{session_key}@example.com"}
            )
            return UserProfile.objects.filter(user=anon_user)
        return UserProfile.objects.none()
    
    def _get_or_create_user(self, request):
        """Get or create a user for the request"""
        if request.user.is_authenticated:
            return request.user
        
        # For anonymous users, create a temporary user based on session
        User = get_user_model()
        session_key = request.session.session_key
        
        # Create a session if one doesn't exist
        if not session_key:
            request.session.create()
            session_key = request.session.session_key
        
        # Create a unique username based on session
        username = f"anon_{session_key}"
        
        # Get or create user
        user, created = User.objects.get_or_create(
            username=username,
            defaults={'email': f"{username}@example.com"}
        )
        
        return user
    
    @action(detail=False, methods=['get'])
    def me(self, request):
        """Get the current user's profile"""
        user = self._get_or_create_user(request)
        
        # Get or create profile
        profile, created = UserProfile.objects.get_or_create(user=user)
        serializer = self.get_serializer(profile)
        return Response(serializer.data)
    
    @action(detail=False, methods=['post', 'put', 'patch'])
    def update_me(self, request):
        """Update the current user's profile"""
        user = self._get_or_create_user(request)
        
        # Get or create profile
        profile, created = UserProfile.objects.get_or_create(user=user)
        
        # Update profile with request data
        serializer = self.get_serializer(profile, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        
        return Response(serializer.data)