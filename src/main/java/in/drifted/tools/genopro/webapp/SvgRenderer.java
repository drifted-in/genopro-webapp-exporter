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

import in.drifted.tools.genopro.model.Birth;
import in.drifted.tools.genopro.model.BoundaryRect;
import in.drifted.tools.genopro.model.DateFormatter;
import in.drifted.tools.genopro.model.Death;
import in.drifted.tools.genopro.model.Family;
import in.drifted.tools.genopro.model.Gender;
import in.drifted.tools.genopro.model.GenoMap;
import in.drifted.tools.genopro.model.GenoMapData;
import in.drifted.tools.genopro.model.Hyperlink;
import in.drifted.tools.genopro.model.Individual;
import in.drifted.tools.genopro.model.Name;
import in.drifted.tools.genopro.model.PedigreeLink;
import in.drifted.tools.genopro.model.Position;
import in.drifted.tools.genopro.model.Rect;
import in.drifted.tools.genopro.webapp.model.RenderOptions;
import in.drifted.tools.genopro.util.StringUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class SvgRenderer {

    public static void render(GenoMapData genoMapData, OutputStream outputStream, RenderOptions renderOptions) throws IOException {

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
            writer.writeAttribute("width", "100%");
            writer.writeAttribute("height", "100%");
            Position topLeft = genoMap.getBoundaryRect().getTopLeft();
            Position bottomRight = genoMap.getBoundaryRect().getBottomRight();
            int shiftX = topLeft.getX();
            int shiftY = topLeft.getY() + renderOptions.getMainLineHeightInPixels() / 2;
            int width = bottomRight.getX() - topLeft.getX();
            int height = topLeft.getY() - bottomRight.getY();
            writer.writeAttribute("viewBox", "0 0 " + width + " " + height);

            for (Family family : genoMapData.getFamilyCollection()) {
                renderFamilyRelations(writer, family, shiftX, shiftY, renderOptions);
            }

            for (Individual individual : genoMapData.getIndividualCollection()) {
                if (!individual.isAnonymized()) {
                    renderIndividual(writer, individual, shiftX, shiftY, renderOptions);
                }
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();

        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private static void renderFamilyRelations(XMLStreamWriter writer, Family family, int shiftX, int shiftY, RenderOptions renderOptions) throws XMLStreamException {

        Position position = family.getPosition();
        BoundaryRect topBoundaryRect = family.getTopBoundaryRect();
        BoundaryRect bottomBoundaryRect = family.getBottomBoundaryRect();

        Rect topRect = null;
        Rect bottomRect = null;

        if (topBoundaryRect != null) {

            topRect = new Rect(topBoundaryRect);

            int y = shiftY - topRect.getY();

            writer.writeStartElement("path");
            writer.writeAttribute("d", "M" + (topRect.getX() - shiftX) + " " + y + "h" + topRect.getWidth());
            writer.writeAttribute("class", "family-line");
            writer.writeEndElement();

            if (family.getLabel() != null) {

                String label = family.getLabel();

                int labelWidthInPixels = renderOptions.getMainFontMetrics().stringWidth(label);
                double centerX = topRect.getX() - shiftX + topRect.getWidth() / 2.0;
                int fontSize = renderOptions.getMainFontMetrics().getFont().getSize();
                double textPadding = (renderOptions.getMainLineHeightInPixels() - fontSize) / 2.0;

                writer.writeStartElement("rect");
                writer.writeAttribute("x", String.valueOf(centerX - labelWidthInPixels / 2.0));
                writer.writeAttribute("y", String.valueOf(y - 1.3 * renderOptions.getMainLineHeightInPixels()));
                writer.writeAttribute("width", String.valueOf(labelWidthInPixels));
                writer.writeAttribute("height", String.valueOf(renderOptions.getMainLineHeightInPixels()));
                writer.writeAttribute("class", "family-label");
                writer.writeEndElement();

                writer.writeStartElement("text");
                writer.writeAttribute("x", String.valueOf(centerX));
                writer.writeAttribute("y", String.valueOf(y - 0.3 * renderOptions.getMainLineHeightInPixels() - textPadding));
                writer.writeAttribute("class", "family-label");
                writer.writeCharacters(label);
                writer.writeEndElement();
            }
        }

        if (bottomBoundaryRect != null) {

            // if children are anonymized, the bottom rect can be skipped
            boolean hasChildren = false;

            for (PedigreeLink pedigreeLink : family.getPedigreeLinkList()) {
                if (pedigreeLink.getType() != PedigreeLink.PARENT) {
                    hasChildren = true;
                    break;
                }
            }

            if (hasChildren) {

                bottomRect = new Rect(bottomBoundaryRect);

                writer.writeStartElement("path");
                writer.writeAttribute("d", "M" + (position.getX() - shiftX) + " " + (shiftY - position.getY()) + "v" + (position.getY() - bottomRect.getY()));
                writer.writeAttribute("class", "family-line");
                writer.writeEndElement();

                writer.writeStartElement("path");
                writer.writeAttribute("d", "M" + (bottomRect.getX() - shiftX) + " " + (shiftY - bottomRect.getY()) + "h" + bottomRect.getWidth());
                writer.writeAttribute("class", "family-line");
                writer.writeEndElement();
            }
        }

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

            if (pedigreeLink.getType() == PedigreeLink.PARENT) {
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

                writer.writeStartElement("path");
                writer.writeAttribute("d", pathData.toString());

                switch (pedigreeLink.getType()) {

                    case PedigreeLink.PARENT:
                        writer.writeAttribute("class", "parent");
                        break;

                    case PedigreeLink.ADOPTED:
                        writer.writeAttribute("class", "adopted");
                        break;

                    default:
                        writer.writeAttribute("class", "biological");
                        break;
                }

                writer.writeEndElement();
            }
        }
    }

    private static void renderIndividual(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY, RenderOptions renderOptions) throws XMLStreamException {

        writer.writeStartElement("g");
        writer.writeAttribute("id", individual.getId());

        Hyperlink hyperlink = individual.getHyperlink();

        if (hyperlink != null) {
            writer.writeAttribute("data-target-id", hyperlink.getId());
        }

        renderIndividualDates(writer, individual, shiftX, shiftY, renderOptions);
        renderIndividualSymbol(writer, individual, shiftX, shiftY, renderOptions);
        renderIndividualAge(writer, individual, shiftX, shiftY, renderOptions);

        if (hyperlink != null) {
            renderActiveArea(writer, individual, shiftX, shiftY, renderOptions);
            renderIndividualLabel(writer, individual, shiftX, shiftY, renderOptions);

        } else {
            renderIndividualLabel(writer, individual, shiftX, shiftY, renderOptions);
            renderActiveArea(writer, individual, shiftX, shiftY, renderOptions);
        }

        writer.writeEndElement();
    }

    private static void renderIndividualDates(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY, RenderOptions renderOptions) throws XMLStreamException {

        DateFormatter dateFormatter = renderOptions.getDateFormatter();

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
                deathLabel = renderOptions.getResourceBundle().getString("deathAbbrev") + " " + deathLabel;
            }
            dateList.add(deathLabel);
        }

        Rect rect = new Rect(individual.getBoundaryRect());

        int fontSize = renderOptions.getMainFontMetrics().getFont().getSize();
        double textPadding = (renderOptions.getMainLineHeightInPixels() - fontSize) / 2.0;

        int baseTopY = shiftY - rect.getY() + renderOptions.getMainLineHeightInPixels() / 2;

        for (int i = 0; i < dateList.size(); i++) {

            String date = dateList.get(i);
            int dateWidth = renderOptions.getMainFontMetrics().stringWidth(date);

            int topY = baseTopY + i * renderOptions.getMainLineHeightInPixels();

            writer.writeStartElement("rect");
            writer.writeAttribute("x", String.valueOf(individual.getPosition().getX() - dateWidth / 2 - shiftX));
            writer.writeAttribute("y", String.valueOf(topY));
            writer.writeAttribute("width", String.valueOf(dateWidth));
            writer.writeAttribute("height", String.valueOf(renderOptions.getMainLineHeightInPixels()));
            writer.writeAttribute("class", "individual-label");
            writer.writeEndElement();

            writer.writeStartElement("text");
            writer.writeAttribute("x", String.valueOf(individual.getPosition().getX() - shiftX));
            writer.writeAttribute("y", String.valueOf(topY - textPadding + renderOptions.getMainLineHeightInPixels()));
            writer.writeAttribute("class", "individual-label");

            writer.writeCharacters(dateList.get(i));
            writer.writeEndElement();
        }
    }

    private static void renderIndividualSymbol(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY, RenderOptions renderOptions) throws XMLStreamException {

        Position position = individual.getPosition();

        switch (individual.getGender()) {

            case Gender.MALE: {
                writer.writeStartElement("rect");
                writer.writeAttribute("x", String.valueOf(position.getX() - shiftX - 9));
                writer.writeAttribute("y", String.valueOf(shiftY - position.getY() - 9));
                writer.writeAttribute("width", "18");
                writer.writeAttribute("height", "18");
                writer.writeAttribute("class", "individual-symbol");
                writer.writeEndElement();

                break;
            }

            case Gender.FEMALE: {
                writer.writeStartElement("circle");
                writer.writeAttribute("cx", String.valueOf(position.getX() - shiftX));
                writer.writeAttribute("cy", String.valueOf(shiftY - position.getY()));
                writer.writeAttribute("r", "9");
                writer.writeAttribute("class", "individual-symbol");
                writer.writeEndElement();

                break;
            }

            case Gender.UNKNOWN: {
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

    private static void renderIndividualAge(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY, RenderOptions renderOptions) throws XMLStreamException {

        Position position = individual.getPosition();

        if (individual.isDead()) {

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

            writer.writeStartElement("path");
            writer.writeAttribute("d", dataBuilder.toString());
            writer.writeAttribute("class", "individual-deceased");
            writer.writeEndElement();
        }

        Birth birth = individual.getBirth();
        Death death = individual.getDeath();

        String age = renderOptions.getAgeFormatter().format(birth, death);

        if ((death == null || !death.hasDate()) && individual.isDead()) {
            age = null;
        }

        if (age != null) {

            // smaller font requires smaller metrics
            double ageWidth = renderOptions.getAgeFontMetrics().stringWidth(age);
            int fontSize = renderOptions.getAgeFontMetrics().getFont().getSize();

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

    private static void renderIndividualLabel(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY, RenderOptions renderOptions) throws XMLStreamException {

        Name name = individual.getName();

        if (name != null) {

            Rect rect = new Rect(individual.getBoundaryRect());
            int fontSize = renderOptions.getMainFontMetrics().getFont().getSize();
            double textPadding = (renderOptions.getMainLineHeightInPixels() - fontSize) / 2.0;

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

            List<String> wrappedLineList = StringUtil.getWrappedLineList(String.join(" ", nameList), rect.getWidth() - 2 * 8, renderOptions.getMainFontMetrics());

            int baseTopY = shiftY - individual.getPosition().getY() + renderOptions.getMainLineHeightInPixels();

            boolean isHyperlink = individual.getHyperlink() != null;

            for (int i = 0; i < wrappedLineList.size(); i++) {

                String line = wrappedLineList.get(i);
                double lineWidth = renderOptions.getMainFontMetrics().stringWidth(line);

                int topY = baseTopY + i * renderOptions.getMainLineHeightInPixels();

                writer.writeStartElement("rect");
                writer.writeAttribute("x", String.valueOf(individual.getPosition().getX() - lineWidth / 2 - shiftX));
                writer.writeAttribute("y", String.valueOf(topY));
                writer.writeAttribute("width", String.valueOf(lineWidth));
                writer.writeAttribute("height", String.valueOf(renderOptions.getMainLineHeightInPixels()));
                writer.writeAttribute("class", "individual-label");
                writer.writeEndElement();
            }

            for (int i = 0; i < wrappedLineList.size(); i++) {

                int topY = baseTopY + i * renderOptions.getMainLineHeightInPixels();

                writer.writeStartElement("text");
                writer.writeAttribute("x", String.valueOf(individual.getPosition().getX() - shiftX));
                writer.writeAttribute("y", String.valueOf(topY - textPadding + renderOptions.getMainLineHeightInPixels()));
                writer.writeAttribute("class", isHyperlink ? "individual-label-hyperlink" : "individual-label");

                writer.writeCharacters(wrappedLineList.get(i));
                writer.writeEndElement();
            }
        }
    }

    private static void renderActiveArea(XMLStreamWriter writer, Individual individual, int shiftX, int shiftY, RenderOptions renderOptions) throws XMLStreamException {

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
}
