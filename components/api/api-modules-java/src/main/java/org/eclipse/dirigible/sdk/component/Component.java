/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a client Java class as a managed bean. The runtime instantiates it once per
 * {@code ClientClassLoader} generation (a singleton), resolving its constructor arguments and
 * {@link Inject @Inject} fields from the other beans in the same generation. The resulting instance
 * can be injected into other beans (by constructor or field) and looked up via
 * {@link Beans#get(Class)}.
 *
 * <p>
 * Just like Spring's {@code @Component}, the bean is given a name: the explicit {@link #value()}
 * when provided, otherwise the decapitalized simple class name ({@code OrderService} →
 * {@code orderService}). The name disambiguates {@link Beans#get(String, Class)} lookups and
 * by-name injection when several beans share a type.
 *
 * <p>
 * {@link Repository @Repository} and {@code @Controller} are themselves meta-annotated with
 * {@code @Component}, so they are beans and participate in injection without any extra annotation.
 * An extension point is just an interface; a contribution is a {@code @Component} implementing it,
 * consumed via {@code List<...>} collection injection.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Component
 * public class GreetingService {
 *     public String greet(String name) { return "Hello, " + name; }
 * }
 *
 * {@literal @}Controller
 * public class GreetingController {
 *     private final GreetingService greetings;
 *     public GreetingController(GreetingService greetings) { this.greetings = greetings; }
 *
 *     {@literal @}Get("/{name}")
 *     public String hello({@literal @}PathParam("name") String name) { return greetings.greet(name); }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {

    /**
     * Explicit bean name. When empty (the default) the name is the decapitalized simple class name,
     * matching Spring's default bean-naming convention.
     *
     * @return the bean name, or empty for the default
     */
    String value() default "";

}
