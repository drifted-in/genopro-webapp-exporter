/* Copyright (c) 2018 Jan Tošovský */
const dynamic = "${dynamic}";
let svgPanZoomInstance;
let genoMapSvg = null;
let genoMapId = getGenoMapId();
let genoMapSelectedIndividualId = null;
let keywords = [];
let searchResultsSelectedEntryDetail = null;
let searchResultsSelectedIndividualId = null;
let pinnedEntry = null;
let dragging = false;
let rowsProcessed = 0;
let zoom;
let pan;
let theme = "light";

window.addEventListener("hashchange", processParams);
window.addEventListener("beforeprint", setRealSize);
window.addEventListener("afterprint", restoreSize);
document.addEventListener("DOMContentLoaded", init);
document.getElementById("keywords").addEventListener("input", triggerSearch);
document.getElementById("searchButton").addEventListener("click", triggerSearch);
document.getElementById("clearSearchInputButton").addEventListener("click", hideResults);
document.getElementById("themeSwitch").addEventListener("click", switchTheme);

function processParams() {

    const passedGenoMapId = getGenoMapId();

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

        const genoMapSelect = document.getElementById("genomap-list");

        genoMapMap.forEach(function(value, key, map) {
            const genoMapSelectItem = document.createElement("option");
            genoMapSelectItem.value = key;
            genoMapSelectItem.text = value;
            genoMapSelect.appendChild(genoMapSelectItem);
        });

        if (genoMapId !== null) {
            genoMapSelect.value = genoMapId;
        }

        const genoMaps = document.getElementById("content").getElementsByTagName("svg");

        for (let i = 0; i < genoMaps.length; i++) {
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

        if (genoMapId === null) {

            genoMapId = genoMapMap.keys().next().value;
            genoMapSvg = document.getElementById(genoMapId);
            genoMapSvg.style.display = "block";

            initSvgListeners();
            initSvgPanZoom();

            callback();

        } else {

            fetch(genoMapId + ".svg")
                .then(response => response.text())
                .then(data => {
                    const parser = new DOMParser();
                    const xml = parser.parseFromString(data, "application/xml");
                    const svg = xml.documentElement;

                    const content = document.getElementById("content");
                    const nodes = content.getElementsByTagName("svg");

                    while (nodes[0]) {
                        nodes[0].parentNode.removeChild(nodes[0]);
                    }

                    genoMapSvg = content.appendChild(svg);
                    genoMapSvg.style.display = "block";

                    genoMapSelectedIndividualId = null;

                    initSvgListeners();
                    initSvgPanZoom();

                    callback();
                })
                .catch(console.error);
        }

    } else {
        if (svgPanZoomInstance) {
            /* removing the selection */
            const nodes = genoMapSvg.getElementsByClassName("handle");

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
    const familyLines = document.getElementsByClassName("family-line");
    for (let i = 0; i < familyLines.length; i++) {
        familyLines[i].addEventListener("touchend", selectPath);
        familyLines[i].addEventListener("mousedown", selectPath);
    }

    const pedigreeLinks = document.getElementsByClassName("pedigree-link");
    for (let i = 0; i < pedigreeLinks.length; i++) {
        pedigreeLinks[i].addEventListener("touchend", selectPath);
        pedigreeLinks[i].addEventListener("mousedown", selectPath);
    }

    const individuals = document.getElementsByClassName("individual-active-area");
    for (let i = 0; i < individuals.length; i++) {
        individuals[i].addEventListener("touchend", select);
        individuals[i].addEventListener("mousedown", select);
    }

    const hyperlinks = document.getElementsByClassName("individual-label-hyperlink");
    for (let i = 0; i < hyperlinks.length; i++) {
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
            haltEventListeners: [
                "touchstart",
                "touchend",
                "touchmove",
                "touchleave",
                "touchcancel"
            ],
            init: function(options) {

                let instance = options.instance;
                let initialScale = 1;
                let pannedX = 0;
                let pannedY = 0;

                /* Listen only for pointer and touch events */
                this.hammer = Hammer(options.svgElement, {
                    inputClass: Hammer.SUPPORT_POINTER_EVENTS ? Hammer.PointerEventInput : Hammer.TouchInput
                });

                /* Enable pinch */
                this.hammer.get("pinch").set({enable: true});

                /* Handle pan */
                this.hammer.on("panstart panmove", function(e) {

                    if (e.type === "panstart") {
                        pannedX = 0;
                        pannedY = 0;
                    }

                    instance.panBy(
                            {
                                x: e.deltaX - pannedX,
                                y: e.deltaY - pannedY
                            });

                    pannedX = e.deltaX;
                    pannedY = e.deltaY;
                });

                /* Handle pinch */
                this.hammer.on("pinchstart pinchmove", function(e) {

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

    const rawKeywords = document.getElementById("keywords").value.trim();

    if (rawKeywords.length > 0) {
        const newKeywords = rawKeywords.toLowerCase().split(/\s+/);
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

    const id = e.target.parentElement.getAttribute("data-target-id");
    scrollIntoViewById(id);
}

function scrollIntoViewById(id) {

    if (iMap.get(id)[0] !== genoMapId) {

        genoMapId = iMap.get(id)[0];
        document.getElementById("genomap-list").value = genoMapId;

        switchGenoMap(function() {

            const currentPosition = getCurrentPosition(id);
            const targetPosition = getTargetPosition();
            const reverseZoom = 1 / svgPanZoomInstance.getSizes().realZoom;

            svgPanZoomInstance.panBy(
                    {
                        x: targetPosition.x - currentPosition.x,
                        y: targetPosition.y - currentPosition.y
                    });

            svgPanZoomInstance.zoomAtPointBy(reverseZoom, targetPosition);

            selectById(id);
        });

    } else {

        const currentPosition = getCurrentPosition(id);
        const targetPosition = getTargetPosition();

        svgPanZoomInstance.panBy(
                {
                    x: targetPosition.x - currentPosition.x,
                    y: targetPosition.y - currentPosition.y
                });

        selectById(id);
    }
}

function select(e) {

    e.preventDefault();

    selectById(e.target.parentElement.id);
}

function selectPath(e) {
    e.preventDefault();

    /* remove previous link highlights */
    const highlightedLinks = document.getElementsByClassName("link-highlight");
    while (highlightedLinks.length > 0) {
       highlightedLinks[0].classList.remove("link-highlight");
    }

    /* find family ID of the selected element */
    const classes = e.target.classList;
    let familyClass;
    for (let i = 0; i < classes.length; i++) {
        if ( /^fam[0-9]+/.test(classes[i]) ) {
            familyClass = classes[i];
            break;
        }
    }

    /* add highlight class to all elements with the same family ID */
    const paths = document.getElementsByClassName(familyClass);
    for (let i = 0; i < paths.length; i++) {
        paths[i].classList.add("link-highlight");
    }

}

function selectById(id) {

    genoMapSelectedIndividualId = id;

    /* remove previous handles */
    const nodes = genoMapSvg.getElementsByClassName("handle");

    while (nodes[0]) {
        nodes[0].parentNode.removeChild(nodes[0]);
    }

    /* get bounding box */
    const boundingBoxElement = genoMapSvg.getElementById(id + '-bb');
    const x = Number(boundingBoxElement.getAttribute("x"));
    const y = Number(boundingBoxElement.getAttribute("y"));
    const width = Number(boundingBoxElement.getAttribute("width"));
    const height = Number(boundingBoxElement.getAttribute("height"));

    /* create handles */
    const parent = genoMapSvg.getElementById(id);
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

        const entry = e.target.closest(".entry");

        if (entry.id !== searchResultsSelectedIndividualId) {
            searchResultsSelectedIndividualId = entry.id;

        } else {
            searchResultsSelectedIndividualId = null;
        }

        scrollIntoViewById(entry.id.substring(1));
    }
}

function getCurrentPosition(id) {

    const boundingBoxElement = genoMapSvg.getElementById(id + '-bb');
    const boundingRect = boundingBoxElement.getBoundingClientRect();
    let x = boundingRect.left + boundingRect.width / 2;
    let y = boundingRect.top + boundingRect.height / 2;

    const parentBoundingRect = genoMapSvg.getBoundingClientRect();
    const parentX = parentBoundingRect.left;
    const parentY = parentBoundingRect.top;

    x -= parentX;
    y -= parentY;

    return {x: x, y: y};
}

function getTargetPosition() {

    const results = document.getElementById("results");

    return {
        x: (document.documentElement.offsetWidth + results.offsetWidth) / 2,
        y: document.documentElement.offsetHeight / 2
    };
}

function getFullName(individualInfo) {

    const nameArray = new Array();

    for (let i = 1; i <= 3; i++) {
        if (individualInfo[i].length > 0) {
            nameArray.push(individualInfo[i]);
        }
    }

    return nameArray.join(" ");
}

function showResults() {

    const results = document.getElementById("results");
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

    const clearSearchInputButton = document.getElementById("clearSearchInputButton");
    if (clearSearchInputButton.style.display === "none") {
        const searchButton = document.getElementById("searchButton");
        searchButton.style.display = "none";
        clearSearchInputButton.style.display = "block";
    }

    addMoreSearchResultEntries(results);
}

function addMoreSearchResultEntries(resultsElement) {

    let row = 0;
    let addedEntries = 0;

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

    const entry = document.createElement('div');
    entry.classList.add("entry");
    entry.id = "s" + id;
    resultsElement.appendChild(entry);

    const hitArea = document.createElement('div');
    hitArea.classList.add("hitarea");

    hitArea.addEventListener("touchstart", resetDragging);
    hitArea.addEventListener("touchmove", setDragging);
    hitArea.addEventListener("touchend", pinEntry);
    hitArea.addEventListener("mousedown", pinEntry);

    entry.appendChild(hitArea);

    const info = document.createElement('div');
    info.classList.add("info");

    info.addEventListener("touchstart", resetDragging);
    info.addEventListener("touchmove", setDragging);
    info.addEventListener("touchend", showDetail);
    info.addEventListener("mousedown", showDetail);

    entry.appendChild(info);

    const topRow = document.createElement('div');
    topRow.classList.add("top");
    info.appendChild(topRow);

    const name = document.createElement('div');
    const names = [];
    if (value[1].length > 0) {
        names.push(value[1]);
    }
    if (value[2].length > 0) {
        names.push(value[2]);
    }
    name.textContent = names.join(" ").trim();
    name.classList.add("name");
    topRow.appendChild(name);

    const lastname = document.createElement('div');
    lastname.textContent = value[3];
    lastname.classList.add("lastname");
    topRow.appendChild(lastname);

    const birthDate = document.createElement('div');
    birthDate.textContent = value[4];
    birthDate.classList.add("birthdate");
    topRow.appendChild(birthDate);

    const detail = document.createElement('div');
    detail.classList.add("detail");
    if ((value[6] + value[7] + value[8]).length > 0) {

        const fatherId = value[7];
        if (fatherId.length > 0) {
            const fatherName = getFullName(iMap.get(fatherId));
            if (fatherName.length > 0) {
                const father = document.createElement("div");
                father.classList.add("father");
                father.textContent = fatherName;
                detail.appendChild(father);
            }
        }

        const motherId = value[8];
        if (motherId.length > 0) {
            const mother = document.createElement("div");
            mother.classList.add("mother");
            mother.textContent = getFullName(iMap.get(motherId));
            detail.appendChild(mother);
        }

        const delimitedMateIds = value[6];
        if (delimitedMateIds.length > 0) {
            const matesArray = new Array();
            const mateIds = delimitedMateIds.split(",");

            for (let m = 0; m < mateIds.length; m++) {
                const individualInfo = iMap.get(mateIds[m]);
                const fullName = getFullName(individualInfo);
                if (fullName.length > 0) {
                    matesArray.push(fullName);
                }
            }

            const mate = document.createElement("div");
            mate.classList.add("mate");
            mate.textContent = matesArray.join(", ");
            detail.appendChild(mate);
        }
    }

    info.appendChild(detail);

    const bottomRow = document.createElement('div');
    bottomRow.classList.add("bottom");
    info.appendChild(bottomRow);

    const genoMapName = document.createElement('div');
    genoMapName.classList.add("genomapname");
    genoMapName.textContent = genoMapMap.get(value[0]);
    bottomRow.appendChild(genoMapName);
}

function hideResults() {

    const searchButton = document.getElementById("searchButton");
    const clearSearchInputButton = document.getElementById("clearSearchInputButton");

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

        const entry = e.target.closest(".entry");

        pinnedEntry = entry.cloneNode(true);

        pinnedEntry.classList.add("pinned");
        document.getElementById("container").appendChild(pinnedEntry);

        const hitArea = pinnedEntry.getElementsByClassName("hitarea")[0];
        hitArea.addEventListener("touchend", unpinEntry);
        hitArea.addEventListener("mousedown", unpinEntry);

        const info = pinnedEntry.getElementsByClassName("info")[0];
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
    svgPanZoomInstance.panBy({x: 0, y: 0});

    const sizes = svgPanZoomInstance.getSizes();

    const content = document.getElementById("content");
    content.style.width = sizes.viewBox.width + "px";
    content.style.height = sizes.viewBox.height + "px";
}

function restoreSize(e) {

    const content = document.getElementById("content");
    content.style.width = "100vw";
    content.style.height = "100vh";

    svgPanZoomInstance.zoom(zoom);
    svgPanZoomInstance.pan(pan);
}

function matches(value, keywords) {

    let result = true;

    for (let k = 0; k < keywords.length; k++) {
        let found = false;
        for (let i = 1; i < 5; i++) {
            const normalizedValue = value[i].toLowerCase();
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
    const result = location.hash.match("#/sheet/([a-z0-9-]*)");
    return result && genoMapMap.has(result[1]) ? result[1] : null;
}

function switchTheme() {
    theme = (theme === "dark") ? "light" : "dark";
    setTheme();
}

function setTheme() {
    if (theme === "dark") {
        document.documentElement.setAttribute('data-theme', 'dark');
    } else {
        document.documentElement.setAttribute('data-theme', 'light');
    }
    localStorage.setItem("theme", theme);
}