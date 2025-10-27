# Django Admin Security Options

## Current Issue
Your Django admin is publicly accessible at `/admin/`. Here are several options to secure it:

## Option 1: IP Whitelist (Most Secure for Fixed Locations)

Add this to your `settings.py`:

```python
# IP Whitelist for admin access
ADMIN_ALLOWED_IPS = [
    '127.0.0.1',  # localhost
    '192.168.1.100',  # Your office IP
    # Add client IPs here
]

# Add to MIDDLEWARE
MIDDLEWARE = [
    # ... existing middleware ...
    'maplocation.middleware.IPWhitelistMiddleware',
]
```

Create `maplocation/middleware.py`:

```python
from django.http import HttpResponseForbidden
from django.conf import settings

class IPWhitelistMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response
        
    def __call__(self, request):
        if request.path.startswith('/admin/'):
            client_ip = self.get_client_ip(request)
            allowed_ips = getattr(settings, 'ADMIN_ALLOWED_IPS', [])
            
            if client_ip not in allowed_ips:
                return HttpResponseForbidden('Access denied')
                
        return self.get_response(request)
    
    def get_client_ip(self, request):
        x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
        if x_forwarded_for:
            ip = x_forwarded_for.split(',')[0]
        else:
            ip = request.META.get('REMOTE_ADDR')
        return ip
```

## Option 2: Move Admin to Secret URL

In `urls.py`:

```python
import os

# Use environment variable for admin URL
ADMIN_URL = os.environ.get('DJANGO_ADMIN_URL', 'client-portal/')

urlpatterns = [
    path(ADMIN_URL, admin.site.urls),  # Instead of 'admin/'
    # ... other patterns ...
]
```

## Option 3: Two-Factor Authentication

Install django-otp:

```bash
pip install django-otp qrcode
```

Update `settings.py`:

```python
INSTALLED_APPS = [
    'django.contrib.admin',
    # ... other apps ...
    'django_otp',
    'django_otp.plugins.otp_totp',
    'django_otp.plugins.otp_static',
]

MIDDLEWARE = [
    # ... existing middleware ...
    'django_otp.middleware.OTPMiddleware',
]
```

And admin configuration:

```python
from django.contrib import admin
from django_otp.admin import OTPAdminSite

admin.site.__class__ = OTPAdminSite
```

## Option 4: VPN-Only Access

If using AWS/Cloud:
- Set up a VPN server
- Configure security groups to only allow admin access from VPN IP

## Option 5: Basic HTTP Authentication (Quick Fix)

Add to `.env`:
```
BASIC_AUTH_USERNAME=clientusername
BASIC_AUTH_PASSWORD=securepassword123
```

Use the BasicAuthMiddleware already created.

## Option 6: Custom Admin with Better Permissions

Create a custom admin app with granular permissions:

```python
# In a new app called 'secure_admin'
from django.contrib.auth.decorators import user_passes_test
from django.contrib.admin.views.decorators import staff_member_required

def is_client(user):
    return user.groups.filter(name='clients').exists()

@user_passes_test(is_client)
@staff_member_required
def client_admin_view(request):
    # Custom admin view with limited access
    pass
```

## Option 7: Separate Admin Domain

Use nginx to serve admin on a different subdomain:

```nginx
server {
    server_name admin.yourdomain.com;
    
    location / {
        proxy_pass http://localhost:8000/admin/;
        # Add IP restrictions here
        allow 192.168.1.0/24;
        deny all;
    }
}
```

## Fixing the Horizontal Scroll Issue

For the form that needs horizontal scroll, add this to your admin.py:

```python
@admin.register(Provider)
class ProviderAdmin(admin.ModelAdmin):
    class Media:
        css = {
            'all': ('admin/css/custom_admin.css',)
        }
```

Create `static/admin/css/custom_admin.css`:

```css
/* Enable horizontal scroll for wide forms */
.form-row {
    overflow-x: auto;
    max-width: 100%;
}

/* Make the change form wider */
#content-main {
    width: 95% !important;
    max-width: none !important;
}

/* Wide input fields */
input[type="text"], textarea {
    width: 100% !important;
    max-width: 800px;
}

/* Horizontal scroll for fieldsets */
fieldset {
    overflow-x: auto;
}

/* For specific form - add to your ProviderAdmin */
.field-insurance_accepted input,
.field-services textarea,
.field-specializations textarea {
    width: 100% !important;
    min-width: 600px;
}
```

## Recommended Approach

For immediate security with client access:

1. **Use Option 2** (Secret URL) + **Option 5** (Basic Auth) for quick protection
2. Set up proper Django user accounts for each client
3. Use Django's built-in permission system to limit what each client can edit
4. Add the CSS fixes for horizontal scrolling

Example implementation:

```python
# settings.py
ADMIN_URL = os.environ.get('DJANGO_ADMIN_URL', 'client-portal/')

# Create client group with limited permissions
from django.contrib.auth.models import Group, Permission

client_group = Group.objects.create(name='Clients')
# Only give permission to change providers, not delete
client_group.permissions.add(
    Permission.objects.get(codename='change_provider'),
    Permission.objects.get(codename='change_providerv2'),
)
```

Would you like me to implement any of these specific options for you?
