<template>
  <div class="sidebar-container" :class="{ 'mobile-open': showMobileSidebar }">
    <div class="sidebar">
      <!-- CHLA Header -->
      <div class="chla-header">
        <div class="chla-logo-container">
          <img
            src="@/assets/chla-logo.svg"
            alt="Children's Hospital Los Angeles"
            class="chla-logo"
          />
        </div>
        <div class="chla-mission">
          <p class="chla-tagline">We create hope and build healthier futures</p>
        </div>
      </div>

      <!-- Display Toggle -->
      <div class="display-toggle mb-3">
        <div class="btn-group w-100 d-flex">
          <button
            class="btn flex-grow-1"
            :class="{
              'btn-chla-primary': displayType === 'regionalCenters',
              'btn-chla-outline': displayType !== 'regionalCenters',
            }"
            @click="$emit('display-type-change', 'regionalCenters')"
          >
            <i class="bi bi-building me-1"></i>
            <span>Regional Centers</span>
          </button>
          <button
            class="btn flex-grow-1"
            :class="{
              'btn-chla-primary': displayType === 'providers',
              'btn-chla-outline': displayType !== 'providers',
            }"
            @click="$emit('display-type-change', 'providers')"
          >
            <i class="bi bi-hospital me-1"></i>
            <span>Services</span>
          </button>
        </div>
      </div>

      <!-- User Profile Summary -->
      <div
        class="info-card-section mb-3"
        v-if="userProfile.age || userProfile.diagnosis || userProfile.therapies?.length"
      >
        <div class="form-control info-card border-primary bg-primary bg-opacity-10">
          <div class="info-card-header">
            <i class="bi bi-person-circle text-primary me-2"></i>
            <strong>Your Profile</strong>
          </div>
          <div class="info-card-content mt-2">
            <div class="info-card-item" v-if="userProfile.age">
              <i class="bi bi-calendar text-muted me-2"></i>
              <span>{{ userProfile.age }}</span>
            </div>
            <div class="info-card-item" v-if="userProfile.diagnosis">
              <i class="bi bi-heart-pulse text-danger me-2"></i>
              <span>{{ userProfile.diagnosis }}</span>
            </div>
            <div class="info-card-item" v-if="userProfile.therapies?.length">
              <i class="bi bi-tools text-info me-2"></i>
              <span>{{ userProfile.therapies.join(", ") }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Regional Center Info -->
      <div class="info-card-section mb-3" v-if="regionalCenter">
        <div class="form-control info-card border-info bg-info bg-opacity-10">
          <div class="info-card-header">
            <i class="bi bi-building-fill text-info me-2"></i>
            <strong>Your Regional Center</strong>
          </div>
          <div class="info-card-content mt-2">
            <div class="info-card-item">
              <strong>Your regional center is:</strong>
              <span class="ms-1">{{ regionalCenter.name || "Detecting location..." }}</span>
            </div>
            <div class="info-card-item" v-if="regionalCenter.address">
              <i class="bi bi-geo-alt-fill text-primary me-2"></i>
              <span>{{ regionalCenter.address }}</span>
            </div>
            <div class="info-card-item" v-if="regionalCenter.phone">
              <i class="bi bi-telephone-fill text-success me-2"></i>
              <a :href="'tel:' + regionalCenter.phone" class="text-decoration-none">
                {{ regionalCenter.phone }}
              </a>
            </div>
            <div class="info-card-item" v-if="regionalCenter.website">
              <i class="bi bi-globe text-warning me-2"></i>
              <a :href="regionalCenter.website" target="_blank" class="text-decoration-none">
                Visit Website
              </a>
            </div>
          </div>
        </div>
      </div>

      <!-- Search Bar Slot -->
      <div class="search-section mb-3">
        <slot name="search"></slot>
      </div>

      <!-- Filter Panel Slot -->
      <div class="filter-section">
        <slot name="filters"></slot>
      </div>

      <!-- Provider List / RC List Slot -->
      <div class="results-section">
        <slot name="results"></slot>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "SidebarPanel",

  props: {
    showMobileSidebar: {
      type: Boolean,
      default: false,
    },
    displayType: {
      type: String,
      default: "providers",
      validator: (value) => ["providers", "regionalCenters"].includes(value),
    },
    userProfile: {
      type: Object,
      default: () => ({
        age: null,
        diagnosis: null,
        therapies: [],
      }),
    },
    regionalCenter: {
      type: Object,
      default: null,
    },
  },

  emits: ["display-type-change"],
};
</script>

<style scoped>
.sidebar-container {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  width: 400px;
  z-index: 1000;
  transition: transform 0.3s ease;
  background: white;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.sidebar {
  height: 100%;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* CHLA Header */
.chla-header {
  text-align: center;
  padding: 1rem 0;
  border-bottom: 2px solid #e9ecef;
  margin-bottom: 1rem;
}

.chla-logo-container {
  margin-bottom: 0.5rem;
}

.chla-logo {
  max-width: 200px;
  height: auto;
}

.chla-mission {
  margin-top: 0.5rem;
}

.chla-tagline {
  font-size: 0.9rem;
  color: #6c757d;
  font-style: italic;
  margin: 0;
}

/* Display Toggle */
.display-toggle {
  margin-bottom: 1rem;
}

.btn-group {
  display: flex;
  gap: 0.5rem;
}

.btn {
  flex: 1;
  padding: 0.75rem;
  font-size: 0.9rem;
  border-radius: 8px;
  transition: all 0.2s;
  border: 2px solid #004877;
}

.btn-chla-primary {
  background: #004877;
  color: white;
  border-color: #004877;
}

.btn-chla-outline {
  background: transparent;
  color: #004877;
  border-color: #004877;
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 72, 119, 0.2);
}

/* Info Cards */
.info-card-section {
  margin-bottom: 1rem;
}

.info-card {
  padding: 1rem;
  border-radius: 8px;
  border: 2px solid;
}

.info-card-header {
  display: flex;
  align-items: center;
  font-size: 1rem;
  margin-bottom: 0.5rem;
}

.info-card-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-card-item {
  display: flex;
  align-items: center;
  font-size: 0.9rem;
  line-height: 1.4;
}

.info-card-item i {
  flex-shrink: 0;
}

/* Mobile responsiveness */
@media (max-width: 768px) {
  .sidebar-container {
    transform: translateX(-100%);
  }

  .sidebar-container.mobile-open {
    transform: translateX(0);
  }
}

/* Scrollbar styling */
.sidebar::-webkit-scrollbar {
  width: 6px;
}

.sidebar::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.sidebar::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

.sidebar::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
