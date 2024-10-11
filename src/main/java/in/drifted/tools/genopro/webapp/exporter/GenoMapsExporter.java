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

import in.drifted.tools.genopro.core.model.GenoMap;
import in.drifted.tools.genopro.core.model.GenoMapData;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GenoMapsExporter {

    public static void export(Path genomapsPath, List<GenoMapData> genoMapDataList) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(genomapsPath)) {

            writer.write("const genoMapMap = new Map()\n");

            for (GenoMapData genoMapData : genoMapDataList) {
                GenoMap genoMap = genoMapData.genoMap();
                if (genoMap.title() != null) {
                    writer.write("genoMapMap.set(\"" + genoMap.id() + "\",\"" + genoMap.title() + "\")\n");
                }
            }
        }
    }
}
