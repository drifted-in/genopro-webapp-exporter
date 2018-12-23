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
import in.drifted.tools.genopro.model.DocumentInfo;
import in.drifted.tools.genopro.model.GenoMapData;
import in.drifted.tools.genopro.webapp.model.GeneratingOptions;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.w3c.dom.Document;

public class App {

    private static final String PARAM_INPUT_PATH = "-in";
    private static final String PARAM_OUTPUT_FOLDER_PATH = "-out";
    private static final String PARAM_RELATIVE_APP_URL = "-relativeAppUrl";
    private static final String PARAM_MODE = "-mode";
    private static final String PARAM_LANGUAGE = "-lang";
    private static final String PARAM_ANONYMIZED_YEARS = "-anonymizedYears";
    private static final String PARAM_DATE_PATTERN = "-datePattern";
    private static final String PARAM_FONT_FAMILY = "-fontFamily";
    private static final String PARAM_RELATIVE_FONT_PATH = "-relativeFontPath";
    private static final String PARAM_GA_TRACKING_ID = "-gaTrackingId";

    private static final String DEFAULT_MODE = "dynamic";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final int DEFAULT_ANONYMIZED_YEARS = 100;
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    private static final String DEFAULT_FONT_FAMILY = "Open Sans";
    private static final String DEFAULT_RELATIVE_FONT_PATH = "res/OpenSans-Regular-webfont.woff";

    private static final String RESOURCE_BUNDLE_PATH = "in/drifted/tools/genopro/webapp/messages";

    public static void main(String[] args) throws IOException {

        Map<String, String> passedValuesMap = new HashMap<>();

        for (String arg : args) {
            int index = arg.indexOf(":");
            if (index > 0 && index < arg.length() - 1) {
                passedValuesMap.put(arg.substring(0, index), arg.substring(index + 1));
            }
        }

        String mode = passedValuesMap.getOrDefault(PARAM_MODE, DEFAULT_MODE);
        boolean dynamic = mode.equals("dynamic");

        if (passedValuesMap.containsKey(PARAM_INPUT_PATH) && passedValuesMap.containsKey(PARAM_OUTPUT_FOLDER_PATH) && (!dynamic || (dynamic && passedValuesMap.containsKey(PARAM_RELATIVE_APP_URL)))) {

            Path inputPath = Paths.get(passedValuesMap.get(PARAM_INPUT_PATH));
            Path outputFolderFolder = Paths.get(passedValuesMap.get(PARAM_OUTPUT_FOLDER_PATH));

            String language = passedValuesMap.getOrDefault(PARAM_LANGUAGE, DEFAULT_LANGUAGE);
            Locale locale = new Locale(language);
            ResourceBundle resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_PATH, locale);

            int anonymizedYears = DEFAULT_ANONYMIZED_YEARS;

            if (passedValuesMap.containsKey(PARAM_ANONYMIZED_YEARS)) {
                anonymizedYears = Integer.parseInt(passedValuesMap.get(PARAM_ANONYMIZED_YEARS));
            }

            String datePattern = passedValuesMap.getOrDefault(PARAM_DATE_PATTERN, DEFAULT_DATE_PATTERN);
            String fontFamily = passedValuesMap.getOrDefault(PARAM_FONT_FAMILY, DEFAULT_FONT_FAMILY);
            String relativeFontPath = passedValuesMap.getOrDefault(PARAM_RELATIVE_FONT_PATH, DEFAULT_RELATIVE_FONT_PATH);

            DateFormatter dateFormatter = new DateFormatter(datePattern, locale, new HashMap<>());
            AgeFormatter ageFormatter = new BasicAgeFormatter(resourceBundle);

            Map<String, String> additionalOptionMap = new HashMap<>();
            additionalOptionMap.put("relativeFontPath", relativeFontPath);
            if (passedValuesMap.containsKey(PARAM_GA_TRACKING_ID)) {
                additionalOptionMap.put("gaTrackingId", passedValuesMap.get(PARAM_GA_TRACKING_ID));
            }

            GeneratingOptions generatingOptions = new GeneratingOptions(locale, resourceBundle, fontFamily, dateFormatter, ageFormatter, additionalOptionMap);

            Path individualsPath = outputFolderFolder.resolve("individuals.js");
            Path genomapsPath = outputFolderFolder.resolve("genomaps.js");
            Path reportPath = outputFolderFolder.resolve("index.html");

            Document document = DataParser.getDocument(inputPath);
            DocumentInfo documentInfo = DataParser.getDocumentInfo(document);
            LocalDate anonymizedSinceLocalDate = (anonymizedYears < 0) ? null : (anonymizedYears == 0) ? LocalDate.now() : LocalDate.now().minus(Period.ofYears(anonymizedYears));
            List<GenoMapData> genoMapDataList = DataUtil.getGenoMapDataList(document, anonymizedSinceLocalDate);

            GenoMapsExporter.export(genomapsPath, genoMapDataList);
            IndividualsExporter.export(individualsPath, genoMapDataList, dateFormatter);

            if (dynamic) {
                generatingOptions.getAdditionalOptionsMap().put("relativeAppUrl", passedValuesMap.get(PARAM_RELATIVE_APP_URL));
                WebAppExporter.export(reportPath, documentInfo, genoMapDataList, generatingOptions);

            } else {
                WebAppExporter.exportAsStaticPage(reportPath, documentInfo, genoMapDataList, generatingOptions);
            }

        } else {

            System.out.println("Specify at least: \n"
                    + "         - input GenoPro file path (-in:) \n"
                    + "         - output folder (-out:) \n"
                    + "         - relative app URL (-relativeAppUrl:) \n\n"
                    + "Usage: java -jar genopro-webapp-exporter.jar \n"
                    + "         -in:C:\\family-tree.gno \n"
                    + "         -out:C:\\family-tree \n"
                    + "         -relativeAppUrl:/family-tree \n"
                    + "        [-mode:dynamic] \n"
                    + "        [-lang:en] \n"
                    + "        [-anonymizedYears:100] \n"
                    + "        [-datePattern:yyyy-MM-dd] \n"
                    + "        [-fontFamily:\"Open Sans\"] \n"
                    + "        [-relativeFontPath:\"res/OpenSans-Regular-webfont.woff\"] \n"
                    + "        [-gaTrackingId:<empty>]"
            );
        }
    }
}
