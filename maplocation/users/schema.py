import graphene
from graphene_django import DjangoObjectType
from django.contrib.auth.models import User
from .models import UserProfile

class UserType(DjangoObjectType):
    class Meta:
        model = User
        fields = ('id', 'username', 'email', 'first_name', 'last_name')

class UserProfileType(DjangoObjectType):
    diagnosis_display = graphene.String()
    
    class Meta:
        model = UserProfile
        fields = (
            'id', 'user', 'age', 'address', 'city', 'state', 'zip_code',
            'latitude', 'longitude', 'diagnosis', 'other_diagnosis',
            'created_at', 'updated_at'
        )
    
    def resolve_diagnosis_display(self, info):
        return self.get_diagnosis_display_name()

class Query(graphene.ObjectType):
    user_profile = graphene.Field(UserProfileType)
    all_user_profiles = graphene.List(UserProfileType)
    
    def resolve_user_profile(self, info):
        user = info.context.user
        if user.is_anonymous:
            return None
        return UserProfile.objects.get_or_create(user=user)[0]
    
    def resolve_all_user_profiles(self, info):
        user = info.context.user
        if not user.is_staff:
            return UserProfile.objects.filter(user=user)
        return UserProfile.objects.all()

class UpdateUserProfile(graphene.Mutation):
    class Arguments:
        age = graphene.Int(required=False)
        address = graphene.String(required=False)
        city = graphene.String(required=False)
        state = graphene.String(required=False)
        zip_code = graphene.String(required=False)
        diagnosis = graphene.String(required=False)
        other_diagnosis = graphene.String(required=False)
    
    user_profile = graphene.Field(UserProfileType)
    
    def mutate(self, info, **kwargs):
        user = info.context.user
        if user.is_anonymous:
            raise Exception("Authentication required")
        
        profile, created = UserProfile.objects.get_or_create(user=user)
        
        # Update fields
        for field, value in kwargs.items():
            if value is not None:
                setattr(profile, field, value)
        
        profile.save()
        return UpdateUserProfile(user_profile=profile)

class Mutation(graphene.ObjectType):
    update_user_profile = UpdateUserProfile.Field()