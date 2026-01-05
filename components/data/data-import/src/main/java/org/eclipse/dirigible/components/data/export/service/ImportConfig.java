/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.service;

public class ImportConfig {

    private final boolean header;
    private final boolean useHeaderNames;
    private final boolean distinguishEmptyFromNull;
    private final String delimField;
    private final String delimEnclosing;
    private final String sequence;

    public ImportConfig(boolean header, boolean useHeaderNames, boolean distinguishEmptyFromNull, String delimField, String delimEnclosing,
            String sequence) {
        this.header = header;
        this.useHeaderNames = useHeaderNames;
        this.distinguishEmptyFromNull = distinguishEmptyFromNull;
        this.delimField = delimField;
        this.delimEnclosing = delimEnclosing;
        this.sequence = sequence;
    }

    public boolean isHeader() {
        return header;
    }

    public boolean isUseHeaderNames() {
        return useHeaderNames;
    }

    public boolean isDistinguishEmptyFromNull() {
        return distinguishEmptyFromNull;
    }

    public String getDelimField() {
        return delimField;
    }

    public String getDelimEnclosing() {
        return delimEnclosing;
    }

    public String getSequence() {
        return sequence;
    }
}
