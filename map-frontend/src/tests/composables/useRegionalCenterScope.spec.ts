import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createApp, nextTick } from 'vue';
import { flushPromises } from '@vue/test-utils';
import { readFileSync } from 'node:fs';
import { useRegionalCenterScope, reconcileRegionalCenter, regionalCenterKey } from '@/composables/useRegionalCenterScope';
import { LA_REGIONAL_CENTERS } from '@/constants/regionalCenters';

const ca = JSON.parse(readFileSync('public/assets/geo/ca_regional_centers.geojson', 'utf8'));
const la = JSON.parse(readFileSync('public/assets/geo/la_rc_7.geojson', 'utf8'));
const cleanups: Array<() => void> = [];
function harness() {
  let scope!: ReturnType<typeof useRegionalCenterScope>;
  const app = createApp({ setup() { scope = useRegionalCenterScope(); return () => null; } });
  app.mount(document.createElement('div'));
  cleanups.push(() => app.unmount());
  return { scope, unmount: () => app.unmount() };
}
function fakeMap() {
  const sources = new Map<string, any>();
  const layers = new Map<string, any>();
  const events = new Map<string, Set<() => void>>();
  const map = {
    ready: true,
    isStyleLoaded: () => map.ready,
    on: vi.fn((event: string, callback: () => void) => {
      if (!events.has(event)) events.set(event, new Set());
      events.get(event)!.add(callback);
    }),
    off: vi.fn((event: string, callback: () => void) => { events.get(event)?.delete(callback); }),
    once: vi.fn((event: string, callback: () => void) => { map.on(event, callback); }),
    fire(event: string) {
      const callbacks = [...(events.get(event) || [])];
      if (event === 'idle') events.delete(event);
      callbacks.forEach(callback => callback());
    },
    getSource: (id: string) => sources.get(id),
    addSource: vi.fn((id: string, source: any) => {
      const stored = { data: source.data, setData(data: any) { stored.data = data; map.ready = false; } };
      sources.set(id, stored);
      map.ready = false;
    }),
    getLayer: (id: string) => layers.get(id),
    addLayer: vi.fn((layer: any) => { layers.set(layer.id, layer); }),
    setFilter: vi.fn((id: string, filter: any) => { layers.get(id).filter = filter; }),
    setLayoutProperty: vi.fn(),
    setPaintProperty: vi.fn((id: string, key: string, value: any) => { layers.get(id).paint[key] = value; }),
    fitBounds: vi.fn(),
    renderedNames() {
      const allowed = layers.get('rc-static-fill')?.filter?.[2]?.[1] || [];
      return (sources.get('rc')?.data.features || []).map((f: any) => f.properties.REGIONALCENTER)
        .filter((name: string) => allowed.includes(name));
    },
    reload() { sources.clear(); layers.clear(); map.ready = true; map.fire('style.load'); },
  };
  return map;
}
beforeEach(() => {
  vi.mocked(fetch).mockImplementation(async url => ({ ok: true,
    json: async () => structuredClone(String(url).includes('ca_regional') ? ca : la),
  }) as Response);
});
afterEach(() => { cleanups.splice(0).forEach(cleanup => cleanup()); vi.clearAllMocks(); });
async function loaded() {
  const { scope } = harness();
  const map = fakeMap();
  await scope.loadRegionalCenterScope(map as any);
  await nextTick();
  return { scope, map };
}

describe('existing map regional center scope', () => {
  it('defaults to seven LA cards, polygons and existing colors', async () => {
    const { scope, map } = await loaded();
    expect(scope.regionalCenterScope.value).toBe('los-angeles');
    expect(scope.scopeCenters.value).toHaveLength(7);
    expect(map.renderedNames()).toHaveLength(7);
    for (const center of scope.scopeCenters.value) expect(center.color).toBe(LA_REGIONAL_CENTERS[center.name].color);
  });
  it('retries pending source data on idle, renders all 21, and preserves the fit', async () => {
    const { scope, map } = await loaded();
    expect(map.ready).toBe(false);
    map.fitBounds.mockClear();
    scope.regionalCenterScope.value = 'california';
    await flushPromises();
    expect(map.fitBounds).not.toHaveBeenCalled();
    map.ready = true;
    map.fire('idle');
    await flushPromises();
    expect(scope.scopeCenters.value).toHaveLength(21);
    expect(map.renderedNames()).toHaveLength(21);
    expect(map.fitBounds).toHaveBeenCalledTimes(1);
    expect(map.fitBounds.mock.calls[0][0].getNorth()).toBeGreaterThan(41);
    expect(map.fitBounds.mock.calls[0][0].getSouth()).toBeLessThan(33);
  });
  it('updates filters while source is pending and normalizes acronym punctuation', async () => {
    const { scope, map } = await loaded();
    scope.regionalCenterQuery.value = 'SGPRC';
    await nextTick();
    expect(scope.visibleScopeCenters.value.map(c => c.name)).toEqual(['San Gabriel/Pomona Regional Center']);
    expect(map.renderedNames()).toEqual(['San Gabriel/Pomona Regional Center']);
    scope.regionalCenterQuery.value = 'no matching center';
    await nextTick();
    expect(map.renderedNames()).toHaveLength(0);
    scope.regionalCenterQuery.value = '';
    await nextTick();
    expect(map.renderedNames()).toHaveLength(7);
  });
  it('restores statewide geometry, search and user highlight after style reload', async () => {
    const { scope, map } = await loaded();
    scope.regionalCenterScope.value = 'california';
    await nextTick();
    scope.regionalCenterQuery.value = 'Fresno';
    scope.highlightedRegionalCenter.value = 'Central Valley Regional Center';
    await flushPromises();
    map.reload();
    await flushPromises();
    expect(map.renderedNames()).toEqual(['Central Valley Regional Center']);
    expect(map.getLayer('rc-static-fill').paint['fill-opacity']).toEqual([
      'case', ['==', ['get', 'REGIONALCENTER'], 'Central Valley Regional Center'], 0.25, 0.15,
    ]);
    expect(map.fitBounds).toHaveBeenCalledTimes(2);
  });
  it('returns to LA geometry and bounds without stale search after rapid toggles', async () => {
    const { scope, map } = await loaded();
    scope.regionalCenterScope.value = 'california';
    await flushPromises();
    map.ready = true;
    map.fire('idle');
    await flushPromises();
    scope.regionalCenterQuery.value = 'Fresno';
    scope.regionalCenterScope.value = 'los-angeles';
    await flushPromises();
    map.ready = true;
    map.fire('idle');
    await flushPromises();
    expect(scope.regionalCenterQuery.value).toBe('');
    expect(map.renderedNames()).toHaveLength(7);
    expect(map.getSource('rc').data.features).toHaveLength(7);
    expect(map.fitBounds.mock.lastCall![0].getNorth()).toBeLessThan(36);
    expect(map.addSource).toHaveBeenCalledTimes(1);
  });
  it('resets pending statewide rendering to LA without overriding the provider camera', async () => {
    const { scope, map } = await loaded();
    scope.regionalCenterScope.value = 'california';
    await flushPromises();
    map.fitBounds.mockClear();
    scope.resetRegionalCenterScopeWithoutFit();
    await flushPromises();
    map.ready = true;
    map.fire('idle');
    await flushPromises();
    expect(map.renderedNames()).toHaveLength(7);
    expect(map.getSource('rc').data.features).toHaveLength(7);
    expect(map.fitBounds).not.toHaveBeenCalled();
    map.reload();
    await flushPromises();
    expect(map.fitBounds).not.toHaveBeenCalled();
    scope.regionalCenterScope.value = 'california';
    await flushPromises();
    map.ready = true;
    map.fire('idle');
    await flushPromises();
    expect(map.renderedNames()).toHaveLength(21);
    expect(map.fitBounds).toHaveBeenCalledTimes(1);
  });
  it('cancels a pending initial fit and same-tick scope toggle when provider mode takes over', async () => {
    let resolve!: () => void;
    const gate = new Promise<void>(done => { resolve = done; });
    vi.mocked(fetch).mockImplementation(async url => {
      await gate;
      return { ok: true, json: async () => String(url).includes('ca_regional') ? ca : la } as Response;
    });
    const { scope } = harness();
    const map = fakeMap();
    const initial = scope.loadRegionalCenterScope(map as any);
    scope.regionalCenterScope.value = 'california';
    scope.resetRegionalCenterScopeWithoutFit();
    scope.resetRegionalCenterScopeWithoutFit();
    resolve();
    await initial;
    await flushPromises();
    expect(map.renderedNames()).toHaveLength(7);
    expect(map.fitBounds).not.toHaveBeenCalled();
  });
  it('uses latest scope when geometry completes late', async () => {
    let resolve!: () => void;
    const gate = new Promise<void>(done => { resolve = done; });
    vi.mocked(fetch).mockImplementation(async url => {
      await gate;
      return { ok: true, json: async () => String(url).includes('ca_regional') ? ca : la } as Response;
    });
    const { scope } = harness();
    const map = fakeMap();
    const initial = scope.loadRegionalCenterScope(map as any);
    scope.regionalCenterScope.value = 'california';
    await nextTick();
    scope.regionalCenterScope.value = 'los-angeles';
    await nextTick();
    resolve();
    await initial;
    await flushPromises();
    expect(map.renderedNames()).toHaveLength(7);
    expect(fetch).toHaveBeenCalledTimes(2);
  });
  it.each(['counties', 'officeLat', 'name'])('rejects invalid %s metadata and allows retry', async field => {
    const malformed = structuredClone(ca);
    delete malformed.features[0].properties[field];
    vi.mocked(fetch).mockImplementationOnce(async () => ({ ok: true, json: async () => malformed }) as Response);
    const { scope, map } = await loaded();
    expect(scope.scopeError.value).toBe(true);
    expect(scope.scopeCenters.value).toEqual([]);
    expect(map.addSource).not.toHaveBeenCalled();
    await scope.loadRegionalCenterScope(map as any);
    expect(scope.scopeError.value).toBe(false);
    expect(map.renderedNames()).toHaveLength(7);
  });
  it('ignores late requests after unmount', async () => {
    let resolve!: () => void;
    const gate = new Promise<void>(done => { resolve = done; });
    vi.mocked(fetch).mockImplementation(async url => {
      await gate;
      return { ok: true, json: async () => String(url).includes('ca_regional') ? ca : la } as Response;
    });
    const { scope, unmount } = harness();
    const map = fakeMap();
    const loading = scope.loadRegionalCenterScope(map as any);
    unmount();
    resolve();
    await loading;
    expect(map.addSource).not.toHaveBeenCalled();
    expect(map.off).toHaveBeenCalledWith('style.load', expect.any(Function));
  });
  it('reconciles API IDs without duplicate suites or mismatched office coordinates', async () => {
    const { scope } = await loaded();
    const center = scope.scopeCenters.value.find(c => c.name.includes('North Los'))!;
    const merged = reconcileRegionalCenter(center, [{ id: '11', name: 'North LA County Regional Center (Main)',
      zip_codes: ['91406'], suite: '170', latitude: 1, longitude: 2 }]);
    expect(merged.id).toBe('11');
    expect(merged.zip_codes).toEqual(['91406']);
    expect(merged.suite).toBeUndefined();
    expect(merged.latitude).toBe(center.latitude);
    expect(regionalCenterKey('San Gabriel / Pomona Regional Center')).toBe(regionalCenterKey('San Gabriel/Pomona Regional Center'));
  });
});
