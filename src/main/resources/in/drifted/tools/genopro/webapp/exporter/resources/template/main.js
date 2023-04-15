/* Copyright (c) 2018 Jan Tošovský */
var svgPanZoomInstance;
var genoMapSvg = null;
var genoMapId = getGenoMapId();
var genoMapSelectedIndividualId = null;
var keywords = [];
var searchResultsSelectedEntryDetail = null;
var searchResultsSelectedIndividualId = null;
var pinnedEntry = null;
var dragging = false;
var dynamic = "${dynamic}";
var rowsProcessed = 0;
var zoom;
var pan;
var theme = "light";

window.addEventListener("hashchange", processParams);
window.addEventListener("beforeprint", setRealSize);
window.addEventListener("afterprint", restoreSize);
document.addEventListener("DOMContentLoaded", init);
document.getElementById("keywords").addEventListener("input", triggerSearch);
document.getElementById("searchButton").addEventListener("click", triggerSearch);
document.getElementById("clearSearchInputButton").addEventListener("click", hideResults);
document.getElementById("themeSwitch").addEventListener("click", switchTheme);

function processParams() {

    var passedGenoMapId = getGenoMapId();

    if (passedGenoMapId !== null && passedGenoMapId !== genoMapId) {
        genoMapId = passedGenoMapId;
        document.getElementById("genomap-list").value = genoMapId;

        switchGenoMap(function() {
            svgPanZoomInstance.zoomBy(1 / svgPanZoomInstance.getSizes().realZoom);
        });
    }
}

function init(e) {

    if (dynamic && (location.protocol.indexOf("http") === -1)) {

        document.getElementById("search").style.display = "none";
        document.getElementById("genomap-list").style.display = "none";
        document.getElementById("content").innerHTML = "${alertDynamicOnFileSystem}";

    } else {
        
        theme = localStorage.getItem('theme') ? localStorage.getItem('theme') : null;
        setTheme();

        var genoMapSelect = document.getElementById("genomap-list");

        genoMapMap.forEach(function (value, key, map) {
            var genoMapSelectItem = document.createElement("option");
            genoMapSelectItem.value = key;
            genoMapSelectItem.text = value;
            genoMapSelect.appendChild(genoMapSelectItem);
        });

        if (genoMapId !== null) {
            genoMapSelect.value = genoMapId;
        }

        var genoMaps = document.getElementById("content").getElementsByTagName("svg");

        for (var i = 0; i < genoMaps.length; i++) {
            genoMaps[i].style.display = "none";
        }

        switchGenoMap(function() {
            svgPanZoomInstance.zoomBy(1 / svgPanZoomInstance.getSizes().realZoom);
        });

        genoMapSelect.addEventListener("change", onGenoMapSelectChange);

        document.getElementById("keywords").placeholder = "${searchPlaceholder}";

        if (typeof attributionBody !== "undefined") {
            document.getElementById("attribution").innerHTML = attributionBody;
        }

        triggerSearch();
    }
}

function onGenoMapSelectChange(e) {

    genoMapId = e.target.value;

    switchGenoMap(function() {
        svgPanZoomInstance.zoomBy(1 / svgPanZoomInstance.getSizes().realZoom);
    });
}

function switchGenoMap(callback) {

    if (dynamic) {

        var httpRequest = new XMLHttpRequest();

        httpRequest.onreadystatechange = function() {
            if (httpRequest.readyState === XMLHttpRequest.DONE && httpRequest.status === 200) {

                var svg = httpRequest.responseXML.documentElement;

                var content = document.getElementById("content");

                var nodes = content.getElementsByTagName("svg");

                while (nodes[0]) {
                    nodes[0].parentNode.removeChild(nodes[0]);
                }

                genoMapSvg = content.appendChild(svg);
                genoMapSvg.style.display = "block";

                genoMapSelectedIndividualId = null;

                initSvgListeners();
                initSvgPanZoom();

                callback();
            }
        };

        if (genoMapId === null) {

            genoMapId = genoMapMap.keys().next().value;
            genoMapSvg = document.getElementById(genoMapId);
            genoMapSvg.style.display = "block";

            initSvgListeners();
            initSvgPanZoom();

            callback();

        } else {
            httpRequest.open("GET", genoMapId + ".svg");
            httpRequest.send();
        }

    } else {
        if (svgPanZoomInstance) {
            /* removing the selection */
            var nodes = genoMapSvg.getElementsByClassName("handle");

            while (nodes[0]) {
                nodes[0].parentNode.removeChild(nodes[0]);
            }

            genoMapSvg.style.display = "none";
        }

        if (genoMapId === null) {
            genoMapId = genoMapMap.keys().next().value;
        }

        genoMapSvg = document.getElementById(genoMapId);
        genoMapSvg.style.display = "block";

        initSvgListeners();
        initSvgPanZoom();

        callback();
    }

    location.hash = "#/sheet/" + genoMapId;
}

function initSvgListeners() {

    var individuals = document.getElementsByClassName("individual-active-area");
    for (var i = 0; i < individuals.length; i++) {
        individuals[i].addEventListener("touchend", select);
        individuals[i].addEventListener("mousedown", select);
    }

    var hyperlinks = document.getElementsByClassName("individual-label-hyperlink");
    for (var i = 0; i < hyperlinks.length; i++) {
        hyperlinks[i].addEventListener("touchend", scrollIntoView);
        hyperlinks[i].addEventListener("mousedown", scrollIntoView);
    }
}

function initSvgPanZoom() {

    svgPanZoomInstance = svgPanZoom(genoMapSvg, {
        zoomScaleSensitivity: 0.25,
        minZoom: 0.01,
        maxZoom: 100,
        dblClickZoomEnabled: false,
        customEventsHandler: {
            haltEventListeners: ["touchstart", "touchend", "touchmove", "touchleave", "touchcancel"],
            init: function(options) {

                var instance = options.instance;
                var initialScale = 1;
                var pannedX = 0;
                var pannedY = 0;

                /* Listen only for pointer and touch events */
                this.hammer = Hammer(options.svgElement, {
                    inputClass: Hammer.SUPPORT_POINTER_EVENTS ? Hammer.PointerEventInput : Hammer.TouchInput
                });

                /* Enable pinch */
                this.hammer.get("pinch").set({enable: true});

                /* Handle pan */
                this.hammer.on("panstart panmove", function(e){

                    if (e.type === "panstart") {
                        pannedX = 0;
                        pannedY = 0;
                    }

                    instance.panBy({x: e.deltaX - pannedX, y: e.deltaY - pannedY});
                    pannedX = e.deltaX;
                    pannedY = e.deltaY;
                });

                /* Handle pinch */
                this.hammer.on("pinchstart pinchmove", function(e){

                    if (e.type === "pinchstart") {
                        initialScale = instance.getZoom();
                        instance.zoom(initialScale * e.scale);
                    }

                    instance.zoom(initialScale * e.scale);
                });

                /* Prevent moving the page on some devices when panning over SVG */
                options.svgElement.addEventListener("touchmove", function(e) {
                    e.preventDefault();
                });
            }
        }
    });
}

function triggerSearch(e) {

    var rawKeywords = document.getElementById("keywords").value.trim();

    if (rawKeywords.length > 0) {
        var newKeywords = rawKeywords.toLowerCase().split(/\s+/);
        if (!isSame(keywords, newKeywords)) {
            keywords = newKeywords;
            rowsProcessed = 0;
            showResults();
        }

    } else {
        keywords = [];
        rowsProcessed = 0;
        hideResults();
    }
}

function scrollIntoView(e) {

    e.preventDefault();

    var id = e.target.parentElement.getAttribute("data-target-id");
    scrollIntoViewById(id);
}

function scrollIntoViewById(id) {

    if (iMap.get(id)[0] !== genoMapId) {

        genoMapId = iMap.get(id)[0];
        document.getElementById("genomap-list").value = genoMapId;

        switchGenoMap(function() {

            var currentPosition = getCurrentPosition(id);
            var targetPosition = getTargetPosition();

            svgPanZoomInstance.panBy({ x: targetPosition.x - currentPosition.x, y: targetPosition.y - currentPosition.y });
            svgPanZoomInstance.zoomAtPointBy(1 / svgPanZoomInstance.getSizes().realZoom, targetPosition);

            selectById(id);
        });

    } else {

        var currentPosition = getCurrentPosition(id);
        var targetPosition = getTargetPosition();

        svgPanZoomInstance.panBy({ x: targetPosition.x - currentPosition.x, y: targetPosition.y - currentPosition.y });

        selectById(id);
    }
}

function select(e) {

    e.preventDefault();

    selectById(e.target.parentElement.id);
}

function selectById(id) {

    genoMapSelectedIndividualId = id;

    /* remove previous handles */
    var nodes = genoMapSvg.getElementsByClassName("handle");

    while (nodes[0]) {
        nodes[0].parentNode.removeChild(nodes[0]);
    }

    /* get bounding box */
    var boundingBoxElement = genoMapSvg.getElementById(id + '-bb');
    var x = Number(boundingBoxElement.getAttribute("x"));
    var y = Number(boundingBoxElement.getAttribute("y"));
    var width = Number(boundingBoxElement.getAttribute("width"));
    var height = Number(boundingBoxElement.getAttribute("height"));

    /* create handles */
    var parent = genoMapSvg.getElementById(id);
    createHandle(parent, x - 8, y - 8);
    createHandle(parent, x + width / 2 - 4, y - 8);
    createHandle(parent, x + width, y - 8);
    createHandle(parent, x - 8, y + height / 2 - 4);
    createHandle(parent, x + width, y + height / 2 - 4);
    createHandle(parent, x - 8, y + height);
    createHandle(parent, x + width / 2 - 4, y + height);
    createHandle(parent, x + width, y + height);
}

function createHandle(parent, x, y) {
    handle = document.createElementNS("http://www.w3.org/2000/svg", "rect");
    handle.setAttribute("x", x);
    handle.setAttribute("y", y);
    handle.setAttribute("width", 8);
    handle.setAttribute("height", 8);
    handle.setAttribute("class", "handle");
    parent.appendChild(handle);
}

function showDetail(e) {

    if (!dragging) {

        e.preventDefault();

        document.getElementById("keywords").blur();

        var entry = e.target.closest(".entry");
        
        if (entry.id !== searchResultsSelectedIndividualId) {
            searchResultsSelectedIndividualId = entry.id;

        } else {
            searchResultsSelectedIndividualId = null;
        }

        scrollIntoViewById(entry.id.substring(1));
    }
}

function getCurrentPosition(id) {

    var boundingBoxElement = genoMapSvg.getElementById(id + '-bb');
    var boundingRect = boundingBoxElement.getBoundingClientRect();
    var x = boundingRect.left + boundingRect.width / 2;
    var y = boundingRect.top + boundingRect.height / 2;

    var parentBoundingRect = genoMapSvg.getBoundingClientRect();
    var parentX = parentBoundingRect.left;
    var parentY = parentBoundingRect.top;

    x -= parentX;
    y -= parentY;

    return { x: x, y: y };
}

function getTargetPosition() {

    var results = document.getElementById("results");

    return {
        x: (document.documentElement.offsetWidth + results.offsetWidth) / 2,
        y: document.documentElement.offsetHeight / 2
    };
}

function getFullName(individualInfo) {

    var nameArray = new Array();

    for (var i = 1; i <= 3; i++) {
        if (individualInfo[i].length > 0) {
            nameArray.push(individualInfo[i]);
        }
    }

    return nameArray.join(" ");
}

function showResults() {

    var results = document.getElementById("results");
    results.style.display = "block";

    results.addEventListener("scroll", function() {
        if ((results.scrollTop + results.clientHeight + 1) >= results.scrollHeight) {
            addMoreSearchResultEntries(results);
        }
    });

    /* centering */
    if (searchResultsSelectedIndividualId !== null) {
        scrollIntoViewById(searchResultsSelectedIndividualId.substring(1));
    } else if (genoMapSelectedIndividualId !== null) {
        scrollIntoViewById(genoMapSelectedIndividualId);
    }

    if (results.hasChildNodes()) {
        while (results.firstChild) {
            results.removeChild(results.firstChild);
        }
    }

    var clearSearchInputButton = document.getElementById("clearSearchInputButton");
    if (clearSearchInputButton.style.display === "none") {
        var searchButton = document.getElementById("searchButton");
        searchButton.style.display = "none";
        clearSearchInputButton.style.display = "block";
    }

    addMoreSearchResultEntries(results);
}

function addMoreSearchResultEntries(resultsElement) {

    var row = 0;
    var addedEntries = 0;

    for (let [key, value] of iMap) {

        row++;

        if (row > rowsProcessed) {

            if (matches(value, keywords)) {

                createSearchResultEntry(resultsElement, key, value);

                addedEntries++;

                if (addedEntries > 25) {
                    break;
                }
            }
        }
    }

    rowsProcessed = row;
}

function createSearchResultEntry(resultsElement, id, value) {

    var entry = document.createElement('div');
    entry.classList.add("entry");
    entry.id = "s" + id;
    resultsElement.appendChild(entry);

    var hitArea = document.createElement('div');
    hitArea.classList.add("hitarea");

    hitArea.addEventListener("touchstart", resetDragging);
    hitArea.addEventListener("touchmove", setDragging);
    hitArea.addEventListener("touchend", pinEntry);
    hitArea.addEventListener("mousedown", pinEntry);

    entry.appendChild(hitArea);

    var info = document.createElement('div');
    info.classList.add("info");

    info.addEventListener("touchstart", resetDragging);
    info.addEventListener("touchmove", setDragging);
    info.addEventListener("touchend", showDetail);
    info.addEventListener("mousedown", showDetail);

    entry.appendChild(info);

    var topRow = document.createElement('div');
    topRow.classList.add("top");
    info.appendChild(topRow);

    var name = document.createElement('div');
    var names = [];
    if (value[1].length > 0) {
        names.push(value[1]);
    }
    if (value[2].length > 0) {
        names.push(value[2]);
    }
    name.textContent = names.join(" ").trim();
    name.classList.add("name");
    topRow.appendChild(name);

    var lastname = document.createElement('div');
    lastname.textContent = value[3];
    lastname.classList.add("lastname");
    topRow.appendChild(lastname);

    var birthDate = document.createElement('div');
    birthDate.textContent = value[4];
    birthDate.classList.add("birthdate");
    topRow.appendChild(birthDate);

    var detail = document.createElement('div');
    detail.classList.add("detail");
    if ((value[6] + value[7] + value[8]).length > 0) {

        var fatherId = value[7];
        if (fatherId.length > 0) {
            var fatherName = getFullName(iMap.get(fatherId));
            if (fatherName.length > 0) {
                var father = document.createElement("div");
                father.classList.add("father");
                father.textContent = fatherName;
                detail.appendChild(father);
            }
        }

        var motherId = value[8];
        if (motherId.length > 0) {
            var mother = document.createElement("div");
            mother.classList.add("mother");
            mother.textContent = getFullName(iMap.get(motherId));
            detail.appendChild(mother);
        }

        var delimitedMateIds = value[6];
        if (delimitedMateIds.length > 0) {
            var matesArray = new Array();
            var mateIds = delimitedMateIds.split(",");

            for (var m = 0; m < mateIds.length; m++) {
                var individualInfo = iMap.get(mateIds[m]);
                var fullName = getFullName(individualInfo);
                if (fullName.length > 0) {
                    matesArray.push(fullName);
                }
            }

            var mate = document.createElement("div");
            mate.classList.add("mate");
            mate.textContent = matesArray.join(", ");
            detail.appendChild(mate);     
        }
    }

    info.appendChild(detail);

    var bottomRow = document.createElement('div');
    bottomRow.classList.add("bottom");
    info.appendChild(bottomRow);

    var genoMapName = document.createElement('div');
    genoMapName.classList.add("genomapname");
    genoMapName.textContent = genoMapMap.get(value[0]);
    bottomRow.appendChild(genoMapName);
}

function hideResults() {

    var searchButton = document.getElementById("searchButton");
    var clearSearchInputButton = document.getElementById("clearSearchInputButton");

    searchButton.style.display = "block";
    clearSearchInputButton.style.display = "none";

    if (pinnedEntry !== null) {
        pinnedEntry.parentElement.removeChild(pinnedEntry);
    }

    document.getElementById("keywords").value = "";
    document.getElementById("results").style.display = "none";

    if (searchResultsSelectedIndividualId !== null) {
        scrollIntoViewById(searchResultsSelectedIndividualId.substring(1));
    } else if (genoMapSelectedIndividualId !== null) {
        scrollIntoViewById(genoMapSelectedIndividualId);
    }

    searchResultsSelectedIndividualId = null;
}

function pinEntry(e) {

    if (!dragging) {
        e.preventDefault();

        document.getElementById("keywords").blur();
        document.getElementById("search").style.display = "none";
        document.getElementById("results").style.display = "none";
        document.getElementById("genomap-list").style.display = "none";

        var entry = e.target.closest(".entry");

        pinnedEntry = entry.cloneNode(true);

        pinnedEntry.classList.add("pinned");
        document.getElementById("container").appendChild(pinnedEntry);

        var hitArea = pinnedEntry.getElementsByClassName("hitarea")[0];
        hitArea.addEventListener("touchend", unpinEntry);
        hitArea.addEventListener("mousedown", unpinEntry);

        var info = pinnedEntry.getElementsByClassName("info")[0];
        info.addEventListener("touchend", showDetail);
        info.addEventListener("mousedown", showDetail);

        scrollIntoViewById(hitArea.parentElement.id.substring(1));
    }
}

function unpinEntry(e) {

    e.preventDefault();

    searchResultsSelectedIndividualId = pinnedEntry.id;
    document.getElementById("container").removeChild(pinnedEntry);
    /* it is not deleted directly because of global variable */
    pinnedEntry = null;
    document.getElementById("search").style.display = "flex";
    document.getElementById("results").style.display = "block";
    document.getElementById("genomap-list").style.display = "inline-block";

    /* centering */
    scrollIntoViewById(genoMapSelectedIndividualId);
}

function setDragging(e) {
    dragging = true;
}

function resetDragging(e) {
    dragging = false;
}

function setRealSize(e) {

    pan = svgPanZoomInstance.getPan();
    zoom = svgPanZoomInstance.getZoom();
    svgPanZoomInstance.zoomBy(1.0);
    svgPanZoomInstance.panBy({x:0, y:0});

    var sizes = svgPanZoomInstance.getSizes();

    var content = document.getElementById("content");
    content.style.width = sizes.viewBox.width + "px";
    content.style.height = sizes.viewBox.height + "px";
}

function restoreSize(e) {

    var content = document.getElementById("content");
    content.style.width = "100vw";
    content.style.height = "100vh";

    svgPanZoomInstance.zoom(zoom);
    svgPanZoomInstance.pan(pan);
}

function matches(value, keywords) {

    var result = true;

    for (var k = 0; k < keywords.length; k++) {
        var found = false;
        for (var i = 1; i < 5; i++) {
            var normalizedValue = value[i].toLowerCase();
            if (normalizedValue.indexOf(keywords[k]) !== -1) {
                found = true;
                break;
            }
        }
        result &= found;
    }

    return result;
}

function isSame(array1, array2) {
    return (typeof array1 !== "undefined" && array1.length === array2.length) && array1.every(function(element, index) {
        return element === array2[index];
    });
}

function getGenoMapId() {
    var result = location.hash.match("#/sheet/([a-z0-9-]*)");
    return result && genoMapMap.has(result[1]) ? result[1] : null;
}

function switchTheme() {
    theme = (theme === "dark") ? "light" : "dark";
    setTheme();
}

function setTheme() {
    if (theme === "dark") {
        document.documentElement.setAttribute('data-theme', 'dark');
    }
    else {
        document.documentElement.setAttribute('data-theme', 'light');
    }
    localStorage.setItem("theme", theme);
}