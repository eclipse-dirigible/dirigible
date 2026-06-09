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
 * Full-text search over the documents previously indexed via {@link Writer}. The query string uses
 * Lucene query syntax — field-qualified ({@code title:invoice}), boolean
 * ({@code title:invoice AND status:open}), wildcard ({@code invoice*}), fuzzy ({@code invoice~}).
 * Results are returned as a JSON document with location, content excerpt, and metadata for each
 * hit.
 * <p>
 * Indexes are named so the same JVM can host multiple corpora ("products", "orders", "docs"); the
 * {@code index} name on every call selects which corpus to search.
 */
public final class Searcher {

    private Searcher() {}

    public static String search(String index, String term) throws IOException {
        return IndexingFacade.search(index, term);
    }
}
