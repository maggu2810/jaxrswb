/*-
 * #%L
 * jaxrswb-utils
 * %%
 * Copyright (C) 2019 - 2020 maggu2810
 * %%
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
 * #L%
 */

package de.maggu2810.jaxrswb.gen;

import java.io.IOException;
import java.util.Map;

/**
 * Base interface for a generator relying on JAX-RS Whiteboard.
 *
 * @author Markus Rathgeb
 */
public interface JaxRsWhiteboardBaseGenerator {

    /**
     * Generates a JSON string holding the specific generated information.
     *
     * @return JSON string
     * @throws IOException on conversion errors
     */
    String generateJSON() throws IOException;

    /**
     * Generates a map that could be used as DTO holding the specific generated information.
     *
     * @return a map to be used as DTO
     * @throws IOException on conversion errors
     */
    Map<String, Object> generateMap() throws IOException;

}
