/**
 * Map Layers Composable
 * Handles adding/removing GeoJSON layers to Mapbox GL map
 * Extracted from MapView.vue to reduce component complexity
 */

import { ref } from 'vue';
import type { Map as MapboxMap } from 'mapbox-gl';

export interface LayerState {
  serviceAreas: {
    visible: boolean;
    data: any | null;
  };
  laZipCodes: {
    visible: boolean;
    data: any | null;
  };
  laRegionalCenters: {
    visible: boolean;
    data: any | null;
  };
}

export function useMapLayers() {
  const layerState = ref<LayerState>({
    serviceAreas: { visible: false, data: null },
    laZipCodes: { visible: false, data: null },
    laRegionalCenters: { visible: false, data: null }
  });

  /**
   * Remove service areas from map
   */
  const removeServiceAreas = (map: MapboxMap) => {
    if (!map) return;

    console.log("Removing service areas from map");

    // List of service area layers to remove
    const serviceLayers = [
      "service-areas-fill",
      "service-areas-outline",
      "california-counties-fill",
      "california-counties-outline"
    ];

    serviceLayers.forEach(layerId => {
      try {
        if (map.getLayer(layerId)) {
          map.removeLayer(layerId);
        }
      } catch (e) {
        console.warn(`Failed to remove layer ${layerId}:`, e);
      }
    });

    // Remove sources
    const serviceSources = ["service-areas", "california-counties"];
    serviceSources.forEach(sourceId => {
      try {
        if (map.getSource(sourceId)) {
          map.removeSource(sourceId);
        }
      } catch (e) {
        console.warn(`Failed to remove source ${sourceId}:`, e);
      }
    });

    layerState.value.serviceAreas.visible = false;
  };

  /**
   * Remove LA ZIP codes from map
   */
  const removeLAZipCodes = (map: MapboxMap) => {
    if (!map) return;

    console.log("Removing LA ZIP codes from map");

    // Remove colored ZIP layers
    const zipLayers = [
      "la-zip-codes-colored-fill",
      "la-zip-codes-colored-outline",
      "la-zip-codes-fill",
      "la-zip-codes-outline"
    ];

    zipLayers.forEach(layerId => {
      try {
        if (map.getLayer(layerId)) {
          map.removeLayer(layerId);
        }
      } catch (e) {
        console.warn(`Failed to remove ZIP layer ${layerId}:`, e);
      }
    });

    // Remove sources
    const zipSources = ["la-zip-codes-colored", "la-zip-codes"];
    zipSources.forEach(sourceId => {
      try {
        if (map.getSource(sourceId)) {
          map.removeSource(sourceId);
        }
      } catch (e) {
        console.warn(`Failed to remove ZIP source ${sourceId}:`, e);
      }
    });

    layerState.value.laZipCodes.visible = false;
  };

  /**
   * Remove LA Regional Centers from map
   */
  const removeLARegionalCenters = (map: MapboxMap) => {
    if (!map) return;

    console.log("Removing LA Regional Centers from map");

    // Remove regional center layers
    const rcLayers = ["rc-static-fill", "rc-static-outline"];

    rcLayers.forEach(layerId => {
      try {
        if (map.getLayer(layerId)) {
          map.removeLayer(layerId);
        }
      } catch (e) {
        console.warn(`Failed to remove RC layer ${layerId}:`, e);
      }
    });

    // Remove source
    try {
      if (map.getSource("rc-static")) {
        map.removeSource("rc-static");
      }
    } catch (e) {
      console.warn(`Failed to remove RC source:`, e);
    }

    layerState.value.laRegionalCenters.visible = false;
  };

  /**
   * Add service areas to map
   * Note: This is a simplified version - full implementation needs service area data fetching
   */
  const addServiceAreas = async (map: MapboxMap, serviceAreasData: any, apiRoot: string) => {
    if (!map || !serviceAreasData) {
      console.error("Map or service areas data not available");
      return;
    }

    if (!map.loaded()) {
      console.log("Map not loaded yet, waiting...");
      map.once("load", () => addServiceAreas(map, serviceAreasData, apiRoot));
      return;
    }

    try {
      removeServiceAreas(map);

      console.log("Adding service areas source and layers...");

      // Add California counties
      const countiesResponse = await fetch(`${apiRoot}/api/california-counties/`);
      const countiesData = await countiesResponse.json();

      if (!map.getSource("california-counties")) {
        map.addSource("california-counties", {
          type: "geojson",
          data: countiesData
        });
      }

      if (!map.getLayer("california-counties-fill")) {
        map.addLayer({
          id: "california-counties-fill",
          type: "fill",
          source: "california-counties",
          paint: {
            "fill-color": "#f0f0f0",
            "fill-opacity": 0.3
          }
        });
      }

      if (!map.getLayer("california-counties-outline")) {
        map.addLayer({
          id: "california-counties-outline",
          type: "line",
          source: "california-counties",
          paint: {
            "line-color": "#999",
            "line-width": 1
          }
        });
      }

      // Add service areas
      if (!map.getSource("service-areas")) {
        map.addSource("service-areas", {
          type: "geojson",
          data: serviceAreasData
        });
      }

      if (!map.getLayer("service-areas-fill")) {
        map.addLayer({
          id: "service-areas-fill",
          type: "fill",
          source: "service-areas",
          paint: {
            "fill-color": [
              "match",
              ["get", "id"],
              1, "#e74c3c",
              2, "#3498db",
              3, "#2ecc71",
              4, "#f39c12",
              5, "#9b59b6",
              6, "#1abc9c",
              7, "#e67e22",
              "#95a5a6"
            ],
            "fill-opacity": 0.3
          }
        });
      }

      if (!map.getLayer("service-areas-outline")) {
        map.addLayer({
          id: "service-areas-outline",
          type: "line",
          source: "service-areas",
          paint: {
            "line-color": "#fff",
            "line-width": 2
          }
        });
      }

      layerState.value.serviceAreas.visible = true;
      layerState.value.serviceAreas.data = serviceAreasData;

      console.log("✅ Service areas added successfully");
    } catch (error) {
      console.error("Error adding service areas:", error);
    }
  };

  /**
   * Add LA Regional Centers to map
   */
  const addLARegionalCenters = async (map: MapboxMap, regionalCentersData: any) => {
    if (!map || !regionalCentersData) {
      console.error("Map or regional centers data not available");
      return;
    }

    if (!map.loaded() || !map.isStyleLoaded()) {
      console.log("Map not ready, waiting...");
      map.once("styledata", () => addLARegionalCenters(map, regionalCentersData));
      return;
    }

    try {
      removeLARegionalCenters(map);

      console.log("Adding LA Regional Centers to map...");

      if (!map.getSource("rc-static")) {
        map.addSource("rc-static", {
          type: "geojson",
          data: regionalCentersData
        });
      }

      if (!map.getLayer("rc-static-fill")) {
        map.addLayer({
          id: "rc-static-fill",
          type: "fill",
          source: "rc-static",
          paint: {
            "fill-color": [
              "match",
              ["get", "REGIONALCENTER"],
              "Harbor Regional Center", "#2196f3",
              "South Central Los Angeles Regional Center", "#f44336",
              "Westside Regional Center", "#e91e63",
              "Eastern Los Angeles Regional Center", "#ff9800",
              "North Los Angeles County Regional Center", "#f1c40f",
              "San Gabriel/Pomona Regional Center", "#4caf50",
              "Frank D. Lanterman Regional Center", "#9c27b0",
              "#cccccc"
            ],
            "fill-opacity": 0.3
          }
        });
      }

      if (!map.getLayer("rc-static-outline")) {
        map.addLayer({
          id: "rc-static-outline",
          type: "line",
          source: "rc-static",
          paint: {
            "line-color": "#ffffff",
            "line-width": 2
          }
        });
      }

      // Move RC layers to top of map layers
      // Note: Mapbox markers are DOM overlays with z-index 1000+, so they will still appear above these layers
      try {
        map.moveLayer("rc-static-fill");
        map.moveLayer("rc-static-outline");
      } catch (e) {
        console.warn("Could not move RC layers:", e);
      }

      layerState.value.laRegionalCenters.visible = true;
      layerState.value.laRegionalCenters.data = regionalCentersData;

      console.log("✅ LA Regional Centers added successfully");
    } catch (error) {
      console.error("Error adding LA Regional Centers:", error);
    }
  };

  /**
   * Update regional center highlighting based on selection
   */
  const updateRegionalCenterHighlighting = (
    map: MapboxMap,
    selectedCenters: Record<string, boolean>
  ) => {
    if (!map || !map.getLayer("rc-static-fill")) return;

    try {
      const selectedNames = Object.keys(selectedCenters).filter(name => selectedCenters[name]);

      if (selectedNames.length === 0) {
        // No selection - show all at normal opacity
        map.setPaintProperty("rc-static-fill", "fill-opacity", 0.3);
      } else {
        // Highlight selected, dim others
        map.setPaintProperty("rc-static-fill", "fill-opacity", [
          "case",
          ["in", ["get", "REGIONALCENTER"], ["literal", selectedNames]],
          0.5, // Selected
          0.1  // Not selected
        ]);
      }
    } catch (error) {
      console.error("Error updating RC highlighting:", error);
    }
  };

  return {
    layerState,
    removeServiceAreas,
    removeLAZipCodes,
    removeLARegionalCenters,
    addServiceAreas,
    addLARegionalCenters,
    updateRegionalCenterHighlighting
  };
}
