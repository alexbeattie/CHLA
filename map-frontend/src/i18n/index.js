import { createI18n } from 'vue-i18n'
import en from './locales/en.json'
import es from './locales/es.json'

// Get saved language from localStorage or detect from browser
function getDefaultLocale() {
  // Check localStorage first
  const saved = localStorage.getItem('kindd-locale')
  if (saved && ['en', 'es'].includes(saved)) {
    return saved
  }
  
  // Detect from browser
  const browserLang = navigator.language || navigator.userLanguage
  if (browserLang.startsWith('es')) {
    return 'es'
  }
  
  return 'en'
}

// Create i18n instance
const i18n = createI18n({
  legacy: false, // Use Composition API mode
  locale: getDefaultLocale(),
  fallbackLocale: 'en',
  messages: {
    en,
    es
  },
  // Number formatting for distances, etc.
  numberFormats: {
    en: {
      decimal: {
        style: 'decimal',
        minimumFractionDigits: 1,
        maximumFractionDigits: 1
      },
      distance: {
        style: 'decimal',
        minimumFractionDigits: 1,
        maximumFractionDigits: 1
      }
    },
    es: {
      decimal: {
        style: 'decimal',
        minimumFractionDigits: 1,
        maximumFractionDigits: 1
      },
      distance: {
        style: 'decimal',
        minimumFractionDigits: 1,
        maximumFractionDigits: 1
      }
    }
  }
})

// Helper to change language and persist
export function setLocale(locale) {
  i18n.global.locale.value = locale
  localStorage.setItem('kindd-locale', locale)
  document.documentElement.lang = locale
}

// Get current locale
export function getLocale() {
  return i18n.global.locale.value
}

// Available locales with display names
export const availableLocales = [
  { code: 'en', name: 'English', flag: '🇺🇸' },
  { code: 'es', name: 'Español', flag: '🇲🇽' }
]

export default i18n
