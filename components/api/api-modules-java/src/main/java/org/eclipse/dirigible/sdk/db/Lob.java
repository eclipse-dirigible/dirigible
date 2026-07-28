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
 * Marks a {@code String} property as large text: a column the database sizes for itself
 * ({@code CLOB} on H2, {@code TEXT} on PostgreSQL) instead of a bounded {@code VARCHAR}.
 *
 * <p>
 * Declare it on any property whose column is a large-text one. Without it the property is mapped as
 * a {@code VARCHAR} of {@link Column#length()} - which defaults to 255 - and the entity layer
 * resizes the column to match, so an existing {@code CLOB} column would silently become a
 * {@code VARCHAR(255)}.
 *
 * <p>
 * Signature mirrors {@code jakarta.persistence.Lob}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Lob {
}
