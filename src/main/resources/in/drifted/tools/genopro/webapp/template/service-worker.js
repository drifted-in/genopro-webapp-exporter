var relativeAppUrl = "${relativeAppUrl}";
var currentCacheId = "${currentCacheId}";

self.addEventListener("install", function(event) {
    event.waitUntil(
        caches.open(currentCacheId).then(function(cache) {
            return cache.addAll([
                relativeAppUrl + "/",
                relativeAppUrl + "/attribution.js",
                relativeAppUrl + "/genomaps.js",
                relativeAppUrl + "/index.html",
                relativeAppUrl + "/individuals.js",
                relativeAppUrl + "/manifest.json",
                relativeAppUrl + "/res/hammer.min.js",
                relativeAppUrl + "/res/OpenSans-Regular-webfont.woff",
                relativeAppUrl + "/res/style.css",
                relativeAppUrl + "/res/svg-pan-zoom.min.js",
                relativeAppUrl + "/favicon/android-chrome-192x192.png",
                relativeAppUrl + "/favicon/android-chrome-512x512.png",
                relativeAppUrl + "/favicon/apple-touch-icon.png",
                relativeAppUrl + "/favicon/favicon.ico",
                relativeAppUrl + "/favicon/favicon-16x16.png",
                relativeAppUrl + "/favicon/favicon-32x32.png",
                "${genoMapPathList}"
            ]);
        })
    );
});

self.addEventListener('activate', function(event) {
    event.waitUntil(
        caches.keys().then(function(keyList) {
            return Promise.all(keyList.map(function(key) {
                if (currentCacheId.indexOf(key) === -1) {
                    return caches.delete(key);
                }
            }));
        })
    );
});

self.addEventListener("fetch", function(event) {
    event.respondWith(
        caches.match(event.request).then(function(response) {
            if (response) {
                return response;
            } else {
                return fetch(event.request);
            }
        })
    );
});
