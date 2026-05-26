from django.contrib.auth.models import User
from django.contrib import admin
from django.apps import apps


def check_admin_access(username):
    try:
        user = User.objects.get(username=username)
        print(f"\n=== Admin Access for '{username}' ===")
        print(f"Is Staff: {user.is_staff}")
        print(f"Is Superuser: {user.is_superuser}")

        print("\nAccessible Models in Admin:")

        # Get all registered models
        for model, model_admin in admin.site._registry.items():
            app_label = model._meta.app_label
            model_name = model._meta.model_name

            # Check if user has any permission for this model
            perms = []
            if user.has_perm(f"{app_label}.view_{model_name}"):
                perms.append("view")
            if user.has_perm(f"{app_label}.add_{model_name}"):
                perms.append("add")
            if user.has_perm(f"{app_label}.change_{model_name}"):
                perms.append("change")
            if user.has_perm(f"{app_label}.delete_{model_name}"):
                perms.append("delete")

            if perms or user.is_superuser:
                print(
                    f"  ✓ {app_label}.{model.__name__} - Permissions: {', '.join(perms) if perms else 'ALL (superuser)'}"
                )

        # Specifically check ProviderV2
        print("\nProviderV2 Specific Check:")
        from locations.models import ProviderV2

        print(f"  - Model exists: {ProviderV2 is not None}")
        print(f"  - Registered in admin: {ProviderV2 in admin.site._registry}")
        print(f"  - Can view: {user.has_perm('locations.view_providerv2')}")
        print(f"  - Can add: {user.has_perm('locations.add_providerv2')}")
        print(f"  - Can change: {user.has_perm('locations.change_providerv2')}")
        print(f"  - Record count: {ProviderV2.objects.count()}")

    except User.DoesNotExist:
        print(f"User '{username}' not found!")


# Check both users
check_admin_access("admin")
check_admin_access("client1")
