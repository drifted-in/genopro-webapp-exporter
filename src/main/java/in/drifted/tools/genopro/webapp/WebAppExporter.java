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

import in.drifted.tools.genopro.model.GenoMap;
import in.drifted.tools.genopro.model.GenoMapData;
import in.drifted.tools.genopro.webapp.model.RenderOptions;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

public class WebAppExporter {

    private static final String RESOURCE_PATH = "/in/drifted/tools/genopro/webapp/template";

    public static void export(Path reportPath, List<GenoMapData> genoMapDataList, RenderOptions renderOptions) throws IOException {

        ResourceBundle resourceBundle = renderOptions.getResourceBundle();

        try (BufferedWriter writer = Files.newBufferedWriter(reportPath)) {

            String scriptTemplate = getResourceAsString(RESOURCE_PATH + "/template.js");

            scriptTemplate = scriptTemplate.replace("${searchPlaceholder}", resourceBundle.getString("searchPlaceholder"));
            scriptTemplate = scriptTemplate.replace("${searchResults}", resourceBundle.getString("searchResults"));
            scriptTemplate = scriptTemplate.replace("${genoMap}", resourceBundle.getString("genoMap"));
            scriptTemplate = scriptTemplate.replace("${firstName}", resourceBundle.getString("firstName"));
            scriptTemplate = scriptTemplate.replace("${middleName}", resourceBundle.getString("middleName"));
            scriptTemplate = scriptTemplate.replace("${lastName}", resourceBundle.getString("lastName"));
            scriptTemplate = scriptTemplate.replace("${birth}", resourceBundle.getString("birth"));
            scriptTemplate = scriptTemplate.replace("${death}", resourceBundle.getString("death"));
            scriptTemplate = scriptTemplate.replace("${mate}", resourceBundle.getString("mate"));
            scriptTemplate = scriptTemplate.replace("${father}", resourceBundle.getString("father"));
            scriptTemplate = scriptTemplate.replace("${mother}", resourceBundle.getString("mother"));

            StringBuilder svgContentBuilder = new StringBuilder();

            for (GenoMapData genoMapData : genoMapDataList) {

                GenoMap genoMap = genoMapData.getGenoMap();

                if (genoMap.getTitle() != null) {

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    SvgRenderer.render(genoMapData, outputStream, renderOptions);
                    svgContentBuilder.append(new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
                }
            }

            String htmlTemplate = getResourceAsString(RESOURCE_PATH + "/template.html");

            htmlTemplate = htmlTemplate.replace("${content}", svgContentBuilder.toString());
            htmlTemplate = htmlTemplate.replace("${script}", scriptTemplate.replaceAll("\\s+", " "));

            writer.write(htmlTemplate);
        }
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
