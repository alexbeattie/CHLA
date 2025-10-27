<template>
  <div class="rc-legend" :class="{ 'is-collapsed': isCollapsed }">
    <!-- Header -->
    <div class="legend-header" @click="toggleCollapse">
      <div class="legend-title">
        <i class="bi bi-map-fill"></i>
        <h4>LA Regional Centers</h4>
      </div>
      <button class="btn-collapse" :aria-label="isCollapsed ? 'Expand legend' : 'Collapse legend'">
        <i :class="isCollapsed ? 'bi bi-chevron-down' : 'bi bi-chevron-up'"></i>
      </button>
    </div>

    <!-- Legend Content -->
    <div v-if="!isCollapsed" class="legend-content">
      <div class="user-rc-notice" v-if="userRegionalCenter">
        <i class="bi bi-geo-alt-fill"></i>
        <span>Your Regional Center: <strong>{{ userRegionalCenter }}</strong></span>
      </div>

      <div class="legend-items">
        <div
          v-for="rc in regionalCenters"
          :key="rc.name"
          class="legend-item"
          :class="{ 'is-active': rc.name === userRegionalCenter }"
          @click="handleRCClick(rc)"
        >
          <div class="color-box" :style="{ backgroundColor: rc.color }"></div>
          <div class="rc-info">
            <div class="rc-name">{{ rc.name }}</div>
            <div class="rc-coverage" v-if="rc.name === userRegionalCenter">
              <i class="bi bi-check-circle-fill"></i>
              <span>Your location</span>
            </div>
          </div>
        </div>
      </div>

      <div class="legend-footer">
        <small>Regional Centers are assigned by ZIP code</small>
      </div>
    </div>
  </div>
</template>

<script>
/**
 * Regional Center Legend
 * Displays the 7 LA County Regional Centers with color coding
 * Highlights the user's matched Regional Center
 */
export default {
  name: 'RegionalCenterLegend',

  props: {
    // The user's matched Regional Center name
    userRegionalCenter: {
      type: String,
      default: null
    },
    // Whether to start collapsed
    startCollapsed: {
      type: Boolean,
      default: false
    }
  },

  emits: ['rc-select'],

  data() {
    return {
      isCollapsed: this.startCollapsed,

      // 7 LA County Regional Centers - colors match polygon rendering
      regionalCenters: [
        {
          name: 'North Los Angeles County Regional Center',
          color: '#f1c40f', // Yellow
          abbreviation: 'NLACRC'
        },
        {
          name: 'San Gabriel/Pomona Regional Center',
          color: '#4caf50', // Green
          abbreviation: 'SGPRC'
        },
        {
          name: 'Eastern Los Angeles Regional Center',
          color: '#ff9800', // Orange
          abbreviation: 'ELARC'
        },
        {
          name: 'Westside Regional Center',
          color: '#e91e63', // Pink
          abbreviation: 'WRC'
        },
        {
          name: 'Frank D. Lanterman Regional Center',
          color: '#9c27b0', // Purple
          abbreviation: 'FDLRC'
        },
        {
          name: 'South Central Los Angeles Regional Center',
          color: '#f44336', // Red
          abbreviation: 'SCLARC'
        },
        {
          name: 'Harbor Regional Center',
          color: '#2196f3', // Blue
          abbreviation: 'HRC'
        }
      ]
    };
  },

  methods: {
    toggleCollapse() {
      this.isCollapsed = !this.isCollapsed;
    },

    handleRCClick(rc) {
      this.$emit('rc-select', rc);
    }
  }
};
</script>

<style scoped>
.rc-legend {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  min-width: 280px;
  max-width: 320px;
}

.legend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  cursor: pointer;
  user-select: none;
}

.legend-header:hover {
  background: #e9ecef;
}

.legend-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-title i {
  color: #6c757d;
  font-size: 18px;
}

.legend-title h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #212529;
}

.btn-collapse {
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: #6c757d;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-collapse:hover {
  color: #495057;
}

.legend-content {
  padding: 12px;
}

.user-rc-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #e7f5ff;
  border: 1px solid #74c0fc;
  border-radius: 6px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #1864ab;
}

.user-rc-notice i {
  font-size: 16px;
  color: #1864ab;
}

.user-rc-notice strong {
  font-weight: 600;
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.legend-item:hover {
  background: #f8f9fa;
}

.legend-item.is-active {
  background: #e7f5ff;
  border-color: #74c0fc;
}

.color-box {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  flex-shrink: 0;
  border: 2px solid rgba(0, 0, 0, 0.1);
}

.rc-info {
  flex: 1;
  min-width: 0;
}

.rc-name {
  font-size: 13px;
  font-weight: 500;
  color: #212529;
  line-height: 1.3;
}

.legend-item.is-active .rc-name {
  font-weight: 600;
  color: #1864ab;
}

.rc-coverage {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-size: 11px;
  color: #1864ab;
  font-weight: 500;
}

.rc-coverage i {
  font-size: 12px;
}

.legend-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e9ecef;
  text-align: center;
}

.legend-footer small {
  font-size: 11px;
  color: #6c757d;
  font-style: italic;
}

/* Collapsed state */
.rc-legend.is-collapsed {
  min-width: auto;
}

.rc-legend.is-collapsed .legend-content {
  display: none;
}

/* Responsive */
@media (max-width: 768px) {
  .rc-legend {
    min-width: 100%;
    max-width: 100%;
  }

  .user-rc-notice {
    font-size: 12px;
  }

  .rc-name {
    font-size: 12px;
  }
}
</style>
