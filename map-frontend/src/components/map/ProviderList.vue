<template>
  <div class="provider-list">
    <!-- List Header -->
    <div class="list-header">
      <div class="header-content">
        <h2 class="list-title">
          <i class="bi bi-hospital"></i>
          <span>Providers</span>
        </h2>
        <div v-if="!loading && providers.length > 0" class="provider-count">
          {{ providers.length }} {{ providers.length === 1 ? 'result' : 'results' }}
        </div>
      </div>

      <!-- Sort Controls -->
      <div v-if="showSortControls && !loading && providers.length > 0" class="sort-controls">
        <label for="sort-select" class="sort-label">Sort by:</label>
        <select
          id="sort-select"
          v-model="currentSort"
          @change="handleSortChange"
          class="sort-select"
        >
          <option value="distance">Distance</option>
          <option value="name">Name</option>
          <option value="type">Type</option>
        </select>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner-container">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Loading providers...</span>
        </div>
        <p class="loading-text">Finding providers...</p>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading && providers.length === 0" class="empty-state">
      <div class="empty-icon">
        <i class="bi bi-search"></i>
      </div>
      <h3 class="empty-title">{{ emptyTitle }}</h3>
      <p class="empty-message">{{ emptyMessage }}</p>
      <slot name="empty-actions"></slot>
    </div>

    <!-- Provider List -->
    <div v-else class="list-container" ref="listContainer">
      <div class="provider-items">
        <ProviderCard
          v-for="provider in sortedProviders"
          :key="provider.id"
          :provider="provider"
          :selected="provider.id === selectedProviderId"
          :distance="getProviderDistance(provider)"
          :showInsurance="showInsurance"
          :showTherapies="showTherapies"
          :showAgeGroups="showAgeGroups"
          :maxTherapiesToShow="maxTherapiesToShow"
          @click="handleProviderClick"
          @select="handleProviderSelect"
        />
      </div>

      <!-- Scroll to Top Button -->
      <button
        v-if="showScrollTop"
        class="scroll-top-btn"
        @click="scrollToTop"
        aria-label="Scroll to top"
      >
        <i class="bi bi-arrow-up"></i>
      </button>
    </div>

    <!-- Load More (if pagination enabled) -->
    <div v-if="showLoadMore && hasMore && !loading" class="load-more-container">
      <button class="load-more-btn" @click="handleLoadMore">
        <i class="bi bi-arrow-down-circle"></i>
        <span>Load More</span>
      </button>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue';
import ProviderCard from './ProviderCard.vue';
import { useProviderStore } from '@/stores/providerStore';
import { useMapStore } from '@/stores/mapStore';
import { calculateDistance } from '@/utils/map';

/**
 * ProviderList Component
 * Displays a scrollable list of provider cards
 * Week 4: Component Extraction
 */
export default {
  name: 'ProviderList',

  components: {
    ProviderCard
  },

  props: {
    // Override providers (optional - uses store by default)
    providers: {
      type: Array,
      default: null
    },
    // Loading state override
    loading: {
      type: Boolean,
      default: null
    },
    // Selected provider ID override
    selectedProviderId: {
      type: Number,
      default: null
    },
    // Show insurance badges on cards
    showInsurance: {
      type: Boolean,
      default: true
    },
    // Show therapy types on cards
    showTherapies: {
      type: Boolean,
      default: true
    },
    // Show age groups on cards
    showAgeGroups: {
      type: Boolean,
      default: false
    },
    // Max therapies to show on each card
    maxTherapiesToShow: {
      type: Number,
      default: 3
    },
    // Show sort controls
    showSortControls: {
      type: Boolean,
      default: true
    },
    // Empty state title
    emptyTitle: {
      type: String,
      default: 'No providers found'
    },
    // Empty state message
    emptyMessage: {
      type: String,
      default: 'Try adjusting your search or filters to find providers.'
    },
    // Enable pagination/load more
    showLoadMore: {
      type: Boolean,
      default: false
    },
    // Whether there are more items to load
    hasMore: {
      type: Boolean,
      default: false
    },
    // Auto-scroll to selected provider
    autoScrollToSelected: {
      type: Boolean,
      default: true
    }
  },

  emits: [
    'provider-click',     // When provider card is clicked
    'provider-select',    // When provider is selected
    'scroll',             // When list is scrolled
    'load-more'           // When load more is triggered
  ],

  setup(props, { emit }) {
    const listContainer = ref(null);
    const currentSort = ref('distance');
    const showScrollTop = ref(false);

    const providerStore = useProviderStore();
    const mapStore = useMapStore();

    // Use prop or store data
    const providers = computed(() => {
      return props.providers !== null ? props.providers : providerStore.providers;
    });

    const loading = computed(() => {
      return props.loading !== null ? props.loading : providerStore.loading;
    });

    const selectedProviderId = computed(() => {
      return props.selectedProviderId !== null
        ? props.selectedProviderId
        : providerStore.selectedProviderId;
    });

    const userLocation = computed(() => mapStore.userLocation);

    /**
     * Calculate distance from user to provider
     */
    const getProviderDistance = (provider) => {
      if (!userLocation.value || !provider.latitude || !provider.longitude) {
        return null;
      }

      return calculateDistance(
        userLocation.value.lat,
        userLocation.value.lng,
        provider.latitude,
        provider.longitude
      );
    };

    /**
     * Sort providers based on current sort option
     */
    const sortedProviders = computed(() => {
      if (!providers.value || providers.value.length === 0) {
        return [];
      }

      const sorted = [...providers.value];

      switch (currentSort.value) {
        case 'distance':
          return sorted.sort((a, b) => {
            const distA = getProviderDistance(a);
            const distB = getProviderDistance(b);

            // Providers without distance go to end
            if (distA === null) return 1;
            if (distB === null) return -1;

            return distA - distB;
          });

        case 'name':
          return sorted.sort((a, b) => {
            return (a.name || '').localeCompare(b.name || '');
          });

        case 'type':
          return sorted.sort((a, b) => {
            // Sort by type first, then by name
            const typeCompare = (a.type || '').localeCompare(b.type || '');
            if (typeCompare !== 0) return typeCompare;
            return (a.name || '').localeCompare(b.name || '');
          });

        default:
          return sorted;
      }
    });

    /**
     * Handle provider card click
     */
    const handleProviderClick = (provider) => {
      console.log(`📋 ProviderList: Provider clicked - ${provider.id}`);
      emit('provider-click', provider);
    };

    /**
     * Handle provider selection
     */
    const handleProviderSelect = (providerId) => {
      console.log(`📋 ProviderList: Provider selected - ${providerId}`);

      // Update store
      providerStore.selectProvider(providerId);
      mapStore.selectProvider(providerId);

      emit('provider-select', providerId);

      // Auto-scroll to selected provider
      if (props.autoScrollToSelected) {
        nextTick(() => {
          scrollToProvider(providerId);
        });
      }
    };

    /**
     * Handle sort change
     */
    const handleSortChange = () => {
      console.log(`📋 ProviderList: Sort changed to ${currentSort.value}`);
    };

    /**
     * Scroll to specific provider
     */
    const scrollToProvider = (providerId) => {
      if (!listContainer.value) return;

      const providerElement = listContainer.value.querySelector(
        `.provider-card[data-provider-id="${providerId}"]`
      );

      if (providerElement) {
        providerElement.scrollIntoView({
          behavior: 'smooth',
          block: 'nearest'
        });
      }
    };

    /**
     * Scroll to top of list
     */
    const scrollToTop = () => {
      if (!listContainer.value) return;

      listContainer.value.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    };

    /**
     * Handle scroll event
     */
    const handleScroll = () => {
      if (!listContainer.value) return;

      const scrollTop = listContainer.value.scrollTop;

      // Show scroll-to-top button after scrolling down
      showScrollTop.value = scrollTop > 300;

      emit('scroll', {
        scrollTop,
        scrollHeight: listContainer.value.scrollHeight,
        clientHeight: listContainer.value.clientHeight
      });
    };

    /**
     * Handle load more
     */
    const handleLoadMore = () => {
      console.log('📋 ProviderList: Load more triggered');
      emit('load-more');
    };

    // Watch for selected provider changes
    watch(selectedProviderId, (newId) => {
      if (newId && props.autoScrollToSelected) {
        nextTick(() => {
          scrollToProvider(newId);
        });
      }
    });

    // Set up scroll listener
    onMounted(() => {
      if (listContainer.value) {
        listContainer.value.addEventListener('scroll', handleScroll);
      }
    });

    onBeforeUnmount(() => {
      if (listContainer.value) {
        listContainer.value.removeEventListener('scroll', handleScroll);
      }
    });

    return {
      listContainer,
      currentSort,
      showScrollTop,
      providers,
      loading,
      selectedProviderId,
      sortedProviders,
      getProviderDistance,
      handleProviderClick,
      handleProviderSelect,
      handleSortChange,
      scrollToTop,
      handleLoadMore
    };
  }
};
</script>

<style scoped>
.provider-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f9fafb;
}

/* Header */
.list-header {
  padding: 16px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.list-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.list-title i {
  color: #2563eb;
  font-size: 20px;
}

.provider-count {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  background-color: #f3f4f6;
  padding: 4px 12px;
  border-radius: 12px;
}

/* Sort Controls */
.sort-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  margin: 0;
}

.sort-select {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  color: #374151;
  background-color: white;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.sort-select:hover {
  border-color: #2563eb;
}

.sort-select:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* Loading State */
.loading-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
}

.spinner-container {
  text-align: center;
}

.spinner-border {
  width: 48px;
  height: 48px;
  border-width: 4px;
}

.loading-text {
  margin-top: 16px;
  font-size: 14px;
  color: #6b7280;
}

/* Empty State */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background-color: #f3f4f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.empty-icon i {
  font-size: 36px;
  color: #9ca3af;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.empty-message {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
  max-width: 320px;
}

/* List Container */
.list-container {
  flex: 1;
  overflow-y: auto;
  position: relative;
}

.provider-items {
  padding: 16px;
}

/* Scroll to Top Button */
.scroll-top-btn {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 48px;
  height: 48px;
  background-color: #2563eb;
  color: white;
  border: none;
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: all 0.2s ease;
  z-index: 10;
}

.scroll-top-btn:hover {
  background-color: #1d4ed8;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.5);
}

.scroll-top-btn:active {
  transform: translateY(0);
}

/* Load More */
.load-more-container {
  padding: 16px;
  background: white;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.load-more-btn {
  width: 100%;
  padding: 12px;
  background-color: #f3f4f6;
  color: #374151;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.load-more-btn:hover {
  background-color: #e5e7eb;
  border-color: #9ca3af;
}

.load-more-btn i {
  font-size: 18px;
}

/* Scrollbar Styling */
.list-container::-webkit-scrollbar {
  width: 8px;
}

.list-container::-webkit-scrollbar-track {
  background: #f3f4f6;
}

.list-container::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 4px;
}

.list-container::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* Responsive */
@media (max-width: 768px) {
  .list-header {
    padding: 12px;
  }

  .list-title {
    font-size: 16px;
  }

  .provider-items {
    padding: 12px;
  }

  .scroll-top-btn {
    bottom: 16px;
    right: 16px;
    width: 44px;
    height: 44px;
  }
}

/* Accessibility */
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
</style>
