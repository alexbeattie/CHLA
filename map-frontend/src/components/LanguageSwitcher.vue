<template>
  <div class="language-switcher" :class="{ open: isOpen }">
    <button
      class="language-btn"
      @click="toggleDropdown"
      :aria-label="$t('accessibility.selectLanguage')"
      :aria-expanded="isOpen"
    >
      <span class="current-lang">
        <span class="lang-flag">{{ currentLocale.flag }}</span>
        <span class="lang-code">{{ currentLocale.code.toUpperCase() }}</span>
      </span>
      <i class="bi bi-chevron-down chevron" :class="{ rotated: isOpen }"></i>
    </button>
    
    <div v-if="isOpen" class="language-dropdown">
      <button
        v-for="locale in availableLocales"
        :key="locale.code"
        class="language-option"
        :class="{ active: locale.code === currentLocale.code }"
        @click="selectLanguage(locale.code)"
      >
        <span class="lang-flag">{{ locale.flag }}</span>
        <span class="lang-name">{{ locale.name }}</span>
        <i v-if="locale.code === currentLocale.code" class="bi bi-check2"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale, availableLocales } from '@/i18n'

const { locale } = useI18n()
const isOpen = ref(false)

const currentLocale = computed(() => {
  return availableLocales.find(l => l.code === locale.value) || availableLocales[0]
})

function toggleDropdown() {
  isOpen.value = !isOpen.value
}

function selectLanguage(code) {
  setLocale(code)
  isOpen.value = false
}

// Close dropdown when clicking outside
function handleClickOutside(event) {
  const switcher = document.querySelector('.language-switcher')
  if (switcher && !switcher.contains(event.target)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.language-switcher {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.language-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: inherit;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.language-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
}

.current-lang {
  display: flex;
  align-items: center;
  gap: 4px;
}

.lang-flag {
  font-size: 16px;
  line-height: 1;
}

.lang-code {
  font-weight: 500;
  font-size: 13px;
}

.chevron {
  font-size: 10px;
  transition: transform 0.2s ease;
}

.chevron.rotated {
  transform: rotate(180deg);
}

.language-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  min-width: 140px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 1000;
  animation: fadeIn 0.15s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.language-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 14px;
  background: none;
  border: none;
  color: #333;
  cursor: pointer;
  text-align: left;
  font-size: 14px;
  transition: background 0.15s ease;
}

.language-option:hover {
  background: #f5f5f5;
}

.language-option.active {
  background: #e8f4ff;
  color: #0066cc;
}

.lang-name {
  flex: 1;
}

.language-option .bi-check2 {
  color: #0066cc;
  font-size: 16px;
}

/* Dark theme variant (for light backgrounds) */
.language-switcher.dark .language-btn {
  border-color: rgba(0, 0, 0, 0.2);
  color: #333;
}

.language-switcher.dark .language-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 0, 0, 0.3);
}
</style>
