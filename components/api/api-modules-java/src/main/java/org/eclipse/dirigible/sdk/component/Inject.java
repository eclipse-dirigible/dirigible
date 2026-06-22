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
 * Marks an injection point on a client bean. Three forms are supported, mirroring Spring:
 * <ul>
 * <li><b>Constructor</b> — annotate the constructor the container should use. With a single
 * constructor the annotation is optional (it is selected automatically); with several it
 * disambiguates which one to wire. This is the preferred, most testable form.</li>
 * <li><b>Field</b> — annotate a field; the container sets it after construction. Kept for
 * convenience and backward compatibility.</li>
 * <li><b>Parameter</b> — rarely needed; available for marking individual constructor
 * parameters.</li>
 * </ul>
 *
 * <p>
 * Each injection point is resolved from the other beans ({@code @Component} / {@code @Repository} /
 * {@code @Controller}) in the same {@code ClientClassLoader} generation. A {@code List<T>} /
 * {@code Collection<T>} / {@code Set<T>} injection point receives <em>every</em> bean assignable to
 * {@code T} (collection injection); any other type resolves to the single matching bean
 * (disambiguated by name when several share a type).
 *
 * <p>
 * Unlike Spring's {@code @Autowired}, this is resolved by the engine's own client bean container —
 * client classes are not Spring-scanned, so Spring's {@code @Autowired} would silently no-op. To
 * reach a platform service use {@link Beans}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
public @interface Inject {
}
