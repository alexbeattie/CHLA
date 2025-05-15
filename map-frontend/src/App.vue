<template>
  <router-view />
</template>

<style>
/* Global animations */
@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.9;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.pulse-marker {
  animation: pulse 1.5s infinite;
}
</style>
