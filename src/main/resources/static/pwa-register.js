// Registers the Flight Log service worker, surfaces offline UX hooks
// and forces a reload when an updated worker takes over.
if ('serviceWorker' in navigator) {
    let reloading = false;
    navigator.serviceWorker.addEventListener('controllerchange', () => {
        if (reloading) return;
        reloading = true;
        window.location.reload();
    });

    window.addEventListener('load', () => {
        navigator.serviceWorker
            .register('/service-worker.js', { scope: '/' })
            .then((reg) => {
                // Pick up newly installed SW immediately.
                if (reg.waiting) reg.waiting.postMessage({ type: 'skip-waiting' });
                reg.addEventListener('updatefound', () => {
                    const sw = reg.installing;
                    if (!sw) return;
                    sw.addEventListener('statechange', () => {
                        if (sw.state === 'installed' && navigator.serviceWorker.controller) {
                            sw.postMessage({ type: 'skip-waiting' });
                        }
                    });
                });
                // Periodically check for an updated worker.
                setInterval(() => reg.update().catch(() => { }), 60_000);
            })
            .catch((err) => console.warn('SW registration failed:', err));
    });

    navigator.serviceWorker.addEventListener('message', (event) => {
        const data = event.data || {};
        if (data.type === 'sync-conflict') {
            window.dispatchEvent(new CustomEvent('flightlog:sync-conflict', { detail: data }));
            alert('Záznam byl přepsán novějšími daty.');
        }
        if (data.type === 'queued-offline') {
            window.dispatchEvent(new CustomEvent('flightlog:queued-offline', { detail: data }));
        }
    });

    window.addEventListener('online', () => {
        navigator.serviceWorker.controller && navigator.serviceWorker.controller.postMessage({ type: 'flush-outbox' });
    });
}

// Tiny offline indicator helper for embedded pages.
window.flightlogUpdateOfflineBanner = function () {
    let banner = document.getElementById('flightlog-offline-banner');
    if (!banner) {
        banner = document.createElement('div');
        banner.id = 'flightlog-offline-banner';
        banner.style.cssText = 'position:fixed;top:0;left:0;right:0;background:#b71c1c;color:#fff;padding:.5rem;text-align:center;z-index:9999;font-family:sans-serif;';
        banner.textContent = 'Aplikace pracuje v offline režimu. Vaše změny budou odeslány po obnovení připojení.';
        document.body && document.body.appendChild(banner);
    }
    banner.style.display = navigator.onLine ? 'none' : 'block';
};
window.addEventListener('online', window.flightlogUpdateOfflineBanner);
window.addEventListener('offline', window.flightlogUpdateOfflineBanner);
window.addEventListener('DOMContentLoaded', window.flightlogUpdateOfflineBanner);
