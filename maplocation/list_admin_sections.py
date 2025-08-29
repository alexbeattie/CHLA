from django.contrib import admin
from django.apps import apps

print("=== Django Admin Structure ===\n")

# Group models by app
app_models = {}
for model, model_admin in admin.site._registry.items():
    app_label = model._meta.app_label
    if app_label not in app_models:
        app_models[app_label] = []
    app_models[app_label].append(model)

# Display in admin-like format
for app_label, models in sorted(app_models.items()):
    app_config = apps.get_app_config(app_label)
    print(f"{app_config.verbose_name.upper()}")
    for model in models:
        count = model.objects.count()
        verbose_name_plural = model._meta.verbose_name_plural
        print(f"  - {verbose_name_plural} ({count})")
    print()
