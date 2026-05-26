# 🚀 CHLA Project Startup Guide

## ⚠️ **CRITICAL: ALWAYS START FROM THE RIGHT DIRECTORY!**

### **🚫 WRONG Directory (where you keep ending up):**
```
/Users/alexbeattie/Documents/Cline/CHLAProj/
```
**This is the ROOT directory - NO `manage.py` file here!**

### **✅ CORRECT Directory (where the server must run):**
```
/Users/alexbeattie/Documents/Cline/CHLAProj/maplocation/
```
**This is where `manage.py` is located!**

---

## 🎯 **Quick Commands to Remember:**

### **To get to the right directory:**
```bash
cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation
```

### **To start the server:**
```bash
cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation
source .venv/bin/activate
python3 manage.py runserver 8000
```

---

## 🧠 **Why This Matters:**
- **Root directory** (`CHLAProj/`) = Contains project files, docs, frontend
- **Backend directory** (`CHLAProj/maplocation/`) = Contains Django server (`manage.py`)
- **Frontend directory** (`CHLAProj/map-frontend/`) = Contains Vue.js app

---

## 💡 **Pro Tips:**

### **1. Create Aliases (Recommended):**
Add these to your `~/.bashrc` or `~/.zshrc`:
```bash
alias chla-backend="cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation"
alias chla-frontend="cd /Users/alexbeattie/Documents/Cline/CHLAProj/map-frontend"
alias chla-root="cd /Users/alexbeattie/Documents/Cline/CHLAProj"
```

Then just type:
- `chla-backend` → Gets you to the right directory for Django
- `chla-frontend` → Gets you to the Vue.js app
- `chla-root` → Gets you to the project root

### **2. Use the Startup Script:**
We've created a startup script that handles this automatically!

---

## 🚀 **Automated Startup (NEW!):**

### **Option 1: Use the Startup Script**
```bash
cd /Users/alexbeattie/Documents/Cline/CHLAProj
./startup.sh
```

### **Option 2: Manual (if you prefer)**
```bash
# Terminal 1: Backend
cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation
source .venv/bin/activate
python manage.py runserver 8000

# Terminal 2: Frontend  
cd /Users/alexbeattie/Documents/Cline/CHLAProj/map-frontend
npm run dev
```

---

## 🔍 **How to Verify You're in the Right Place:**

### **✅ Correct Directory Should Show:**
```bash
$ ls
manage.py          # ← This file MUST be present!
maplocation/       # ← Django project folder
locations/         # ← Django app folder
static/            # ← Static files
templates/         # ← Templates
```

### **❌ Wrong Directory Shows:**
```bash
$ ls
map-frontend/      # ← Frontend folder
maplocation/       # ← Backend folder (but no manage.py here!)
README.md          # ← Project docs
```

---

## 🚨 **NEVER AGAIN:**
- ❌ Don't run `python manage.py` from `CHLAProj/`
- ❌ Don't run `npm run dev` from `CHLAProj/maplocation/`
- ❌ Don't get confused about which directory you're in

---

## 📝 **Quick Reference Card:**
```bash
# Backend (Django) - ALWAYS from maplocation/ directory
cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation
source .venv/bin/activate
python3 manage.py runserver 8000

# Frontend (Vue.js) - ALWAYS from map-frontend/ directory  
cd /Users/alexbeattie/Documents/Cline/CHLAProj/map-frontend
npm run dev
```

---

**Remember: The directory structure is your friend, not your enemy! 🎯**
