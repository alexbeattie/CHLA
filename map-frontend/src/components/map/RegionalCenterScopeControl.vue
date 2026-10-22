<template>
  <fieldset class="regional-center-scope">
    <legend>{{ $t('regionalCenterScope.label') }}</legend>
    <div class="btn-group w-100" role="group" :aria-label="$t('regionalCenterScope.label')">
      <button v-for="scope in scopes" :key="scope" type="button" class="btn"
        :class="modelValue === scope ? 'btn-chla-primary' : 'btn-chla-outline'"
        :aria-pressed="modelValue === scope" @click="$emit('update:modelValue', scope)">
        {{ $t(`regionalCenterScope.${scope}`) }}
      </button>
    </div>
    <p class="coverage-note">{{ $t('regionalCenterScope.coverageNote') }}</p>
    <label for="regional-center-search">{{ $t('regionalCenterScope.searchLabel') }}</label>
    <input id="regional-center-search" type="search" class="form-control" :value="query"
      :placeholder="$t('regionalCenterScope.searchPlaceholder')"
      @input="$emit('update:query', ($event.target as HTMLInputElement).value)" />
    <p v-if="loading" role="status">{{ $t('regionalCenterScope.loading') }}</p>
    <p v-if="error" role="alert">
      {{ $t('regionalCenterScope.loadError') }}
      <button type="button" class="btn btn-link" @click="$emit('retry')">{{ $t('regionalCenterScope.retry') }}</button>
    </p>
  </fieldset>
</template>

<script setup lang="ts">
import type { RegionalCenterScope } from '@/composables/useRegionalCenterScope';
defineProps<{ modelValue: RegionalCenterScope; query: string; loading: boolean; error: boolean }>();
defineEmits<{ 'update:modelValue': [scope: RegionalCenterScope]; 'update:query': [query: string]; retry: [] }>();
const scopes: RegionalCenterScope[] = ['los-angeles', 'california'];
</script>

<style scoped>
.regional-center-scope { border: 0; padding: 0 12px; margin: 0 0 1rem; min-width: 0; }
legend { font-size: 0.875rem; font-weight: 600; margin-bottom: 0.4rem; }
.coverage-note { font-size: 0.8rem; color: #475569; margin: 0.5rem 0; }
label { font-size: 0.875rem; margin-bottom: 0.3rem; }
.btn-group > button:first-child { border-radius: 6px 0 0 6px; }
.btn-group > button:last-child { border-radius: 0 6px 6px 0; }
button:focus-visible, input:focus-visible { outline: 3px solid #2563eb; outline-offset: 2px; }
</style>
