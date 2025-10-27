/**
 * Stores Index
 * Central export point for all Pinia stores
 * Week 3: Pinia Store Architecture
 */

export * from './providerStore';
export * from './mapStore';
export * from './filterStore';

// Re-export commonly used types
export type {
  Provider,
  SearchParams,
  RegionalCenterInfo,
  ProviderSearchResult
} from './providerStore';

export type {
  Coordinates,
  MapViewport,
  MapBounds,
  MapUIState
} from './mapStore';

export type {
  FilterOptions,
  UserData
} from './filterStore';
