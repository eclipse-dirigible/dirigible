/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a client Java class as a persistent entity managed by the {@code data-store-java} module.
 *
 * <p>
 * Signatures mirror {@code jakarta.persistence.Entity} so existing JPA knowledge transfers, but
 * Dirigible's runtime processes the annotation itself — it is not propagated to Hibernate via JPA.
 * The {@code data-store-java} module reflects over these annotations and builds Hibernate mappings
 * (HBM XML, dynamic-map mode) on the fly.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {

    /**
     * Logical entity name. Defaults to the simple class name when blank.
     */
    String name() default "";

}
