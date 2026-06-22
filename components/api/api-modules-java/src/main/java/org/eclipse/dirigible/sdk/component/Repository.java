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
 * Marks a client class as a singleton repository discoverable via {@link Inject}. Repositories are
 * typically thin typed wrappers around {@code JavaEntityStore} (extend
 * {@code JavaRepository<Entity>} from {@code data-store-java}) but the annotation is engine-level
 * so non-entity components can plug into the same injection mechanism.
 *
 * <p>
 * {@code @Repository} is meta-annotated with {@link Component @Component}, so a repository is a
 * fully managed bean: it is instantiated once per generation (with constructor injection) and is
 * itself injectable into controllers and other beans via {@link Inject @Inject} or a constructor
 * parameter.
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Repository {
}
