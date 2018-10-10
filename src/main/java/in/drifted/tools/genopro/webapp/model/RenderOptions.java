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
import java.awt.FontMetrics;
import java.util.Locale;
import java.util.ResourceBundle;

public class RenderOptions {

    private final Locale locale;
    private final ResourceBundle resourceBundle;
    private final DateFormatter dateFormatter;
    private final AgeFormatter ageFormatter;
    private final FontMetrics mainFontMetrics;
    private final FontMetrics ageFontMetrics;
    private final int mainLineHeightInPixels;

    public RenderOptions(Locale locale, ResourceBundle resourceBundle, DateFormatter dateFormatter, AgeFormatter ageFormatter, FontMetrics mainFontMetrics, FontMetrics ageFontMetrics, int mainLineHeightInPixels) {
        this.resourceBundle = resourceBundle;
        this.locale = locale;
        this.dateFormatter = dateFormatter;
        this.ageFormatter = ageFormatter;
        this.mainFontMetrics = mainFontMetrics;
        this.ageFontMetrics = ageFontMetrics;
        this.mainLineHeightInPixels = mainLineHeightInPixels;
    }

    public Locale getLocale() {
        return locale;
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
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

    public int getMainLineHeightInPixels() {
        return mainLineHeightInPixels;
    }

}
