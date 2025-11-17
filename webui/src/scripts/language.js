/*
 * Copyright (C) 2024-2024 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Import core translations statically
import enTranslations from '../locales/strings/en.json';
import languages from '../locales/languages.json';

// Cache for translations
const cachedEnglishTranslations = enTranslations;
let currentTranslations = null;

// Dynamic imports for non-English translations
const translationModules = import.meta.glob(
  '../locales/strings/!(en).json',
  { eager: false }
);

// --- File helper functions ---
async function readFile(path) {
  try {
    const { stdout, errno } = await window.executeCommand(`cat ${path}`);
    if (errno === 0 && stdout.trim()) return stdout.trim();
  } catch (e) {
    console.warn('Failed to read file:', e);
  }
  return null;
}

async function writeFile(path, data) {
  try {
    const dir = path.substring(0, path.lastIndexOf('/'));
    await window.executeCommand(`mkdir -p ${dir}`);
    await window.executeCommand(`echo '${data}' > ${path}`);
  } catch (e) {
    console.error('Failed to write file:', e);
  }
}

// --- Synchronous translation lookup ---
function getTranslationSync(key, ...args) {
  if (!currentTranslations) {
    console.error('Translations not loaded!');
    return key;
  }

  const keys = key.split('.');
  let value = currentTranslations;

  // Try current language first
  for (const k of keys) {
    value = value?.[k];
    if (!value) break;
  }

  // Fallback to English
  if (!value) {
    value = cachedEnglishTranslations;
    for (const k of keys) {
      value = value?.[k];
      if (!value) break;
    }
  }

  if (!value) return key;

  // Handle placeholders like {0}, {1}, ...
  if (args.length > 0 && typeof value === 'string') {
    return value.replace(/\{(\d+)\}/g, (match, index) => {
      const idx = parseInt(index);
      return args[idx] !== undefined ? args[idx] : match;
    });
  }

  return value;
}

// Expose globally
window.getTranslation = getTranslationSync;

// --- Load translation file dynamically ---
async function loadTranslations(lang) {
  if (lang === 'en') return cachedEnglishTranslations;

  const filePath = `../locales/strings/${lang}.json`;

  if (translationModules[filePath]) {
    try {
      const module = await translationModules[filePath]();
      return module.default;
    } catch (error) {
      console.error(`Failed to load ${lang} translations:`, error);
      return cachedEnglishTranslations;
    }
  } else {
    console.warn(`No translation file for ${lang}, falling back to English`);
    return cachedEnglishTranslations;
  }
}

// --- Apply translations to elements ---
function applyTranslations(translations) {
  document.querySelectorAll('[data-lang]').forEach(el => {
    const keys = el.getAttribute('data-lang').split('.');
    let value = translations;

    for (const key of keys) {
      value = value?.[key];
      if (value === undefined) break;
    }

    if (value !== undefined) {
      el.textContent = value;
    }
  });
}

// --- Initialize language system ---
async function setupLang() {
  const selector = document.getElementById('languageSelector');
  if (!selector) return;

  const LANG_FILE = '/data/adb/.config/AZenith/API/current_language';
  const allLanguages = { en: "English", ...languages };

  try {
    // Populate selector
    selector.innerHTML = '';
    for (const [code, name] of Object.entries(allLanguages)) {
      const option = document.createElement('option');
      option.value = code;
      option.textContent = name;
      selector.appendChild(option);
    }

    // Read saved language from file
    let lang = await readFile(LANG_FILE);
    if (!lang) {
      // Detect browser/system language
      const browserLangs = [
        ...(navigator.languages || []),
        navigator.language,
        navigator.userLanguage
      ].filter(Boolean);

      lang = 'en';
      for (const browserLang of browserLangs) {
        const normalized = browserLang.toLowerCase().replace(/_/g, '-');
        if (allLanguages[normalized]) { lang = normalized; break; }
        const baseLang = normalized.split('-')[0];
        if (allLanguages[baseLang]) { lang = baseLang; break; }
      }

      // Save detected language
      await writeFile(LANG_FILE, lang);
    }

    selector.value = lang;
    currentTranslations = await loadTranslations(lang);
    applyTranslations(currentTranslations);

    // Handle language change
    selector.addEventListener('change', async (e) => {
      const newLang = e.target.value;
      if (newLang === lang) return;

      try {
        await writeFile(LANG_FILE, newLang);
        setTimeout(() => {
          location.reload();
        }, 300);
      } catch (error) {
        console.error('Failed to switch language:', error);
        selector.value = lang;
      }
    });

  } catch (error) {
    console.error('i18n initialization failed:', error);
  }
}

// --- Initialize immediately if DOM ready ---
if (document.readyState !== 'loading') {
  setupLang();
} else {
  document.addEventListener('DOMContentLoaded', setupLang);
}