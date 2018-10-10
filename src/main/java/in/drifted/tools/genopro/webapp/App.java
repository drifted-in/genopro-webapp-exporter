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

import in.drifted.tools.genopro.DataParser;
import in.drifted.tools.genopro.DataUtil;
import in.drifted.tools.genopro.model.AgeFormatter;
import in.drifted.tools.genopro.model.BasicAgeFormatter;
import in.drifted.tools.genopro.model.DateFormatter;
import in.drifted.tools.genopro.model.GenoMapData;
import in.drifted.tools.genopro.webapp.model.RenderOptions;
import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.w3c.dom.Document;

public class App {

    private static final String PARAM_INPUT_PATH = "-in";
    private static final String PARAM_OUTPUT_FOLDER_PATH = "-out";
    private static final String PARAM_LANGUAGE = "-lang";
    private static final String PARAM_DATE_PATTERN = "-datePattern";
    private static final String PARAM_FONT_FAMILY = "-fontFamily";
    private static final String PARAM_MAIN_FONT_SIZE_IN_PIXELS = "-mainFontSizePx";
    private static final String PARAM_AGE_FONT_SIZE_IN_PIXELS = "-ageFontSizePx";
    private static final String PARAM_MAIN_LINE_HEIGHT_IN_PIXELS = "-mainLineHeightPx";

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    private static final String DEFAULT_FONT_FAMILY = "Open Sans";
    private static final int DEFAULT_MAIN_FONT_SIZE_IN_PIXELS = 11;
    private static final int DEFAULT_AGE_FONT_SIZE_IN_PIXELS = 9;
    private static final int DEFAULT_MAIN_LINE_HEIGHT_IN_PIXELS = 14;

    private static final String RESOURCE_BUNDLE_PATH = "in/drifted/tools/genopro/webapp/messages";

    public static void main(String[] args) throws IOException {

        Map<String, String> passedValuesMap = new HashMap<>();

        for (String arg : args) {
            int index = arg.indexOf(":");
            if (index > 0 && index < arg.length() - 1) {
                passedValuesMap.put(arg.substring(0, index), arg.substring(index + 1));
            }
        }

        if (passedValuesMap.containsKey(PARAM_INPUT_PATH) && passedValuesMap.containsKey(PARAM_OUTPUT_FOLDER_PATH)) {

            Path inputPath = Paths.get(passedValuesMap.get(PARAM_INPUT_PATH));
            Path outputFolderFolder = Paths.get(passedValuesMap.get(PARAM_OUTPUT_FOLDER_PATH));

            String language = DEFAULT_LANGUAGE;

            if (passedValuesMap.containsKey(PARAM_LANGUAGE)) {
                language = passedValuesMap.get(PARAM_LANGUAGE);
            }

            Locale locale = new Locale(language);
            ResourceBundle resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_PATH, locale);

            String datePattern = DEFAULT_DATE_PATTERN;

            if (passedValuesMap.containsKey(PARAM_DATE_PATTERN)) {
                datePattern = passedValuesMap.get(PARAM_DATE_PATTERN);
            }

            String fontFamily = DEFAULT_FONT_FAMILY;

            if (passedValuesMap.containsKey(PARAM_FONT_FAMILY)) {
                fontFamily = passedValuesMap.get(PARAM_FONT_FAMILY);
            }

            int mainFontSizeInPixels = DEFAULT_MAIN_FONT_SIZE_IN_PIXELS;

            if (passedValuesMap.containsKey(PARAM_MAIN_FONT_SIZE_IN_PIXELS)) {
                mainFontSizeInPixels = Integer.parseInt(passedValuesMap.get(PARAM_MAIN_FONT_SIZE_IN_PIXELS));
            }

            int ageFontSizeInPixels = DEFAULT_AGE_FONT_SIZE_IN_PIXELS;

            if (passedValuesMap.containsKey(PARAM_AGE_FONT_SIZE_IN_PIXELS)) {
                ageFontSizeInPixels = Integer.parseInt(passedValuesMap.get(PARAM_AGE_FONT_SIZE_IN_PIXELS));
            }

            int mainLineHeightInPixels = DEFAULT_MAIN_LINE_HEIGHT_IN_PIXELS;

            if (passedValuesMap.containsKey(PARAM_MAIN_LINE_HEIGHT_IN_PIXELS)) {
                mainLineHeightInPixels = Integer.parseInt(passedValuesMap.get(PARAM_MAIN_LINE_HEIGHT_IN_PIXELS));
            }

            DateFormatter dateFormatter = new DateFormatter(datePattern, new HashMap<>());
            AgeFormatter ageFormatter = new BasicAgeFormatter(resourceBundle);

            FontMetrics mainFontMetrics = new Canvas().getFontMetrics(new Font(fontFamily, Font.PLAIN, mainFontSizeInPixels));
            FontMetrics ageFontMetrics = new Canvas().getFontMetrics(new Font(fontFamily, Font.PLAIN, ageFontSizeInPixels));

            RenderOptions renderOptions = new RenderOptions(locale, resourceBundle, dateFormatter, ageFormatter, mainFontMetrics, ageFontMetrics, mainLineHeightInPixels);

            Path individualsPath = outputFolderFolder.resolve("individuals.js");
            Path genomapsPath = outputFolderFolder.resolve("genomaps.js");
            Path reportPath = outputFolderFolder.resolve("index.html");

            Document document = DataParser.getDocument(inputPath);
            List<GenoMapData> genoMapDataList = DataUtil.getGenoMapDataList(document);

            GenoMapsExporter.export(genomapsPath, genoMapDataList);
            IndividualsExporter.export(individualsPath, genoMapDataList, dateFormatter);
            WebAppExporter.export(reportPath, genoMapDataList, renderOptions);

        } else {

            System.out.println("Specify at least: \n"
                    + "         - input GenoPro file path (-in:)\n"
                    + "         - output folder (-out:)\n\n"
                    + "Usage: java -jar genopro-webapp-exporter.jar \n"
                    + "         -in:C:\\family-tree.gno \n"
                    + "         -out:C:\\family-tree \n"
                    + "        [-lang:en] \n"
                    + "        [-datePattern:yyyy-MM-dd] \n"
                    + "        [-fontFamily:\"Open Sans\"] \n"
                    + "        [-mainFontSizePx:11] \n"
                    + "        [-ageFontSizePx:9] \n"
                    + "        [-mainLineHeightPx:14]"
            );
        }
    }
}
