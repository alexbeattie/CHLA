/**
 * useSeo
 *
 * Lightweight head management for the public KiNDD site.
 * Updates document title, meta description, canonical link, and Open Graph /
 * Twitter tags imperatively. The values are picked up by:
 *   - the browser at runtime
 *   - the prerender build step (Puppeteer captures the final DOM)
 *
 * This composable intentionally avoids pulling in a dedicated head library so
 * the SPA bundle stays small and the prerender step has nothing extra to
 * coordinate with.
 */

import { onMounted, onUnmounted, watchEffect } from "vue";
import type { WatchStopHandle } from "vue";
import { getSeoForPath } from "@/seo/siteConfig";

export interface SeoInput {
  /** Explicit canonical path; defaults to current location when omitted. */
  path?: string;
  /** Override the resolved title. */
  title?: string;
  /** Override the resolved description. */
  description?: string;
  /** Override the resolved canonical URL. */
  canonical?: string;
}

export type SeoInputSource = SeoInput | (() => SeoInput);

/**
 * Apply per-route SEO head tags. Accepts either a static object or a getter
 * so consumers can react to changing route params.
 */
export function useSeo(input?: SeoInputSource): void {
  const resolve = () => {
    const value: SeoInput = typeof input === "function" ? input() : input || {};
    const path =
      value.path ||
      (typeof window !== "undefined" ? window.location.pathname : "/");
    const fallback = getSeoForPath(path);
    return {
      title: value.title || fallback.title,
      description: value.description || fallback.description,
      canonical: value.canonical || fallback.canonical,
      ogImage: fallback.ogImage,
      twitterImage: fallback.twitterImage,
    };
  };

  const apply = () => {
    if (typeof document === "undefined") {
      return;
    }
    const seo = resolve();
    setTitle(seo.title);
    setMeta("name", "description", seo.description);
    setLink("canonical", seo.canonical);
    setMeta("property", "og:title", seo.title);
    setMeta("property", "og:description", seo.description);
    setMeta("property", "og:url", seo.canonical);
    setMeta("property", "og:image", seo.ogImage);
    setMeta("name", "twitter:title", seo.title);
    setMeta("name", "twitter:description", seo.description);
    setMeta("name", "twitter:url", seo.canonical);
    setMeta("name", "twitter:image", seo.twitterImage);
  };

  let stop: WatchStopHandle | undefined;
  onMounted(() => {
    stop = watchEffect(apply);
  });
  onUnmounted(() => {
    if (stop) {
      stop();
    }
  });
}

function setTitle(title: string) {
  if (title && document.title !== title) {
    document.title = title;
  }
}

function setMeta(attr: string, key: string, value: string) {
  if (!value) {
    return;
  }
  let element = document.head.querySelector(`meta[${attr}="${key}"]`);
  if (!element) {
    element = document.createElement("meta");
    element.setAttribute(attr, key);
    document.head.appendChild(element);
  }
  if (element.getAttribute("content") !== value) {
    element.setAttribute("content", value);
  }
}

function setLink(rel: string, href: string) {
  if (!href) {
    return;
  }
  let element = document.head.querySelector(`link[rel="${rel}"]`);
  if (!element) {
    element = document.createElement("link");
    element.setAttribute("rel", rel);
    document.head.appendChild(element);
  }
  if (element.getAttribute("href") !== href) {
    element.setAttribute("href", href);
  }
}
