/*-
 * #%L
 * jaxrswb-swagger1-gen
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

package de.maggu2810.jaxrswb.swagger1.gen.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.maggu2810.jaxrswb.gen.JaxRsWhiteboardGeneratorConfig;
import de.maggu2810.jaxrswb.swagger1.gen.JaxRsWhiteboardSwaggerGenerator;
import de.maggu2810.jaxrswb.swagger1.gen.JaxRsWhiteboardSwaggerSpecialGenerator;
import de.maggu2810.jaxrswb.utils.JaxRsHelper;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import io.swagger.util.Json;

/**
 * An JAX-RS Whiteboard Swagger 1 generator implementation.
 *
 * @author Markus Rathgeb
 */
@Component(name = "de.maggu2810.jaxrswb.swagger1.gen", service = { JaxRsWhiteboardSwaggerSpecialGenerator.class,
        JaxRsWhiteboardSwaggerGenerator.class })
@Designate(ocd = JaxRsWhiteboardGeneratorConfig.class)
public class JaxRsWhiteboardSwaggerGeneratorImpl implements JaxRsWhiteboardSwaggerSpecialGenerator {

    private static class SwaggerReader extends Reader {
        public SwaggerReader(final Swagger swagger) {
            super(swagger);
        }

        @Override
        public Swagger read(final Class<?> cls, final String parentPath, final String parentMethod,
                final boolean isSubresource, final String[] parentConsumes, final String[] parentProduces,
                final Map<String, Tag> parentTags, final List<Parameter> parentParameters) {
            return super.read(cls, parentPath, parentMethod, isSubresource, parentConsumes, parentProduces, parentTags,
                    parentParameters);
        }
    }

    private final BundleContext bc;
    private final JaxRsWhiteboardGeneratorConfig config;

    /**
     * Creates a new instance.
     *
     * @param bc the bundle context
     * @param config the configuration
     */
    @Activate
    public JaxRsWhiteboardSwaggerGeneratorImpl(final BundleContext bc, final JaxRsWhiteboardGeneratorConfig config) {
        this.bc = bc;
        this.config = config;
    }

    @Override
    public Swagger generate() {
        final Swagger swagger = newPreConfiguredSwagger(config);
        final SwaggerReader reader = new SwaggerReader(swagger);

        final JaxRsHelper jaxRsHelper = new JaxRsHelper(bc);
        final Map<String, Set<Class<?>>> basePathAndClasses = jaxRsHelper.getBasePathAndClasses();
        basePathAndClasses.forEach((basePath, classes) -> {
            classes.forEach(clazz -> {
                final String parentPath = basePath.startsWith("/") ? basePath : "/" + basePath;
                reader.read(clazz, parentPath, null, false, new String[0], new String[0],
                        new LinkedHashMap<String, Tag>(), new ArrayList<Parameter>());
            });
        });

        return reader.getSwagger();
    }

    @Override
    public String toJSON(final Swagger info) throws IOException {
        final ObjectMapper mapper = Json.mapper();
        return mapper.writeValueAsString(info);
    }

    @Override
    public Map<String, Object> toMap(final Swagger info) throws IOException {
        final ObjectMapper mapper = Json.mapper();
        final String jsonString = mapper.writeValueAsString(info);
        return mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
        });
    }

    private Swagger newPreConfiguredSwagger(final JaxRsWhiteboardGeneratorConfig config) {
        final Swagger swagger = new Swagger();

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
        infoAdded |= ifNotEmpty(config.info_version(), value -> info.setVersion(value));

        // Swagger
        ifTrue(infoAdded, () -> swagger.setInfo(info));

        return swagger;
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
