# CHLA Provider Map - Startup Guide

## 🚀 Quick Start

### Prerequisites
- Python 3.13+ installed
- Node.js and npm installed
- Git repository cloned

### Directory Structure
```
CHLAProj/
├── maplocation/          # Django Backend
│   ├── venv/            # Python virtual environment
│   ├── manage.py        # Django management script
│   └── locations/       # Main Django app
└── map-frontend/        # Vue.js Frontend
    ├── src/             # Vue source code
    └── package.json     # Node dependencies
```

## 🔧 Starting the Application

### Step 1: Start the Backend (Django)

1. **Open Terminal 1** and navigate to the backend directory:
```bash
cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation
```

2. **Activate the virtual environment:**
```bash
source venv/bin/activate
```

3. **Start the Django server:**
```bash
python manage.py runserver 8000
```

✅ **Backend will be running at:** http://127.0.0.1:8000/

### Step 2: Start the Frontend (Vue.js)

1. **Open Terminal 2** and navigate to the frontend directory:
```bash
cd /Users/alexbeattie/Documents/Cline/CHLAProj/map-frontend
```

2. **Start the development server:**
```bash
npm run dev
```

✅ **Frontend will be running at:** http://localhost:3000/

## 🌐 Application URLs

| Service | URL | Description |
|---------|-----|-------------|
| **Main App** | http://localhost:3000/ | Vue.js frontend application |
| **API Backend** | http://127.0.0.1:8000/api/ | Django REST API |
| **Admin Panel** | http://127.0.0.1:8000/admin/ | Django admin interface |

## 🔍 Testing the Setup

### Backend Health Check
```bash
curl http://127.0.0.1:8000/api/providers/ | head -3
```
Should return JSON data with provider information.

### Frontend Health Check
```bash
curl http://localhost:3000/ | head -5
```
Should return HTML with Vue.js application.

### Integration Test
Open http://localhost:3000/ in your browser and verify:
- Map loads successfully
- Provider markers appear on the map
- Filtering works (insurance, regional center, etc.)
- Location search functions properly

## 🛠️ Features Working

### ✅ Fixed Issues
- ✅ Insurance filtering with geographic search
- ✅ Regional center filtering 
- ✅ Frontend-backend API connectivity
- ✅ Port configuration (backend: 8000, frontend: 3000)

### 🎯 Key Functionality
- **Provider Search**: Search by location, radius, and various filters
- **Insurance Filtering**: Filter providers by insurance acceptance
- **Regional Center Filtering**: Find providers accepting regional center funding  
- **Interactive Map**: Mapbox-powered map with provider markers
- **Location Services**: Automatic location detection and manual address entry

## 🚨 Troubleshooting

### Backend Issues
- **Virtual environment not activated**: Make sure to run `source venv/bin/activate`
- **Port already in use**: Kill existing processes with `pkill -f "manage.py runserver"`
- **Database issues**: Run `python manage.py migrate` if needed

### Frontend Issues  
- **Port conflicts**: Frontend will automatically find an available port (3000, 3001, etc.)
- **Dependencies missing**: Run `npm install` in the map-frontend directory
- **API connection errors**: Verify backend is running on port 8000

### Integration Issues
- **CORS errors**: Backend has CORS configured for localhost development
- **API endpoint errors**: Check that API URLs in frontend point to port 8000
- **Data not loading**: Verify both services are running and check browser console

## 📝 Development Notes

### Git Branch
Currently working on: `workingdemo` branch (stable version)

### Database
SQLite database with pre-loaded provider and regional center data.

### Environment
- Backend: Django 5.2 with Django REST Framework
- Frontend: Vue.js 3 with Vite, Mapbox GL JS
- Styling: Custom CSS with responsive design

## 🎉 Success Indicators

When everything is working correctly, you should see:

1. **Backend Terminal**: Django server logs showing successful API requests
2. **Frontend Terminal**: Vite development server running without errors  
3. **Browser**: Map loads with provider markers and functional filtering
4. **Network Tab**: Successful API calls to `/api/providers/comprehensive_search/`

---

## 🆘 Need Help?

If you encounter issues:
1. Check both terminal windows for error messages
2. Verify you're on the `workingdemo` git branch
3. Ensure virtual environment is activated for backend
4. Check browser developer console for JavaScript errors
5. Verify API endpoints are responding with test curl commands

**App is ready to use!** 🎉
