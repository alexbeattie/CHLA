<template>
  <div id="app">
    <AppNavBar v-if="showAppNavBar" />
    <main :class="{ 'with-app-navbar': showAppNavBar }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import AppNavBar from "@/components/AppNavBar.vue";

const route = useRoute();

// MapView ("/") renders its own integrated top bar with map-specific
// actions (sidebar toggle, profile, regional center chip). Suppress the
// shared nav there so the homepage keeps a single, unified header.
const showAppNavBar = computed(() => route.name !== "home");
</script>

<style>
:root {
  --kindd-font-family: "Nunito Sans", -apple-system, BlinkMacSystemFont,
    "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

#app {
  min-height: 100vh;
  margin: 0;
  padding: 0;
  font-family: var(--kindd-font-family);
}

main.with-app-navbar {
  padding-top: 60px;
}

body {
  margin: 0;
  padding: 0;
  font-family: var(--kindd-font-family);
}
</style>
