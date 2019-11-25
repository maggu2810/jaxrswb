/*-
 * #%L
 * jaxrswb-utils
 * %%
 * Copyright (C) 2019 maggu2810
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
 *
 * @param <T> the type of the specific information
 */
public interface JaxRsWhiteboardBaseSpecialGenerator<T> extends JaxRsWhiteboardBaseGenerator {

    /**
     * Creates a specific information object.
     *
     * @return a specific object
     */
    T generate();

    /**
     * Converts the specific reference to a JSON string.
     *
     * <p>
     * If you need a JSON string and would like to provide it to some special tooling you should prefer this method
     * instead of doing it yourself.
     * This method guarantees that the JSON string looks like expected by other tooling as it uses the upstream JSON
     * serializer.
     *
     * @param info the specific information
     * @return JSON string throws IOException
     * @throws IOException on conversion errors
     */
    String toJSON(T info) throws IOException;

    /**
     * Converts the specific reference to a map that could be used as DTO.
     *
     * <p>
     * If you would like to use the specific reference as e.g. a response of a REST entpoint or other of other code that
     * needs to serialize the object, we should prefer to use a DTO compatible data structure. Otherwise the serialized
     * object mostly looks different if different serializer implementations are used.
     * For example if you use a JAX-RS endpoint that produces JSON and a message body writer that is using Gson to
     * convert the entity to JSON, the result looks very different than using what is used by official Swagger
     * implementations (and this will result into a very strange behavior if you provide that JSON string to Swagger
     * UI).
     * So, let's build up a string-object-map and expect that this one will be nearly serialized identically regardless
     * of the chosen implementation.
     * To build that map use the mapper provided by Swagger to rely that this one is tested and working for the specific
     * reference.
     *
     * @param info the specific information
     * @return a map to be used as DTO
     * @throws IOException on conversion errors
     */
    Map<String, Object> toMap(T info) throws IOException;

    @Override
    default String generateJSON() throws IOException {
        return toJSON(generate());
    }

    @Override
    default Map<String, Object> generateMap() throws IOException {
        return toMap(generate());
    }

}
