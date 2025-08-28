#!/usr/bin/env python
"""
Create client users with limited permissions for the admin portal.
Run this script with: python manage.py shell < create_client_users.py
"""

from django.contrib.auth.models import User, Group, Permission
from django.contrib.contenttypes.models import ContentType

# Create or get the client group
client_group, created = Group.objects.get_or_create(name='Clients')

if created:
    print("Created new 'Clients' group")
else:
    print("Using existing 'Clients' group")

# Import the models
try:
    from locations.models import Provider, ProviderV2, RegionalCenter
    
    # Get content types
    provider_ct = ContentType.objects.get_for_model(Provider)
    provider2_ct = ContentType.objects.get_for_model(ProviderV2)
    regional_center_ct = ContentType.objects.get_for_model(RegionalCenter)
    
    # Clear existing permissions
    client_group.permissions.clear()
    
    # Add permissions (add and change only, no delete)
    permissions_to_add = [
        # Provider permissions
        Permission.objects.get(content_type=provider_ct, codename='add_provider'),
        Permission.objects.get(content_type=provider_ct, codename='change_provider'),
        Permission.objects.get(content_type=provider_ct, codename='view_provider'),
        
        # ProviderV2 permissions
        Permission.objects.get(content_type=provider2_ct, codename='add_providerv2'),
        Permission.objects.get(content_type=provider2_ct, codename='change_providerv2'),
        Permission.objects.get(content_type=provider2_ct, codename='view_providerv2'),
        
        # Regional Center permissions (view only)
        Permission.objects.get(content_type=regional_center_ct, codename='view_regionalcenter'),
    ]
    
    client_group.permissions.add(*permissions_to_add)
    print(f"Added {len(permissions_to_add)} permissions to Clients group")
    
except Exception as e:
    print(f"Error setting permissions: {e}")

# Create a sample client user
username = 'client1'
email = 'client@example.com'
password = 'client-password-123'  # Change this!

if User.objects.filter(username=username).exists():
    print(f"User '{username}' already exists")
    client_user = User.objects.get(username=username)
else:
    client_user = User.objects.create_user(
        username=username,
        password=password,
        email=email,
        first_name='Client',
        last_name='User',
        is_staff=True  # Required for admin access
    )
    client_user.groups.add(client_group)
    print(f"Created user '{username}' with password '{password}'")
    print("*** IMPORTANT: Change this password immediately! ***")

# Display summary
print("\n=== Client Access Summary ===")
print(f"Group: {client_group.name}")
print(f"Permissions: {client_group.permissions.count()} total")
print(f"Users in group: {User.objects.filter(groups=client_group).count()}")
print("\nAccess URL: /client-portal/")
print("Basic Auth: Set BASIC_AUTH_USERNAME and BASIC_AUTH_PASSWORD in .env")
print("Django Login: Use the client username and password created above")
