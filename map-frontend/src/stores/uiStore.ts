/**
 * UI Store
 * Centralized state management for UI visibility and modal states
 * Created during MapView.vue refactoring
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useUIStore = defineStore('ui', () => {
  // ==================== STATE ====================

  // Modals
  const showFundingInfo = ref(false);
  const showOnboarding = ref(false);
  const showUserMenu = ref(false);

  // Sidebars
  const showMobileSidebar = ref(false);
  const showMobileSearch = ref(false);

  // Map Overlays
  const showServiceAreas = ref(false);
  const pinServiceAreas = ref(false);
  const showLARegionalCenters = ref(false);
  const zipViewOnly = ref(false);

  // ==================== GETTERS ====================

  const hasAnyModalOpen = computed(() =>
    showFundingInfo.value || showOnboarding.value || showUserMenu.value
  );

  const hasAnySidebarOpen = computed(() =>
    showMobileSidebar.value || showMobileSearch.value
  );

  const hasAnyOverlayActive = computed(() =>
    showServiceAreas.value || showLARegionalCenters.value
  );

  // ==================== ACTIONS ====================

  /**
   * Toggle funding info modal
   */
  function toggleFundingInfo() {
    showFundingInfo.value = !showFundingInfo.value;
  }

  /**
   * Toggle onboarding flow
   */
  function toggleOnboarding() {
    showOnboarding.value = !showOnboarding.value;
  }

  /**
   * Show/hide onboarding
   */
  function setOnboarding(visible: boolean) {
    showOnboarding.value = visible;
  }

  /**
   * Toggle user menu
   */
  function toggleUserMenu() {
    showUserMenu.value = !showUserMenu.value;
  }

  /**
   * Toggle mobile sidebar
   */
  function toggleMobileSidebar() {
    showMobileSidebar.value = !showMobileSidebar.value;
  }

  /**
   * Toggle mobile search
   */
  function toggleMobileSearch() {
    showMobileSearch.value = !showMobileSearch.value;
  }

  /**
   * Toggle service areas overlay
   */
  function toggleServiceAreas() {
    showServiceAreas.value = !showServiceAreas.value;
  }

  /**
   * Toggle LA regional centers overlay
   */
  function toggleLARegionalCenters() {
    showLARegionalCenters.value = !showLARegionalCenters.value;
  }

  /**
   * Toggle pin service areas
   */
  function togglePinServiceAreas() {
    pinServiceAreas.value = !pinServiceAreas.value;
  }

  /**
   * Set ZIP view only mode
   */
  function setZipViewOnly(enabled: boolean) {
    zipViewOnly.value = enabled;
  }

  /**
   * Close all modals
   */
  function closeAllModals() {
    showFundingInfo.value = false;
    showOnboarding.value = false;
    showUserMenu.value = false;
  }

  /**
   * Close all sidebars
   */
  function closeAllSidebars() {
    showMobileSidebar.value = false;
    showMobileSearch.value = false;
  }

  /**
   * Reset all UI state
   */
  function resetUI() {
    closeAllModals();
    closeAllSidebars();
    showServiceAreas.value = false;
    pinServiceAreas.value = false;
    showLARegionalCenters.value = false;
    zipViewOnly.value = false;
  }

  return {
    // State
    showFundingInfo,
    showOnboarding,
    showUserMenu,
    showMobileSidebar,
    showMobileSearch,
    showServiceAreas,
    pinServiceAreas,
    showLARegionalCenters,
    zipViewOnly,

    // Getters
    hasAnyModalOpen,
    hasAnySidebarOpen,
    hasAnyOverlayActive,

    // Actions
    toggleFundingInfo,
    toggleOnboarding,
    setOnboarding,
    toggleUserMenu,
    toggleMobileSidebar,
    toggleMobileSearch,
    toggleServiceAreas,
    toggleLARegionalCenters,
    togglePinServiceAreas,
    setZipViewOnly,
    closeAllModals,
    closeAllSidebars,
    resetUI
  };
});
