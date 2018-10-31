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
package in.drifted.tools.genopro.webapp;

import in.drifted.tools.genopro.model.DocumentInfo;
import in.drifted.tools.genopro.model.GenoMap;
import in.drifted.tools.genopro.model.GenoMapData;
import in.drifted.tools.genopro.webapp.model.RenderOptions;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

public class WebAppExporter {

    private static final String RESOURCE_PATH = "/in/drifted/tools/genopro/webapp/template";
    private static final String HTML_TEMPLATE_RESOURCE_PATH = RESOURCE_PATH + "/template.html";
    private static final String SCRIPT_TEMPLATE_RESOURCE_PATH = RESOURCE_PATH + "/template.js";
    private static final String[] SCRIPT_TEMPLATE_PLACEHOLDERS = {
        "searchPlaceholder", "searchResults", "genoMap", "firstName", "middleName", "lastName",
        "birth", "death", "mate", "father", "mother", "alertDynamicOnFileSystem"
    };

    public static void export(Path reportPath, DocumentInfo documentInfo, List<GenoMapData> genoMapDataList, RenderOptions renderOptions) throws IOException {

        for (GenoMapData genoMapData : genoMapDataList) {

            GenoMap genoMap = genoMapData.getGenoMap();

            if (genoMap.getTitle() != null) {
                try (OutputStream outputStream = Files.newOutputStream(reportPath.getParent().resolve(genoMap.getId() + ".svg"))) {
                    SvgRenderer.render(genoMapData, outputStream, renderOptions);
                }
            }
        }

        String content;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            SvgRenderer.render(genoMapDataList.iterator().next(), outputStream, renderOptions);
            content = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }

        String script = getScript(SCRIPT_TEMPLATE_RESOURCE_PATH, renderOptions.getResourceBundle());
        script = script.replace("\"${dynamic}\"", "true");

        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put("title", documentInfo.getTitle());
        placeholderMap.put("content", content);
        placeholderMap.put("script", script.replaceAll("\\s+", " "));

        String html = getHtml(HTML_TEMPLATE_RESOURCE_PATH, placeholderMap);

        try (BufferedWriter writer = Files.newBufferedWriter(reportPath)) {
            writer.write(html);
        }
    }

    public static void exportAsStaticPage(Path reportPath, DocumentInfo documentInfo, List<GenoMapData> genoMapDataList, RenderOptions renderOptions) throws IOException {

        StringBuilder svgContentBuilder = new StringBuilder();

        for (GenoMapData genoMapData : genoMapDataList) {

            GenoMap genoMap = genoMapData.getGenoMap();

            if (genoMap.getTitle() != null) {

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                SvgRenderer.render(genoMapData, outputStream, renderOptions);
                svgContentBuilder.append(new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
            }
        }

        String script = getScript(SCRIPT_TEMPLATE_RESOURCE_PATH, renderOptions.getResourceBundle());
        script = script.replace("\"${dynamic}\"", "false");

        Map<String, String> placeholderMap = new HashMap<>();
        placeholderMap.put("title", documentInfo.getTitle());
        placeholderMap.put("content", svgContentBuilder.toString());
        placeholderMap.put("script", script.replaceAll("\\s+", " "));

        String html = getHtml(HTML_TEMPLATE_RESOURCE_PATH, placeholderMap);

        try (BufferedWriter writer = Files.newBufferedWriter(reportPath)) {
            writer.write(html);
        }
    }

    private static String getHtml(String resourcePath, Map<String, String> placeholderMap) throws IOException {

        String html = getResourceAsString(resourcePath);

        for (Entry<String, String> entry : placeholderMap.entrySet()) {
            html = html.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        return html;
    }

    private static String getScript(String resourcePath, ResourceBundle resourceBundle) throws IOException {

        String script = getResourceAsString(resourcePath);

        for (String placeholder : SCRIPT_TEMPLATE_PLACEHOLDERS) {
            script = script.replace("${" + placeholder + "}", resourceBundle.getString(placeholder));
        }

        return script;
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
