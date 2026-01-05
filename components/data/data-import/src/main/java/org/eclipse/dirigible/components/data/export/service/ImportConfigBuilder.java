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

public class ImportConfigBuilder {

    private boolean header;
    private boolean useHeaderNames;
    private boolean distinguishEmptyFromNull;
    private String delimField;
    private String delimEnclosing;
    private String sequence;

    public ImportConfigBuilder() {
        this.header = true;
        this.useHeaderNames = true;
        this.distinguishEmptyFromNull = false;
        this.delimField = ",";
        this.delimEnclosing = "\"";
        this.sequence = null;
    }

    public ImportConfigBuilder setHeader(boolean header) {
        this.header = header;
        return this;
    }

    public ImportConfigBuilder setUseHeaderNames(boolean useHeaderNames) {
        this.useHeaderNames = useHeaderNames;
        return this;
    }

    public ImportConfigBuilder setDistinguishEmptyFromNull(boolean distinguishEmptyFromNull) {
        this.distinguishEmptyFromNull = distinguishEmptyFromNull;
        return this;
    }

    public ImportConfigBuilder setDelimField(String delimField) {
        this.delimField = delimField;
        return this;
    }

    public ImportConfigBuilder setDelimEnclosing(String delimEnclosing) {
        this.delimEnclosing = delimEnclosing;
        return this;
    }

    public ImportConfigBuilder setSequence(String sequence) {
        this.sequence = sequence;
        return this;
    }

    public ImportConfig build() {
        return new ImportConfig(header, useHeaderNames, distinguishEmptyFromNull, delimField, delimEnclosing, sequence);
    }
}
