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

package de.maggu2810.jaxrswb.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * JAX-RS Whiteboard helper.
 *
 * @author Markus Rathgeb
 */
public class JaxRsHelper {

    private final BundleContext bc;

    /**
     * Creates a new JAX-RS helper instance.
     *
     * @param bc the bundle context
     */
    public JaxRsHelper(final BundleContext bc) {
        this.bc = bc;
    }

    /**
     * Gets all base paths and its classes.
     *
     * @return map of base paths and the correspondent classes
     */
    public Map<String, Set<Class<?>>> getBasePathAndClasses() {
        // Map that holds the service reference to the applications as key.
        // For every application it holds the JAX-RS resources that are using that application.
        final Map<ServiceReference<Application>, Set<ServiceReference<?>>> appsAndResources = new HashMap<>();

        // Gets the default application.
        final ServiceReference<Application> jaxRsApplicationDefaultRef = getJaxRsDefaultApplication();

        // Gets all JAX-RS resources.
        ServiceReference<?>[] jaxRsResourceRefs;
        try {
            jaxRsResourceRefs = bc.getServiceReferences((String) null, "(osgi.jaxrs.resource=true)");
        } catch (final InvalidSyntaxException ex) {
            throw new IllegalStateException("The hardcoded filter expression must be valid!.");
        }
        if (jaxRsResourceRefs != null) {
            // Inspect every JAX-RS resource.
            for (final ServiceReference<?> ref : jaxRsResourceRefs) {
                // Find the JAX-RS application for the JAX-RS resource.
                final String jaxRsApplicationSelect = getJaxRsApplicationSelect(ref);
                final ServiceReference<Application> jaxRsApplicationRef;
                if (jaxRsApplicationSelect == null) {
                    jaxRsApplicationRef = jaxRsApplicationDefaultRef;
                } else {
                    final Collection<ServiceReference<Application>> jaxRsApplicationRefs;
                    try {
                        jaxRsApplicationRefs = bc.getServiceReferences(Application.class, jaxRsApplicationSelect);
                    } catch (final InvalidSyntaxException ex) {
                        throw new IllegalStateException("We cannot build chain because of an invalid target filter",
                                ex);
                    }
                    jaxRsApplicationRef = getTopRanked(jaxRsApplicationRefs);
                }

                // Adds the JAX-RS to the respective JAX-RS application.
                appsAndResources.computeIfAbsent(jaxRsApplicationRef, key -> new HashSet<>()).add(ref);
            }
        }

        // Map that contains the base path of the application and the resource classes below that path.
        final Map<String, Set<Class<?>>> basePathAndClasses = new HashMap<>();
        appsAndResources.forEach((appRef, resources) -> {
            // Convert the set of service reference to a set of classes of the referenced services.
            final Set<Class<?>> classes = new HashSet<>();
            resources.stream().forEach(ref -> {
                final Object srv = bc.getService(ref);
                if (srv != null) {
                    try {
                        classes.add(srv.getClass());
                    } finally {
                        bc.ungetService(ref);
                    }
                }
            });

            // Resolve the base path.
            final String basePath = Optional.ofNullable(getJaxRsApplicationBase(appRef)).orElse("");

            // Add the base path with its classes to the map.
            if (basePathAndClasses.put(basePath, classes) != null) {
                throw new IllegalStateException("base path conflict");
            }
        });

        // Return the base path of the apps with its resources.
        return basePathAndClasses;
    }

    private @Nullable ServiceReference<Application> getJaxRsDefaultApplication() {
        // Get the default application.
        // The default application can be an app
        // * using osgi.jaxrs.name of .default
        // OR
        // * another application using a base of /
        Collection<ServiceReference<Application>> jaxRsApplicationDefaultRefs;
        try {
            jaxRsApplicationDefaultRefs = bc.getServiceReferences(Application.class,
                    "(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=.default)");
        } catch (final InvalidSyntaxException ex) {
            throw new IllegalStateException("The hardcoded filter expression must be valid!.");
        }
        if (jaxRsApplicationDefaultRefs == null) {
            return null;
        }
        return getTopRanked(jaxRsApplicationDefaultRefs);
    }

    private <T> @Nullable ServiceReference<T> getTopRanked(final Collection<ServiceReference<T>> coll) {
        Integer serviceRanking = null;
        ServiceReference<T> top = null;
        for (final ServiceReference<T> entry : coll) {
            final int ranking = getServiceRanking(entry);
            if (serviceRanking == null || ranking > serviceRanking) {
                serviceRanking = ranking;
                top = entry;
            }
        }
        return top;
    }

    private int getServiceRanking(final ServiceReference<?> ref) {
        final Object value = ref.getProperty(Constants.SERVICE_RANKING);
        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            return 0;
        }
    }

    private @Nullable String getJaxRsApplicationBase(final ServiceReference<?> ref) {
        return getSrvPropOptString(ref, JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE);
    }

    private @Nullable String getJaxRsApplicationSelect(final ServiceReference<?> ref) {
        return getSrvPropOptString(ref, JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT);
    }

    private @Nullable String getSrvPropOptString(final ServiceReference<?> ref, final String prop) {
        final Object obj = ref.getProperty(prop);
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return null;
        }
    }

}
