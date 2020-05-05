/*
 * Copyright (c) 2018 Jan Tošovský <jan.tosovsky.cz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package in.drifted.tools.genopro.webapp.exporter;

import in.drifted.tools.genopro.model.DocumentInfo;
import in.drifted.tools.genopro.model.GenoMap;
import in.drifted.tools.genopro.model.GenoMapData;
import in.drifted.tools.genopro.webapp.exporter.model.GeneratingOptions;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

public class WebAppExporter {

    private static final String RESOURCE_PATH = "/in/drifted/tools/genopro/webapp/exporter/resources/template";

    private static final String MAIN_HTML_TEMPLATE_RESOURCE_PATH = RESOURCE_PATH + "/index.html";
    private static final String MAIN_SCRIPT_TEMPLATE_RESOURCE_PATH = RESOURCE_PATH + "/main.js";
    private static final String CSS_TEMPLATE_RESOURCE_PATH = RESOURCE_PATH + "/style.css";
    private static final String SERVICE_WORKER_TEMPLATE_RESOURCE_PATH = RESOURCE_PATH + "/service-worker.js";
    private static final String MANIFEST_TEMPLATE_RESOURCE_PATH = RESOURCE_PATH + "/manifest.json";
    private static final String GTAG_TEMPLATE_RESOURCE_PATH = RESOURCE_PATH + "/gtag.js";

    private static final String[] MAIN_HTML_TEMPLATE_LOCALIZED_PLACEHOLDERS = {
        "keywords", "search", "clearSearchInput", "selectGenoMap"
    };

    private static final String[] MAIN_SCRIPT_TEMPLATE_LOCALIZED_PLACEHOLDERS = {
        "searchPlaceholder", "genoMap", "firstName", "middleName", "lastName",
        "birth", "death", "mate", "father", "mother", "alertDynamicOnFileSystem"
    };

    public static void export(Path reportPath, DocumentInfo documentInfo, List<GenoMapData> genoMapDataList, GeneratingOptions generatingOptions) throws IOException {

        String relativeAppUrl = generatingOptions.getAdditionalOptionsMap().getOrDefault("relativeAppUrl", "");

        generateGenoMaps(reportPath.getParent(), genoMapDataList, generatingOptions);
        generateMainHtml(reportPath, documentInfo, genoMapDataList, generatingOptions, true);
        generateCss(reportPath.getParent().resolve("style.css"), generatingOptions);
        generateServiceWorker(reportPath.getParent().resolve("service-worker.js"), genoMapDataList, relativeAppUrl);
        generateManifest(reportPath.getParent().resolve("manifest.json"), documentInfo, relativeAppUrl);
    }

    public static void exportAsStaticPage(Path reportPath, DocumentInfo documentInfo, List<GenoMapData> genoMapDataList, GeneratingOptions generatingOptions) throws IOException {

        generateMainHtml(reportPath, documentInfo, genoMapDataList, generatingOptions, false);
        generateCss(reportPath.getParent().resolve("style.css"), generatingOptions);
        generateManifest(reportPath.getParent().resolve("manifest.json"), documentInfo, "");
    }

    private static void generateGenoMaps(Path folderPath, List<GenoMapData> genoMapDataList, GeneratingOptions generatingOptions) throws IOException {

        for (GenoMapData genoMapData : genoMapDataList) {

            GenoMap genoMap = genoMapData.getGenoMap();

            if (genoMap.getTitle() != null) {
                try (OutputStream outputStream = Files.newOutputStream(folderPath.resolve(genoMap.getId() + ".svg"))) {
                    SvgRenderer.render(genoMapData, outputStream, generatingOptions);
                }
            }
        }
    }

    private static void generateMainHtml(Path reportPath, DocumentInfo documentInfo, List<GenoMapData> genoMapDataList, GeneratingOptions generatingOptions, boolean dynamic) throws IOException {

        String content;

        if (dynamic) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                SvgRenderer.render(genoMapDataList.iterator().next(), outputStream, generatingOptions);
                content = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            }

        } else {

            StringBuilder svgContentBuilder = new StringBuilder();

            for (GenoMapData genoMapData : genoMapDataList) {

                GenoMap genoMap = genoMapData.getGenoMap();

                if (genoMap.getTitle() != null) {

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    SvgRenderer.render(genoMapData, outputStream, generatingOptions);
                    svgContentBuilder.append(new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
                }
            }

            content = svgContentBuilder.toString();
        }

        String mainScript = getMainScript(MAIN_SCRIPT_TEMPLATE_RESOURCE_PATH, generatingOptions.getResourceBundle(), dynamic);
        String gtag = getGtag(GTAG_TEMPLATE_RESOURCE_PATH, generatingOptions);

        Map<String, String> placeholderMap = new HashMap<>();

        placeholderMap.put("language", generatingOptions.getLocale().getLanguage());
        placeholderMap.put("title", documentInfo.getTitle());
        placeholderMap.put("description", documentInfo.getDescription());
        placeholderMap.put("content", content);
        placeholderMap.put("script", mainScript.replaceAll("\\s+", " "));
        placeholderMap.put("dynamic", dynamic ? "true" : "false");
        placeholderMap.put("relativeAppUrl", generatingOptions.getAdditionalOptionsMap().getOrDefault("relativeAppUrl", ""));
        placeholderMap.put("gtag", gtag);
        placeholderMap.put("timestamp", "<!-- " + LocalDateTime.now().toString() + " -->");

        String mainHtml = getResourceAsString(MAIN_HTML_TEMPLATE_RESOURCE_PATH);

        for (Entry<String, String> entry : placeholderMap.entrySet()) {
            mainHtml = mainHtml.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        for (String placeholder : MAIN_HTML_TEMPLATE_LOCALIZED_PLACEHOLDERS) {
            mainHtml = mainHtml.replace("${" + placeholder + "}", generatingOptions.getResourceBundle().getString(placeholder));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(reportPath)) {
            writer.write(mainHtml);
        }
    }

    private static void generateCss(Path cssPath, GeneratingOptions generatingOptions) throws IOException {

        Map<String, String> additionalOptionMap = generatingOptions.getAdditionalOptionsMap();

        try (BufferedWriter writer = Files.newBufferedWriter(cssPath)) {

            String css = getResourceAsString(CSS_TEMPLATE_RESOURCE_PATH);
            css = css.replace("${fontFamily}", generatingOptions.getFontFamily());
            css = css.replace("${relativeFontPath}", additionalOptionMap.get("relativeFontPath"));

            writer.write(css);
        }
    }

    private static void generateServiceWorker(Path serviceWorkerPath, List<GenoMapData> genoMapDataList, String relativeAppUrl) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(serviceWorkerPath)) {

            String serviceWorker = getResourceAsString(SERVICE_WORKER_TEMPLATE_RESOURCE_PATH);
            serviceWorker = serviceWorker.replace("${relativeAppUrl}", relativeAppUrl);
            serviceWorker = serviceWorker.replace("${currentCacheId}", LocalDateTime.now().toString());

            List<String> genoMapPathList = new ArrayList<>();

            for (GenoMapData genoMapData : genoMapDataList) {
                genoMapPathList.add("relativeAppUrl + \"/" + genoMapData.getGenoMap().getId() + ".svg\"");
            }

            serviceWorker = serviceWorker.replace("\"${genoMapPathList}\"", String.join(",\n", genoMapPathList));

            writer.write(serviceWorker);
        }
    }

    private static void generateManifest(Path manifestPath, DocumentInfo documentInfo, String relativeAppUrl) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(manifestPath)) {

            String manifest = getResourceAsString(MANIFEST_TEMPLATE_RESOURCE_PATH);
            manifest = manifest.replace("${title}", documentInfo.getTitle());
            manifest = manifest.replace("${relativeAppUrl}", relativeAppUrl);

            writer.write(manifest);
        }
    }

    private static String getMainScript(String resourcePath, ResourceBundle resourceBundle, boolean dynamic) throws IOException {

        String mainScript = getResourceAsString(resourcePath);

        for (String placeholder : MAIN_SCRIPT_TEMPLATE_LOCALIZED_PLACEHOLDERS) {
            mainScript = mainScript.replace("${" + placeholder + "}", resourceBundle.getString(placeholder));
        }

        mainScript = mainScript.replace("\"${dynamic}\"", dynamic ? "true" : "false");

        return mainScript;
    }

    private static String getGtag(String resourcePath, GeneratingOptions generatingOptions) throws IOException {

        String gtag = "";

        String gaTrackingId = generatingOptions.getAdditionalOptionsMap().get("gaTrackingId");

        if (gaTrackingId != null) {
            gtag = getResourceAsString(resourcePath).replace("${gaTrackingId}", gaTrackingId);
        }

        return gtag;
    }

    private static String getResourceAsString(String resourcePath) throws IOException {

        try (
                InputStream inputStream = WebAppExporter.class.getResourceAsStream(resourcePath);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            int pos;
            byte[] data = new byte[16 * 1024];

            while ((pos = inputStream.read(data, 0, data.length)) != -1) {
                outputStream.write(data, 0, pos);
            }

            outputStream.flush();

            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
