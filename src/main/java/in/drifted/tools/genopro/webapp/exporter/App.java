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

import in.drifted.tools.genopro.core.model.Color;
import in.drifted.tools.genopro.core.model.DisplayStyle;
import in.drifted.tools.genopro.core.model.DocumentInfo;
import in.drifted.tools.genopro.core.model.GenoMapData;
import in.drifted.tools.genopro.core.parser.DocumentParser;
import in.drifted.tools.genopro.core.parser.DocumentParserOptions;
import in.drifted.tools.genopro.core.util.DocumentDataUtil;
import in.drifted.tools.genopro.core.util.formatter.AgeFormatter;
import in.drifted.tools.genopro.core.util.formatter.BasicAgeFormatter;
import in.drifted.tools.genopro.core.util.formatter.DateFormatter;
import in.drifted.tools.genopro.webapp.exporter.model.GeneratingOptions;
import in.drifted.tools.genopro.webapp.exporter.model.PedigreeLinksSelectionMode;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.w3c.dom.Document;

public class App {

    private static final String PARAM_INPUT_PATH = "-in";
    private static final String PARAM_OUTPUT_FOLDER_PATH = "-out";
    private static final String PARAM_RELATIVE_APP_URL = "-relativeAppUrl";
    private static final String PARAM_MODE = "-mode";
    private static final String PARAM_LOCALE = "-locale";
    private static final String PARAM_ANONYMIZED_YEARS = "-anonymizedYears";
    private static final String PARAM_DATE_PATTERN = "-datePattern";
    private static final String PARAM_FONT_FAMILY = "-fontFamily";
    private static final String PARAM_RELATIVE_FONT_PATH = "-relativeFontPath";
    private static final String PARAM_UNSUPPORTED_LABEL_HEX_COLOR_SET = "-unsupportedLabelHexColorSet";
    private static final String PARAM_MONOCHROME_LABELS = "-monochromeLabels";
    private static final String PARAM_PEDIGREE_LINKS_SELECTION_MODE = "-pedigreeLinksSelectionMode";
    private static final String PARAM_GA_TRACKING_ID = "-gaTrackingId";
    private static final String PARAM_HIGHLIGHT_MODE = "-highlightMode";

    private static final String DEFAULT_MODE = "dynamic";
    private static final int DEFAULT_ANONYMIZED_YEARS = 100;
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    private static final String DEFAULT_FONT_FAMILY = "Open Sans";
    private static final String DEFAULT_RELATIVE_FONT_PATH = "res/OpenSans-Regular-webfont.woff";

    private static final String RESOURCE_BUNDLE_PATH = "in/drifted/tools/genopro/webapp/exporter/resources/l10n/messages";

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

        if (passedValuesMap.containsKey(PARAM_INPUT_PATH)
                && passedValuesMap.containsKey(PARAM_OUTPUT_FOLDER_PATH)
                && (!dynamic || (dynamic && passedValuesMap.containsKey(PARAM_RELATIVE_APP_URL)))) {

            Path inputPath = Paths.get(passedValuesMap.get(PARAM_INPUT_PATH));
            Path outputFolderFolder = Paths.get(passedValuesMap.get(PARAM_OUTPUT_FOLDER_PATH));

            Locale locale = Locale.getDefault();

            if (passedValuesMap.containsKey(PARAM_LOCALE)) {
                locale = Locale.of(passedValuesMap.get(PARAM_LOCALE));
            }

            // changing default Locale which is then used for ResourceBundle retrieval as a fallback Locale
            Locale.setDefault(Locale.US);

            ResourceBundle resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_PATH, locale);

            int anonymizedYears = DEFAULT_ANONYMIZED_YEARS;

            if (passedValuesMap.containsKey(PARAM_ANONYMIZED_YEARS)) {
                anonymizedYears = Integer.parseInt(passedValuesMap.get(PARAM_ANONYMIZED_YEARS));
            }

            String datePattern = passedValuesMap.getOrDefault(PARAM_DATE_PATTERN, DEFAULT_DATE_PATTERN);
            String fontFamily = passedValuesMap.getOrDefault(PARAM_FONT_FAMILY, DEFAULT_FONT_FAMILY);
            String relativeFontPath = passedValuesMap.getOrDefault(PARAM_RELATIVE_FONT_PATH, DEFAULT_RELATIVE_FONT_PATH);

            Set<Color> unsupportedLabelColorSet = new HashSet<>();
            String unsupportedLabelHexColors = passedValuesMap.getOrDefault(PARAM_UNSUPPORTED_LABEL_HEX_COLOR_SET, "{}");
            if (unsupportedLabelHexColors.startsWith("{") && unsupportedLabelHexColors.endsWith("}")) {
                if (unsupportedLabelHexColors.length() > 2) {
                    unsupportedLabelHexColors = unsupportedLabelHexColors.substring(0, unsupportedLabelHexColors.length() - 2);
                    for (String unsupportedLabelHexColor : unsupportedLabelHexColors.split(",")) {
                        unsupportedLabelColorSet.add(Color.fromHex(unsupportedLabelHexColor));
                    }
                }
            }

            boolean monochromeLabels = Boolean.parseBoolean(passedValuesMap.getOrDefault(PARAM_MONOCHROME_LABELS, "false"));

            DateFormatter dateFormatter = new DateFormatter(datePattern, locale, new HashMap<>());
            AgeFormatter ageFormatter = new BasicAgeFormatter(resourceBundle);

            Map<String, String> additionalOptionMap = new HashMap<>();
            additionalOptionMap.put("relativeFontPath", relativeFontPath);
            if (passedValuesMap.containsKey(PARAM_GA_TRACKING_ID)) {
                additionalOptionMap.put("gaTrackingId", passedValuesMap.get(PARAM_GA_TRACKING_ID));
            }

            if (passedValuesMap.containsKey(PARAM_HIGHLIGHT_MODE)) {
                int highlightMode = Integer.parseInt(passedValuesMap.get(PARAM_HIGHLIGHT_MODE));
                additionalOptionMap.put("highlightMode", String.valueOf(highlightMode));
            }

            Path individualsPath = outputFolderFolder.resolve("individuals.js");
            Path genomapsPath = outputFolderFolder.resolve("genomaps.js");
            Path reportPath = outputFolderFolder.resolve("index.html");

            Document document = DocumentParser.getDocument(inputPath);
            DocumentInfo documentInfo = DocumentParser.getDocumentInfo(document);

            DocumentParserOptions parserOptions = new DocumentParserOptions();
            parserOptions.setUntitledGenoMapsExcluded(true);
            parserOptions.setHyperlinkedIndividualInstancesDeduplicated(true);
            LocalDate anonymizedSinceLocalDate = (anonymizedYears < 0) ? null
                    : (anonymizedYears == 0) ? LocalDate.now() : LocalDate.now().minus(Period.ofYears(anonymizedYears));
            parserOptions.setAnonymizedSinceDate(anonymizedSinceLocalDate);

            List<GenoMapData> genoMapDataList = DocumentDataUtil.getGenoMapDataList(document, parserOptions);

            GenoMapsExporter.export(genomapsPath, genoMapDataList);
            IndividualsExporter.export(individualsPath, genoMapDataList, dateFormatter);

            if (dynamic) {
                additionalOptionMap.put("relativeAppUrl", passedValuesMap.get(PARAM_RELATIVE_APP_URL));
            }

            DisplayStyle displayStyle = documentInfo.displayStyle();

            if (displayStyle == DisplayStyle.YEAR_OF_BIRTH_AND_YEAR_OF_DEATH
                    || displayStyle == DisplayStyle.YEAR_OF_BIRTH_AND_YEAR_OF_DEATH_ID) {

                dateFormatter = new DateFormatter("yyyy", locale, dateFormatter.getPrefixReplacementMap());
            }

            PedigreeLinksSelectionMode pedigreeLinksSelectionMode = PedigreeLinksSelectionMode.NONE;
            if (passedValuesMap.containsKey(PARAM_PEDIGREE_LINKS_SELECTION_MODE)) {
                if (passedValuesMap.get(PARAM_PEDIGREE_LINKS_SELECTION_MODE).equals("manual")) {
                    pedigreeLinksSelectionMode = PedigreeLinksSelectionMode.MANUAL;
                }
            }

            GeneratingOptions generatingOptions = new GeneratingOptions(locale, resourceBundle, fontFamily,
                    displayStyle, dateFormatter, ageFormatter, unsupportedLabelColorSet, monochromeLabels,
                    pedigreeLinksSelectionMode, additionalOptionMap);

            if (dynamic) {
                WebAppExporter.export(reportPath, documentInfo, genoMapDataList, generatingOptions);

            } else {
                WebAppExporter.exportAsStaticPage(reportPath, documentInfo, genoMapDataList, generatingOptions);
            }

        } else {

            System.out.println("""
                               Specify at least:
                                        - input GenoPro file path (-in:)
                                        - output folder (-out:)
                                        - relative app URL (-relativeAppUrl:)\n
                               Usage: java -jar genopro-webapp-exporter.jar
                                        -in:C:\\family-tree.gno
                                        -out:C:\\family-tree
                                        -relativeAppUrl:/family-tree
                                       [-mode:dynamic]
                                       [-locale:en]
                                       [-anonymizedYears:100]
                                       [-datePattern:yyyy-MM-dd]
                                       [-fontFamily:"Open Sans"]
                                       [-relativeFontPath:"res/OpenSans-Regular-webfont.woff"]
                                       [-unsupportedLabelHexColorSet:{<empty>}], example: {#FF0000,#C8C8FF}
                                       [-monochromeLabels:0]
                                       [-pedigreeLinksSelectionMode:none|manual]
                                       [-gaTrackingId:<empty>]
                                       [-highlightMode:0]
                               """);
        }
    }
}
