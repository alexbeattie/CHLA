import { onBeforeUnmount, ref, shallowRef, type Ref } from 'vue';
import mapboxgl from 'mapbox-gl';
import type { Feature, FeatureCollection, MultiPolygon, Polygon, Position } from 'geojson';

export interface CaRegionalCenter {
  name: string;
  acronym: string;
  color: string;
  slug: string | null;
  counties: string[];
  officeType: 'Main' | 'Field';
  address: string;
  city: string;
  zip: string;
  phone: string;
  website: string;
  officeLng: number;
  officeLat: number;
}

type CenterFeature = Feature<Polygon | MultiPolygon, CaRegionalCenter>;

const GEOJSON_URL = '/assets/geo/ca_regional_centers.geojson';
const CALIFORNIA_BOUNDS: [[number, number], [number, number]] = [
  [-124.5, 32.5],
  [-114.1, 42.05],
];

function featureBounds(feature: CenterFeature): mapboxgl.LngLatBounds {
  const bounds = new mapboxgl.LngLatBounds();
  const polygons: Position[][][] =
    feature.geometry.type === 'Polygon'
      ? [feature.geometry.coordinates]
      : feature.geometry.coordinates;
  for (const polygon of polygons) {
    for (const [lng, lat] of polygon[0]) bounds.extend([lng, lat]);
  }
  return bounds;
}

export function useCaliforniaRegionalCentersMap(container: Ref<HTMLElement | null>) {
  const centers = ref<CaRegionalCenter[]>([]);
  const selected = ref<CaRegionalCenter | null>(null);
  const loadError = ref(false);
  const map = shallowRef<mapboxgl.Map | null>(null);
  let features: CenterFeature[] = [];
  let hoveredId: number | null = null;
  let selectedId: number | null = null;

  function setState(id: number | null, key: 'hover' | 'selected', value: boolean) {
    if (id !== null) map.value?.setFeatureState({ source: 'centers', id }, { [key]: value });
  }

  function select(acronym: string | null, { fly = true } = {}) {
    const index = features.findIndex((f) => f.properties.acronym === acronym);
    setState(selectedId, 'selected', false);
    selectedId = index >= 0 ? index : null;
    setState(selectedId, 'selected', true);
    selected.value = index >= 0 ? features[index].properties : null;
    if (!map.value) return;
    if (index >= 0 && fly) {
      map.value.fitBounds(featureBounds(features[index]), { padding: 48, maxZoom: 9, duration: 700 });
    } else if (index < 0) {
      map.value.fitBounds(CALIFORNIA_BOUNDS, { padding: 24, duration: 700 });
    }
  }

  async function init(token: string) {
    let data: FeatureCollection<Polygon | MultiPolygon, CaRegionalCenter>;
    try {
      const response = await fetch(GEOJSON_URL);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      data = await response.json();
    } catch {
      loadError.value = true;
      return;
    }
    features = data.features.map((f, id) => ({ ...f, id }));
    centers.value = features
      .map((f) => f.properties)
      .sort((a, b) => a.name.localeCompare(b.name));

    if (!container.value || !token) {
      loadError.value = !token;
      return;
    }
    mapboxgl.accessToken = token;
    const instance = new mapboxgl.Map({
      container: container.value,
      style: 'mapbox://styles/mapbox/light-v11',
      bounds: CALIFORNIA_BOUNDS,
      fitBoundsOptions: { padding: 24 },
      cooperativeGestures: true,
    });
    map.value = instance;
    instance.addControl(new mapboxgl.NavigationControl({ showCompass: false }), 'top-right');

    instance.on('load', () => {
      instance.addSource('centers', { type: 'geojson', data: { type: 'FeatureCollection', features } });
      instance.addSource('offices', {
        type: 'geojson',
        data: {
          type: 'FeatureCollection',
          features: features.map((f) => ({
            type: 'Feature',
            properties: { acronym: f.properties.acronym, color: f.properties.color },
            geometry: { type: 'Point', coordinates: [f.properties.officeLng, f.properties.officeLat] },
          })),
        },
      });
      instance.addLayer({
        id: 'center-fill',
        type: 'fill',
        source: 'centers',
        paint: {
          'fill-color': ['get', 'color'],
          'fill-opacity': [
            'case',
            ['boolean', ['feature-state', 'selected'], false], 0.7,
            ['boolean', ['feature-state', 'hover'], false], 0.6,
            0.4,
          ],
        },
      });
      instance.addLayer({
        id: 'center-outline',
        type: 'line',
        source: 'centers',
        paint: {
          'line-color': ['case', ['boolean', ['feature-state', 'selected'], false], '#1f2937', '#ffffff'],
          'line-width': ['case', ['boolean', ['feature-state', 'selected'], false], 2.5, 1],
        },
      });
      instance.addLayer({
        id: 'office-dot',
        type: 'circle',
        source: 'offices',
        paint: {
          'circle-radius': 5,
          'circle-color': ['get', 'color'],
          'circle-stroke-color': '#1f2937',
          'circle-stroke-width': 1.5,
        },
      });
      instance.addLayer({
        id: 'office-label',
        type: 'symbol',
        source: 'offices',
        minzoom: 6,
        layout: {
          'text-field': ['get', 'acronym'],
          'text-size': 11,
          'text-offset': [0, 1.1],
          'text-anchor': 'top',
          'text-font': ['DIN Pro Medium', 'Arial Unicode MS Regular'],
        },
        paint: { 'text-color': '#1f2937', 'text-halo-color': '#ffffff', 'text-halo-width': 1.5 },
      });

      instance.on('mousemove', 'center-fill', (e) => {
        const id = (e.features?.[0]?.id as number | undefined) ?? null;
        if (id === hoveredId) return;
        setState(hoveredId, 'hover', false);
        hoveredId = id;
        setState(hoveredId, 'hover', true);
        instance.getCanvas().style.cursor = 'pointer';
      });
      instance.on('mouseleave', 'center-fill', () => {
        setState(hoveredId, 'hover', false);
        hoveredId = null;
        instance.getCanvas().style.cursor = '';
      });
      instance.on('click', 'center-fill', (e) => {
        const acronym = e.features?.[0]?.properties?.acronym as string | undefined;
        if (acronym) select(acronym, { fly: false });
      });
    });
  }

  onBeforeUnmount(() => {
    map.value?.remove();
    map.value = null;
  });

  return { centers, selected, loadError, init, select };
}
