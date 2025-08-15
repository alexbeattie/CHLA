# CHLA Provider Map

A comprehensive healthcare provider mapping application for Children's Hospital Los Angeles, helping families find autism and developmental disability services in California.

## 🌟 Features

- **Interactive Map**: Searchable map of healthcare providers and regional centers
- **Mobile-Responsive**: Optimized mobile experience with slide-out sidebar
- **Smart Filtering**: Filter by payment options, age groups, and diagnosis matching
- **Location-Based**: Automatic location detection and distance-based results
- **Service Areas**: Visual representation of county-based service coverage
- **User Profiles**: Save user information for personalized recommendations

## 🚀 Live Application

- **Frontend**: <https://kinddhelp.com>
- **API**: <https://api.kinddhelp.com>

## 🏗️ Architecture

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

## 🛠️ Quick Start

### Prerequisites

- Python 3.12+
- Node.js 18+
- PostgreSQL (for local development)
- AWS CLI configured

### Local Development Setup

1. **Clone and setup**:

   ```bash
   git clone <repository-url>
   cd CHLAProj
   ./setup-local.sh
   ```

2. **Start backend**:

   ```bash
   ./start-backend.sh
   ```

   Backend available at: <http://127.0.0.1:8000>

3. **Start frontend** (in new terminal):

   ```bash
   ./start-frontend.sh
   ```

   Frontend available at: <http://localhost:3000>

## 📚 Documentation

- **[Deployment Guide](DEPLOYMENT_GUIDE.md)**: Complete AWS setup and deployment instructions
- **[GitHub Setup](GITHUB_SETUP.md)**: CI/CD pipeline configuration
- **[Startup Guide](STARTUP_GUIDE.md)**: Original project setup notes

## 🔧 Development

### Project Structure

```
CHLAProj/
├── maplocation/           # Django backend
│   ├── manage.py
│   ├── requirements.txt
│   ├── Procfile
│   └── locations/         # Main Django app
├── map-frontend/          # Vue.js frontend
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── .github/workflows/     # GitHub Actions CI/CD
└── scripts/              # Deployment scripts
```

### Key Technologies

- **Backend**: Django, Django REST Framework, PostGIS, Gunicorn
- **Frontend**: Vue.js 3, Vite, Bootstrap 5, Mapbox GL
- **Database**: PostgreSQL with PostGIS extension
- **Deployment**: AWS (EB, RDS, S3, CloudFront, Route 53)

## 🚀 Deployment

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

Push to main branch triggers automatic deployment:

```bash
git add .
git commit -m "Your changes"
git push origin main
```

## 📱 Mobile Features

The application includes a responsive mobile design with:

- **Hidden sidebar by default** on mobile devices
- **Hamburger menu toggle** in top-left corner
- **Slide-out sidebar** with smooth animations
- **Full-width map** when sidebar is hidden
- **Touch-friendly interface** with proper tap targets

## 🗺️ API Endpoints

### Providers

- `GET /api/providers/` - List all healthcare providers
- `GET /api/providers/{id}/` - Get specific provider details

### Regional Centers

- `GET /api/regional-centers/` - List all regional centers
- `GET /api/regional-centers/{id}/` - Get specific center details

### Geographic Data

- `GET /api/california-counties/` - Get California county boundaries
- `GET /api/service-areas/` - Get service area coverage

## 🔐 Environment Variables

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

## 🧪 Testing

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

## 🐛 Troubleshooting

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

See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for detailed troubleshooting.

## 📊 Performance

- **Frontend**: Cached via CloudFront globally
- **Backend**: Auto-scaling via Elastic Beanstalk
- **Database**: PostGIS optimized queries for geographic data
- **Mobile**: Optimized for mobile devices with efficient rendering

## 🔒 Security

- **HTTPS**: SSL certificates via AWS Certificate Manager
- **CORS**: Properly configured for cross-origin requests
- **Environment Variables**: Sensitive data stored securely
- **Database**: Private subnets with security groups

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and test locally
4. Submit a pull request
5. CI/CD will test your changes automatically

## 📞 Support

For questions or issues:

1. Check the documentation files
2. Review troubleshooting guides
3. Check GitHub Issues
4. Contact the development team

## 📄 License

[Add your license information here]

---

**Built with ❤️ for Children's Hospital Los Angeles**
