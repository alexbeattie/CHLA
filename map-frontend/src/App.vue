<template>
  <div id="app">
    <nav class="navbar">
      <div class="nav-container">
        <div class="nav-brand">
          <img src="./assets/chla-logo.svg" alt="CHLA" class="nav-logo" />
          <span class="nav-title">Provider Map</span>
        </div>
        <div class="nav-links">
          <router-link to="/" class="nav-link">Map</router-link>
          <router-link to="/providers" class="nav-link">Manage Providers</router-link>
        </div>
      </div>
    </nav>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<style>
/* Note: Removed duplicate pulse animation - handled in MapView.vue */

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 1rem 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 15px;
}

.nav-logo {
  height: 40px;
  width: auto;
}

.nav-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
}

.nav-links {
  display: flex;
  gap: 20px;
}

.nav-link {
  text-decoration: none;
  color: #666;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 4px;
  transition: all 0.2s;
}

.nav-link:hover {
  color: #007bff;
  background-color: #f8f9fa;
}

.nav-link.router-link-active {
  color: #007bff;
  background-color: #e7f3ff;
}

.main-content {
  flex: 1;
}

@media (max-width: 768px) {
  .nav-container {
    flex-direction: column;
    gap: 15px;
  }

  .nav-links {
    gap: 10px;
  }

  .nav-link {
    padding: 6px 12px;
    font-size: 14px;
  }
}
</style>
