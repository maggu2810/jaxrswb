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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "JAX-RS Whiteboard information generator configuration")
public @interface JaxRsWhiteboardGeneratorConfig {
    @AttributeDefinition(description = "the title (not used if empty)")
    String info_title() default "";

    @AttributeDefinition(description = "the description (not used if empty)")
    String info_description() default "";

    @AttributeDefinition(description = "the version (not used if empty)")
    String info_version() default "";

    @AttributeDefinition(description = "the contact's name (not used if empty)")
    String contact_name() default "";

    @AttributeDefinition(description = "the contact's email (not used if empty)")
    String contact_mail() default "";
}
