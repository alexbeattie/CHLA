<template>
  <router-view />
</template>

<style>
html, body {
  margin: 0;
  padding: 0;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  /* overflow: hidden; */
  height: 100%;
  width: 100%;
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  box-sizing: border-box;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  height: 100vh;
  width: 100vw;
  /* overflow: hidden; */
  position: relative;
}

/* Global styles for consistent UI */
.btn-close:focus {
  box-shadow: none;
  outline: none;
}

.form-control, .form-select, .btn {
  font-size: 0.95rem;
}

.form-control:focus, 
.form-select:focus,
.btn:focus {
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.2);
  border-color: #86b7fe;
  outline: none;
}

/* Fix button appearing disabled on mobile */
.btn-primary:active {
  background-color: #0d6efd !important;
  border-color: #0a58ca !important;
}

/* Improve button touch feedback */
.btn {
  position: relative;
  overflow: hidden;
  transition: all 0.2s ease;
}

.btn:active::after {
  content: '';
  display: block;
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
  background-image: radial-gradient(circle, #fff 10%, transparent 10%);
  background-repeat: no-repeat;
  background-position: 50%;
  transform: scale(10, 10);
  opacity: 0;
  transition: transform 0.5s, opacity 0.5s;
}

/* Map control styling improvements */
.mapboxgl-ctrl-group {
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  border-radius: 6px;
  overflow: hidden;
  z-index: 20 !important; /* Ensure controls are above overlay */
}

.mapboxgl-ctrl-group button {
  border: none !important;
  width: 32px;
  height: 32px;
  background-color: white;
}

.mapboxgl-ctrl-top-right {
  z-index: 20 !important; /* Ensure control container is above all overlays */
}

/* CRITICAL MAP FIXES */
.mapboxgl-map {
  position: absolute !important;
  top: 0 !important;
  bottom: 0 !important;
  width: 100% !important;
  height: 100% !important;
}

/* Make popups visible */
.mapboxgl-popup {
  z-index: 10000 !important; /* Extremely high to ensure visibility */
  pointer-events: auto !important;
}

.mapboxgl-popup-content {
  padding: 12px;
  border-radius: 6px;
  box-shadow: 0 3px 15px rgba(0,0,0,0.2);
  max-width: 300px; /* Prevent excessively wide popups */
  overflow-wrap: break-word; /* Handle long text */
}

/* Make sure all map controls are visible */
.mapboxgl-ctrl-top-right,
.mapboxgl-ctrl-top-left,
.mapboxgl-ctrl-bottom-right,
.mapboxgl-ctrl-bottom-left {
  z-index: 10001 !important; /* Above everything */
}

/* Mobile adjustments */
@media (max-width: 768px) {
  /* Fix for mobile scroll issues */
  .mapboxgl-ctrl-group {
    touch-action: none;
  }
  
  /* Adjust control size for touch */
  .mapboxgl-ctrl-group button {
    width: 36px;
    height: 36px;
  }
  
  /* Larger touch targets on mobile */
  .form-control, .form-select, .btn {
    min-height: 40px;
    padding-top: 8px;
    padding-bottom: 8px;
  }
  
  .form-control::placeholder {
    font-size: 0.9rem;
  }
}

/* Extra fix for iOS Safari */
@supports (-webkit-touch-callout: none) {
  html, body {
    position: fixed;
    width: 100%;
    height: -webkit-fill-available;
    overscroll-behavior: none;
  }
  
  #app {
    position: fixed;
    height: -webkit-fill-available;
    width: 100%;
  }
  
  /* Prevent pull-to-refresh Safari behavior */
  body {
    overscroll-behavior-y: none;
    -webkit-overflow-scrolling: auto;
  }
  
  /* Remove tap highlight on interactive elements */
  * {
    -webkit-tap-highlight-color: transparent;
  }
}
</style>