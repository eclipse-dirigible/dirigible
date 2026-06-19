/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.sdk.http.Body;
import org.eclipse.dirigible.sdk.http.Context;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Delete;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.http.Patch;
import org.eclipse.dirigible.sdk.http.PathParam;
import org.eclipse.dirigible.sdk.http.Post;
import org.eclipse.dirigible.sdk.http.Put;
import org.eclipse.dirigible.sdk.http.QueryParam;
import org.eclipse.dirigible.sdk.security.Roles;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.controller.openapi.JavaControllerOpenApiPublisher;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Built-in {@link JavaClassConsumer} that registers client classes annotated with
 * {@link Controller} with the {@link ControllerRouter}. A controller may not also be a
 * {@link JavaHandler}; if a class carries both shapes it is rejected with an error log and skipped.
 *
 * <p>
 * Each registration walks the class via reflection once and produces an immutable
 * {@link ControllerEntry}. Hot-reload swaps the entry atomically through the router.
 */
@Component
@Order(300) // Run after the ComponentContainer (built in JavaLoader before this load pass) has
            // instantiated and injected every bean, so the controller instance fetched here is ready.
public class ControllerClassConsumer implements JavaClassConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerClassConsumer.class);

    private final ControllerRouter router;

    private final JavaControllerOpenApiPublisher openApiPublisher;

    private final ComponentContainer componentContainer;

    @Autowired
    public ControllerClassConsumer(ControllerRouter router, Optional<JavaControllerOpenApiPublisher> openApiPublisher,
            ComponentContainer componentContainer) {
        this.router = router;
        this.openApiPublisher = openApiPublisher.orElse(null);
        this.componentContainer = componentContainer;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return clazz.isAnnotationPresent(Controller.class);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Class<?> type = info.type();
        if (JavaHandler.class.isAssignableFrom(type)) {
            LOGGER.error("Class [{}] is annotated @Controller and implements JavaHandler — the two are mutually exclusive; skipped.",
                    info.fqn());
            return;
        }
        try {
            ControllerEntry entry = build(info);
            if (entry.routes()
                     .isEmpty()) {
                LOGGER.warn("Controller [{}] has no @Get/@Post/@Put/@Patch/@Delete methods; not registered.", info.fqn());
                return;
            }
            router.register(entry);
            if (openApiPublisher != null) {
                openApiPublisher.publish(entry);
            }
        } catch (Exception | LinkageError e) {
            // Includes LinkageError so a controller whose @Inject field references a class that failed
            // to compile (NoClassDefFoundError on getDeclaredFields) doesn't abort the rebuild and take
            // every other previously-registered controller with it.
            LOGGER.error("Failed to register controller [{}]: {}", info.fqn(), e.getMessage(), e);
        }
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        router.unregister(info.project(), info.fqn());
        if (openApiPublisher != null) {
            openApiPublisher.retract(info.project(), info.fqn());
        }
    }

    /**
     * Build a {@link ControllerEntry} from a {@code @Controller}-annotated class. Public so tests can
     * drive the build path without going through Spring; the production caller is
     * {@link #onClassLoaded}.
     */
    public ControllerEntry build(LoadedClass info) {
        Class<?> type = info.type();
        Object instance = componentContainer.instanceOf(type)
                                            .orElseThrow(() -> new IllegalStateException("Controller [" + info.fqn()
                                                    + "] was not instantiated by the bean container — check earlier logs for construction or injection errors."));
        List<String> classRoles = readRoles(type.getAnnotation(Roles.class));

        List<Route> routes = new ArrayList<>();
        Set<String> uniqueness = new HashSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic() || method.isBridge()) {
                continue;
            }
            HttpMethod httpMethod = httpMethodOf(method);
            if (httpMethod == null) {
                continue;
            }
            String template = pathTemplateOf(method);
            PathPattern.Compiled compiled = PathPattern.compile(template);
            List<String> roles = methodRoles(method, classRoles);
            List<ParamBinding> bindings = bindingsFor(method);
            Route route = new Route(httpMethod, template, compiled.pattern(), compiled.placeholders(), method, bindings, roles);
            String key = httpMethod.name() + " " + route.pathPattern()
                                                        .pattern();
            if (!uniqueness.add(key)) {
                LOGGER.warn("Controller [{}] has duplicate route [{} {}] — keeping the first; method [{}] ignored.", info.fqn(), httpMethod,
                        template, method.getName());
                continue;
            }
            routes.add(route);
        }
        routes.sort(Comparator.comparingInt(r -> PathPattern.specificity(r.pathTemplate(), r.placeholders()
                                                                                            .size())));

        return new ControllerEntry(info.project(), info.fqn(), ControllerRouter.fqnToBasePath(info.fqn()), instance, List.copyOf(routes));
    }

    private static HttpMethod httpMethodOf(Method method) {
        if (method.isAnnotationPresent(Get.class))
            return HttpMethod.GET;
        if (method.isAnnotationPresent(Post.class))
            return HttpMethod.POST;
        if (method.isAnnotationPresent(Put.class))
            return HttpMethod.PUT;
        if (method.isAnnotationPresent(Patch.class))
            return HttpMethod.PATCH;
        if (method.isAnnotationPresent(Delete.class))
            return HttpMethod.DELETE;
        return null;
    }

    private static String pathTemplateOf(Method method) {
        Get get = method.getAnnotation(Get.class);
        if (get != null)
            return get.value();
        Post post = method.getAnnotation(Post.class);
        if (post != null)
            return post.value();
        Put put = method.getAnnotation(Put.class);
        if (put != null)
            return put.value();
        Patch patch = method.getAnnotation(Patch.class);
        if (patch != null)
            return patch.value();
        Delete delete = method.getAnnotation(Delete.class);
        if (delete != null)
            return delete.value();
        return "";
    }

    private static List<String> methodRoles(Method method, List<String> classRoles) {
        Roles methodRoles = method.getAnnotation(Roles.class);
        if (methodRoles != null) {
            return readRoles(methodRoles);
        }
        return classRoles;
    }

    private static List<String> readRoles(Roles roles) {
        if (roles == null || roles.value().length == 0) {
            return List.of();
        }
        return List.copyOf(Arrays.asList(roles.value()));
    }

    private static List<ParamBinding> bindingsFor(Method method) {
        Parameter[] params = method.getParameters();
        List<ParamBinding> out = new ArrayList<>(params.length);
        boolean bodySeen = false;
        for (Parameter p : params) {
            ParamBinding binding = bindingFor(p, bodySeen);
            if (binding.kind() == ParamBinding.Kind.BODY) {
                bodySeen = true;
            }
            out.add(binding);
        }
        return List.copyOf(out);
    }

    private static ParamBinding bindingFor(Parameter p, boolean bodyAlreadyBound) {
        Body body = findAnnotation(p, Body.class);
        PathParam path = findAnnotation(p, PathParam.class);
        QueryParam query = findAnnotation(p, QueryParam.class);
        Context ctx = findAnnotation(p, Context.class);

        int present = (body != null ? 1 : 0) + (path != null ? 1 : 0) + (query != null ? 1 : 0) + (ctx != null ? 1 : 0);
        if (present > 1) {
            throw new IllegalStateException("Parameter [" + p.getName() + "] on method [" + p.getDeclaringExecutable()
                                                                                             .getName()
                    + "] has more than one of @Body/@PathParam/@QueryParam/@Context");
        }

        if (body != null) {
            if (bodyAlreadyBound) {
                throw new IllegalStateException("Method [" + p.getDeclaringExecutable()
                                                              .getName()
                        + "] declares more than one @Body parameter");
            }
            return new ParamBinding(ParamBinding.Kind.BODY, null, p.getType(), p.getParameterizedType());
        }
        if (path != null) {
            return new ParamBinding(ParamBinding.Kind.PATH, path.value(), p.getType(), p.getParameterizedType());
        }
        if (query != null) {
            return new ParamBinding(ParamBinding.Kind.QUERY, query.value(), p.getType(), p.getParameterizedType());
        }
        if (ctx != null || HttpServletRequest.class.isAssignableFrom(p.getType()) || HttpServletResponse.class.isAssignableFrom(p.getType())
                || java.util.Map.class.isAssignableFrom(p.getType())) {
            return contextBinding(p);
        }
        throw new IllegalStateException("Parameter [" + p.getName() + "] on method [" + p.getDeclaringExecutable()
                                                                                         .getName()
                + "] has no @Body/@PathParam/@QueryParam/@Context annotation and is not an HttpServletRequest/HttpServletResponse/Map");
    }

    private static ParamBinding contextBinding(Parameter p) {
        Class<?> type = p.getType();
        if (HttpServletRequest.class.isAssignableFrom(type)) {
            return new ParamBinding(ParamBinding.Kind.CTX_REQUEST, null, type, p.getParameterizedType());
        }
        if (HttpServletResponse.class.isAssignableFrom(type)) {
            return new ParamBinding(ParamBinding.Kind.CTX_RESPONSE, null, type, p.getParameterizedType());
        }
        if (java.util.Map.class.isAssignableFrom(type)) {
            return new ParamBinding(ParamBinding.Kind.CTX_PARAMS, null, type, p.getParameterizedType());
        }
        throw new IllegalStateException("Unsupported @Context parameter type: " + type.getName());
    }

    private static <A extends Annotation> A findAnnotation(Parameter p, Class<A> annotationType) {
        return p.getAnnotation(annotationType);
    }
}
