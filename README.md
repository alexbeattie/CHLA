# KiNDD - NDD Resource Navigator

A comprehensive healthcare provider mapping application helping families navigate neurodevelopmental disorder (NDD) services in California.

## Features

- **Interactive Map**: Searchable map of healthcare providers and regional centers
- **Mobile-Responsive**: Optimized mobile experience with slide-out sidebar
- **Smart Filtering**: Filter by payment options, age groups, and diagnosis matching
- **Location-Based**: Automatic location detection and distance-based results
- **Service Areas**: Visual representation of county-based service coverage
- **User Profiles**: Save user information for personalized recommendations

## Live Application

- **Frontend**: <https://kinddhelp.com>
- **API**: <https://api.kinddhelp.com>

## Architecture

### Frontend (Vue.js)

- **Framework**: Vue.js 3 with Vite
- **Deployment**: S3 Static Website + CloudFront CDN
- **Features**: Mobile-responsive design, interactive maps, real-time filtering

### Backend (Django)

- **Framework**: Django 5.2 with Django REST Framework
- **Deployment**: AWS Elastic Beanstalk (Python 3.12)
- **Database**: PostgreSQL on AWS RDS with PostGIS extension
- **Features**: RESTful API, geographic queries, user management

### Infrastructure

- **DNS**: Route 53 with custom domain
- **SSL**: AWS Certificate Manager for HTTPS
- **CDN**: CloudFront for global content delivery
- **Monitoring**: Elastic Beanstalk health monitoring

## Quick Start

### Prerequisites

- Python 3.12+
- Node.js 18+
- PostgreSQL (for local development)
- AWS CLI configured

### Local Development Setup

1. **Backend Setup**:

   ```bash
   # Navigate to backend directory
   cd maplocation

   # Create virtual environment
   python3.12 -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate

   # Install dependencies
   pip install -r requirements.txt

   # Set up environment variables (see maplocation/README.md)
   export DB_NAME="your_db"
   export DB_USER="your_user"
   export DB_PASSWORD="your_password"
   # ... other variables

   # Run migrations
   python manage.py migrate

   # Create superuser
   python manage.py createsuperuser

   # Start backend
   python manage.py runserver
   ```

   Backend available at: <http://127.0.0.1:8000>

2. **Frontend Setup** (in new terminal):

   ```bash
   # Navigate to frontend directory
   cd map-frontend

   # Install dependencies
   npm install

   # Start development server
   npm run dev
   ```

   Frontend available at: <http://localhost:3000>

**For detailed setup instructions, see [Backend Getting Started Guide](maplocation/README.md)**

## Documentation

### Quick Links

- **[CI/CD Pipeline Guide](.github/CICD_GUIDE.md)** - **NEW!** Comprehensive CI/CD documentation
- **[GitHub Secrets Setup](.github/SECRETS.md)** - **NEW!** Configure deployment secrets
- **[Documentation Index](docs/README.md)** - Complete documentation directory
- **[Getting Started - Backend](maplocation/README.md)** - Set up Django backend
- **[Deployment Guide](docs/DEPLOYMENT.md)** - Deploy to production
- **[GitHub Actions/CI/CD](docs/GITHUB_ACTIONS.md)** - Automatic deployment setup
- **[Database Sync](docs/DATABASE_SYNC.md)** - Manage migrations and data
- **[Stack Documentation](docs/STACK.md)** - Complete architecture reference

### By Role

- **New Developers**: Start with [Getting Started](maplocation/README.md) and [Stack Docs](docs/STACK.md)
- **Developers**: Use [Deployment](docs/DEPLOYMENT.md) and [Database Sync](docs/DATABASE_SYNC.md)
- **DevOps**: See [GitHub Actions](docs/GITHUB_ACTIONS.md) and [Deployment](docs/DEPLOYMENT.md)

**All documentation organized in [/docs](docs/)** - Old/outdated docs moved to [/docs/archive](docs/archive/)

## Development

### Project Structure

```
repo-root/
├── maplocation/           # Django backend
│   ├── manage.py
│   ├── requirements.txt
│   ├── Procfile
│   ├── locations/         # Main Django app (providers, regional centers)
│   ├── users/            # User authentication app
│   └── README.md         # Backend getting started guide
├── map-frontend/          # Vue.js frontend
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── docs/                  # Organized documentation
│   ├── README.md         # Documentation index
│   ├── DEPLOYMENT.md
│   ├── GITHUB_ACTIONS.md
│   └── DATABASE_SYNC.md
├── .github/workflows/     # GitHub Actions CI/CD
└── docs/STACK.md          # Architecture reference
```

### Key Technologies

- **Backend**: Django, Django REST Framework, PostGIS, Gunicorn
- **Frontend**: Vue.js 3, Vite, Bootstrap 5, Mapbox GL
- **Database**: PostgreSQL with PostGIS extension
- **Deployment**: AWS (EB, RDS, S3, CloudFront, Route 53)

## Deployment

### Manual Deployment

```bash
# Backend
cd maplocation
eb deploy

# Frontend
cd map-frontend
npm run build
aws s3 sync dist s3://kinddhelp-frontend-1755148345 --delete
aws cloudfront create-invalidation --distribution-id E2W6EECHUV4LMM --paths "/*"
```

### GitHub Actions (Recommended)

Push to main branch triggers automatic deployment with comprehensive CI/CD:

```bash
git add .
git commit -m "Your changes"
git push origin main

# Monitor deployment
gh run watch
```

**New Features:**
- Automated testing (backend + frontend)
- Linting and code quality checks
- Health checks with automatic rollback
- Database migration automation
- Deployment verification

**See:** [CI/CD Guide](.github/CICD_GUIDE.md) | [Secrets Setup](.github/SECRETS.md)

**Quick Setup:**
```bash
# Configure GitHub secrets
./scripts/setup-ci.sh

# Test locally before deploying
./scripts/deploy-test.sh
```

## Mobile Features

The application includes a responsive mobile design with:

- **Hidden sidebar by default** on mobile devices
- **Hamburger menu toggle** in top-left corner
- **Slide-out sidebar** with smooth animations
- **Full-width map** when sidebar is hidden
- **Touch-friendly interface** with proper tap targets

## API Endpoints

### Providers

- `GET /api/providers/` - List all healthcare providers
- `GET /api/providers/{id}/` - Get specific provider details

### Regional Centers

- `GET /api/regional-centers/` - List all regional centers
- `GET /api/regional-centers/{id}/` - Get specific center details

### Geographic Data

- `GET /api/california-counties/` - Get California county boundaries
- `GET /api/service-areas/` - Get service area coverage

## Environment Variables

### Backend (Django)

```bash
DJANGO_SECRET_KEY=your-secret-key
DJANGO_DEBUG=false
ALLOWED_HOSTS=api.kinddhelp.com,localhost
CORS_ALLOWED_ORIGINS=https://kinddhelp.com,https://www.kinddhelp.com
CSRF_TRUSTED_ORIGINS=https://kinddhelp.com,https://www.kinddhelp.com
DB_NAME=postgres
DB_USER=chla_admin
DB_PASSWORD=your-db-password
DB_HOST=your-rds-endpoint
DB_PORT=5432
```

### Frontend (Vue.js)

```bash
VITE_API_BASE_URL=https://api.kinddhelp.com/api
```

## Testing

### Backend Tests

```bash
cd maplocation
python manage.py test
```

### Frontend Tests

```bash
cd map-frontend
npm run test
```

## Troubleshooting

### Common Issues

1. **Mobile sidebar not appearing**
   - Clear browser cache or use incognito mode
   - Check CloudFront cache invalidation

2. **API calls failing**
   - Verify CORS settings in Django
   - Check API URL in frontend environment

3. **Database connection issues**
   - Verify RDS security group settings
   - Check environment variables

4. **Deployment failures**
   - Check AWS credentials and permissions
   - Review GitHub Actions logs

See [Deployment Guide](docs/DEPLOYMENT.md) and [GitHub Actions Guide](docs/GITHUB_ACTIONS.md) for detailed troubleshooting.

## Performance

- **Frontend**: Cached via CloudFront globally
- **Backend**: Auto-scaling via Elastic Beanstalk
- **Database**: PostGIS optimized queries for geographic data
- **Mobile**: Optimized for mobile devices with efficient rendering

## Security

- **HTTPS**: SSL certificates via AWS Certificate Manager
- **CORS**: Properly configured for cross-origin requests
- **Environment Variables**: Sensitive data stored securely
- **Database**: Private subnets with security groups

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and test locally
4. Submit a pull request
5. CI/CD will test your changes automatically

## Support

For questions or issues:

1. Check the documentation files
2. Review troubleshooting guides
3. Check GitHub Issues
4. Contact the development team

## License

[Add your license information here]

---

**KiNDD - NDD Resource Navigator**
