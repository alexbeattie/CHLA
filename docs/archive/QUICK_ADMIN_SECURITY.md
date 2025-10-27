# Quick Admin Security Implementation

## Immediate Steps (5 minutes)

### 1. Change Admin URL (Do this first!)

Edit `/maplocation/maplocation/urls.py`:

```python
# Change line 26 from:
path('admin/', admin.site.urls),

# To something secret:
path('client-portal/', admin.site.urls),
```

### 2. Add Basic HTTP Authentication

Add to `/maplocation/maplocation/settings.py`:

```python
# At the bottom of settings.py
BASIC_AUTH_USERNAME = os.environ.get('BASIC_AUTH_USERNAME', 'clientaccess')
BASIC_AUTH_PASSWORD = os.environ.get('BASIC_AUTH_PASSWORD', 'changeme123!')

# Add to MIDDLEWARE (after SecurityMiddleware)
MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    'maplocation.middleware.BasicAuthMiddleware',  # ADD THIS LINE
    'whitenoise.middleware.WhiteNoiseMiddleware',
    # ... rest of middleware
]
```

### 3. Set Environment Variables

Create or update `.env` file:

```bash
DJANGO_SECRET_KEY=your-production-secret-key-here
BASIC_AUTH_USERNAME=yourclient
BASIC_AUTH_PASSWORD=secure-password-here
DJANGO_DEBUG=false
ALLOWED_HOSTS=yourdomain.com,www.yourdomain.com
```

### 4. Create Client User Accounts

Run Django shell:

```bash
python manage.py shell
```

Then create limited client users:

```python
from django.contrib.auth.models import User, Group, Permission
from django.contrib.contenttypes.models import ContentType

# Create a client group
client_group, created = Group.objects.get_or_create(name='Clients')

# Add permissions (change only, no delete)
from locations.models import Provider, ProviderV2
provider_ct = ContentType.objects.get_for_model(Provider)
provider2_ct = ContentType.objects.get_for_model(ProviderV2)

# Add change permissions
client_group.permissions.add(
    Permission.objects.get(content_type=provider_ct, codename='change_provider'),
    Permission.objects.get(content_type=provider2_ct, codename='change_providerv2'),
    Permission.objects.get(content_type=provider_ct, codename='add_provider'),
    Permission.objects.get(content_type=provider2_ct, codename='add_providerv2'),
)

# Create client user
client_user = User.objects.create_user(
    username='client1',
    password='client-password-here',
    email='client@example.com',
    is_staff=True  # Needed for admin access
)
client_user.groups.add(client_group)
```

## Access Instructions for Clients

After implementing the above:

1. **Admin URL**: `https://yourdomain.com/client-portal/`
2. **First Login Prompt** (Basic Auth):
   - Username: `yourclient` (from BASIC_AUTH_USERNAME)
   - Password: `secure-password-here` (from BASIC_AUTH_PASSWORD)
3. **Django Admin Login**:
   - Username: `client1`
   - Password: `client-password-here`

## Additional Security (Optional but Recommended)

### Force HTTPS

Add to `settings.py`:

```python
if not DEBUG:
    SECURE_SSL_REDIRECT = True
    SESSION_COOKIE_SECURE = True
    CSRF_COOKIE_SECURE = True
```

### Session Timeout

Add to `settings.py`:

```python
SESSION_COOKIE_AGE = 3600  # 1 hour
SESSION_SAVE_EVERY_REQUEST = True
```

### IP Whitelist (if clients have static IPs)

Add to the BasicAuthMiddleware in `middleware.py`:

```python
# After basic auth check, add:
ADMIN_ALLOWED_IPS = getattr(settings, 'ADMIN_ALLOWED_IPS', [])
if ADMIN_ALLOWED_IPS:
    client_ip = self.get_client_ip(request)
    if client_ip not in ADMIN_ALLOWED_IPS:
        return HttpResponse('Access denied', status=403)
```

## Testing

1. Test the new admin URL works
2. Test that `/admin/` returns 404
3. Test basic auth prompt appears
4. Test client login with limited permissions

## Deployment Checklist

- [ ] Changed admin URL to secret path
- [ ] Added BasicAuthMiddleware
- [ ] Set strong passwords in environment
- [ ] Created client user accounts
- [ ] Tested access with client credentials
- [ ] Disabled DEBUG in production
- [ ] Set proper ALLOWED_HOSTS
- [ ] Configured HTTPS (if applicable)

## Notes

- The horizontal scroll CSS is already added to your Provider forms
- Clients can only add/edit providers, not delete them
- Basic auth adds an extra layer - even if someone finds the URL, they need two sets of credentials
- Consider using a password manager to generate and store secure passwords
