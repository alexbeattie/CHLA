from django.contrib.auth.models import User
from django.contrib.contenttypes.models import ContentType
from locations.models import ProviderV2

# Check if client1 has permissions
try:
    u = User.objects.get(username="client1")
    print(f"User: {u.username}")
    print(f"Is staff: {u.is_staff}")
    print(f"Is superuser: {u.is_superuser}")

    print("\nDirect permissions:")
    for p in u.user_permissions.all():
        print(f"  - {p.content_type.app_label}.{p.codename}")

    print("\nGroup permissions:")
    for g in u.groups.all():
        print(f"  Group: {g.name}")
        for p in g.permissions.all():
            print(f"    - {p.content_type.app_label}.{p.codename}")

    # Check ProviderV2 specifically
    pv2_ct = ContentType.objects.get_for_model(ProviderV2)
    print(f"\nProviderV2 content type: {pv2_ct}")

    # Check if user can view ProviderV2 in admin
    from django.contrib import admin
    from locations.admin import ProviderV2Admin

    print(f"\nProviderV2 registered in admin: {ProviderV2 in admin.site._registry}")

except User.DoesNotExist:
    print("User 'client1' not found!")
