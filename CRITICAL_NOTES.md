# 🚨 CRITICAL NOTES - ALWAYS READ BEFORE RUNNING COMMANDS

## ⚠️ **DIRECTORY STRUCTURE - NEVER FORGET!**

### **🚫 WRONG Directory (where I keep ending up):**
```
/Users/alexbeattie/Documents/Cline/CHLAProj/
```
**This is the ROOT directory - NO `manage.py` file here!**

### **✅ CORRECT Directory (where Django server must run):**
```
/Users/alexbeattie/Documents/Cline/CHLAProj/maplocation/
```
**This is where `manage.py` is located!**

---

## 🐍 **PYTHON COMMAND - NEVER FORGET!**

### **❌ WRONG:**
```bash
python manage.py runserver 8000
```

### **✅ CORRECT:**
```bash
python3 manage.py runserver 8000
```

**On macOS with Homebrew, it's `python3`, not `python`!**

---

## 🎯 **ALWAYS DO THIS FIRST:**

### **Step 1: Navigate to correct directory**
```bash
cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation
```

### **Step 2: Verify you're in the right place**
```bash
ls
# Should show: manage.py, maplocation/, locations/, static/, templates/
```

### **Step 3: Activate virtual environment**
```bash
source .venv/bin/activate
```

### **Step 4: Start server with correct Python command - ALWAYS FOREGROUND FIRST!**
```bash
python3 manage.py runserver 8000
```

**🚨 CRITICAL: NEVER use `is_background: True` until you've confirmed the server starts successfully in foreground!**

---

## 🚨 **NEVER AGAIN - I PROMISE:**

- ❌ Don't run `python manage.py` from `CHLAProj/` (root)
- ❌ Don't use `python` instead of `python3`
- ❌ Don't get confused about which directory you're in
- ❌ Don't waste time debugging "manage.py not found" errors
- ❌ Don't create multiple shells - use existing ones
- ❌ Don't start servers in background until confirmed working
- ❌ Don't leave orphaned processes running

---

## 🧹 **PROCESS MANAGEMENT - ALWAYS DO THIS:**

### **Before starting a new server:**
```bash
# Check if server is already running
ps aux | grep "python3 manage.py runserver"

# Kill any existing processes if needed
pkill -f "python3 manage.py runserver"

# Check what's using port 8000
lsof -i :8000
```

### **Start server properly:**
```bash
# 1. Navigate to correct directory
cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation

# 2. Activate virtual environment
source .venv/bin/activate

# 3. Start in foreground FIRST to see any errors
python3 manage.py runserver 8000
```

### **Only after confirming it works:**
```bash
# Then you can background it if needed
# But I should avoid this until I'm better at process management
```

---

## 💡 **QUICK FIXES:**

### **If you're in the wrong directory:**
```bash
# From anywhere, get to the right place:
cd /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation
```

### **If you used wrong Python command:**
```bash
# Use this instead:
python3 manage.py runserver 8000
```

### **If you're confused:**
```bash
# Check where you are:
pwd
# Should show: /Users/alexbeattie/Documents/Cline/CHLAProj/maplocation

# Check if manage.py exists:
ls manage.py
# Should show: manage.py
```

---

## 🔄 **USE THE STARTUP SCRIPT (RECOMMENDED):**

Instead of remembering all this, just run:
```bash
cd /Users/alexbeattie/Documents/Cline/CHLAProj
./startup.sh
```

This script handles everything automatically!

---

**I WILL READ THIS FILE BEFORE RUNNING ANY COMMANDS FROM NOW ON! 🎯**

**Last updated: $(date)**
**By: AI Assistant (who keeps making the same mistakes)**
