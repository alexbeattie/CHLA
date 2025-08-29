# Provider Management Authentication Guide

## Overview

The provider management system is now protected with a secure authentication system. Users must log in to access the provider and regional center management interfaces.

## Login Credentials

### Admin User (Full Access)
- **Username:** `admin`
- **Password:** `admin123`
- **Permissions:** Full access to all features, including Django admin panel

### Client User (Limited Access)
- **Username:** `client`
- **Password:** `client123`
- **Permissions:** Can manage providers and regional centers

## Accessing the System

### 1. Provider Management Interface
- **URL:** http://localhost:3001/providers
- When you navigate to this URL, you'll be automatically redirected to the login page
- After successful login, you'll be taken to the provider management interface

### 2. Login Page
- **URL:** http://localhost:3001/login
- Beautiful, modern login interface matching your app's design
- Option to "Remember me" for persistent login

### 3. Django Admin Panel (Admin Users Only)
- **URL:** http://localhost:8000/client-portal/
- **Basic Auth:** `clientaccess` / `changeme123!`
- **Django Login:** Use admin credentials above

## Features

### Security Features
- Token-based authentication
- Automatic logout on 401 errors
- Session/Local storage based on "Remember me" option
- Protected routes with automatic redirection

### User Interface Features
- Clean, modern login page with your KINDD branding
- User info display in provider management
- Logout button in the management interface
- Loading states and error messages

## Creating Additional Users

### Via Django Shell
```bash
cd maplocation
python3 manage.py shell
```

```python
from django.contrib.auth.models import User, Group

# Create a new user
user = User.objects.create_user('newuser', 'email@example.com', 'password123')

# Add to Clients group for provider management access
clients_group, _ = Group.objects.get_or_create(name='Clients')
user.groups.add(clients_group)

# Or make them staff for full access
user.is_staff = True
user.save()
```

### Via Django Admin
1. Navigate to http://localhost:8000/client-portal/
2. Use basic auth: `clientaccess` / `changeme123!`
3. Login with admin credentials
4. Go to Users section
5. Add new user and assign to "Clients" group

## API Endpoints

The authentication system uses these endpoints:

- **Login:** `POST /api/users/auth/login/`
  - Body: `{ "username": "...", "password": "..." }`
  - Returns: `{ "token": "...", "user": {...} }`

- **Logout:** `POST /api/users/auth/logout/`
  - Requires: Authorization header with token

- **Current User:** `GET /api/users/auth/current/`
  - Requires: Authorization header with token
  - Returns: Current user information

## Troubleshooting

### "Invalid credentials" error
- Verify username and password are correct
- Check that the user exists in the database
- Ensure Django server is running

### Can't access provider management
- Make sure you're logged in
- Check browser console for errors
- Verify the Vue dev server is running on port 3001

### Token issues
- Tokens are stored in localStorage/sessionStorage
- Clear browser storage if experiencing issues
- Tokens persist based on "Remember me" selection

## Development Notes

### Adding More Protected Routes

In `router/index.js`, add `meta: { requiresAuth: true }` to any route:

```javascript
{
  path: '/regional-centers',
  name: 'regional-centers',
  component: RegionalCenterManagement,
  meta: { requiresAuth: true }
}
```

### Customizing Authentication

The authentication service is in `services/auth.js`. You can extend it with additional methods like password reset, user registration, etc.

### Permissions

The auth service includes methods to check permissions:
- `authService.isStaff()` - Check if user is staff
- `authService.isSuperuser()` - Check if user is superuser
- `authService.hasPermission('permission_name')` - Check specific permission

## Security Considerations

1. **Change default passwords** before deploying to production
2. **Use HTTPS** in production to protect tokens
3. **Set secure CORS headers** for production
4. **Rotate tokens** periodically
5. **Implement token expiration** for added security
