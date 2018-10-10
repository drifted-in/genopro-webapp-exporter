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

import in.drifted.tools.genopro.DataUtil;
import in.drifted.tools.genopro.model.DateFormatter;
import in.drifted.tools.genopro.model.FamilyRelation;
import in.drifted.tools.genopro.model.GenoMapData;
import in.drifted.tools.genopro.model.Individual;
import in.drifted.tools.genopro.model.Name;
import in.drifted.tools.genopro.util.MapUtil;
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
        individualMap = MapUtil.sortByValue(individualMap);

        Map<String, FamilyRelation> familyRelationMap = DataUtil.getFamilyRelationMap(genoMapDataList, individualMap);

        try (BufferedWriter writer = Files.newBufferedWriter(individualsPath)) {

            writer.write("const iMap = new Map()\n");

            for (Individual individual : individualMap.values()) {

                Collection<String> data = new ArrayList<>();

                String individualId = individual.getId();
                Name name = individual.getName();

                if (name != null) {

                    FamilyRelation familyRelation = familyRelationMap.get(individualId);

                    // genomap
                    data.add(individual.getGenoMap().getId());

                    // names
                    String firstName = name.getFirst();
                    String middleName = name.getMiddle();
                    String lastName = name.getLast();

                    data.add(firstName != null ? firstName : "");
                    data.add(middleName != null ? middleName : "");
                    data.add(lastName != null ? lastName : "");

                    // birth
                    data.add(DataUtil.getFormattedDate(individual.getBirth(), dateFormatter));

                    // death
                    data.add(DataUtil.getFormattedDate(individual.getDeath(), dateFormatter));

                    // mates
                    data.add((familyRelation.getMateIdList() != null) ? String.join(",", familyRelation.getMateIdList()) : "");

                    // father
                    data.add(familyRelation.getFatherId() != null ? familyRelation.getFatherId() : "");

                    // mother
                    data.add(familyRelation.getMotherId() != null ? familyRelation.getMotherId() : "");

                    writer.write("iMap.set(\"" + individualId + "\",[\"" + String.join("\",\"", data) + "\"])\n");
                }
            }
        }
    }

    private static Map<String, Individual> getValidIndividualMap(List<GenoMapData> genoMapDataList) {

        Map<String, Individual> individualMap = new HashMap<>();

        for (GenoMapData genoMapData : genoMapDataList) {
            for (Individual individual : genoMapData.getIndividualCollection()) {
                if (individual.getName() != null) {
                    individualMap.put(individual.getId(), individual);
                }
            }
        }

        return individualMap;
    }
}
