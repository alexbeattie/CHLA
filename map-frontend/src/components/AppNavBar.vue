<template>
  <nav class="app-navbar">
    <div class="navbar-content">
      <div class="navbar-brand">
        <router-link to="/" class="kindd-logo-link" aria-label="KiNDD home">
          <img
            src="/kindd-logo.png"
            alt="KiNDD"
            class="kindd-logo-image"
            width="78"
            height="36"
          />
        </router-link>
        <span class="brand-separator d-none d-md-inline">|</span>
        <span class="brand-subtitle d-none d-md-inline">NDD Resource Map</span>
      </div>

      <nav class="navbar-links d-none d-md-flex">
        <router-link to="/" class="nav-link">
          <i class="bi bi-map me-1"></i>
          Map
        </router-link>
        <router-link to="/regional-centers" class="nav-link">
          <i class="bi bi-building me-1"></i>
          {{ $t("nav.regionalCenters") }}
        </router-link>
        <router-link to="/faq" class="nav-link">
          <i class="bi bi-question-circle me-1"></i>
          {{ $t("nav.faq") }}
        </router-link>
        <router-link to="/about" class="nav-link">
          <i class="bi bi-info-circle me-1"></i>
          {{ $t("nav.about") }}
        </router-link>
        <router-link to="/clinicians" class="nav-link">
          <i class="bi bi-clipboard2-pulse me-1"></i>
          Clinicians
        </router-link>
      </nav>

      <div class="navbar-actions">
        <div class="mobile-nav-menu d-md-none">
          <button
            class="btn-icon"
            type="button"
            aria-label="Toggle menu"
            @click="toggleMobileNav"
            :class="{ active: showMobileNav }"
          >
            <i class="bi bi-three-dots-vertical"></i>
          </button>
          <div v-if="showMobileNav" class="mobile-nav-dropdown">
            <router-link
              to="/"
              class="mobile-nav-item"
              @click="closeMobileNav"
            >
              <i class="bi bi-map me-2"></i>
              Map
            </router-link>
            <router-link
              to="/regional-centers"
              class="mobile-nav-item"
              @click="closeMobileNav"
            >
              <i class="bi bi-building me-2"></i>
              {{ $t("nav.regionalCenters") }}
            </router-link>
            <router-link
              to="/faq"
              class="mobile-nav-item"
              @click="closeMobileNav"
            >
              <i class="bi bi-question-circle me-2"></i>
              {{ $t("nav.faq") }}
            </router-link>
            <router-link
              to="/about"
              class="mobile-nav-item"
              @click="closeMobileNav"
            >
              <i class="bi bi-info-circle me-2"></i>
              {{ $t("nav.about") }}
            </router-link>
            <router-link
              to="/clinicians"
              class="mobile-nav-item"
              @click="closeMobileNav"
            >
              <i class="bi bi-clipboard2-pulse me-2"></i>
              Clinicians
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref } from "vue";

const showMobileNav = ref(false);

const toggleMobileNav = () => {
  showMobileNav.value = !showMobileNav.value;
};

const closeMobileNav = () => {
  showMobileNav.value = false;
};
</script>

<style scoped>
.app-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  display: flex;
  align-items: center;
}

.navbar-content {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kindd-logo-link {
  display: inline-flex;
  align-items: center;
  text-decoration: none;
  cursor: pointer;
}

.kindd-logo-image {
  display: block;
  height: 36px;
  width: auto;
  object-fit: contain;
}

.brand-separator {
  color: #e0e0e0;
  font-size: 20px;
}

.brand-subtitle {
  font-size: 16px;
  color: #666;
  font-weight: 500;
}

.navbar-links {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  margin-right: auto;
}

.navbar-links .nav-link {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #004877;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.navbar-links .nav-link:hover {
  background-color: #f0f9ff;
  color: #0d9ddb;
}

.navbar-links .nav-link.router-link-active {
  background-color: #004877;
  color: white;
}

.navbar-links .nav-link.router-link-active:hover {
  background-color: #003355;
}

.navbar-links .nav-link i {
  font-size: 16px;
}

.navbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mobile-nav-menu {
  position: relative;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 22px;
  color: #333;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.btn-icon:hover,
.btn-icon.active {
  background-color: #f0f9ff;
  color: #0d9ddb;
}

.mobile-nav-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 220px;
  z-index: 1001;
  padding: 8px;
  animation: app-navbar-slide-down 0.2s ease-out;
}

@keyframes app-navbar-slide-down {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: #004877;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s ease;
}

.mobile-nav-item:hover {
  background-color: #f0f9ff;
  color: #0d9ddb;
}

.mobile-nav-item.router-link-active {
  background-color: #004877;
  color: white;
}

.mobile-nav-item i {
  font-size: 16px;
}

@media (max-width: 767.98px) {
  .navbar-brand {
    margin-left: 8px;
  }

  .kindd-logo-image {
    height: 28px;
  }
}
</style>
