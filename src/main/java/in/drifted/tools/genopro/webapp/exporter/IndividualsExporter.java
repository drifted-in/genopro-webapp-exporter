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

import in.drifted.tools.genopro.core.model.FamilyRelation;
import in.drifted.tools.genopro.core.model.GenoMapData;
import in.drifted.tools.genopro.core.model.Individual;
import in.drifted.tools.genopro.core.model.Name;
import in.drifted.tools.genopro.core.util.DocumentDataUtil;
import in.drifted.tools.genopro.core.util.MapUtil;
import in.drifted.tools.genopro.core.util.comparator.IndividualBirthDateComparator;
import in.drifted.tools.genopro.core.util.formatter.DateFormatter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndividualsExporter {

    public static void export(Path individualsPath, List<GenoMapData> genoMapDataList, DateFormatter dateFormatter) throws IOException {

        Map<String, Individual> individualMap = getValidIndividualMap(genoMapDataList);
        individualMap = MapUtil.sortByValue(individualMap, new IndividualBirthDateComparator(true));

        Map<String, FamilyRelation> familyRelationMap = DocumentDataUtil.getFamilyRelationMap(genoMapDataList, individualMap);

        try (BufferedWriter writer = Files.newBufferedWriter(individualsPath)) {

            writer.write("const iMap = new Map()\n");

            for (Individual individual : individualMap.values()) {

                Collection<String> data = new ArrayList<>();

                String individualId = individual.id();
                Name name = individual.name();

                if (name != null) {

                    FamilyRelation familyRelation = familyRelationMap.get(individualId);

                    // genomap
                    data.add(individual.genoMap().id());

                    // names
                    String firstName = name.first();
                    String middleName = name.middle();
                    String lastName = name.last();

                    data.add(firstName != null ? firstName : "");
                    data.add(middleName != null ? middleName : "");
                    data.add(lastName != null ? lastName : "");

                    // birth
                    data.add(DocumentDataUtil.getFormattedDate(individual.birth(), dateFormatter));

                    // death
                    data.add(DocumentDataUtil.getFormattedDate(individual.death(), dateFormatter));

                    // mates
                    data.add((familyRelation.mateIdList() != null) ? String.join(",", familyRelation.mateIdList()) : "");

                    // father
                    data.add(familyRelation.fatherId() != null ? familyRelation.fatherId() : "");

                    // mother
                    data.add(familyRelation.motherId() != null ? familyRelation.motherId() : "");

                    writer.write("iMap.set(\"" + individualId + "\",[\"" + String.join("\",\"", data) + "\"])\n");
                }
            }
        }
    }

    private static Map<String, Individual> getValidIndividualMap(List<GenoMapData> genoMapDataList) {

        Map<String, Individual> individualMap = new HashMap<>();

        for (GenoMapData genoMapData : genoMapDataList) {
            for (Individual individual : genoMapData.individualSet()) {
                if (individual.name() != null) {
                    individualMap.put(individual.id(), individual);
                }
            }
        }

        return individualMap;
    }
}
