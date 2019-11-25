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

package de.maggu2810.osgi.jaxrswb.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;

import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS Whiteboard helper.
 *
 * @author Markus Rathgeb
 */
public class JaxRsHelper {

    private final Logger logger = LoggerFactory.getLogger(JaxRsHelper.class);
    private final BundleContext bc;

    public JaxRsHelper(final BundleContext bc) {
        this.bc = bc;
    }

    public Map<String, Set<Class<?>>> getBasePathAndClasses() {
        final Map<ServiceReference<Application>, Set<ServiceReference<?>>> appsAndResources = new HashMap<>();

        final ServiceReference<Application> jaxRsApplicationDefaultRef = getJaxRsDefaultApplication();
        ServiceReference<?>[] jaxRsResourceRefs;
        try {
            jaxRsResourceRefs = bc.getServiceReferences((String) null, "(osgi.jaxrs.resource=true)");
        } catch (final InvalidSyntaxException ex) {
            throw new IllegalStateException("The hardcoded filter expression must be valid!.");
        }
        if (jaxRsResourceRefs != null) {
            for (final ServiceReference<?> ref : jaxRsResourceRefs) {
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
                appsAndResources.computeIfAbsent(jaxRsApplicationRef, key -> new HashSet<>()).add(ref);
            }
        }

        final Map<String, Set<Class<?>>> basePathAndClasses = new HashMap<>();
        appsAndResources.forEach((appRef, resources) -> {
            final Set<Class<?>> classes = new HashSet<>();
            resources.forEach(ref -> forService(ref, srv -> classes.add(srv.getClass())));
            // String basePath = buildPath(getJaxRsApplicationChain(appRef, jaxRsApplicationDefaultRef));
            String basePath = Optional.ofNullable(getJaxRsApplicationBase(appRef)).orElse("");
            if (basePath == null) {
                basePath = "";
            }
            if (basePathAndClasses.put(basePath, classes) != null) {
                throw new IllegalStateException("base path conflict");
            }
            ;
        });

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
            jaxRsApplicationDefaultRefs = bc.getServiceReferences(Application.class, "(osgi.jaxrs.name=.default)");
        } catch (final InvalidSyntaxException ex) {
            throw new IllegalStateException("The hardcoded filter expression must be valid!.");
        }
        if (jaxRsApplicationDefaultRefs == null) {
            return null;
        }
        return getTopRanked(jaxRsApplicationDefaultRefs);
    }

    private @Nullable List<ServiceReference<Application>> getJaxRsApplicationChain(
            final ServiceReference<Application> ref, final @Nullable ServiceReference<Application> refDfl) {
        final List<ServiceReference<Application>> appChain = new LinkedList<>();
        appChain.add(ref);
        if (ref.equals(refDfl)) {
            return appChain;
        }
        for (ServiceReference<Application> refCur = ref; refCur != refDfl;) {
            final String jaxRsApplicationSelect = getJaxRsApplicationSelect(refCur);
            final ServiceReference<Application> jaxRsApplicationRef;
            if (jaxRsApplicationSelect == null) {
                jaxRsApplicationRef = refDfl;
            } else {
                final Collection<ServiceReference<Application>> jaxRsApplicationRefs;
                try {
                    jaxRsApplicationRefs = bc.getServiceReferences(Application.class, jaxRsApplicationSelect);
                } catch (final InvalidSyntaxException ex) {
                    logger.debug("Ignore invalid syntax.", ex);
                    throw new IllegalStateException("We cannot build chain because of an invalid target filter", ex);
                }
                jaxRsApplicationRef = getTopRanked(jaxRsApplicationRefs);
            }
            if (jaxRsApplicationRef != null) {
                if (appChain.contains(jaxRsApplicationRef)) {
                    throw new IllegalStateException("app recursion");
                }
                appChain.add(jaxRsApplicationRef);
            }
            refCur = jaxRsApplicationRef;
        }
        return appChain;
    }

    private String buildPath(final List<ServiceReference<Application>> appRefs) {
        String path = "";
        for (final ServiceReference<Application> appRef : appRefs) {
            String base = getJaxRsApplicationBase(appRef);
            if (base == null) {
                base = "";
            } else {
                if (base.endsWith("/")) {
                    base = base.substring(0, base.length() - 1);
                }
            }
            if (base.isEmpty()) {
                continue;
            }
            if (path.isEmpty()) {
                path = base;
            } else {
                path = base + "/" + path;
            }
        }
        return path;
    }

    private void print(final @Nullable ServiceReference<?> reference) {
        if (reference == null) {
            System.out.println("<null>");
        } else {
            System.out.println(Arrays.stream(reference.getPropertyKeys()).map(key -> {
                final Object value = reference.getProperty(key);
                final String valueStr;
                if (value == null) {
                    valueStr = "<null>";
                } else {
                    final Class<?> cls = value.getClass();
                    if (cls != null && cls.isArray()) {
                        valueStr = Arrays.toString((Object[]) value);
                    } else {
                        valueStr = value.toString();
                    }
                }
                return String.format("%s: %s", key, valueStr);
            }).collect(Collectors.joining("; ", "properties: ", "")));
        }
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

    private @Nullable String getJaxRsName(final ServiceReference<?> app) {
        final Object obj = app.getProperty(JaxrsWhiteboardConstants.JAX_RS_NAME);
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return null;
        }
    }

    private @Nullable String getJaxRsApplicationBase(final ServiceReference<?> ref) {
        final Object obj = ref.getProperty(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE);
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return null;
        }
    }

    private @Nullable String getJaxRsApplicationSelect(final ServiceReference<?> ref) {
        final Object obj = ref.getProperty(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT);
        if (obj instanceof String) {
            return (String) obj;
        } else {
            return null;
        }
    }

    private <T> void forService(final ServiceReference<T> ref, final Consumer<T> consumer) {
        final T srv = bc.getService(ref);
        try {
            consumer.accept(srv);
        } finally {
            bc.ungetService(ref);
        }
    }
}
