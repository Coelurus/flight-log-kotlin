/* Flight Log service worker (FR-02 / NFR-04).
 *  - Network-first for HTML navigations (so updates always reach the browser)
 *  - Network-first for /api/admin|auth|writer
 *  - Stale-while-revalidate for read-mostly lookup endpoints
 *  - Cache-first for versioned static assets only
 *  - Offline POST replay via IndexedDB outbox
 */

const CACHE_NAME = 'flightlog-shell-v7';
const RUNTIME_CACHE = 'flightlog-runtime-v7';

// Don't pre-cache HTML pages — we always prefer network for those.
const APP_SHELL = [
    '/manifest.webmanifest',
    '/pwa-register.js',
];

const NETWORK_FIRST_PREFIXES = ['/api/admin/', '/api/auth/', '/api/writer/'];
const STALE_WHILE_REVALIDATE_PREFIXES = ['/airplane', '/user', '/api/clubdb/'];
const OUTBOX_PATHS = ['/api/writer/flights'];

/* ----------------- IndexedDB outbox ----------------- */
const DB_NAME = 'flightlog-outbox';
const STORE = 'requests';

function openDb() {
    return new Promise((resolve, reject) => {
        const req = indexedDB.open(DB_NAME, 1);
        req.onupgradeneeded = () => req.result.createObjectStore(STORE, { keyPath: 'id', autoIncrement: true });
        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
    });
}

async function enqueue(record) {
    const db = await openDb();
    return new Promise((resolve, reject) => {
        const tx = db.transaction(STORE, 'readwrite');
        tx.objectStore(STORE).add(record);
        tx.oncomplete = () => resolve();
        tx.onerror = () => reject(tx.error);
    });
}

async function drainOutbox() {
    const db = await openDb();
    const tx = db.transaction(STORE, 'readwrite');
    const store = tx.objectStore(STORE);
    const all = await new Promise((res, rej) => {
        const r = store.getAll(); r.onsuccess = () => res(r.result); r.onerror = () => rej(r.error);
    });
    for (const item of all) {
        try {
            const resp = await fetch(item.url, {
                method: item.method, headers: item.headers, body: item.body, credentials: 'include',
            });
            if (resp.status === 409) {
                await notifyClients({ type: 'sync-conflict', url: item.url });
            } else if (!resp.ok) {
                continue;
            }
            store.delete(item.id);
        } catch (err) {
            break;
        }
    }
    await new Promise((res) => { tx.oncomplete = res; });
}

async function notifyClients(payload) {
    const clients = await self.clients.matchAll({ includeUncontrolled: true });
    clients.forEach((c) => c.postMessage(payload));
}

/* ----------------- Lifecycle ----------------- */
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL).catch(() => { }))
    );
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil((async () => {
        const keys = await caches.keys();
        await Promise.all(
            keys.filter((k) => ![CACHE_NAME, RUNTIME_CACHE].includes(k)).map((k) => caches.delete(k))
        );
        await self.clients.claim();
    })());
});

self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'flush-outbox') event.waitUntil(drainOutbox());
    if (event.data && event.data.type === 'skip-waiting') self.skipWaiting();
});

self.addEventListener('sync', (event) => {
    if (event.tag === 'flush-outbox') event.waitUntil(drainOutbox());
});

/* ----------------- Fetch routing ----------------- */
self.addEventListener('fetch', (event) => {
    const req = event.request;
    const url = new URL(req.url);

    if (url.origin !== self.location.origin) return;

    if (req.method !== 'GET') {
        if (OUTBOX_PATHS.some((p) => url.pathname.startsWith(p))) {
            event.respondWith(handleWriteRequest(req));
        }
        return;
    }

    // HTML navigations: always try network first so UI updates reach users.
    if (req.mode === 'navigate' || (req.headers.get('accept') || '').includes('text/html')) {
        event.respondWith(networkFirstNavigation(req));
        return;
    }

    if (NETWORK_FIRST_PREFIXES.some((p) => url.pathname.startsWith(p))) {
        event.respondWith(networkFirst(req));
        return;
    }
    if (STALE_WHILE_REVALIDATE_PREFIXES.some((p) => url.pathname.startsWith(p))) {
        event.respondWith(staleWhileRevalidate(req));
        return;
    }
    event.respondWith(cacheFirst(req));
});

async function networkFirstNavigation(req) {
    try {
        const resp = await fetch(req);
        if (resp && resp.ok) {
            const cache = await caches.open(RUNTIME_CACHE);
            cache.put(req, resp.clone());
        }
        return resp;
    } catch (err) {
        const cached = await caches.match(req);
        if (cached) return cached;
        return new Response('<h1>Offline</h1><p>Aplikace není dostupná.</p>',
            { status: 503, headers: { 'Content-Type': 'text/html; charset=utf-8' } });
    }
}

async function handleWriteRequest(req) {
    try {
        return await fetch(req.clone());
    } catch (err) {
        const body = await req.clone().text();
        const headers = {};
        req.headers.forEach((v, k) => { headers[k] = v; });
        await enqueue({ url: req.url, method: req.method, headers, body, ts: Date.now() });
        await notifyClients({ type: 'queued-offline', url: req.url });
        return new Response(JSON.stringify({ queued: true }), {
            status: 202, headers: { 'Content-Type': 'application/json' },
        });
    }
}

async function networkFirst(req) {
    try {
        const resp = await fetch(req);
        if (resp && resp.status === 200) {
            const cache = await caches.open(RUNTIME_CACHE);
            cache.put(req, resp.clone());
        }
        return resp;
    } catch (err) {
        const cached = await caches.match(req);
        if (cached) return cached;
        throw err;
    }
}

async function staleWhileRevalidate(req) {
    const cache = await caches.open(RUNTIME_CACHE);
    const cached = await cache.match(req);
    const network = fetch(req).then((resp) => {
        if (resp && resp.status === 200) cache.put(req, resp.clone());
        return resp;
    }).catch(() => cached);
    return cached || network;
}

async function cacheFirst(req) {
    const cached = await caches.match(req);
    if (cached) return cached;
    const resp = await fetch(req);
    if (resp && resp.status === 200 && resp.type === 'basic') {
        const cache = await caches.open(RUNTIME_CACHE);
        cache.put(req, resp.clone());
    }
    return resp;
}
