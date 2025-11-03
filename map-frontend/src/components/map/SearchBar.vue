<template>
  <div class="search-bar">
    <div class="search-container">
      <input
        ref="searchInput"
        type="text"
        v-model="searchQuery"
        @input="handleInput"
        @keyup.enter="handleSearch"
        @focus="handleFocus"
        @blur="handleBlur"
        class="form-control search-input"
        :placeholder="placeholder"
        :disabled="loading"
      />

      <!-- Clear button -->
      <button
        v-if="searchQuery"
        class="clear-btn"
        @click="handleClear"
        :disabled="loading"
        type="button"
        aria-label="Clear search"
      >
        <i class="bi bi-x-circle"></i>
      </button>

      <!-- Search button -->
      <button
        class="search-btn"
        @click="handleSearch"
        :disabled="loading || !searchQuery"
        type="button"
        aria-label="Search"
      >
        <i v-if="!loading" class="bi bi-search"></i>
        <span v-else class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
      </button>
    </div>

    <!-- Search validation message -->
    <div v-if="validationMessage" class="validation-message" :class="validationClass">
      <i :class="validationIcon"></i>
      {{ validationMessage }}
    </div>

    <!-- Search results summary -->
    <div v-if="showResultsSummary && resultsCount !== null" class="results-summary">
      <i class="bi bi-info-circle"></i>
      Found {{ resultsCount }} {{ resultsCount === 1 ? 'provider' : 'providers' }}
      <span v-if="searchType === 'zip'"> in ZIP code {{ lastSearchQuery }}</span>
      <span v-else-if="searchType === 'location'"> near {{ lastSearchQuery }}</span>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue';
import { useProviderStore } from '@/stores/providerStore';
import { useMapStore } from '@/stores/mapStore';

/**
 * SearchBar Component
 * Handles ZIP code and location-based provider search
 * Week 4: Component Extraction
 */
export default {
  name: 'SearchBar',

  props: {
    // Placeholder text for the search input
    placeholder: {
      type: String,
      default: 'Enter city or ZIP code'
    },
    // Whether to show results summary
    showResultsSummary: {
      type: Boolean,
      default: true
    },
    // Auto-focus the input on mount
    autoFocus: {
      type: Boolean,
      default: false
    },
    // Debounce delay in milliseconds
    debounceDelay: {
      type: Number,
      default: 300
    }
  },

  emits: [
    'search',           // Emitted when search is performed
    'clear',            // Emitted when search is cleared
    'validation-error', // Emitted when validation fails
    'results'           // Emitted when results are received
  ],

  setup(props, { emit }) {
    const searchInput = ref(null);
    const searchQuery = ref('');
    const lastSearchQuery = ref('');
    const isFocused = ref(false);
    const debounceTimer = ref(null);
    const validationMessage = ref('');
    const validationClass = ref('');
    const resultsCount = ref(null);
    const searchType = ref(null); // 'zip' or 'location'

    const providerStore = useProviderStore();
    const mapStore = useMapStore();

    const loading = computed(() => providerStore.loading);

    const validationIcon = computed(() => {
      if (validationClass.value === 'error') return 'bi bi-exclamation-circle';
      if (validationClass.value === 'success') return 'bi bi-check-circle';
      return 'bi bi-info-circle';
    });

    /**
     * Validate ZIP code format
     */
    const isValidZipCode = (text) => {
      return /^\d{5}$/.test(text.trim());
    };

    /**
     * Validate search input
     */
    const validateSearch = () => {
      const query = searchQuery.value.trim();

      if (!query) {
        validationMessage.value = '';
        validationClass.value = '';
        return false;
      }

      // Check if it's a ZIP code
      if (/^\d/.test(query)) {
        if (isValidZipCode(query)) {
          validationMessage.value = '';
          validationClass.value = '';
          searchType.value = 'zip';
          return true;
        } else if (query.length < 5) {
          validationMessage.value = 'ZIP code must be 5 digits';
          validationClass.value = 'error';
          emit('validation-error', 'ZIP code must be 5 digits');
          return false;
        } else {
          validationMessage.value = 'Invalid ZIP code format';
          validationClass.value = 'error';
          emit('validation-error', 'Invalid ZIP code format');
          return false;
        }
      }

      // Location search (city, address, etc.)
      if (query.length < 3) {
        validationMessage.value = 'Please enter at least 3 characters';
        validationClass.value = 'error';
        emit('validation-error', 'Please enter at least 3 characters');
        return false;
      }

      validationMessage.value = '';
      validationClass.value = '';
      searchType.value = 'location';
      return true;
    };

    /**
     * Perform search
     */
    const handleSearch = async () => {
      console.log('🔍 SearchBar: handleSearch called');

      if (!validateSearch()) {
        console.log('❌ SearchBar: Validation failed');
        return;
      }

      const query = searchQuery.value.trim();
      lastSearchQuery.value = query;

      console.log(`🔍 SearchBar: Searching for "${query}" (type: ${searchType.value})`);

      try {
        let results;

        if (searchType.value === 'zip') {
          // ZIP code search
          console.log(`🔍 SearchBar: ZIP code search for ${query}`);
          results = await providerStore.searchByZipCode(query);
          resultsCount.value = results.count || results.providers.length;

          // Update map to show regional center area
          if (results.center) {
            // CRITICAL FIX: Set user location so directions work from ZIP search location
            mapStore.setUserLocation(results.center);
            mapStore.centerOn(results.center, 11);
            console.log('✅ SearchBar: Set user location from ZIP search:', results.center);
          } else if (results.providers.length > 0) {
            const firstProvider = results.providers[0];
            if (firstProvider.latitude && firstProvider.longitude) {
              const center = {
                lat: firstProvider.latitude,
                lng: firstProvider.longitude
              };
              // CRITICAL FIX: Set user location so directions work
              mapStore.setUserLocation(center);
              mapStore.centerOn(center, 11);
              console.log('✅ SearchBar: Set user location from first provider:', center);
            }
          }
        } else {
          // Location search (city, address, etc.)
          console.log(`🔍 SearchBar: Location search for ${query}`);
          results = await providerStore.searchByLocation(query);
          resultsCount.value = results.count || results.providers.length;

          // Update map to show search location
          if (results.center) {
            mapStore.setUserLocation(results.center);
            mapStore.centerOn(results.center, 12);
          }
        }

        console.log(`✅ SearchBar: Found ${resultsCount.value} providers`);

        validationMessage.value = '';
        validationClass.value = '';

        emit('search', {
          query,
          type: searchType.value,
          results: results.providers,
          count: resultsCount.value
        });

        emit('results', results);

      } catch (error) {
        console.error('❌ SearchBar: Search error:', error);
        validationMessage.value = error.message || 'Search failed. Please try again.';
        validationClass.value = 'error';
        emit('validation-error', validationMessage.value);
      }
    };

    /**
     * Handle input changes with debouncing
     */
    const handleInput = () => {
      // Clear previous timer
      if (debounceTimer.value) {
        clearTimeout(debounceTimer.value);
      }

      // Clear validation on input
      validationMessage.value = '';
      validationClass.value = '';

      // Set new timer
      if (props.debounceDelay > 0) {
        debounceTimer.value = setTimeout(() => {
          validateSearch();
        }, props.debounceDelay);
      }
    };

    /**
     * Clear search
     */
    const handleClear = () => {
      console.log('🔍 SearchBar: Clearing search');

      searchQuery.value = '';
      lastSearchQuery.value = '';
      validationMessage.value = '';
      validationClass.value = '';
      resultsCount.value = null;
      searchType.value = null;

      // Clear providers in store
      providerStore.clearProviders();

      emit('clear');

      // Focus input after clear
      if (searchInput.value) {
        searchInput.value.focus();
      }
    };

    /**
     * Handle focus
     */
    const handleFocus = () => {
      isFocused.value = true;
    };

    /**
     * Handle blur
     */
    const handleBlur = () => {
      isFocused.value = false;
    };

    /**
     * Focus the search input (exposed method)
     */
    const focus = () => {
      if (searchInput.value) {
        searchInput.value.focus();
      }
    };

    // Auto-focus on mount if requested
    if (props.autoFocus) {
      setTimeout(() => focus(), 100);
    }

    // Watch for provider store changes
    watch(
      () => providerStore.providers,
      (newProviders) => {
        if (lastSearchQuery.value && newProviders) {
          resultsCount.value = newProviders.length;
        }
      }
    );

    return {
      searchInput,
      searchQuery,
      lastSearchQuery,
      isFocused,
      validationMessage,
      validationClass,
      validationIcon,
      resultsCount,
      searchType,
      loading,
      handleSearch,
      handleInput,
      handleClear,
      handleFocus,
      handleBlur,
      focus
    };
  }
};
</script>

<style scoped>
.search-bar {
  width: 100%;
}

.search-container {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0;
}

.search-input {
  flex: 1;
  padding: 12px 80px 12px 16px;
  font-size: 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  outline: none;
  transition: all 0.2s ease;
}

.search-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.search-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.clear-btn {
  position: absolute;
  right: 48px;
  background: none;
  border: none;
  color: #999;
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
}

.clear-btn:hover:not(:disabled) {
  color: #666;
}

.clear-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.search-btn {
  position: absolute;
  right: 8px;
  background-color: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  min-width: 36px;
  height: 36px;
}

.search-btn:hover:not(:disabled) {
  background-color: #1d4ed8;
  transform: translateY(-1px);
}

.search-btn:active:not(:disabled) {
  transform: translateY(0);
}

.search-btn:disabled {
  background-color: #93c5fd;
  cursor: not-allowed;
}

.validation-message {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.validation-message.error {
  background-color: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.validation-message.success {
  background-color: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.validation-message.info {
  background-color: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.results-summary {
  margin-top: 8px;
  padding: 8px 12px;
  background-color: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.results-summary i {
  color: #2563eb;
}

/* Responsive styles */
@media (max-width: 768px) {
  .search-input {
    font-size: 16px; /* Prevent zoom on iOS */
    padding: 14px 80px 14px 14px;
  }

  .search-btn {
    right: 6px;
  }

  .clear-btn {
    right: 46px;
  }
}

/* Loading spinner animation */
.spinner-border {
  width: 1rem;
  height: 1rem;
  border-width: 2px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.spinner-border {
  animation: spin 0.75s linear infinite;
}
</style>
