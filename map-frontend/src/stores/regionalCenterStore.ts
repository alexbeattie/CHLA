/**
 * Regional Center Store
 * Centralized state management for LA Regional Center data
 * Created during MapView.vue refactoring
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface RegionalCenter {
  name: string;
  zipCodes: string[];
  color: string;
  selected: boolean;
}

export const useRegionalCenterStore = defineStore('regionalCenter', () => {
  // ==================== STATE ====================

  const selectedCenters = ref<Record<string, boolean>>({});
  const zipToCenter = ref<Record<string, string>>({});
  const laRegionalCentersData = ref<any | null>(null);
  const matchedRegionalCenter = ref<string | null>(null);

  // Regional Center definitions
  const regionalCenters = ref<RegionalCenter[]>([
    {
      name: "North Los Angeles County Regional Center",
      zipCodes: [],
      color: "#f1c40f",
      selected: false
    },
    {
      name: "San Gabriel/Pomona Regional Center",
      zipCodes: [],
      color: "#4caf50",
      selected: false
    },
    {
      name: "Eastern Los Angeles Regional Center",
      zipCodes: [],
      color: "#ff9800",
      selected: false
    },
    {
      name: "Westside Regional Center",
      zipCodes: [],
      color: "#e91e63",
      selected: false
    },
    {
      name: "Frank D. Lanterman Regional Center",
      zipCodes: [],
      color: "#9c27b0",
      selected: false
    },
    {
      name: "South Central Los Angeles Regional Center",
      zipCodes: [],
      color: "#f44336",
      selected: false
    },
    {
      name: "Harbor Regional Center",
      zipCodes: [],
      color: "#2196f3",
      selected: false
    }
  ]);

  // ==================== GETTERS ====================

  const selectedCenterNames = computed(() =>
    Object.keys(selectedCenters.value).filter(name => selectedCenters.value[name])
  );

  const hasSelectedCenters = computed(() => selectedCenterNames.value.length > 0);

  const centerCount = computed(() => regionalCenters.value.length);

  const hasMatchedCenter = computed(() => matchedRegionalCenter.value !== null);

  // ==================== ACTIONS ====================

  /**
   * Toggle selection of a regional center
   */
  function toggleCenterSelection(name: string) {
    selectedCenters.value[name] = !selectedCenters.value[name];
    console.log(`Regional center ${name} toggled:`, selectedCenters.value[name]);
  }

  /**
   * Select a regional center
   */
  function selectCenter(name: string) {
    selectedCenters.value[name] = true;
  }

  /**
   * Deselect a regional center
   */
  function deselectCenter(name: string) {
    selectedCenters.value[name] = false;
  }

  /**
   * Clear all selections
   */
  function clearSelections() {
    selectedCenters.value = {};
  }

  /**
   * Set ZIP to regional center mapping
   */
  function setZipMapping(zipCode: string, centerName: string) {
    zipToCenter.value[zipCode] = centerName;
  }

  /**
   * Get regional center by ZIP code
   */
  function getCenterByZip(zipCode: string): string | null {
    return zipToCenter.value[zipCode] || null;
  }

  /**
   * Set LA Regional Centers GeoJSON data
   */
  function setRegionalCentersData(data: any) {
    laRegionalCentersData.value = data;
  }

  /**
   * Set matched regional center (user's regional center)
   */
  function setMatchedCenter(centerName: string | null) {
    matchedRegionalCenter.value = centerName;
  }

  /**
   * Load regional center data from API or file
   */
  async function loadRegionalCentersData() {
    try {
      const response = await fetch('/assets/geo/la_rc_7.geojson');
      if (response.ok) {
        const data = await response.json();
        setRegionalCentersData(data);
        console.log('✅ Regional centers data loaded');
        return data;
      }
    } catch (error) {
      console.error('Failed to load regional centers data:', error);
    }
    return null;
  }

  return {
    // State
    selectedCenters,
    zipToCenter,
    laRegionalCentersData,
    matchedRegionalCenter,
    regionalCenters,

    // Getters
    selectedCenterNames,
    hasSelectedCenters,
    centerCount,
    hasMatchedCenter,

    // Actions
    toggleCenterSelection,
    selectCenter,
    deselectCenter,
    clearSelections,
    setZipMapping,
    getCenterByZip,
    setRegionalCentersData,
    setMatchedCenter,
    loadRegionalCentersData
  };
});
