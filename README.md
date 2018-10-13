# GenoPro® WebApp exporter

## Motivation

[GenoPro®](https://www.genopro.com/) is one of the best software for drawing family trees and genograms.
If you need to share your family trees with someone else, you can:
1. share your source GNO file, which can be opened even with the free GenoPro version
2. export your data as a set of HTML pages

For my use case I'd rather share my data in the form of a tiny web app, which would offer:
- panning and zooming of visual family trees
- navigating between tree fragment sheets (GenoMaps) via hyperlinks
- displaying search results in a table with basic personal details
- highlighting the person in the family tree if selected in search result table
- anonymizing data related to living individuals

So I finally created a custom exporter. See sample output at http://drifted.in/other/rodokmen-tosovskych/.

![](http://drifted.in/other/genopro-webapp-exporter/web-app.png)

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
    2. Go to File|Properties and select Font tab.
    3. Select Open Sans font.
       ![](http://drifted.in/other/genopro-webapp-exporter/genopro-font.png)
    4. Save the document.

## Data preparation

1. Select GenoMaps for export
    1. Go to File|Properties.
    2. Enter Report Title for any GenoMap which has to be exported. Keep in mind it has to be unique.
    3. Use Alt+Up/Down keys to change the GenMaps order if needed.

2. Review data
    1. Ensure deceased individuals have Deceased flag checked. As anonymization is enabled by default,
       these individuals would be filtered out from the output.

## Export
1. Create output folder, e.g. `C:\family-tree-app`.

2. Open console (on Windows `cmd.exe`).

3. Type `java -jar genopro-webapp-exporter.jar -in:"C:\family-tree.gno" -out:"C:\family-tree-app"` and press enter.

4. All generated files can be found in the output folder.

## Final steps

1. Download additional [resources](http://drifted.in/other/genopro-webapp-exporter/resources.zip) and unzip it into the output folder:
    - `res/style.css` - styles for entire web app
    - `res/svg-pan-zoom.min.js` - 3rd party library for panning and zooming SVG files
    - `res/jspanel.min.js` - 3rd party library for dialogs
    - `res/jspanel.min.css` - 3rd party styles for dialogs
    - `res/resizable-table.js` - library making table columns resizable
    - `res/OpenSans-Regular-webfont.woff` - Open Sans font in the compact form for online use
    - `attribution.js` - optional attribution
    - `analytics.js` - optional analytics

2. Alter attribution:
   Attribution is displayed in bottom right corner. It can contain any HTML code.
   It just need to be put to the `attributionBody` variable inside `attribution.js` file:

   `var attributionBody = "Copyright © 2018 Jan Tošovský";`

   or more complex:

   `var attributionBody = "<span xmlns:cc='http://creativecommons.org/ns#'><a rel='cc:attributionURL' property='cc:attributionName' href='https://github.com/jan-tosovsky-cz/rodokmen-tosovskych'>Rodokmen Tošovských</a> by Jan Tošovský is licensed under a <a rel='license' href='http://creativecommons.org/licenses/by-nc-sa/4.0/'>CC-BY-SA</a> license</span>";`

3. Alter analytics:
   If you are interested how many users visited your app, you can put your analytics code into `analytics.js` file.

4. Test the web application locally.

5. Copy the web application to the server.
