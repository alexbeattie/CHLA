import { defineStore } from "pinia";
import { computed, ref } from "vue";
import axios from "axios";

export interface MapLocation {
  id: number;
  name: string;
  address: string;
  city: string;
  state: string;
  zip_code: string;
  latitude: number | null;
  longitude: number | null;
  description: string | null;
  phone: string | null;
  website: string | null;
  email: string | null;
  is_accessible: boolean;
  category: number;
  category_name: string;
  distance?: number;
}

interface LocationCategory {
  id: number;
  name: string;
}

const INCLUSIVE_PLAYGROUNDS_CATEGORY = "Inclusive Playgrounds";

export const useLocationStore = defineStore("location", () => {
  const locations = ref<MapLocation[]>([]);
  const selectedLocation = ref<MapLocation | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const showInclusivePlaygrounds = ref(false);
  const apiBaseUrl = ref(
    import.meta.env.VITE_API_BASE_URL ||
      import.meta.env.VITE_API_URL ||
      "http://localhost:8000",
  );

  const locationsWithCoordinates = computed(() =>
    locations.value.filter(
      (location) =>
        location.latitude !== null &&
        location.longitude !== null &&
        !Number.isNaN(Number(location.latitude)) &&
        !Number.isNaN(Number(location.longitude)),
    ),
  );

  const selectedLocationId = computed(() => selectedLocation.value?.id || null);

  async function loadInclusivePlaygrounds(): Promise<MapLocation[]> {
    loading.value = true;
    error.value = null;

    try {
      const category = await findInclusivePlaygroundCategory();
      if (!category) {
        locations.value = [];
        return locations.value;
      }

      const response = await axios.get(`${apiBaseUrl.value}/api/locations/`, {
        params: { category: category.id, ordering: "name" },
      });
      locations.value = normalizeLocationResponse(response.data);
      return locations.value;
    } catch (err: any) {
      error.value =
        err.response?.data?.message || err.message || "Failed to load locations";
      locations.value = [];
      return locations.value;
    } finally {
      loading.value = false;
    }
  }

  function selectLocation(locationId: number) {
    selectedLocation.value =
      locations.value.find((location) => location.id === locationId) || null;
  }

  function clearSelection() {
    selectedLocation.value = null;
  }

  function setApiBaseUrl(url: string) {
    apiBaseUrl.value = url;
  }

  function setShowInclusivePlaygrounds(value: boolean) {
    showInclusivePlaygrounds.value = value;
  }

  async function findInclusivePlaygroundCategory(): Promise<LocationCategory | null> {
    const response = await axios.get(`${apiBaseUrl.value}/api/categories/`);
    const categories = normalizeCategoryResponse(response.data);
    return (
      categories.find(
        (category) => category.name === INCLUSIVE_PLAYGROUNDS_CATEGORY,
      ) || null
    );
  }

  function normalizeLocationResponse(data: any): MapLocation[] {
    if (Array.isArray(data)) {
      return data;
    }
    return data?.results || [];
  }

  function normalizeCategoryResponse(data: any): LocationCategory[] {
    if (Array.isArray(data)) {
      return data;
    }
    return data?.results || [];
  }

  return {
    locations,
    selectedLocation,
    loading,
    error,
    showInclusivePlaygrounds,
    locationsWithCoordinates,
    selectedLocationId,
    loadInclusivePlaygrounds,
    selectLocation,
    clearSelection,
    setApiBaseUrl,
    setShowInclusivePlaygrounds,
  };
});
