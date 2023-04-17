if ((location.protocol.indexOf("http") !== -1) && ("serviceWorker" in navigator)) {
    navigator.serviceWorker.register("${relativeAppUrl}/service-worker.js");
}
