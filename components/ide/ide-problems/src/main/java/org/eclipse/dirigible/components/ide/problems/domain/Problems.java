/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.problems.domain;

import java.util.List;

/**
 * The Class Problems.
 */
public class Problems {

    /** The result. */
    private List<Problem> result;

    /** The selected rows. */
    private int selectedRows;

    /** The total rows. */
    private int totalRows;

    /**
     * Instantiates a new response model.
     */
    public Problems() {}

    /**
     * Instantiates a new response model.
     *
     * @param result the result
     * @param selectedRows the selected rows
     * @param totalRows the total rows
     */
    public Problems(List<Problem> result, int selectedRows, int totalRows) {
        this.result = result;
        this.selectedRows = selectedRows;
        this.totalRows = totalRows;
    }

    /**
     * Gets the result.
     *
     * @return the result
     */
    public List<Problem> getResult() {
        return result;
    }

    /**
     * Sets the result.
     *
     * @param result the new result
     */
    public void setResult(List<Problem> result) {
        this.result = result;
    }

    /**
     * Gets the selected rows.
     *
     * @return the selected rows
     */
    public int getSelectedRows() {
        return selectedRows;
    }

    /**
     * Sets the selected rows.
     *
     * @param selectedRows the new selected rows
     */
    public void setSelectedRows(int selectedRows) {
        this.selectedRows = selectedRows;
    }

    /**
     * Gets the total rows.
     *
     * @return the total rows
     */
    public int getTotalRows() {
        return totalRows;
    }

    /**
     * Sets the total rows.
     *
     * @param totalRows the new total rows
     */
    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }
}
