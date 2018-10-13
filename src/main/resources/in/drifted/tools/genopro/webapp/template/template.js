/* Copyright (c) 2018 Jan Tošovský */
var svgPanZoomInstance;
var genoMapSvg;
var genoMapId;
var selectedRow;
var keywords;
var genoMapSelect;

document.addEventListener("DOMContentLoaded", init);

document.getElementById("search").addEventListener("click", triggerSearch);

function triggerSearch(e) {
    var input = document.getElementsByName("search")[0];
    keywords = input.value.trim().toLowerCase().split(" ");
    showDialog();
}

document.getElementsByName("search")[0].addEventListener("keydown", function (e) {
    e = e || window.event;
    if (e.keyCode == 13) {
        var elem = e.srcElement || e.target;
        keywords = elem.value.trim().toLowerCase().split(" ");
        showDialog();
    }
});

var individuals = document.getElementsByClassName("individual-active-area");
for (var i = 0; i < individuals.length; i++) {
    individuals[i].addEventListener("mousedown", select);
}

var hyperlinks = document.getElementsByClassName("individual-label-hyperlink");
for (var i = 0; i < hyperlinks.length; i++) {
    hyperlinks[i].addEventListener("mousedown", scrollIntoView);
}

function init(e) {

    genoMapSelect = document.getElementById("genomap-list");

    genoMapMap.forEach(function (value, key, map) {
        var genoMapSelectItem = document.createElement("option");
        genoMapSelectItem.value = key;
        genoMapSelectItem.text = value;
        genoMapSelect.appendChild(genoMapSelectItem);
    });

    genoMapId = genoMapMap.keys().next().value;
    switchGenoMap();
    svgPanZoomInstance.zoomBy(1 / svgPanZoomInstance.getSizes().realZoom);

    genoMapSelect.addEventListener("change", onGenoMapSelectChange);

    document.getElementsByName("search")[0].placeholder = "${searchPlaceholder}";

    if (typeof attributionBody !== "undefined") {
        document.getElementById("attribution").innerHTML = attributionBody;
    }
}

function onGenoMapSelectChange(e) {
    genoMapId = e.target.value;
    switchGenoMap();
    svgPanZoomInstance.zoomBy(1 / svgPanZoomInstance.getSizes().realZoom);
}

function switchGenoMap() {

    if (svgPanZoomInstance) {
        svgPanZoomInstance.destroy();
        genoMapSvg.style.display = "none";
    }

    genoMapSvg = document.getElementById(genoMapId);
    genoMapSvg.style.display = "block";
    svgPanZoomInstance = svgPanZoom(genoMapSvg, {
        zoomScaleSensitivity: 0.25,
        minZoom: 0.1,
        maxZoom: 20,
        dblClickZoomEnabled: false
    });
}

function scrollIntoView(e) {
    var id = e.target.parentElement.getAttribute("data-target-id");
    scrollIntoViewById(id);
}

function scrollIntoViewById(id) {

    if (iMap.get(id)[0] != genoMapId) {

        genoMapId = iMap.get(id)[0];
        genoMapSelect.value = genoMapId;
        switchGenoMap();

        var currentPosition = getCurrentPosition(id);
        var targetPosition = getTargetPosition();

        svgPanZoomInstance.panBy({ x: targetPosition.x - currentPosition.x, y: targetPosition.y - currentPosition.y });
        svgPanZoomInstance.zoomAtPointBy(1 / svgPanZoomInstance.getSizes().realZoom, targetPosition);

    } else {
        var currentPosition = getCurrentPosition(id);
        var targetPosition = getTargetPosition();

        svgPanZoomInstance.panBy({ x: targetPosition.x - currentPosition.x, y: targetPosition.y - currentPosition.y });
    }

    selectById(id);
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

    var documentElement = document.documentElement;

    if (jsPanel.getPanels().length > 0) {

        var panel = jsPanel.getPanels()[0];

        var top = panel.offsetTop;
        var right = documentElement.offsetWidth - (panel.offsetLeft + panel.offsetWidth);
        var bottom = documentElement.offsetHeight - (panel.offsetTop + panel.offsetHeight);
        var left = panel.offsetLeft;

        var max = Math.max(top, right, bottom, left);

        if (max == top) {
            x = documentElement.offsetWidth / 2;
            y = top / 2;
        } else if (max == left) {
            x = left / 2;
            y = documentElement.offsetHeight / 2;
        } else if (max == bottom) {
            x = documentElement.offsetWidth / 2;
            y = documentElement.offsetHeight - bottom / 2;
        } else {
            x = documentElement.offsetWidth - right / 2;
            y = documentElement.offsetHeight / 2;
        }

    } else {
        x = documentElement.offsetWidth / 2;
        y = documentElement.offsetHeight / 2;
    }

    return { x: x, y: y };
}

function select(e) {
    selectById(e.target.parentElement.id);
}

function selectById(id) {

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

function createIndividualTable() {

    table = document.createElement('table');
    var tableHeader = document.createElement('thead');
    table.appendChild(tableHeader);
    var tableHeaderRow = document.createElement('tr');
    tableHeader.appendChild(tableHeaderRow);
    var headers = ["${genoMap}", "${firstName}", "${middleName}", "${lastName}", "${birth}", "${death}", "${mate}", "${father}", "${mother}"];
    for (var i = 0; i < headers.length; i++) {
        var tableHeaderCell = document.createElement('th');
        tableHeaderCell.textContent = headers[i];
        tableHeaderRow.appendChild(tableHeaderCell);
    }
    /* last row */
    tableHeaderRow.appendChild(document.createElement('th'));

    var tableBody = document.createElement('tbody');
    table.appendChild(tableBody);

    iMap.forEach(function (value, key, map) {
        var tableBodyRow = document.createElement('tr');
        tableBody.appendChild(tableBodyRow);
        tableBodyRow.addEventListener("mousedown", selectRow);

        if (matches(value, keywords)) {
            for (var i = 0; i < headers.length; i++) {
                var tableBodyCell = document.createElement('td');
                if (value[i].length > 0) {
                    if (i == 0) {
                        tableBodyCell.id = key;
                        tableBodyCell.textContent = genoMapMap.get(value[i]);

                    } else if (i < 6) {
                        /* direct copies */
                        tableBodyCell.textContent = value[i];

                    } else if (i == 6) {
                        /* multiple values */
                        var mateIds = value[i].split(",");
                        var matesArray = new Array();
                        for (var m = 0; m < mateIds.length; m++) {
                            console.log();
                            var individualInfo = map.get(mateIds[m]);
                            matesArray.push(getFullName(individualInfo));
                        }
                        tableBodyCell.textContent = "[" + matesArray.length + "] " + matesArray.join(", ");

                    } else {
                        /* single value */
                        if (i < headers.length) {
                            var individualInfo = map.get(value[i]);
                            tableBodyCell.textContent = getFullName(individualInfo);
                        }
                    }
                }
                tableBodyRow.appendChild(tableBodyCell);
            }
            tableBodyRow.appendChild(document.createElement('td'));
        }
    });

    new ResizableTable(table, 1000, [150, 80, 40, 100, 80, 80, 120, 120, 120], document);
    this.content.append(table);
}

function selectRow(e) {

    e.target.parentNode.classList.add("selected");

    if (selectedRow !== undefined && selectedRow !== this) {
        selectedRow.classList.remove("selected");
    }
    selectedRow = this;

    scrollIntoViewById(this.cells[0].id);
}

function getFullName(individualInfo) {

    var nameArray = new Array();

    for (var i = 1; i <= 3; i++) {
        if (individualInfo[i].length != -1) {
            nameArray.push(individualInfo[i]);
        }
    }

    return nameArray.join(" ");
}

function matches(value, keywords) {

    var result = true;

    for (var k = 0; k < keywords.length; k++) {
        var found = false;
        for (var i = 1; i < 5; i++) {
            var normalizedValue = value[i].toLowerCase();
            if (normalizedValue.indexOf(keywords[k]) != -1) {
                found = true;
                break;
            }
        }
        result &= found;
    }

    return result;
}

function showDialog() {
    /* close existing dialog if present */
    for (var i = 0; i < jsPanel.getPanels().length; i++) {
        jsPanel.getPanels()[i].close();
    }
    jsPanel.create({
        theme: 'dodgerblue',
        headerTitle: '${searchResults}',
        position: 'center-top 0 30',
        contentSize: '900 500',
        contentOverflow: 'auto',
        content: createIndividualTable,
        headerControls: {
            smallify: "remove",
            minimize: "remove",
            maximize: "remove"
        },
    });
}
