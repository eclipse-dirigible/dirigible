/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.annotations;

/**
 * ID generation strategies, mirroring {@code jakarta.persistence.GenerationType}. Mapped onto
 * Hibernate's {@code generator class} attribute in the produced HBM mapping:
 * <ul>
 * <li>{@link #AUTO} → {@code native}</li>
 * <li>{@link #IDENTITY} → {@code identity}</li>
 * <li>{@link #SEQUENCE} → {@code sequence}</li>
 * <li>{@link #TABLE} → {@code increment}</li>
 * </ul>
 */
public enum GenerationType {
    AUTO, IDENTITY, SEQUENCE, TABLE
}
