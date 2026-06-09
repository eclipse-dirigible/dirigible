/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.indexing;

import java.io.IOException;
import org.eclipse.dirigible.components.api.indexing.IndexingFacade;

/**
 * Adds documents to a Lucene index so they become searchable via {@link Searcher}. Each call is a
 * single document — pass a JSON {@code parameters} blob for arbitrary additional fields beyond the
 * named ones ({@code location}, {@code contents}, {@code lastModified}).
 * <p>
 * Writes are buffered and committed by the indexing service on its own cadence — callers don't need
 * to flush. For bulk imports prefer to batch by sending many adds in succession; the underlying
 * Lucene writer amortises segment merges well.
 */
public final class Writer {

    private Writer() {}

    public static void add(String index, String location, String contents, String lastModified, String parameters) throws IOException {
        IndexingFacade.add(index, location, contents, lastModified, parameters);
    }
}
