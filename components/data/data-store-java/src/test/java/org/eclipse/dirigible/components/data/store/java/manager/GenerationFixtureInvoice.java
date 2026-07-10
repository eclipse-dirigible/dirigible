/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.manager;

import org.eclipse.dirigible.sdk.db.Entity;
import org.eclipse.dirigible.sdk.db.GeneratedValue;
import org.eclipse.dirigible.sdk.db.GenerationType;
import org.eclipse.dirigible.sdk.db.Id;

/**
 * Top-level fixture for {@code JavaEntityManagerGenerationTest} — deliberately NOT a nested class,
 * because a nested class re-defined in a second classloader cannot resolve its declaring class
 * ({@code IllegalAccessError} from {@code Class.getSimpleName()}), while a top-level class
 * re-defines cleanly, like real client entities do.
 */
@Entity
public class GenerationFixtureInvoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer Id;
    public String Number;
}
