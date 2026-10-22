import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue';
import mapboxgl from 'mapbox-gl';
import type { FeatureCollection, MultiPolygon, Polygon } from 'geojson';
import { LA_REGIONAL_CENTERS } from '@/constants/regionalCenters';
import type { RegionalCenter } from './useRegionalCenterData';

export type RegionalCenterScope = 'los-angeles' | 'california';
export interface CenterProperties {
  name: string;
  acronym: string;
  color: string;
  counties: string[];
  officeType: string;
  address: string;
  city: string;
  zip: string;
  phone: string;
  website: string;
  officeLng: number;
  officeLat: number;
}
type CenterGeometry = FeatureCollection<Polygon | MultiPolygon, CenterProperties & { REGIONALCENTER?: string }>;
export type ScopedCenter = RegionalCenter & { abbreviation: string; color: string; counties: string[] };

// Punctuation and office suffixes differ between the API and geometry catalog.
export function regionalCenterKey(name: string): string {
  return name.toLowerCase().replace(/\bla\b/g, 'los angeles').replace(/\([^)]*\)/g, '').replace(/[^a-z0-9]/g, '');
}

export function reconcileRegionalCenter(center: ScopedCenter, apiCenters: RegionalCenter[]): ScopedCenter {
  const key = regionalCenterKey(center.name);
  const matches = apiCenters.filter(candidate =>
    regionalCenterKey(candidate.regional_center || candidate.name) === key
  );
  const api = matches.find(candidate => candidate.office_type === 'Main') || matches[0];
  if (!api) return center;
  return { ...api, ...center, suite: undefined, id: api.id, center_id: api.center_id, zip_codes: api.zip_codes };
}

export function useRegionalCenterScope() {
  const regionalCenterScope = ref<RegionalCenterScope>('los-angeles');
  const regionalCenterQuery = ref('');
  const highlightedRegionalCenter = ref<string | null>(null);
  const scopeLoading = ref(false);
  const scopeError = ref(false);
  const catalog = shallowRef<CenterGeometry | null>(null);
  const laGeometry = shallowRef<CenterGeometry | null>(null);
  let pending: Promise<void> | null = null;
  let map: mapboxgl.Map | null = null;
  let disposed = false;
  let revision = 0;
  let pendingFit = false;
  let resettingScopeWithoutFit = false;
  const laNames = new Set(Object.keys(LA_REGIONAL_CENTERS).map(regionalCenterKey));

  const scopeCenters = computed<ScopedCenter[]>(() => (catalog.value?.features || [])
    .map(({ properties: p }) => ({
      id: p.acronym,
      name: p.name,
      regional_center: p.name,
      abbreviation: p.acronym,
      color: p.color,
      counties: p.counties,
      county_served: p.counties.join(', '),
      office_type: p.officeType,
      address: p.address,
      city: p.city,
      state: 'CA',
      zip_code: p.zip,
      phone: p.phone,
      website: p.website,
      latitude: p.officeLat,
      longitude: p.officeLng,
      coordinates: { lat: p.officeLat, lng: p.officeLng },
    }))
    .filter(center => regionalCenterScope.value === 'california' || laNames.has(regionalCenterKey(center.name))));

  const visibleScopeCenters = computed(() => {
    const query = regionalCenterQuery.value.trim().toLowerCase();
    const normalizedQuery = regionalCenterKey(query);
    return scopeCenters.value.filter(center =>
      [center.name, center.abbreviation, ...center.counties].some(value => value.toLowerCase().includes(query)) ||
      (!!normalizedQuery && regionalCenterKey(center.abbreviation).includes(normalizedQuery))
    );
  });

  const scopeGeometry = computed<CenterGeometry>(() => {
    const source = regionalCenterScope.value === 'california' ? catalog.value : laGeometry.value;
    const byName = new Map(scopeCenters.value.map(center => [regionalCenterKey(center.name), center]));
    return {
      type: 'FeatureCollection',
      features: (source?.features || []).flatMap(feature => {
        const center = byName.get(regionalCenterKey(feature.properties.REGIONALCENTER || feature.properties.name));
        return center ? [{ ...feature, properties: { ...feature.properties, REGIONALCENTER: center.name, color: center.color } }] : [];
      }),
    };
  });

  async function load() {
    if (catalog.value && laGeometry.value) return;
    if (!pending) {
      scopeLoading.value = true;
      scopeError.value = false;
      pending = Promise.all(['/assets/geo/ca_regional_centers.geojson', '/assets/geo/la_rc_7.geojson'].map(async url => {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`Regional center geometry: ${response.status}`);
        const data = await response.json();
        if (data.type !== 'FeatureCollection' || !Array.isArray(data.features) || !data.features.length ||
          !data.features.every((feature: any) => {
            const p = feature.properties;
            const geometry = feature.geometry;
            if (!p || !geometry || !['Polygon', 'MultiPolygon'].includes(geometry.type) ||
              !Array.isArray(geometry.coordinates) || !geometry.coordinates.length) return false;
            if (url.endsWith('la_rc_7.geojson')) return typeof p.REGIONALCENTER === 'string' && !!p.REGIONALCENTER;
            return ['name', 'acronym', 'color', 'officeType', 'address', 'city', 'zip', 'phone', 'website']
              .every(key => typeof p[key] === 'string') && !!p.name && !!p.acronym &&
              Array.isArray(p.counties) && p.counties.every((county: unknown) => typeof county === 'string') &&
              Number.isFinite(p.officeLat) && Number.isFinite(p.officeLng);
          })) throw new Error('Invalid regional center geometry');
        return data as CenterGeometry;
      })).then(([ca, la]) => {
        if (!disposed) { catalog.value = ca; laGeometry.value = la; }
      }).catch(() => { if (!disposed) scopeError.value = true; })
        .finally(() => { scopeLoading.value = false; pending = null; });
    }
    await pending;
  }

  function filterOverlay() {
    if (!map) return;
    const filter: mapboxgl.FilterSpecification = ['in', ['get', 'REGIONALCENTER'], ['literal', visibleScopeCenters.value.map(center => center.name)]];
    for (const id of ['rc-static-fill', 'rc-static-outline']) {
      if (map.getLayer(id)) map.setFilter(id, filter);
    }
  }

  function fitRegionalCenterScope() {
    if (!map || !scopeGeometry.value.features.length) return;
    const bounds = new mapboxgl.LngLatBounds();
    for (const { geometry } of scopeGeometry.value.features) {
      const polygons = geometry.type === 'Polygon' ? [geometry.coordinates] : geometry.coordinates;
      for (const polygon of polygons) {
        for (const ring of polygon) {
          for (const [longitude, latitude] of ring) bounds.extend([longitude, latitude]);
        }
      }
    }
    if (!bounds.isEmpty()) map.fitBounds(bounds, { padding: 40, maxZoom: 10, duration: 400 });
  }

  function updateHighlight() {
    if (!map?.getLayer('rc-static-fill')) return;
    const name = highlightedRegionalCenter.value;
    map.setPaintProperty('rc-static-fill', 'fill-opacity', name
      ? ['case', ['==', ['get', 'REGIONALCENTER'], name], 0.25, 0.15]
      : 0.15);
  }

  async function render(fit = false) {
    pendingFit ||= fit;
    const request = ++revision;
    await load();
    if (disposed || request !== revision || !map || scopeError.value) return;
    map.off('idle', restoreStyle);
    if (!map.isStyleLoaded()) {
      map.once('idle', restoreStyle);
      return;
    }
    const source = map.getSource('rc') as mapboxgl.GeoJSONSource | undefined;
    if (source) source.setData(scopeGeometry.value);
    else map.addSource('rc', { type: 'geojson', data: scopeGeometry.value });
    if (!map.getLayer('rc-static-fill')) map.addLayer({
      id: 'rc-static-fill', type: 'fill', source: 'rc',
      paint: { 'fill-color': ['get', 'color'], 'fill-opacity': 0.15, 'fill-outline-color': '#ffffff' },
    });
    if (!map.getLayer('rc-static-outline')) map.addLayer({
      id: 'rc-static-outline', type: 'line', source: 'rc',
      paint: { 'line-color': ['get', 'color'], 'line-width': 3, 'line-opacity': 0.9 },
    });
    for (const id of ['la-zip-codes-fill', 'la-zip-codes-outline', 'california-counties-fill', 'california-counties-outline']) {
      if (map.getLayer(id)) map.setLayoutProperty(id, 'visibility', 'none');
    }
    updateHighlight();
    filterOverlay();
    if (pendingFit) {
      pendingFit = false;
      fitRegionalCenterScope();
    }
  }

  const restoreStyle = () => { void render(); };
  function loadRegionalCenterScope(instance: mapboxgl.Map) {
    if (map !== instance) {
      map?.off('style.load', restoreStyle);
      map?.off('idle', restoreStyle);
      map = instance;
      map.on('style.load', restoreStyle);
    }
    return render(true);
  }

  function resetRegionalCenterScopeWithoutFit() {
    revision++;
    pendingFit = false;
    map?.off('idle', restoreStyle);
    resettingScopeWithoutFit = true;
    try {
      regionalCenterScope.value = 'los-angeles';
      regionalCenterQuery.value = '';
    } finally {
      resettingScopeWithoutFit = false;
    }
    if (map) void render();
  }

  watch(regionalCenterScope, () => {
    regionalCenterQuery.value = '';
    if (map && !resettingScopeWithoutFit) void render(true);
  }, { flush: 'sync' });
  watch(visibleScopeCenters, filterOverlay);
  watch(highlightedRegionalCenter, updateHighlight);
  onBeforeUnmount(() => {
    disposed = true;
    revision++;
    map?.off('style.load', restoreStyle);
    map?.off('idle', restoreStyle);
    map = null;
  });

  return { regionalCenterScope, regionalCenterQuery, highlightedRegionalCenter, scopeLoading, scopeError, scopeCenters, visibleScopeCenters,
    scopeGeometry, loadRegionalCenterScope, fitRegionalCenterScope, resetRegionalCenterScopeWithoutFit };
}
