/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.format;

import java.io.OutputStream;
import java.sql.ResultSet;
import java.util.Optional;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.database.DatabaseParameters;
import org.eclipse.dirigible.components.database.helpers.FormattingParameters;

/**
 * The Abstract ResultSet Writer.
 *
 * @param <T> the generic type
 */
public abstract class AbstractResultSetWriter<T> implements ResultSetWriter<T> {

    /** The limit. */
    private int limit = 1000;

    /** The limited. */
    private boolean limited = true;

    /** The stringify. */
    private boolean stringify = true;

    /**
     * Default Constructor.
     */
    public AbstractResultSetWriter() {
        this.limit = Configuration.getAsInt(DatabaseParameters.DIRIGIBLE_DATABASE_DEFAULT_QUERY_LIMIT, 1000);
    }

    /**
     * Getter for the limit.
     *
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Setter for the limit.
     *
     * @param limit the limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Checks if is limited.
     *
     * @return true, if is limited
     */
    public boolean isLimited() {
        return limited;
    }

    /**
     * Sets the limited.
     *
     * @param limited the new limited
     */
    public void setLimited(boolean limited) {
        this.limited = limited;
    }

    /**
     * Checks if is stringified.
     *
     * @return true, if is stringified
     */
    public boolean isStringified() {
        return stringify;
    }

    /**
     * Sets the stringified.
     *
     * @param stringify the new stringified
     */
    public void setStringified(boolean stringify) {
        this.stringify = stringify;
    }

    @Override
    public void write(ResultSet rs, OutputStream output, Optional<FormattingParameters> resultParameters) throws Exception {
        write(rs, output);
    }

}
