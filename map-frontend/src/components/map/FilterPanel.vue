<template>
  <div class="filter-panel">
    <!-- Insurance Types (Multi-Select) -->
    <div class="filter-section" v-if="availableInsuranceTypes.length > 0">
      <div class="section-header">
        <i class="bi bi-credit-card"></i>
        <h4>Insurance Accepted</h4>
      </div>
      <div class="filter-options">
        <label 
          class="filter-option" 
          v-for="insurance in availableInsuranceTypes" 
          :key="insurance"
        >
          <input
            type="checkbox"
            :checked="isInsuranceSelected(insurance)"
            @change="handleInsuranceToggle(insurance)"
          />
          <span class="filter-label">{{ insurance }}</span>
        </label>
      </div>
    </div>

    <!-- Therapy Types (Multi-Select) -->
    <div class="filter-section" v-if="availableTherapyTypes.length > 0">
      <div class="section-header">
        <i class="bi bi-clipboard2-pulse"></i>
        <h4>Therapy Types</h4>
      </div>
      <div class="filter-options">
        <label 
          class="filter-option" 
          v-for="therapy in availableTherapyTypes" 
          :key="therapy"
        >
          <input
            type="checkbox"
            :checked="isTherapySelected(therapy)"
            @change="handleTherapyToggle(therapy)"
          />
          <span class="filter-label">{{ therapy }}</span>
        </label>
      </div>
    </div>

    <!-- My Needs (Based on Profile) -->
    <div class="filter-section" v-if="hasUserProfile">
      <div class="section-header">
        <i class="bi bi-sliders"></i>
        <h4>Filter by My Needs</h4>
        <button class="btn-info" @click="showInfoModal = true" title="What's this?">
          <i class="bi bi-question-circle"></i>
        </button>
      </div>
      <div class="filter-options">
        <label 
          class="filter-option" 
          v-if="userData.age"
        >
          <input
            type="checkbox"
            v-model="localFilters.matchesAge"
            @change="handleFilterChange"
          />
          <span class="filter-label">
            Serves age {{ formatAge(userData.age) }}
          </span>
        </label>
        
        <label 
          class="filter-option" 
          v-if="userData.diagnosis"
        >
          <input
            type="checkbox"
            v-model="localFilters.matchesDiagnosis"
            @change="handleFilterChange"
          />
          <span class="filter-label">
            Treats {{ formatDiagnosis(userData.diagnosis) }}
          </span>
        </label>
        
        <label 
          class="filter-option" 
          v-if="userData.therapy"
        >
          <input
            type="checkbox"
            v-model="localFilters.matchesTherapy"
            @change="handleFilterChange"
          />
          <span class="filter-label">
            Offers {{ userData.therapy }}
          </span>
        </label>

        <div v-if="!userData.age && !userData.diagnosis && !userData.therapy" class="no-profile-hint">
          <i class="bi bi-info-circle"></i>
          <span>Set up your profile to see personalized filters</span>
        </div>
      </div>
    </div>

    <!-- Active Filters Summary -->
    <div v-if="hasActiveFilters" class="active-filters">
      <div class="active-filters-header">
        <span class="active-count">{{ activeFilterCount }} active</span>
        <button class="btn-clear-all" @click="handleReset">
          Clear all
        </button>
      </div>
      <div class="filter-chips">
        <button
          v-if="localFilters.acceptsInsurance"
          class="filter-chip"
          @click="toggleFilter('acceptsInsurance')"
        >
          Insurance
          <i class="bi bi-x"></i>
        </button>
        <button
          v-if="localFilters.acceptsPrivatePay"
          class="filter-chip"
          @click="toggleFilter('acceptsPrivatePay')"
        >
          Private Pay
          <i class="bi bi-x"></i>
        </button>
        <button
          v-if="localFilters.matchesAge"
          class="filter-chip"
          @click="toggleFilter('matchesAge')"
        >
          Age {{ formatAge(userData.age) }}
          <i class="bi bi-x"></i>
        </button>
        <button
          v-if="localFilters.matchesDiagnosis"
          class="filter-chip"
          @click="toggleFilter('matchesDiagnosis')"
        >
          {{ formatDiagnosis(userData.diagnosis) }}
          <i class="bi bi-x"></i>
        </button>
        <button
          v-if="localFilters.matchesTherapy"
          class="filter-chip"
          @click="toggleFilter('matchesTherapy')"
        >
          {{ userData.therapy }}
          <i class="bi bi-x"></i>
        </button>
        <!-- Therapy Type Chips -->
        <button
          v-for="therapy in filterStore.filterOptions.therapies"
          :key="therapy"
          class="filter-chip therapy-chip"
          @click="handleTherapyToggle(therapy)"
        >
          {{ therapy }}
          <i class="bi bi-x"></i>
        </button>
        <!-- Insurance Type Chips -->
        <button
          v-for="insurance in filterStore.filterOptions.insuranceTypes"
          :key="insurance"
          class="filter-chip insurance-chip"
          @click="handleInsuranceToggle(insurance)"
        >
          {{ insurance }}
          <i class="bi bi-x"></i>
        </button>
      </div>
    </div>

    <!-- Info Modal -->
    <div v-if="showInfoModal" class="info-modal-overlay" @click="showInfoModal = false">
      <div class="info-modal" @click.stop>
        <div class="modal-header">
          <h3>Filter by My Needs</h3>
          <button class="btn-close" @click="showInfoModal = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <p>
            These filters help you find providers that match your specific requirements:
          </p>
          <ul>
            <li><strong>Age:</strong> Shows only providers who serve your age group</li>
            <li><strong>Diagnosis:</strong> Shows only providers who treat your condition</li>
            <li><strong>Therapy:</strong> Shows only providers who offer the services you need</li>
          </ul>
          <p class="modal-note">
            These options are based on the profile you set up. You can update your profile anytime by clicking the edit button at the top.
          </p>
        </div>
        <div class="modal-footer">
          <button class="btn-primary" @click="showInfoModal = false">
            Got it
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue';
import { useFilterStore } from '@/stores/filterStore';

export default {
  name: 'FilterPanel',

  props: {
    showFavorites: {
      type: Boolean,
      default: false
    },
    showSummary: {
      type: Boolean,
      default: true
    },
    manualApply: {
      type: Boolean,
      default: false
    }
  },

  emits: [
    'filter-change',
    'apply',
    'reset'
  ],

  setup(props, { emit }) {
    const filterStore = useFilterStore();
    const showInfoModal = ref(false);

    // Local copy of filters
    const localFilters = ref({
      acceptsInsurance: filterStore.filterOptions.acceptsInsurance,
      acceptsPrivatePay: filterStore.filterOptions.acceptsPrivatePay,
      matchesAge: filterStore.filterOptions.matchesAge,
      matchesDiagnosis: filterStore.filterOptions.matchesDiagnosis,
      matchesTherapy: filterStore.filterOptions.matchesTherapy,
      showOnlyFavorites: filterStore.filterOptions.showOnlyFavorites
    });

    // User data from store
    const userData = computed(() => filterStore.userData);
    
    // Available options from store
    const availableTherapyTypes = computed(() => filterStore.availableTherapyTypes);
    const availableInsuranceTypes = computed(() => filterStore.availableInsuranceTypes);
    
    // Check if user has profile data
    const hasUserProfile = computed(() => {
      return !!(userData.value.age || userData.value.diagnosis || userData.value.therapy);
    });

    // Active filter count
    const activeFilterCount = computed(() => {
      let count = 0;
      if (localFilters.value.acceptsInsurance) count++;
      if (localFilters.value.acceptsPrivatePay) count++;
      if (localFilters.value.matchesAge) count++;
      if (localFilters.value.matchesDiagnosis) count++;
      if (localFilters.value.matchesTherapy) count++;
      if (localFilters.value.showOnlyFavorites) count++;
      // Add count for selected therapy types
      count += filterStore.filterOptions.therapies.length;
      return count;
    });

    // Has any active filters
    const hasActiveFilters = computed(() => {
      return activeFilterCount.value > 0 || filterStore.filterOptions.therapies.length > 0;
    });

    /**
     * Format age for display
     */
    const formatAge = (age) => {
      if (!age) return '';
      if (age.includes('-') || age.includes('+')) {
        return `${age} years`;
      }
      return age;
    };

    /**
     * Format diagnosis for display
     */
    const formatDiagnosis = (diagnosis) => {
      if (!diagnosis) return '';
      const shortForms = {
        'Autism Spectrum Disorder': 'Autism',
        'Global Development Delay': 'Development Delay',
        'Intellectual Disability': 'Intellectual Disability',
        'Speech and Language Disorder': 'Speech/Language',
        'ADHD': 'ADHD'
      };
      return shortForms[diagnosis] || diagnosis;
    };

    /**
     * Handle filter change
     */
    const handleFilterChange = () => {
      console.log('🔍 FilterPanel: Filter changed', localFilters.value);
      
      // Apply to store
      applyFiltersToStore();
      emit('filter-change', localFilters.value);
    };

    /**
     * Apply filters to store
     */
    const applyFiltersToStore = () => {
      console.log('🔍 FilterPanel: Applying filters to store', localFilters.value);

      Object.keys(localFilters.value).forEach(key => {
        filterStore.setFilter(key, localFilters.value[key]);
      });
    };

    /**
     * Handle reset
     */
    const handleReset = () => {
      console.log('🔍 FilterPanel: Reset filters');

      // Reset all filters
      localFilters.value = {
        acceptsInsurance: false,
        acceptsPrivatePay: false,
        matchesAge: false,
        matchesDiagnosis: false,
        matchesTherapy: false,
        showOnlyFavorites: false
      };

      // Clear therapy types, diagnoses, and insurance types arrays in store
      filterStore.filterOptions.therapies = [];
      filterStore.filterOptions.diagnoses = [];
      filterStore.filterOptions.insuranceTypes = [];

      applyFiltersToStore();
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
     * Check if a therapy type is selected
     */
    const isTherapySelected = (therapy) => {
      return filterStore.filterOptions.therapies.includes(therapy);
    };

    /**
     * Handle therapy type toggle
     */
    const handleTherapyToggle = (therapy) => {
      console.log(`🎯 FilterPanel: Toggling therapy type: ${therapy}`);
      filterStore.toggleTherapyType(therapy);
      emit('filter-change', localFilters.value);
    };

    /**
     * Check if an insurance type is selected
     */
    const isInsuranceSelected = (insurance) => {
      return filterStore.filterOptions.insuranceTypes.includes(insurance);
    };

    /**
     * Handle insurance type toggle
     */
    const handleInsuranceToggle = (insurance) => {
      console.log(`💳 FilterPanel: Toggling insurance type: ${insurance}`);
      filterStore.toggleInsuranceType(insurance);
      emit('filter-change', localFilters.value);
    };

    // Sync with store when store changes
    watch(
      () => filterStore.filterOptions,
      (newOptions) => {
        localFilters.value = { ...newOptions };
      },
      { deep: true }
    );

    return {
      filterStore,
      localFilters,
      userData,
      availableTherapyTypes,
      availableInsuranceTypes,
      hasUserProfile,
      activeFilterCount,
      hasActiveFilters,
      showInfoModal,
      formatAge,
      formatDiagnosis,
      handleFilterChange,
      handleReset,
      toggleFilter,
      isTherapySelected,
      handleTherapyToggle,
      isInsuranceSelected,
      handleInsuranceToggle
    };
  }
};
</script>

<style scoped>
.filter-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Filter Section */
.filter-section {
  background: white;
  border-radius: 10px;
  padding: 1rem;
  border: 1px solid #e5e7eb;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #f3f4f6;
}

.section-header i {
  font-size: 1.125rem;
  color: #6b7280;
}

.section-header h4 {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
}

.btn-info {
  background: transparent;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  transition: color 0.2s ease;
}

.btn-info:hover {
  color: #004877;
}

.btn-info i {
  font-size: 1rem;
}

/* Filter Options */
.filter-options {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.filter-option {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  user-select: none;
}

.filter-option:hover {
  background-color: #f9fafb;
}

.filter-option input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #004877;
  flex-shrink: 0;
}

.filter-option span {
  font-size: 0.9375rem;
  color: #374151;
  line-height: 1.4;
}

.filter-label {
  font-weight: 400;
}

/* No Profile Hint */
.no-profile-hint {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #f3f4f6;
  border-radius: 6px;
  font-size: 0.8125rem;
  color: #6b7280;
  line-height: 1.5;
}

.no-profile-hint i {
  flex-shrink: 0;
  margin-top: 0.125rem;
}

/* Active Filters */
.active-filters {
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  padding: 1rem;
}

.active-filters-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.active-count {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1e40af;
}

.btn-clear-all {
  background: transparent;
  border: none;
  color: #6b7280;
  font-size: 0.8125rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.btn-clear-all:hover {
  background: #dbeafe;
  color: #1e40af;
}

/* Filter Chips */
.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.625rem;
  background: white;
  border: 1px solid #bfdbfe;
  border-radius: 16px;
  color: #1e40af;
  font-size: 0.8125rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-chip:hover {
  background: #fee2e2;
  border-color: #fecaca;
  color: #dc2626;
}

.filter-chip i {
  font-size: 0.75rem;
}

/* Info Modal */
.info-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 1rem;
}

.info-modal {
  background: white;
  border-radius: 12px;
  max-width: 500px;
  width: 100%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
}

.btn-close {
  background: transparent;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  transition: color 0.2s ease;
}

.btn-close:hover {
  color: #1f2937;
}

.modal-body {
  padding: 1.5rem;
}

.modal-body p {
  margin: 0 0 1rem 0;
  font-size: 0.9375rem;
  color: #374151;
  line-height: 1.6;
}

.modal-body ul {
  margin: 0 0 1rem 0;
  padding-left: 1.5rem;
}

.modal-body li {
  margin-bottom: 0.5rem;
  font-size: 0.9375rem;
  color: #374151;
  line-height: 1.6;
}

.modal-note {
  background: #f9fafb;
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
  color: #6b7280;
  border-left: 3px solid #004877;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
}

.btn-primary {
  background: #004877;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 0.5rem 1.5rem;
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover {
  background: #003a5d;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .filter-section {
    padding: 0.875rem;
  }

  .filter-option {
    padding: 0.625rem 0.5rem;
  }

  .info-modal {
    max-width: 100%;
    margin: 0.5rem;
  }
}
</style>
