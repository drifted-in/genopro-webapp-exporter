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
package in.drifted.tools.genopro.webapp.exporter.model;

import in.drifted.tools.genopro.core.model.Color;
import in.drifted.tools.genopro.core.model.DisplayStyle;
import in.drifted.tools.genopro.core.util.formatter.AgeFormatter;
import in.drifted.tools.genopro.core.util.formatter.DateFormatter;
import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class GeneratingOptions {

    public static final int MAIN_FONT_SIZE_IN_PIXELS = 10;
    public static final int AGE_FONT_SIZE_IN_PIXELS = 9;
    public static final int MAIN_LINE_HEIGHT_IN_PIXELS = 14;

    private final Locale locale;
    private final ResourceBundle resourceBundle;
    private final String fontFamily;
    private final DisplayStyle displayStyle;
    private final DateFormatter dateFormatter;
    private final AgeFormatter ageFormatter;
    private final Canvas canvas;
    private final Font mainFont;
    private final FontMetrics mainFontMetrics;
    private final FontMetrics ageFontMetrics;
    private final Set<Color> unsupportedLabelColorSet;
    private final boolean monochromeLabels;
    private final Map<String, String> additionalOptionsMap;
    private final PedigreeLinksSelectionMode pedigreeLinksSelectionMode;

    public GeneratingOptions(Locale locale, ResourceBundle resourceBundle, String fontFamily,
            DisplayStyle displayStyle, DateFormatter dateFormatter, AgeFormatter ageFormatter,
            Set<Color> unsupportedLabelColorSet, boolean monochromeLabels,
            PedigreeLinksSelectionMode pedigreeLinksSelectionMode, Map<String, String> additionalOptionsMap) {

        Canvas canvas = new Canvas();

        Map<TextAttribute, Object> mainTextAttributes = new HashMap<>();
        mainTextAttributes.put(TextAttribute.FAMILY, fontFamily);
        mainTextAttributes.put(TextAttribute.SIZE, MAIN_FONT_SIZE_IN_PIXELS);
        mainTextAttributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        mainFont = new Font(mainTextAttributes);

        this.locale = locale;
        this.resourceBundle = resourceBundle;
        this.fontFamily = fontFamily;
        this.displayStyle = displayStyle;
        this.dateFormatter = dateFormatter;
        this.ageFormatter = ageFormatter;
        this.canvas = new Canvas();
        this.mainFontMetrics = canvas.getFontMetrics(mainFont);
        this.ageFontMetrics = getFontMetrics(AGE_FONT_SIZE_IN_PIXELS);
        this.unsupportedLabelColorSet = unsupportedLabelColorSet;
        this.monochromeLabels = monochromeLabels;
        this.additionalOptionsMap = additionalOptionsMap;
        this.pedigreeLinksSelectionMode = pedigreeLinksSelectionMode;
    }

    public FontMetrics getFontMetrics(double sizeInPixels) {
        return canvas.getFontMetrics(mainFont.deriveFont((float) sizeInPixels));
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

    public DisplayStyle getDisplayStyle() {
        return displayStyle;
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

    public Set<Color> getUnsupportedLabelColorSet() {
        return unsupportedLabelColorSet;
    }

    public boolean hasMonochromeLabels() {
        return monochromeLabels;
    }

    public Map<String, String> getAdditionalOptionsMap() {
        return additionalOptionsMap;
    }

    public PedigreeLinksSelectionMode getPedigreeLinksSelectionMode() {
        return pedigreeLinksSelectionMode;
    }

}
