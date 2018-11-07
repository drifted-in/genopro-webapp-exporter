# GenoPro® WebApp exporter

## Motivation

[GenoPro®](https://www.genopro.com/) is one of the best software for drawing family trees and genograms.
If you need to share your family trees with someone else, you can:
1. share your source GNO file, which can be opened even with the free GenoPro version
2. export your data as a set of HTML pages

For my use case I'd rather share my data in the form of a tiny web app, which would offer:
- panning and zooming of visual family trees
- selecting tree fragment sheets (GenoMaps) from the list
- instant searching
- showing basic/extended info for the selected person
- highlighting the person in the family tree
- anonymizing data related to living individuals

So I finally created a custom exporter. See sample output at http://drifted.in/other/rodokmen-tosovskych/.

<p float="left">
<img src="http://drifted.in/other/genopro-webapp-exporter/initial-view.png" width="280">
<img src="http://drifted.in/other/genopro-webapp-exporter/search-results.png" width="280">
<img src="http://drifted.in/other/genopro-webapp-exporter/pinned-individual.png" width="280">
</p>

## Do not panic

While the next description looks complex, lot of things needs to be set once.
Any subsequent exports are quite straightforward.

## Prerequisites

### Java

This export is written in Java. To execute it you need Java Virtual Machine. Installers can be found at https://www.java.com/download/.

### Exporter

You can download the pre-built exporter at http://drifted.in/other/genopro-webapp-exporter/app.zip.

For custom builds just clone the project, open it in your favorite IDE and build it.

### Font

As the final web app is multiplatform, it is necessary to switch default (MS Windows specific)
GenoPro font to a different one, which can be either linked or freely distributed together with the web app.

It is recommended to use Open Sans font as it is preconfigured so minimal additional settings is required.

1. Download the font
    1. Go to https://fonts.google.com/specimen/Open+Sans?selection.family=Open+Sans
    2. Click the Select this font button at the top right corner.
    3. Click the black bar at the bottom of the page (expand that minimized window).
    4. Click the download icon.
       ![](http://drifted.in/other/genopro-webapp-exporter/download-font.png)

2. Install the font
    1. Once the archive is downloaded, it has to be unzipped. There are multiple fonts of various styles
       in the archive, but it is sufficient to unzip just `OpenSans-Regular.ttf` (to any location).
    2. Navigate that file in e.g. Windows explorer and via the right click context menu select Install item.

3. Apply the font
    1. In GenoPro open your document.
    2. Go to File | Properties and select Font tab.
    3. Select Open Sans font.
       ![](http://drifted.in/other/genopro-webapp-exporter/genopro-font.png)
    4. Save the document.

## Data preparation

1. Select GenoMaps for export
    1. Go to File | Properties.
    2. In the GenoMaps tab enter Report Title for any GenoMap which has to be exported.
       Keep in mind it has to be unique. Use Alt+Up/Down keys to change the GenMaps order if needed.
    3. In the Document tab enter Title.

2. Review data
    1. Ensure deceased individuals have Deceased flag checked. As anonymization is enabled by default,
       these individuals would be filtered out from the output.

## Output folder preparation

1. Create output folder, e.g. `C:\family-tree`.

2. Download additional [resources](http://drifted.in/other/genopro-webapp-exporter/resources.zip) and unzip it into the output folder:

   App icons for various devices:
    - `favicon/android-chrome-192x192.png`
    - `favicon/android-chrome-512x512.png`
    - `favicon/apple-touch-icon.png`
    - `favicon/favicon-16x16.png`
    - `favicon/favicon-32x32.png`
    - `favicon/favicon.ico`

   Third party libraries:
    - `res/hammer.min.js` (mobile touch events)
    - `res/svg-pan-zoom.min.js` (panning and zooming SVG files)

   Web font:
    - `res/OpenSans-Regular-webfont.woff`

   Auxiliary scripts:
    - `attribution.js` - optional attribution
    - `analytics.js` - optional analytics

3. Alter attribution:

   Attribution is displayed in bottom right corner. It can contain any HTML code.
   It just need to be put to the `attributionBody` variable inside `attribution.js` file:

   `var attributionBody = "Copyright © 2018 Jan Tošovský";`

   or more complex:

   `var attributionBody = "<span xmlns:cc='http://creativecommons.org/ns#'><a rel='cc:attributionURL' property='cc:attributionName' href='https://github.com/jan-tosovsky-cz/rodokmen-tosovskych'>Rodokmen Tošovských</a> by Jan Tošovský is licensed under a <a rel='license' href='http://creativecommons.org/licenses/by-nc-sa/4.0/'>CC-BY-SA</a> license</span>";`

3. Alter analytics:

   If you are interested how many users visited your app, you can put your analytics code into `analytics.js` file.

## Export

1. Open console (on Windows `cmd.exe`).

2. Generate static app which can be tested directly on your local computer:

   Type `java -jar C:\genopro-webapp-exporter.jar -in:"C:\family-tree.gno" -out:"C:\family-tree" -mode:static` and press enter.

3. Once everything looks good, generate dynamic pages:

   Type `java -jar genopro-webapp-exporter.jar -in:"C:\family-tree.gno" -out:"C:\family-tree" -relativeAppUrl:/family-tree` and press enter.

   The last parameter represents the URL fragment relative to the server origin and must match the target app location.
   If your app URL is e.g. https://www.server.com/doe/family-tree/index.html, the `relativeAppUrl` is `/doe/family-tree`.

4. Copy the web application to the server.
