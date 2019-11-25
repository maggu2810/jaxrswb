/*-
 * #%L
 * jaxrswb-oapi-gen
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

package de.maggu2810.osgi.jaxrswb.oapi.gen.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.maggu2810.osgi.jaxrswb.gen.JaxRsWhiteboardGeneratorConfig;
import de.maggu2810.osgi.jaxrswb.oapi.gen.JaxRsWhiteboardOpenAPIGenerator;
import de.maggu2810.osgi.jaxrswb.utils.JaxRsHelper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * An JAX-RS Whiteboard OpenAPI generator implementation.
 *
 * @author Markus Rathgeb
 */
@Component(name = "de.maggu2810.osgi.jaxrswb.oapi.gen")
@Designate(ocd = JaxRsWhiteboardGeneratorConfig.class)
public class JaxRsWhiteboardOpenAPIGeneratorImpl implements JaxRsWhiteboardOpenAPIGenerator {

    private final BundleContext bc;
    private final JaxRsWhiteboardGeneratorConfig config;

    /**
     * Creates a new instance.
     *
     * @param bc the bundle context
     * @param config the configuration
     */
    @Activate
    public JaxRsWhiteboardOpenAPIGeneratorImpl(final BundleContext bc, final JaxRsWhiteboardGeneratorConfig config) {
        this.bc = bc;
        this.config = config;
    }

    @Override
    public OpenAPI generate() {
        final OpenAPI openAPI = newPreConfiguredOpenAPI(config);
        final Reader reader = new Reader(openAPI);

        final JaxRsHelper jaxRsHelper = new JaxRsHelper(bc);
        final Map<String, Set<Class<?>>> basePathAndClasses = jaxRsHelper.getBasePathAndClasses();
        basePathAndClasses.forEach((basePath, classes) -> {
            classes.forEach(clazz -> {
                final String parentPath = basePath.startsWith("/") ? basePath : "/" + basePath;
                reader.read(clazz, parentPath, null, false, null, null, new LinkedHashSet<String>(),
                        new ArrayList<Parameter>(), new HashSet<Class<?>>());
            });
        });

        return reader.getOpenAPI();
    }

    @Override
    public String toJSON(final OpenAPI info) throws IOException {
        final ObjectMapper mapper = Json.mapper();
        return mapper.writeValueAsString(generate());
    }

    @Override
    public Map<String, Object> toMap(final OpenAPI info) throws IOException {
        final ObjectMapper mapper = Json.mapper();
        final String jsonString = mapper.writeValueAsString(info);
        return mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
        });
    }

    private OpenAPI newPreConfiguredOpenAPI(final JaxRsWhiteboardGeneratorConfig config) {
        final OpenAPI openAPI = new OpenAPI();

        final Info info = new Info();
        // Has something been added to "info"
        boolean infoAdded = false;

        // Contact
        final Contact contact = new Contact();
        // Has something been added to contact
        boolean contactAdded = false;
        contactAdded |= ifNotEmpty(config.contact_name(), value -> contact.setName(value));
        contactAdded |= ifNotEmpty(config.contact_mail(), value -> contact.setEmail(value));

        // Info
        infoAdded |= ifTrue(contactAdded, () -> info.setContact(contact));
        infoAdded |= ifNotEmpty(config.info_title(), value -> info.setTitle(value));
        infoAdded |= ifNotEmpty(config.info_description(), value -> info.setDescription(value));

        // OpenAPI
        ifTrue(infoAdded, () -> openAPI.setInfo(info));

        return openAPI;
    }

    private boolean ifNotEmpty(final String str, final Consumer<String> func) {
        if (str.isEmpty()) {
            return false;
        } else {
            func.accept(str);
            return true;
        }
    }

    private boolean ifTrue(final boolean bool, final Runnable func) {
        if (bool) {
            func.run();
            return true;
        } else {
            return false;
        }
    }

}
