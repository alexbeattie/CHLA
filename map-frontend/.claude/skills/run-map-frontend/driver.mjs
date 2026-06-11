// Agent driver for the KiNDD map-frontend (Vue 3 + Vite + Mapbox).
// Uses the project's own puppeteer devDependency - run from map-frontend/.
//
// Usage (from map-frontend/, dev server already running on $PORT or 3000):
//   node .claude/skills/run-map-frontend/driver.mjs smoke
//   node .claude/skills/run-map-frontend/driver.mjs shot /about about.png
//   node .claude/skills/run-map-frontend/driver.mjs eval / "document.title"
//   node .claude/skills/run-map-frontend/driver.mjs click / "a[href='/about']" after-click.png
//
// Screenshots land in /tmp/map-frontend-shots/.

import puppeteer from "puppeteer";
import { mkdirSync } from "fs";
import path from "path";

const BASE = process.env.BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
const SHOTS = "/tmp/map-frontend-shots";
mkdirSync(SHOTS, { recursive: true });

const [, , cmd, ...rest] = process.argv;

async function withPage(fn) {
  const browser = await puppeteer.launch({
    headless: "new",
    args: ["--no-sandbox", "--disable-gpu", "--window-size=1280,900"],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 900 });
  const consoleErrors = [];
  page.on("console", (msg) => {
    if (msg.type() === "error") consoleErrors.push(msg.text());
  });
  page.on("pageerror", (err) => consoleErrors.push(`pageerror: ${err.message}`));
  try {
    await fn(page, consoleErrors);
  } finally {
    await browser.close();
  }
}

async function goto(page, route) {
  // "load" not "networkidle2": hanging /api requests (backend down or wrong
  // server on :8000) keep the network busy forever and time the nav out.
  await page.goto(`${BASE}${route}`, { waitUntil: "load", timeout: 30000 });
  // Let Vue finish mounting and useSeo apply head updates.
  await new Promise((r) => setTimeout(r, 1500));
}

function shotPath(name) {
  return path.join(SHOTS, name.endsWith(".png") ? name : `${name}.png`);
}

if (cmd === "shot") {
  const [route = "/", out = "shot.png"] = rest;
  await withPage(async (page, errs) => {
    await goto(page, route);
    const file = shotPath(out);
    await page.screenshot({ path: file, fullPage: false });
    console.log(`title: ${await page.title()}`);
    console.log(`screenshot: ${file}`);
    if (errs.length) console.log(`console errors (${errs.length}):\n  ${errs.slice(0, 10).join("\n  ")}`);
  });
} else if (cmd === "eval") {
  const [route = "/", expr = "document.title"] = rest;
  await withPage(async (page) => {
    await goto(page, route);
    const result = await page.evaluate((e) => {
      // eslint-disable-next-line no-eval
      const v = eval(e);
      return typeof v === "object" ? JSON.stringify(v) : String(v);
    }, expr);
    console.log(result);
  });
} else if (cmd === "click") {
  const [route = "/", selector, out = "after-click.png"] = rest;
  if (!selector) {
    console.error("usage: driver.mjs click <route> <selector> [out.png]");
    process.exit(1);
  }
  await withPage(async (page, errs) => {
    await goto(page, route);
    await page.waitForSelector(selector, { timeout: 10000 });
    await page.click(selector);
    await new Promise((r) => setTimeout(r, 1500));
    const file = shotPath(out);
    await page.screenshot({ path: file, fullPage: false });
    console.log(`url after click: ${page.url()}`);
    console.log(`title: ${await page.title()}`);
    console.log(`screenshot: ${file}`);
    if (errs.length) console.log(`console errors (${errs.length}):\n  ${errs.slice(0, 10).join("\n  ")}`);
  });
} else if (cmd === "smoke") {
  // Visit the key routes, screenshot each, surface console errors.
  const routes = ["/", "/about", "/faq", "/regional-centers", "/clinicians"];
  let failures = 0;
  await withPage(async (page, errs) => {
    for (const route of routes) {
      errs.length = 0;
      try {
        await goto(page, route);
        const name = route === "/" ? "home" : route.slice(1).replace(/\//g, "-");
        const file = shotPath(`smoke-${name}.png`);
        await page.screenshot({ path: file });
        const appHtml = await page.$eval("#app", (el) => el.innerHTML.length).catch(() => 0);
        const ok = appHtml > 500; // mounted Vue app, not a blank shell
        if (!ok) failures++;
        console.log(`${ok ? "OK  " : "FAIL"} ${route}  app-html=${appHtml}b  shot=${file}` +
          (errs.length ? `  console-errors=${errs.length}` : ""));
        for (const e of errs.slice(0, 3)) console.log(`      err: ${e.slice(0, 160)}`);
      } catch (e) {
        failures++;
        console.log(`FAIL ${route}  ${e.message.slice(0, 120)}`);
      }
    }
  });
  console.log(failures === 0 ? "SMOKE PASS" : `SMOKE FAIL (${failures} route(s))`);
  process.exit(failures === 0 ? 0 : 1);
} else {
  console.log("commands: smoke | shot <route> [out.png] | eval <route> <js> | click <route> <selector> [out.png]");
  process.exit(1);
}
