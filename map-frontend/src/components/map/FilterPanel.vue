<template>
  <div class="filter-panel" :class="{ 'is-collapsed': isCollapsed }">
    <!-- Header -->
    <div class="panel-header">
      <div class="header-title">
        <i class="bi bi-funnel"></i>
        <h3>Filters</h3>
        <span v-if="activeFilterCount > 0" class="filter-count-badge">
          {{ activeFilterCount }}
        </span>
      </div>
      <div class="header-actions">
        <button
          v-if="hasActiveFilters"
          class="btn-reset"
          @click="handleReset"
          aria-label="Reset all filters"
        >
          <i class="bi bi-arrow-counterclockwise"></i>
          <span>Reset</span>
        </button>
        <button
          v-if="showCollapseToggle"
          class="btn-collapse"
          @click="handleToggleCollapse"
          :aria-label="isCollapsed ? 'Expand filters' : 'Collapse filters'"
        >
          <i :class="isCollapsed ? 'bi bi-chevron-down' : 'bi bi-chevron-up'"></i>
        </button>
      </div>
    </div>

    <!-- Filter Content -->
    <div v-if="!isCollapsed" class="panel-content">
      <!-- Insurance Filters -->
      <div class="filter-section">
        <h4 class="section-title">
          <i class="bi bi-credit-card"></i>
          <span>Insurance</span>
        </h4>
        <div class="filter-group">
          <label class="filter-checkbox">
            <input
              type="checkbox"
              v-model="localFilters.acceptsInsurance"
              @change="handleFilterChange"
            />
            <span class="checkbox-label">Accepts Insurance</span>
          </label>
          <label class="filter-checkbox">
            <input
              type="checkbox"
              v-model="localFilters.acceptsRegionalCenter"
              @change="handleFilterChange"
            />
            <span class="checkbox-label">Accepts Regional Center</span>
          </label>
          <label class="filter-checkbox">
            <input
              type="checkbox"
              v-model="localFilters.acceptsPrivatePay"
              @change="handleFilterChange"
            />
            <span class="checkbox-label">Accepts Private Pay</span>
          </label>
        </div>
      </div>

      <!-- Profile Matching Filters -->
      <div class="filter-section">
        <h4 class="section-title">
          <i class="bi bi-person-check"></i>
          <span>Match My Profile</span>
        </h4>
        <div class="filter-group">
          <label class="filter-checkbox" :class="{ 'disabled': !hasUserAge }">
            <input
              type="checkbox"
              v-model="localFilters.matchesAge"
              @change="handleFilterChange"
              :disabled="!hasUserAge"
            />
            <span class="checkbox-label">
              Match Age
              <span v-if="userData.age" class="profile-value">({{ userData.age }})</span>
              <span v-else class="no-data">(Not set)</span>
            </span>
          </label>
          <label class="filter-checkbox" :class="{ 'disabled': !hasUserDiagnosis }">
            <input
              type="checkbox"
              v-model="localFilters.matchesDiagnosis"
              @change="handleFilterChange"
              :disabled="!hasUserDiagnosis"
            />
            <span class="checkbox-label">
              Match Diagnosis
              <span v-if="userData.diagnosis" class="profile-value">({{ userData.diagnosis }})</span>
              <span v-else class="no-data">(Not set)</span>
            </span>
          </label>
          <label class="filter-checkbox" :class="{ 'disabled': !hasUserTherapy }">
            <input
              type="checkbox"
              v-model="localFilters.matchesTherapy"
              @change="handleFilterChange"
              :disabled="!hasUserTherapy"
            />
            <span class="checkbox-label">
              Match Therapy
              <span v-if="userData.therapy" class="profile-value">({{ userData.therapy }})</span>
              <span v-else class="no-data">(Not set)</span>
            </span>
          </label>
        </div>
      </div>

      <!-- Other Filters -->
      <div v-if="showFavorites" class="filter-section">
        <h4 class="section-title">
          <i class="bi bi-star"></i>
          <span>Favorites</span>
        </h4>
        <div class="filter-group">
          <label class="filter-checkbox">
            <input
              type="checkbox"
              v-model="localFilters.showOnlyFavorites"
              @change="handleFilterChange"
            />
            <span class="checkbox-label">Show Only Favorites</span>
          </label>
        </div>
      </div>

      <!-- Apply Button (if manual application is enabled) -->
      <div v-if="manualApply && hasChanges" class="apply-section">
        <button class="btn-apply" @click="handleApply">
          <i class="bi bi-check-circle"></i>
          <span>Apply Filters</span>
        </button>
      </div>

      <!-- Active Filters Summary -->
      <div v-if="showSummary && hasActiveFilters" class="filters-summary">
        <div class="summary-header">
          <i class="bi bi-info-circle"></i>
          <span>Active Filters:</span>
        </div>
        <div class="summary-chips">
          <span
            v-if="localFilters.acceptsInsurance"
            class="filter-chip"
            @click="toggleFilter('acceptsInsurance')"
          >
            Insurance
            <i class="bi bi-x"></i>
          </span>
          <span
            v-if="localFilters.acceptsRegionalCenter"
            class="filter-chip"
            @click="toggleFilter('acceptsRegionalCenter')"
          >
            Regional Center
            <i class="bi bi-x"></i>
          </span>
          <span
            v-if="localFilters.acceptsPrivatePay"
            class="filter-chip"
            @click="toggleFilter('acceptsPrivatePay')"
          >
            Private Pay
            <i class="bi bi-x"></i>
          </span>
          <span
            v-if="localFilters.matchesAge"
            class="filter-chip"
            @click="toggleFilter('matchesAge')"
          >
            Age Match
            <i class="bi bi-x"></i>
          </span>
          <span
            v-if="localFilters.matchesDiagnosis"
            class="filter-chip"
            @click="toggleFilter('matchesDiagnosis')"
          >
            Diagnosis Match
            <i class="bi bi-x"></i>
          </span>
          <span
            v-if="localFilters.matchesTherapy"
            class="filter-chip"
            @click="toggleFilter('matchesTherapy')"
          >
            Therapy Match
            <i class="bi bi-x"></i>
          </span>
          <span
            v-if="localFilters.showOnlyFavorites"
            class="filter-chip"
            @click="toggleFilter('showOnlyFavorites')"
          >
            Favorites Only
            <i class="bi bi-x"></i>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue';
import { useFilterStore } from '@/stores/filterStore';

/**
 * FilterPanel Component
 * Displays and manages filter controls
 * Week 4: Component Extraction
 */
export default {
  name: 'FilterPanel',

  props: {
    // Whether panel starts collapsed
    startCollapsed: {
      type: Boolean,
      default: false
    },
    // Show collapse/expand toggle
    showCollapseToggle: {
      type: Boolean,
      default: true
    },
    // Show favorites filter
    showFavorites: {
      type: Boolean,
      default: false
    },
    // Show active filters summary
    showSummary: {
      type: Boolean,
      default: true
    },
    // Require manual "Apply" button click
    manualApply: {
      type: Boolean,
      default: false
    }
  },

  emits: [
    'filter-change',  // When filters change
    'apply',          // When apply button is clicked
    'reset'           // When reset is clicked
  ],

  setup(props, { emit }) {
    const filterStore = useFilterStore();

    const isCollapsed = ref(props.startCollapsed);
    const hasChanges = ref(false);

    // Local copy of filters for manual apply mode
    const localFilters = ref({
      acceptsInsurance: filterStore.filterOptions.acceptsInsurance,
      acceptsRegionalCenter: filterStore.filterOptions.acceptsRegionalCenter,
      acceptsPrivatePay: filterStore.filterOptions.acceptsPrivatePay,
      matchesAge: filterStore.filterOptions.matchesAge,
      matchesDiagnosis: filterStore.filterOptions.matchesDiagnosis,
      matchesTherapy: filterStore.filterOptions.matchesTherapy,
      showOnlyFavorites: filterStore.filterOptions.showOnlyFavorites
    });

    // User data from store
    const userData = computed(() => filterStore.userData);

    // Check if user has set profile data
    const hasUserAge = computed(() => !!userData.value.age);
    const hasUserDiagnosis = computed(() => !!userData.value.diagnosis);
    const hasUserTherapy = computed(() => !!userData.value.therapy);

    // Active filter count
    const activeFilterCount = computed(() => {
      let count = 0;
      if (localFilters.value.acceptsInsurance) count++;
      if (localFilters.value.acceptsRegionalCenter) count++;
      if (localFilters.value.acceptsPrivatePay) count++;
      if (localFilters.value.matchesAge) count++;
      if (localFilters.value.matchesDiagnosis) count++;
      if (localFilters.value.matchesTherapy) count++;
      if (localFilters.value.showOnlyFavorites) count++;
      return count;
    });

    // Has any active filters
    const hasActiveFilters = computed(() => activeFilterCount.value > 0);

    /**
     * Handle filter change
     */
    const handleFilterChange = () => {
      console.log('🔍 FilterPanel: Filter changed');

      if (props.manualApply) {
        // Mark as changed but don't apply yet
        hasChanges.value = true;
      } else {
        // Apply immediately
        applyFiltersToStore();
        emit('filter-change', localFilters.value);
      }
    };

    /**
     * Apply filters to store
     */
    const applyFiltersToStore = () => {
      console.log('🔍 FilterPanel: Applying filters to store');

      Object.keys(localFilters.value).forEach(key => {
        filterStore.toggleFilter(key, localFilters.value[key]);
      });
    };

    /**
     * Handle manual apply
     */
    const handleApply = () => {
      console.log('🔍 FilterPanel: Apply button clicked');

      applyFiltersToStore();
      hasChanges.value = false;
      emit('apply', localFilters.value);
    };

    /**
     * Handle reset
     */
    const handleReset = () => {
      console.log('🔍 FilterPanel: Reset filters');

      // Reset all filters
      localFilters.value = {
        acceptsInsurance: false,
        acceptsRegionalCenter: false,
        acceptsPrivatePay: false,
        matchesAge: false,
        matchesDiagnosis: false,
        matchesTherapy: false,
        showOnlyFavorites: false
      };

      if (!props.manualApply) {
        applyFiltersToStore();
      }

      hasChanges.value = false;
      emit('reset');
    };

    /**
     * Toggle specific filter
     */
    const toggleFilter = (filterKey) => {
      console.log(`🔍 FilterPanel: Toggling filter ${filterKey}`);

      localFilters.value[filterKey] = !localFilters.value[filterKey];
      handleFilterChange();
    };

    /**
     * Handle collapse toggle
     */
    const handleToggleCollapse = () => {
      isCollapsed.value = !isCollapsed.value;
      console.log(`🔍 FilterPanel: ${isCollapsed.value ? 'Collapsed' : 'Expanded'}`);
    };

    // Sync with store when store changes (from other sources)
    watch(
      () => filterStore.filterOptions,
      (newOptions) => {
        if (!hasChanges.value) {
          localFilters.value = { ...newOptions };
        }
      },
      { deep: true }
    );

    return {
      isCollapsed,
      hasChanges,
      localFilters,
      userData,
      hasUserAge,
      hasUserDiagnosis,
      hasUserTherapy,
      activeFilterCount,
      hasActiveFilters,
      handleFilterChange,
      handleApply,
      handleReset,
      toggleFilter,
      handleToggleCollapse
    };
  }
};
</script>

<style scoped>
.filter-panel {
  background-color: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.filter-panel.is-collapsed .panel-content {
  display: none;
}

/* Header */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background-color: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title i {
  font-size: 20px;
  color: #2563eb;
}

.header-title h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.filter-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  background-color: #2563eb;
  color: white;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-reset,
.btn-collapse {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background-color: transparent;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-reset:hover,
.btn-collapse:hover {
  background-color: #f3f4f6;
  color: #1f2937;
  border-color: #9ca3af;
}

.btn-collapse {
  padding: 6px;
  min-width: 32px;
}

/* Content */
.panel-content {
  padding: 16px;
}

/* Filter Section */
.filter-section {
  margin-bottom: 20px;
}

.filter-section:last-child {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 12px 0;
}

.section-title i {
  font-size: 16px;
  color: #6b7280;
}

/* Filter Group */
.filter-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Checkbox */
.filter-checkbox {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  user-select: none;
}

.filter-checkbox:hover {
  background-color: #f9fafb;
}

.filter-checkbox.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.filter-checkbox.disabled:hover {
  background-color: transparent;
}

.filter-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #2563eb;
}

.filter-checkbox.disabled input[type="checkbox"] {
  cursor: not-allowed;
}

.checkbox-label {
  font-size: 14px;
  color: #4b5563;
  cursor: pointer;
}

.filter-checkbox.disabled .checkbox-label {
  cursor: not-allowed;
}

.profile-value {
  font-weight: 500;
  color: #2563eb;
}

.no-data {
  font-style: italic;
  color: #9ca3af;
  font-size: 13px;
}

/* Apply Section */
.apply-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.btn-apply {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background-color: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-apply:hover {
  background-color: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

/* Filters Summary */
.filters-summary {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 10px;
}

.summary-header i {
  font-size: 14px;
}

.summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background-color: #eff6ff;
  color: #1e40af;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-chip:hover {
  background-color: #dbeafe;
}

.filter-chip i {
  font-size: 12px;
}

/* Responsive */
@media (max-width: 768px) {
  .panel-header {
    padding: 14px;
  }

  .panel-content {
    padding: 14px;
  }

  .filter-section {
    margin-bottom: 16px;
  }
}
</style>
