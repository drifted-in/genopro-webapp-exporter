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

import in.drifted.tools.genopro.model.Alignment;
import in.drifted.tools.genopro.model.Birth;
import in.drifted.tools.genopro.model.BoundaryRect;
import in.drifted.tools.genopro.model.DateFormatter;
import in.drifted.tools.genopro.model.Death;
import in.drifted.tools.genopro.model.DisplayStyle;
import in.drifted.tools.genopro.model.Family;
import in.drifted.tools.genopro.model.FamilyLineType;
import in.drifted.tools.genopro.model.Gender;
import in.drifted.tools.genopro.model.GenoMap;
import in.drifted.tools.genopro.model.GenoMapData;
import in.drifted.tools.genopro.model.HighlightMode;
import in.drifted.tools.genopro.model.Hyperlink;
import in.drifted.tools.genopro.model.Individual;
import in.drifted.tools.genopro.model.Label;
import in.drifted.tools.genopro.model.LabelStyle;
import in.drifted.tools.genopro.model.Name;
import in.drifted.tools.genopro.model.PedigreeLink;
import in.drifted.tools.genopro.model.Position;
import in.drifted.tools.genopro.model.Rect;
import in.drifted.tools.genopro.model.Size;
import in.drifted.tools.genopro.webapp.exporter.model.GeneratingOptions;
import in.drifted.tools.genopro.util.StringUtil;
import java.awt.FontMetrics;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class SvgExporter {

    private static final Map<Size, Double> FONT_SIZE_SCALE_FACTOR_MAP = new HashMap<>();
    private static final Map<Size, Double> STROKE_WIDTH_SCALE_FACTOR_MAP = new HashMap<>();

    static {
        FONT_SIZE_SCALE_FACTOR_MAP.put(Size.T, 0.63);
        FONT_SIZE_SCALE_FACTOR_MAP.put(Size.S, 0.8);
        FONT_SIZE_SCALE_FACTOR_MAP.put(Size.M, 1.109);
        FONT_SIZE_SCALE_FACTOR_MAP.put(Size.L, 1.62);
        FONT_SIZE_SCALE_FACTOR_MAP.put(Size.XL, 2.22);
        FONT_SIZE_SCALE_FACTOR_MAP.put(Size.XXL, 3.2);
        FONT_SIZE_SCALE_FACTOR_MAP.put(Size.XXXL, 4.89);
        FONT_SIZE_SCALE_FACTOR_MAP.put(Size.XXXXL, 9.73);

        STROKE_WIDTH_SCALE_FACTOR_MAP.put(Size.T, 0.27);
        STROKE_WIDTH_SCALE_FACTOR_MAP.put(Size.S, 0.7);
        STROKE_WIDTH_SCALE_FACTOR_MAP.put(Size.M, 1.0);
        STROKE_WIDTH_SCALE_FACTOR_MAP.put(Size.L, 1.35);
        STROKE_WIDTH_SCALE_FACTOR_MAP.put(Size.XL, 1.7);
        STROKE_WIDTH_SCALE_FACTOR_MAP.put(Size.XXL, 2.0);
        STROKE_WIDTH_SCALE_FACTOR_MAP.put(Size.XXXL, 2.2);
        STROKE_WIDTH_SCALE_FACTOR_MAP.put(Size.XXXXL, 2.8);
    }

    public static void export(GenoMapData genoMapData, OutputStream outputStream, GeneratingOptions generatingOptions)
            throws IOException {

        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

        GenoMap genoMap = genoMapData.getGenoMap();

        String id = genoMap.getId();

        try {

            XMLStreamWriter writer = outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
            writer.writeStartDocument();
            writer.writeStartElement("svg");
            writer.writeAttribute("id", id);
            writer.writeAttribute("xmlns", "http://www.w3.org/2000/svg");
            writer.writeAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
            Position topLeft = genoMap.getBoundaryRect().getTopLeft();
            Position bottomRight = genoMap.getBoundaryRect().getBottomRight();
            int shiftX = topLeft.getX();
            int shiftY = topLeft.getY() + GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS / 2;
            int width = bottomRight.getX() - topLeft.getX();
            int height = topLeft.getY() - bottomRight.getY();
            writer.writeAttribute("viewBox", "0 0 " + width + " " + height);

            for (Label label : genoMapData.getLabelCollection()) {
                if (!generatingOptions.getUnsupportedLabelColorSet().contains(label.getLabelStyle().getFillColor())) {
                    renderLabel(writer, label, shiftX, shiftY, generatingOptions);
                }
            }

            Map<String, Individual> individualMap = new HashMap<>();
            for (Individual individual : genoMapData.getIndividualCollection()) {
                individualMap.put(individual.getId(), individual);
            }

            for (Family family : genoMapData.getFamilyCollection()) {
                renderFamilyRelations(writer, family, individualMap, shiftX, shiftY, generatingOptions);
            }

            for (Individual individual : genoMapData.getIndividualCollection()) {
                if (!individual.isAnonymized()) {
                    renderIndividual(writer, individual, shiftX, shiftY, generatingOptions);
                }
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();

        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private static void renderLabel(XMLStreamWriter writer, Label label, int shiftX, int shiftY,
            GeneratingOptions generatingOptions) throws XMLStreamException {

        int width = label.getRect().getWidth();
        int height = label.getRect().getHeight();
        int rectX = label.getRect().getX() - shiftX;
        int rectY = shiftY - label.getRect().getY();

        LabelStyle labelStyle = label.getLabelStyle();

        writer.writeStartElement("rect");
        writer.writeAttribute("x", String.valueOf(rectX));
        writer.writeAttribute("y", String.valueOf(rectY));
        writer.writeAttribute("width", String.valueOf(width));
        writer.writeAttribute("height", String.valueOf(height));
        if (generatingOptions.hasMonochromeLabels()) {
            writer.writeAttribute("class", "monochrome-label");
            writer.writeAttribute("style", "stroke-width: "
                    + getStrokeWidth(labelStyle.getBorder().getSize(), 3.0));
        } else {
            writer.writeAttribute("style", getLabelStyleAttribute(labelStyle));
        }
        writer.writeEndElement();

        int padding = labelStyle.getPadding();
        String id = "idx-" + label.getRect().hashCode();

        writer.writeStartElement("clipPath");
        writer.writeAttribute("id", id);
        writer.writeStartElement("rect");
        writer.writeAttribute("x", String.valueOf(rectX + padding));
        writer.writeAttribute("y", String.valueOf(rectY + padding));
        writer.writeAttribute("width", String.valueOf(width - 2 * padding));
        writer.writeAttribute("height", String.valueOf(height - 2 * padding));
        writer.writeEndElement();
        writer.writeEndElement();

        double scaleFactor = FONT_SIZE_SCALE_FACTOR_MAP.get(label.getLabelStyle().getSize());
        double fontSize = scaleFactor * generatingOptions.getMainFontMetrics().getFont().getSize();

        String text = label.getText();

        if (text != null) {

            List<String> lineList = Arrays.asList(text.split("\n", -1));

            FontMetrics scaledFontMetrics = generatingOptions.getFontMetrics(10 * fontSize);
            int ascent = scaledFontMetrics.getAscent() / 10;
            int descent = scaledFontMetrics.getDescent() / 10;

            List<String> wrappedLineList = new ArrayList<>();

            for (String line : lineList) {
                wrappedLineList.addAll(StringUtil.getWrappedLineList(line,
                        10 * (width - (2 * labelStyle.getPadding())),
                        scaledFontMetrics));
            }

            int baseX = getLabelBaseX(rectX, width, labelStyle);
            int baseY = rectY + padding + ascent;

            int availableHeight = (height - 2 * padding);
            int textBlockHeight = ascent + (int) ((wrappedLineList.size() - 1) * 1.36 * fontSize) + descent;

            if (textBlockHeight < availableHeight && (labelStyle.getVerticalAlignment() != Alignment.TOP)) {
                if (labelStyle.getVerticalAlignment() == Alignment.BOTTOM) {
                    baseY = rectY + height - padding - textBlockHeight + ascent;
                } else {
                    baseY = rectY + padding + (availableHeight - textBlockHeight) / 2 + ascent;
                }
            }

            for (int i = 0; i < wrappedLineList.size(); i++) {

                int y = baseY + (int) (1.36 * i * fontSize);

                writer.writeStartElement("text");
                writer.writeAttribute("clip-path", "url(#" + id + ")");
                writer.writeAttribute("text-anchor", getLabelTextAnchor(labelStyle));
                writer.writeAttribute("x", String.valueOf(baseX));
                writer.writeAttribute("y", String.valueOf(y));
                writer.writeAttribute("style", "font-size:" + fontSize + "px");
                writer.writeCharacters(wrappedLineList.get(i));
                writer.writeEndElement();
            }

            if (textBlockHeight > availableHeight) {
                int lineWidth = width / 2;
                int arrowWidth = 6;
                int startX = rectX + width / 4;
                int endX = startX + lineWidth;
                int startY = rectY + height - padding + 2;

                StringBuilder data = new StringBuilder();
                data.append("M");
                data.append(startX);
                data.append(",");
                data.append(startY);
                data.append("h");
                data.append(lineWidth);

                if (lineWidth > (2 * arrowWidth)) {
                    data.append("M");
                    data.append(startX);
                    data.append(",");
                    data.append(startY);
                    data.append("L");
                    data.append(startX + arrowWidth / 2);
                    data.append(",");
                    data.append(startY + arrowWidth * 0.7);
                    data.append("L");
                    data.append(startX + arrowWidth);
                    data.append(",");
                    data.append(startY);
                    data.append("M");
                    data.append(endX);
                    data.append(",");
                    data.append(startY);
                    data.append("L");
                    data.append(endX - arrowWidth / 2);
                    data.append(",");
                    data.append(startY + arrowWidth * 0.7);
                    data.append("L");
                    data.append(endX - arrowWidth);
                    data.append(",");
                    data.append(startY);
                }

                writer.writeStartElement("path");
                writer.writeAttribute("d", data.toString());
                writer.writeAttribute("style", "stroke: red; stroke-width: 0.5px; fill: none;");
                writer.writeEndElement();
            }
        }
    }

    private static void renderFamilyRelations(XMLStreamWriter writer, Family family,
            Map<String, Individual> individualMap, int shiftX, int shiftY, GeneratingOptions generatingOptions)
            throws XMLStreamException {

        Position position = family.getPosition();
        BoundaryRect topBoundaryRect = family.getTopBoundaryRect();
        BoundaryRect bottomBoundaryRect = family.getBottomBoundaryRect();

        Rect topRect = null;
        Rect bottomRect = null;

        HighlightMode highlightMode = HighlightMode.of(Integer.parseInt(
                generatingOptions.getAdditionalOptionsMap().getOrDefault("highlightMode", "0")));

        boolean hasChildren = false;

        for (PedigreeLink pedigreeLink : family.getPedigreeLinkList()) {
            if (!pedigreeLink.isParent()) {
                hasChildren = true;
                break;
            }
        }

        Individual individual = null;
        int highlightKeysCount = 0;

        if (highlightMode != HighlightMode.NONE) {

            if (highlightMode == HighlightMode.MATERNAL) {
                individual = individualMap.get(family.getMotherId());

            } else {
                individual = individualMap.get(family.getFatherId());
            }

            highlightKeysCount = individual.getHighlightKeySet().size();
        }

        if (topBoundaryRect != null) {

            topRect = new Rect(topBoundaryRect);

            int y = shiftY - topRect.getY();

            String className = "family-line";

            String linePathData = "M" + (topRect.getX() - shiftX) + " " + y + "h" + topRect.getWidth();

            if (highlightMode != HighlightMode.NONE) {

                String[] linePathDataArray = new String[]{
                    "M" + (topRect.getX() - shiftX) + " " + y + "H" + (position.getX() - shiftX),
                    "M" + (position.getX() - shiftX) + " " + y + "H" + (topRect.getX() + topRect.getWidth() - shiftX)
                };

                if (hasChildren) {

                    int i = 0;

                    for (String highlightKey : individual.getHighlightKeySet()) {

                        StringBuilder style = new StringBuilder();
                        style.append("stroke:");
                        style.append(highlightKey.equals("n/a") ? "black" : highlightKey);

                        if (i > 0) {
                            style.append(";stroke-dasharray:");
                            style.append(5 * (highlightKeysCount - i));
                            style.append(",");
                            style.append(5 * i);
                            style.append(";stroke-linecap:butt;");
                            style.append(";fill:none;");
                        }

                        if (bottomBoundaryRect != null) {

                            writer.writeStartElement("path");
                            writer.writeAttribute("d", linePathDataArray[0]);

                            if (highlightMode == HighlightMode.PATERNAL) {
                                writer.writeAttribute("class", className + " highlighted");
                                writer.writeAttribute("style", style.toString());
                            } else {
                                writer.writeAttribute("class", className + " unhighlighted");
                            }

                            writer.writeEndElement();

                            writer.writeStartElement("path");
                            writer.writeAttribute("d", linePathDataArray[1]);

                            if (highlightMode == HighlightMode.MATERNAL) {
                                writer.writeAttribute("class", className + " highlighted");
                                writer.writeAttribute("style", style.toString());
                            } else {
                                writer.writeAttribute("class", className + " unhighlighted");
                            }

                            writer.writeEndElement();

                        } else {
                            writer.writeStartElement("path");
                            writer.writeAttribute("d", linePathData);
                            writer.writeAttribute("class", className + " highlighted");
                            writer.writeAttribute("style", style.toString());
                            writer.writeEndElement();
                        }

                        i++;
                    }

                } else {
                    writer.writeStartElement("path");
                    writer.writeAttribute("d", linePathData);
                    writer.writeAttribute("class", className + " unhighlighted");
                    writer.writeEndElement();
                }

            } else {
                writer.writeStartElement("path");
                writer.writeAttribute("d", linePathData);
                writer.writeAttribute("class", className);
                writer.writeEndElement();
            }

            if (family.getLabel() != null) {

                String label = family.getLabel();

                int labelWidthInPixels = generatingOptions.getMainFontMetrics().stringWidth(label);
                double centerX = topRect.getX() - shiftX + topRect.getWidth() / 2.0;
                int fontSize = generatingOptions.getMainFontMetrics().getFont().getSize();
                double textPadding = (GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS - fontSize) / 2.0;

                writer.writeStartElement("rect");
                writer.writeAttribute("x", String.valueOf(centerX - labelWidthInPixels / 2.0));
                writer.writeAttribute("y", String.valueOf(y - 1.3 * GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS));
                writer.writeAttribute("width", String.valueOf(labelWidthInPixels));
                writer.writeAttribute("height", String.valueOf(GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS));
                writer.writeAttribute("class", "family-label");
                writer.writeEndElement();

                writer.writeStartElement("text");
                writer.writeAttribute("x", String.valueOf(centerX));
                writer.writeAttribute("y",
                        String.valueOf(y - 0.3 * GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS - textPadding));
                writer.writeAttribute("class", "family-label");
                writer.writeCharacters(label);
                writer.writeEndElement();
            }

            if (family.getFamilyLineType() != FamilyLineType.UNSPECIFIED) {

                String familyLineTypeSymbolPathData = null;
                String familyLineTypeSymbolClassName = className;
                double topRight = topRect.getX() + topRect.getWidth() - shiftX;

                switch (family.getFamilyLineType()) {
                    case NO_MORE_CHILDREN:
                        familyLineTypeSymbolPathData = "M" + (topRight - 6.5) + " " + (y + 3) + "h5v5h-5z";
                        familyLineTypeSymbolClassName += "-no-more-children";

                        break;

                    case POSSIBLY_MORE_CHILDREN:
                        familyLineTypeSymbolPathData = "M" + (topRight - 11) + " " + (y + 6) + "h8m-4 -4v8";
                        familyLineTypeSymbolClassName += "-possibly-more-children";
                        break;

                    case TO_BE_COMPLETED:
                        familyLineTypeSymbolPathData = "M" + (topRight - 8.5) + " " + (y + 3) + "l6 6m-6 0l6-6";
                        familyLineTypeSymbolClassName += "-to-be-completed";
                        break;
                }

                if (familyLineTypeSymbolPathData != null) {
                    writer.writeStartElement("path");
                    writer.writeAttribute("d", familyLineTypeSymbolPathData);
                    writer.writeAttribute("class", familyLineTypeSymbolClassName);
                    writer.writeEndElement();
                }
            }
        }

        if (bottomBoundaryRect != null) {

            // if children are anonymized, the bottom rect can be skipped
            if (hasChildren) {

                bottomRect = new Rect(bottomBoundaryRect);

                String className = "family-line";

                StringBuilder verticalPathData = new StringBuilder();
                verticalPathData.append("M");
                verticalPathData.append(position.getX() - shiftX);
                verticalPathData.append(" ");
                verticalPathData.append(shiftY - position.getY());
                verticalPathData.append("v");
                verticalPathData.append(position.getY() - bottomRect.getY());

                StringBuilder horizontalPathData = new StringBuilder();
                horizontalPathData.append("M");
                horizontalPathData.append(bottomRect.getX() - shiftX);
                horizontalPathData.append(" ");
                horizontalPathData.append(shiftY - bottomRect.getY());
                horizontalPathData.append("h");
                horizontalPathData.append(bottomRect.getWidth());

                if (highlightMode != HighlightMode.NONE) {

                    int i = 0;

                    for (String highlightKey : individual.getHighlightKeySet()) {

                        StringBuilder style = new StringBuilder();
                        style.append("stroke:");
                        style.append(highlightKey.equals("n/a") ? "black" : highlightKey);

                        if (i > 0) {
                            style.append(";stroke-dasharray:");
                            style.append(5 * (highlightKeysCount - i));
                            style.append(",");
                            style.append(5 * i);
                            style.append(";stroke-linecap:butt;");
                            style.append(";fill:none;");
                        }

                        writer.writeStartElement("path");
                        writer.writeAttribute("d", verticalPathData.toString());
                        writer.writeAttribute("class", className + " highlighted");
                        writer.writeAttribute("style", style.toString());
                        writer.writeEndElement();

                        writer.writeStartElement("path");
                        writer.writeAttribute("d", horizontalPathData.toString());
                        writer.writeAttribute("class", className + " highlighted");
                        writer.writeAttribute("style", style.toString());
                        writer.writeEndElement();

                        i++;
                    }

                } else {
                    writer.writeStartElement("path");
                    writer.writeAttribute("d", verticalPathData.toString());
                    writer.writeAttribute("class", className);
                    writer.writeEndElement();

                    writer.writeStartElement("path");
                    writer.writeAttribute("d", horizontalPathData.toString());
                    writer.writeAttribute("class", className);
                    writer.writeEndElement();
                }
            }
        }

        String className = "pedigree-link";

        for (PedigreeLink pedigreeLink : family.getPedigreeLinkList()) {

            Position individualPosition = pedigreeLink.getPosition();

            if (individualPosition == null) {
                continue;
            }

            StringBuilder pathData = new StringBuilder();

            boolean isEmpty = true;

            pathData.append("M");
            pathData.append(individualPosition.getX() - shiftX);
            pathData.append(" ");
            pathData.append(shiftY - individualPosition.getY());

            if (pedigreeLink.isParent()) {
                if (topRect != null) {
                    pathData.append("v");
                    pathData.append(individualPosition.getY() - topRect.getY());
                    isEmpty = false;
                }

            } else {
                if (bottomRect != null) {
                    if (pedigreeLink.getTwinPosition() != null) {
                        pathData.append("L");
                        pathData.append(pedigreeLink.getTwinPosition().getX() - shiftX);
                        pathData.append(" ");
                        pathData.append(shiftY - bottomRect.getY());

                    } else {
                        pathData.append("v");
                        pathData.append(individualPosition.getY() - bottomRect.getY());
                    }
                    isEmpty = false;

                } else if (topRect != null) {
                    if (pedigreeLink.getTwinPosition() != null) {
                        pathData.append("L");
                        pathData.append(pedigreeLink.getTwinPosition().getX() - shiftX);
                        pathData.append(" ");
                        pathData.append(shiftY - topRect.getY());

                    } else {
                        pathData.append("v");
                        pathData.append(individualPosition.getY() - topRect.getY());
                    }
                    isEmpty = false;
                }
            }

            if (!isEmpty) {

                if (highlightMode != HighlightMode.NONE) {

                    Individual child = individualMap.get(pedigreeLink.getIndividualId());

                    if ((highlightMode == HighlightMode.PATERNAL && child.isMale())
                            || (highlightMode == HighlightMode.MATERNAL && child.isFemale())) {

                        int childHighlightKeysCount = child.getHighlightKeySet().size();

                        int i = 0;

                        for (String highlightKey : child.getHighlightKeySet()) {

                            StringBuilder style = new StringBuilder();
                            style.append("stroke:");
                            style.append(highlightKey.equals("n/a") ? "black" : highlightKey);

                            if (i > 0) {
                                style.append(";stroke-dasharray:");
                                style.append(5 * (childHighlightKeysCount - i));
                                style.append(",");
                                style.append(5 * i);
                                style.append(";stroke-linecap:butt;");
                                style.append(";fill:none;");
                            }

                            writer.writeStartElement("path");
                            writer.writeAttribute("d", pathData.toString());
                            writer.writeAttribute("class", className + " highlighted");
                            writer.writeAttribute("style", style.toString());
                            writer.writeEndElement();

                            i++;
                        }

                    } else {
                        writer.writeStartElement("path");
                        writer.writeAttribute("d", pathData.toString());
                        writer.writeAttribute("class", className + " unhighlighted");
                        writer.writeEndElement();
                    }

                } else {
                    writer.writeStartElement("path");
                    writer.writeAttribute("d", pathData.toString());

                    switch (pedigreeLink.getPedigreeLinkType()) {

                        case PARENT:
                            writer.writeAttribute("class", className + " parent");
                            break;

                        case ADOPTED:
                            writer.writeAttribute("class", className + " adopted");
                            break;

                        default:
                            writer.writeAttribute("class", className + " biological");
                            break;
                    }

                    writer.writeEndElement();
                }
            }
        }
    }

    private static void renderIndividual(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY,
            GeneratingOptions generatingOptions) throws XMLStreamException {

        writer.writeStartElement("g");
        writer.writeAttribute("id", individual.getId());

        Hyperlink hyperlink = individual.getHyperlink();

        if (hyperlink != null) {
            writer.writeAttribute("data-target-id", hyperlink.getId());
        }

        renderIndividualDates(writer, individual, shiftX, shiftY, generatingOptions);
        renderIndividualSymbol(writer, individual, shiftX, shiftY, generatingOptions);
        renderIndividualAge(writer, individual, shiftX, shiftY, generatingOptions);

        if (hyperlink != null) {
            renderActiveArea(writer, individual, shiftX, shiftY, generatingOptions);
            renderIndividualLabel(writer, individual, shiftX, shiftY, generatingOptions);

        } else {
            renderIndividualLabel(writer, individual, shiftX, shiftY, generatingOptions);
            renderActiveArea(writer, individual, shiftX, shiftY, generatingOptions);
        }

        writer.writeEndElement();
    }

    private static void renderIndividualDates(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY,
            GeneratingOptions generatingOptions) throws XMLStreamException {

        List<String> labelList = new ArrayList<>();
        DisplayStyle displayStyle = generatingOptions.getDisplayStyle();

        if (displayStyle == DisplayStyle.NOTHING) {
            return;
        }

        if (displayStyle == DisplayStyle.ID
                || displayStyle == DisplayStyle.YEAR_OF_BIRTH_AND_YEAR_OF_DEATH_ID
                || displayStyle == DisplayStyle.DATE_OF_BIRTH_AND_DATE_OF_DEATH_ID) {

            labelList.add(individual.getId());
        }

        if (displayStyle == DisplayStyle.YEAR_OF_BIRTH_AND_YEAR_OF_DEATH
                || displayStyle == DisplayStyle.YEAR_OF_BIRTH_AND_YEAR_OF_DEATH_ID
                || displayStyle == DisplayStyle.DATE_OF_BIRTH_AND_DATE_OF_DEATH
                || displayStyle == DisplayStyle.DATE_OF_BIRTH_AND_DATE_OF_DEATH_ON_SEPARATE_LINES
                || displayStyle == DisplayStyle.DATE_OF_BIRTH_AND_DATE_OF_DEATH_ID) {

            DateFormatter dateFormatter = generatingOptions.getDateFormatter();

            Birth birth = individual.getBirth();
            Death death = individual.getDeath();
            boolean hasBirth = false;

            List<String> dateList = new ArrayList<>();

            if (birth != null && birth.getDate() != null) {
                String birthLabel = birth.getDate().getDate(dateFormatter);
                dateList.add(birthLabel);
                hasBirth = true;
            }

            if (death != null && death.getDate() != null) {
                String deathLabel = death.getDate().getDate(dateFormatter);
                if (!hasBirth) {
                    deathLabel = generatingOptions.getResourceBundle().getString("deathAbbrev") + " " + deathLabel;
                }
                dateList.add(deathLabel);
            }

            if (displayStyle == DisplayStyle.DATE_OF_BIRTH_AND_DATE_OF_DEATH_ON_SEPARATE_LINES
                    || displayStyle == DisplayStyle.DATE_OF_BIRTH_AND_DATE_OF_DEATH_ID) {

                labelList.addAll(dateList);

            } else {
                labelList.add(String.join(" – ", dateList));
            }
        }

        Rect rect = new Rect(individual.getBoundaryRect());

        int fontSize = generatingOptions.getMainFontMetrics().getFont().getSize();
        double textPadding = (GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS - fontSize) / 2.0;

        int baseTopY = shiftY - rect.getY() + GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS / 2;

        for (int i = 0; i < labelList.size(); i++) {

            String label = labelList.get(i);
            int labelWidth = generatingOptions.getMainFontMetrics().stringWidth(label);

            int topY = baseTopY + i * GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS;

            writer.writeStartElement("rect");
            writer.writeAttribute("x", String.valueOf(individual.getPosition().getX() - labelWidth / 2 - shiftX));
            writer.writeAttribute("y", String.valueOf(topY));
            writer.writeAttribute("width", String.valueOf(labelWidth));
            writer.writeAttribute("height", String.valueOf(GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS));
            writer.writeAttribute("class", "individual-label");
            writer.writeEndElement();

            writer.writeStartElement("text");
            writer.writeAttribute("x", String.valueOf(individual.getPosition().getX() - shiftX));
            writer.writeAttribute("y",
                    String.valueOf(topY - textPadding + GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS));
            writer.writeAttribute("class", "individual-label");

            writer.writeCharacters(labelList.get(i));
            writer.writeEndElement();
        }
    }

    private static void renderIndividualSymbol(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY,
            GeneratingOptions generatingOptions) throws XMLStreamException {

        Position position = individual.getPosition();

        HighlightMode highlightMode = HighlightMode.of(Integer.parseInt(
                generatingOptions.getAdditionalOptionsMap().getOrDefault("highlightMode", "0")));

        switch (individual.getGender()) {

            case MALE: {

                if (highlightMode == HighlightMode.PATERNAL) {
                    int highlightKeysCount = individual.getHighlightKeySet().size();
                    int i = 0;

                    for (String highlightKey : individual.getHighlightKeySet()) {

                        StringBuilder data = new StringBuilder();
                        data.append("M");
                        data.append(String.valueOf(position.getX() - shiftX - 9));
                        data.append(" ");
                        data.append(String.valueOf(shiftY - position.getY() - 9));
                        data.append("h18v18h-18z");

                        StringBuilder style = new StringBuilder();
                        style.append("stroke:");
                        style.append(highlightKey.equals("n/a") ? "black" : highlightKey);

                        if (i > 0) {
                            style.append(";stroke-dasharray:");
                            style.append(5 * (highlightKeysCount - i));
                            style.append(",");
                            style.append(5 * i);
                            style.append(";stroke-linecap:butt;");
                            style.append(";fill:none;");
                        }

                        writer.writeStartElement("path");
                        writer.writeAttribute("d", data.toString());
                        writer.writeAttribute("class", "individual-symbol highlighted");
                        writer.writeAttribute("style", style.toString());
                        writer.writeEndElement();
                        i++;
                    }

                } else {
                    writer.writeStartElement("rect");
                    writer.writeAttribute("x", String.valueOf(position.getX() - shiftX - 9));
                    writer.writeAttribute("y", String.valueOf(shiftY - position.getY() - 9));
                    writer.writeAttribute("width", "18");
                    writer.writeAttribute("height", "18");
                    writer.writeAttribute("class", "individual-symbol" + (highlightMode == HighlightMode.MATERNAL ? " unhighlighted" : ""));
                    writer.writeEndElement();
                }

                break;
            }

            case FEMALE: {

                if (highlightMode == HighlightMode.MATERNAL) {

                    int highlightKeysCount = individual.getHighlightKeySet().size();
                    int i = 0;

                    for (String highlightKey : individual.getHighlightKeySet()) {

                        StringBuilder data = new StringBuilder();
                        data.append("M");
                        data.append(String.valueOf(position.getX() - shiftX));
                        data.append(" ");
                        data.append(String.valueOf(shiftY - position.getY()));
                        data.append("m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0");

                        StringBuilder style = new StringBuilder();
                        style.append("stroke:");
                        style.append(highlightKey.equals("n/a") ? "black" : highlightKey);

                        if (i > 0) {
                            style.append(";stroke-dasharray:");
                            style.append(5 * (highlightKeysCount - i));
                            style.append(",");
                            style.append(5 * i);
                            style.append(";stroke-linecap:butt;");
                            style.append(";fill:none;");
                        }

                        writer.writeStartElement("path");
                        writer.writeAttribute("d", data.toString());
                        writer.writeAttribute("class", "individual-symbol highlighted");
                        writer.writeAttribute("style", style.toString());
                        writer.writeEndElement();
                        i++;
                    }

                } else {
                    writer.writeStartElement("circle");
                    writer.writeAttribute("cx", String.valueOf(position.getX() - shiftX));
                    writer.writeAttribute("cy", String.valueOf(shiftY - position.getY()));
                    writer.writeAttribute("r", "9");
                    writer.writeAttribute("class", "individual-symbol" + (highlightMode == HighlightMode.PATERNAL ? " unhighlighted" : ""));
                    writer.writeEndElement();
                }

                break;
            }

            case UNKNOWN: {
                writer.writeStartElement("rect");
                writer.writeAttribute("x", String.valueOf(position.getX() - shiftX - 9));
                writer.writeAttribute("y", String.valueOf(shiftY - position.getY() - 9));
                writer.writeAttribute("width", "18");
                writer.writeAttribute("height", "18");
                writer.writeAttribute("class", "individual-symbol-background");
                writer.writeEndElement();

                writer.writeStartElement("text");
                writer.writeAttribute("x", String.valueOf(position.getX() - shiftX));
                writer.writeAttribute("y", String.valueOf(shiftY - position.getY() + 4));
                writer.writeAttribute("class", "individual-symbol");
                writer.writeCharacters("?");
                writer.writeEndElement();

                break;
            }
        }
    }

    private static void renderIndividualAge(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY,
            GeneratingOptions generatingOptions) throws XMLStreamException {

        Position position = individual.getPosition();

        if (individual.isDead()) {

            HighlightMode highlightMode = HighlightMode.of(Integer.parseInt(
                    generatingOptions.getAdditionalOptionsMap().getOrDefault("highlightMode", "0")));

            float delta = (individual.getGender() == Gender.MALE) ? 9 : 6.4f;

            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append("M");
            dataBuilder.append(position.getX() - shiftX - delta);
            dataBuilder.append(" ");
            dataBuilder.append(shiftY - position.getY() - delta);
            dataBuilder.append("L");
            dataBuilder.append(position.getX() - shiftX + delta);
            dataBuilder.append(" ");
            dataBuilder.append(shiftY - position.getY() + delta);
            dataBuilder.append("M");
            dataBuilder.append(position.getX() - shiftX - delta);
            dataBuilder.append(" ");
            dataBuilder.append(shiftY - position.getY() + delta);
            dataBuilder.append("L");
            dataBuilder.append(position.getX() - shiftX + delta);
            dataBuilder.append(" ");
            dataBuilder.append(shiftY - position.getY() - delta);

            String className = "individual-deceased";

            if (highlightMode != HighlightMode.NONE) {

                boolean isHighlighted = highlightMode == HighlightMode.PATERNAL && individual.getGender() == Gender.MALE
                        || highlightMode == HighlightMode.MATERNAL && individual.getGender() == Gender.FEMALE;

                if (isHighlighted) {

                    int highlightKeysCount = individual.getHighlightKeySet().size();
                    int i = 0;

                    for (String highlightKey : individual.getHighlightKeySet()) {

                        StringBuilder style = new StringBuilder();
                        style.append("stroke:");
                        style.append(highlightKey.equals("n/a") ? "black" : highlightKey);

                        if (i > 0) {
                            style.append(";stroke-dasharray:");
                            style.append(5 * (highlightKeysCount - i));
                            style.append(",");
                            style.append(5 * i);
                            style.append(";stroke-linecap:butt;");
                            style.append(";fill:none;");
                        }

                        writer.writeStartElement("path");
                        writer.writeAttribute("d", dataBuilder.toString());
                        writer.writeAttribute("class", className + " highlighted");
                        writer.writeAttribute("style", style.toString());
                        writer.writeEndElement();

                        i++;
                    }

                } else {
                    writer.writeStartElement("path");
                    writer.writeAttribute("d", dataBuilder.toString());
                    writer.writeAttribute("class", className + " unhighlighted");
                    writer.writeEndElement();
                }

            } else {
                writer.writeStartElement("path");
                writer.writeAttribute("d", dataBuilder.toString());
                writer.writeAttribute("class", className);
                writer.writeEndElement();
            }
        }

        Birth birth = individual.getBirth();
        Death death = individual.getDeath();

        String age = generatingOptions.getAgeFormatter().format(birth, death);

        if ((death == null || !death.hasDate()) && individual.isDead()) {
            age = null;
        }

        if (age != null) {

            // smaller font requires smaller metrics
            double ageWidth = generatingOptions.getAgeFontMetrics().stringWidth(age);
            int fontSize = generatingOptions.getAgeFontMetrics().getFont().getSize();

            writer.writeStartElement("rect");
            writer.writeAttribute("x", String.valueOf(position.getX() - ageWidth / 2.0 - shiftX));
            writer.writeAttribute("y", String.valueOf(shiftY - position.getY() - fontSize / 2.0));
            writer.writeAttribute("width", String.valueOf(ageWidth));
            writer.writeAttribute("height", String.valueOf(fontSize));
            writer.writeAttribute("class", "individual-age");
            writer.writeEndElement();

            writer.writeStartElement("text");
            writer.writeAttribute("x", String.valueOf(position.getX() - shiftX));
            writer.writeAttribute("y", String.valueOf(shiftY - position.getY() + (0.7 * fontSize / 2.0)));
            writer.writeAttribute("class", "individual-age");
            writer.writeCharacters(age);
            writer.writeEndElement();
        }
    }

    private static void renderIndividualLabel(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY,
            GeneratingOptions generatingOptions) throws XMLStreamException {

        Name name = individual.getName();

        if (name != null) {

            Rect rect = new Rect(individual.getBoundaryRect());
            int fontSize = generatingOptions.getMainFontMetrics().getFont().getSize();
            double textPadding = (GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS - fontSize) / 2.0;

            String firstName = name.getFirst();
            String middleName = name.getMiddle();
            String lastName = name.getLast();
            String lastName2 = name.getLast2();

            List<String> nameList = new ArrayList<>();

            if (firstName != null) {
                nameList.add(firstName);
            }
            if (middleName != null) {
                nameList.add(middleName);
            }
            if (lastName != null) {
                nameList.add(lastName);
            }
            if (lastName2 != null) {
                nameList.add("(" + lastName2 + ")");
            }

            List<String> wrappedLineList = StringUtil.getWrappedLineList(String.join(" ", nameList),
                    rect.getWidth() - 2 * 8, generatingOptions.getMainFontMetrics());

            int baseTopY = shiftY - individual.getPosition().getY() + GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS;

            boolean isHyperlink = individual.getHyperlink() != null;

            for (int i = 0; i < wrappedLineList.size(); i++) {

                String line = wrappedLineList.get(i);
                double lineWidth = generatingOptions.getMainFontMetrics().stringWidth(line);

                int topY = baseTopY + i * GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS;

                writer.writeStartElement("rect");
                writer.writeAttribute("x", String.valueOf(individual.getPosition().getX() - lineWidth / 2 - shiftX));
                writer.writeAttribute("y", String.valueOf(topY));
                writer.writeAttribute("width", String.valueOf(lineWidth));
                writer.writeAttribute("height", String.valueOf(GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS));
                writer.writeAttribute("class", "individual-label");
                writer.writeEndElement();
            }

            for (int i = 0; i < wrappedLineList.size(); i++) {

                int topY = baseTopY + i * GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS;

                writer.writeStartElement("text");
                writer.writeAttribute("x", String.valueOf(individual.getPosition().getX() - shiftX));
                writer.writeAttribute("y",
                        String.valueOf(topY - textPadding + GeneratingOptions.MAIN_LINE_HEIGHT_IN_PIXELS));
                writer.writeAttribute("class", isHyperlink ? "individual-label-hyperlink" : "individual-label");

                writer.writeCharacters(wrappedLineList.get(i));
                writer.writeEndElement();
            }
        }
    }

    private static void renderActiveArea(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY,
            GeneratingOptions generatingOptions) throws XMLStreamException {

        int boxSize = 8;

        Rect rect = new Rect(individual.getBoundaryRect());

        writer.writeStartElement("rect");
        writer.writeAttribute("id", individual.getId() + "-bb");
        writer.writeAttribute("x", String.valueOf(rect.getX() - shiftX + boxSize));
        writer.writeAttribute("y", String.valueOf(shiftY - rect.getY() + boxSize));
        writer.writeAttribute("width", String.valueOf(rect.getWidth() - 2 * boxSize));
        writer.writeAttribute("height", String.valueOf(rect.getHeight() - 2 * boxSize));
        writer.writeAttribute("class", "individual-active-area");
        writer.writeEndElement();
    }

    private static String getLabelStyleAttribute(LabelStyle labelStyle) {

        StringBuilder style = new StringBuilder();

        style.append("fill: ");
        style.append(labelStyle.getFillColor().getHex());
        style.append(";stroke: ");
        style.append(labelStyle.getBorder().getColor().getHex());
        style.append(";stroke-width: ");
        style.append(getStrokeWidth(labelStyle.getBorder().getSize(), 3.0));

        return style.toString();
    }

    private static String getStrokeWidth(Size size, double defaultStrokeWidth) {
        double factor = STROKE_WIDTH_SCALE_FACTOR_MAP.getOrDefault(size, 1.0);
        return String.valueOf(defaultStrokeWidth * factor);
    }

    private static int getLabelBaseX(int rectX, int width, LabelStyle labelStyle) {

        int padding = labelStyle.getPadding();

        switch (labelStyle.getHorizontalAlignment()) {
            case CENTER:
                return (int) (rectX + width / 2.0);
            case RIGHT:
                return rectX + width - padding;
            default:
                return rectX + padding;
        }
    }

    private static String getLabelTextAnchor(LabelStyle labelStyle) {
        switch (labelStyle.getHorizontalAlignment()) {
            case CENTER:
                return "middle";
            case RIGHT:
                return "end";
            default:
                return "start";
        }
    }
}
