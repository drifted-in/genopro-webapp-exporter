# GenoPro® WebApp exporter

## Motivation

[GenoPro®](https://www.genopro.com/) is one of the best software for drawing family trees and genograms.
If you need to share your family trees with someone else, you can:
1. share your source GNO file, which can be opened even with the free GenoPro version
2. export your data as a set of HTML pages

I missed several features so I created a custom exporter.

## Examples

See sample output at https://drifted.in/other/rodokmen-tosovskych/.

### Light mode
<p float="left">
<img src="http://drifted.in/other/genopro-webapp-exporter/initial-view-light-2021-05.png" height="420">
<img src="http://drifted.in/other/genopro-webapp-exporter/search-results-light-2021-05.png" height="420">
<img src="http://drifted.in/other/genopro-webapp-exporter/pinned-individual-light-2021-05.png" height="420">
</p>

### Dark mode
<p float="left">
<img src="http://drifted.in/other/genopro-webapp-exporter/initial-view-dark-2021-05.png" height="420">
<img src="http://drifted.in/other/genopro-webapp-exporter/search-results-dark-2021-05.png" height="420">
<img src="http://drifted.in/other/genopro-webapp-exporter/pinned-individual-dark-2021-06.png" height="420">
</p>

## Highlights
- a mobile-ready web app which, after initial loading, doesn't need internet connection
- enhanced control of what assets are exported
- enhanced privacy policy (i.e. excluding data related to living individuals)
- instant search
- highlighting DNA inheritance (experimental)

## Limitations
- only basic individual data is exported
- the missing support for custom formatting (gender symbols, family lines, ...)
- the missing support for picture mode
- the missing support for genograms, medical symbols, emotional or social relationships

### Offline mode

If the web app is shown in [modern](https://caniuse.com/#feat=serviceworkers) web browsers, it is automatically cached so it can be served later on even without internet connection.

### Mobile App

The web app conforms to the [Progressive Web Apps](https://en.wikipedia.org/wiki/Progressive_Web_Apps) (PWA) standard which means it can be installed as a mobile app to the home screen if shown in the [supported browser](https://caniuse.com/#feat=web-app-manifest). 

### Managing assets to export

Only genomaps with a report title (can be set via File | Properties | GenoMaps) are exported. So you can easily control which parts of your tree will be included. 
If some labels are private, you can set a dedicated background to them. This color can be then set as a filter applied when exporting items.

### Privacy policy

For living individuals it is possible to specify policy for anonymizing their data. It is controlled by the age. Individuals younger than specified limit are excluded. If the limit equals to zero, individuals are shown, but without birthdates. If the limit is negative, the anonymization is deactivated.

### Instant search

Just type few characters and inspect all matches sorted by the birthdate. By activating entries the corresponding genomap is loaded and the selected individual highlighted.

### Highlighting DNA inheritance

If there are variations in DNA results (especially Y-STR marker values) of distant relatives and every such variation is encoded into a color of the gender symbol, you can visualize projected inheritance of these mutations using the corresponding color. If the estimate is ambiguous, colors are combined in the form of dashed line.

See sample output at https://drifted.in/other/rodokmen-tosovskych-gap/.

<img src="https://drifted.in/other/genopro-webapp-exporter/highlight-y-dna-variations.png" width="400px">

## Do not panic

While the description below looks complex, lot of things needs to be set once.
Any subsequent exports are quite straightforward.

## Prerequisites

### Java

This exporter is written in Java. To execute it you need a Java Virtual Machine (JDK 8.0 or greater). Installers can be found at https://www.java.com/download/.

### Exporter

You can download the pre-built exporter at http://drifted.in/other/genopro-webapp-exporter/app.zip.

For custom builds just clone the project, open it in your favorite Java IDE and build it.

### Font

As the final web app is multiplatform, it is necessary to switch default (MS Windows specific)
GenoPro font to a different one, which can be either linked or freely distributed together with the web app.

It is recommended to use Open Sans font as it is preconfigured so minimal additional settings is required.

1. Download the font
    1. Go to https://fonts.google.com/specimen/Open+Sans?selection.family=Open+Sans
    2. Click the Download family button at the top right corner.

2. Install the font
    1. Once the ZIP archive is downloaded, it has to be unzipped. There are multiple fonts of various styles
       in the archive, but it is sufficient to unzip just `OpenSans-Regular.ttf` (to any location).
    2. Navigate that file in e.g. Windows explorer and via the right click context menu select Install for all users item.

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

3. Alter attribution:

   Attribution is displayed in bottom right corner. It can contain any HTML code.
   It just need to be put to the `attributionBody` variable inside `attribution.js` file:

   `var attributionBody = "Copyright © 2018 Jan Tošovský";`

   or more complex:

   `var attributionBody = "<span xmlns:cc='http://creativecommons.org/ns#'><a rel='cc:attributionURL' property='cc:attributionName' href='https://github.com/jan-tosovsky-cz/rodokmen-tosovskych'>Rodokmen Tošovských</a> by Jan Tošovský is licensed under a <a rel='license' href='http://creativecommons.org/licenses/by-nc-sa/4.0/'>CC-BY-SA</a> license</span>";`

## Export

1. Open console (on Windows `cmd.exe`).

2. Generate static app which can be tested directly on your local computer:

   Type `java -jar C:\genopro-webapp-exporter.jar -in:"C:\family-tree.gno" -out:"C:\family-tree" -mode:static` and press enter.

   Several other options can be used:
    - `locale` - The user interface locale, e.g. en-US, pt-BR. Currently only `en` and `cs` values are supported. If new languages are needed,
      just translate [all the words](https://github.com/drifted-in/genopro-webapp-exporter/blob/master/src/main/resources/in/drifted/tools/genopro/webapp/messages_en.properties) and send them to me via email.
    - `anonymizedYears` - Years before present used for anonymizing data (hiding names and dates). If the zero value is used, only names of living individuals are shown, not their birthdates. A negative value deactivates anonymization completely.
    - `datePattern` - The date pattern consisting of `d`, `M`, `y` symbols (for day, month, year) separated by various punctuation. See the complete [reference](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html).
      To ensure the formatted date doesn't overflow the bounding box the pattern should match the format used in your document.
    - `fontFamily` - If default Open Sans font doesn't suit your needs, it can be overridden by this parameter. In this case the `relativeFontPath` option becomes mandatory. If the font family contains a space, the value needs to be enclosed in quotes. 
    - `relativeFontPath` - The relative path to the custom font in the WOFF format.
    - `unsupportedLabelHexColorSet` - For suppressing some private labels you can set a specific background color to them and then specify this color or comma delimited set of colors in curly braces, e.g. {#FF0000,#C8C8FF}.
    - `monochromeLabels` - If the label color system has no benefit for end-user, it is better to set this attribute to 1. It means colors will be ignored in favor of default styles and switched accordingly when activating the light/dark theme.  
    - `gaTrackingId` - For analyzing the web app traffic via Google Analytics just specify your tracking ID.

    Example: `java -jar C:\genopro-webapp-exporter.jar -in:"C:\family-tree.gno" -out:"C:\family-tree" -mode:static -locale:cs -anonymizedYears:120 -datePattern:dd.MM.yyyy -fontFamily:Muli -relativeFontPath:res/Muli-Regular-webfont.woff -gaTrackingId:UA-00000000-1`

3. Once everything looks good, generate dynamic pages:

   Type `java -jar genopro-webapp-exporter.jar -in:"C:\family-tree.gno" -out:"C:\family-tree" -relativeAppUrl:/family-tree` and press enter.

   The last parameter represents the URL fragment relative to the server origin and must match the target app location.
   If your app URL is e.g. https://www.server.com/doe/family-tree/index.html, the `relativeAppUrl` is `/doe/family-tree`.

   You can use same options as in case of static app.

4. Copy the web application to the server.
