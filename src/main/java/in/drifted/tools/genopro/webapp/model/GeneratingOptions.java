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
package in.drifted.tools.genopro.webapp.model;

import in.drifted.tools.genopro.model.AgeFormatter;
import in.drifted.tools.genopro.model.DateFormatter;
import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class GeneratingOptions {

    public static final int MAIN_FONT_SIZE_IN_PIXELS = 11;
    public static final int AGE_FONT_SIZE_IN_PIXELS = 9;
    public static final int MAIN_LINE_HEIGHT_IN_PIXELS = 14;

    private final Locale locale;
    private final ResourceBundle resourceBundle;
    private final String fontFamily;
    private final DateFormatter dateFormatter;
    private final AgeFormatter ageFormatter;
    private final FontMetrics mainFontMetrics;
    private final FontMetrics ageFontMetrics;
    private final Map<String, String> additionalOptionsMap;

    public GeneratingOptions(Locale locale, ResourceBundle resourceBundle, String fontFamily, DateFormatter dateFormatter, AgeFormatter ageFormatter, Map<String, String> additionalOptionsMap) {

        Canvas canvas = new Canvas();

        this.locale = locale;
        this.resourceBundle = resourceBundle;
        this.fontFamily = fontFamily;
        this.dateFormatter = dateFormatter;
        this.ageFormatter = ageFormatter;
        this.mainFontMetrics = canvas.getFontMetrics(new Font(fontFamily, Font.PLAIN, MAIN_FONT_SIZE_IN_PIXELS));
        this.ageFontMetrics = canvas.getFontMetrics(new Font(fontFamily, Font.PLAIN, AGE_FONT_SIZE_IN_PIXELS));
        this.additionalOptionsMap = additionalOptionsMap;
    }

    public Locale getLocale() {
        return locale;
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public DateFormatter getDateFormatter() {
        return dateFormatter;
    }

    public AgeFormatter getAgeFormatter() {
        return ageFormatter;
    }

    public FontMetrics getMainFontMetrics() {
        return mainFontMetrics;
    }

    public FontMetrics getAgeFontMetrics() {
        return ageFontMetrics;
    }

    public Map<String, String> getAdditionalOptionsMap() {
        return additionalOptionsMap;
    }

}
