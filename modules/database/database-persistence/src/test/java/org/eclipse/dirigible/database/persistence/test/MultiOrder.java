/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.test;

import jakarta.persistence.Column;

/**
 * The Class MultiOrder.
 */
public class MultiOrder extends Order {

    /** The amount. */
    @Column(name = "ORDER_AMOUNT", columnDefinition = "BIGINT", nullable = true)
    private Long amount;

    /** The description. */
    @Column(name = "ORDER_DESCRIPTION", columnDefinition = "VARCHAR", nullable = true, length = 512)
    private String description;

    /**
     * Gets the amount.
     *
     * @return the amount
     */
    public Long getAmount() {
        return amount;
    }

    /**
     * Sets the amount.
     *
     * @param amount the new amount
     */
    public void setAmount(Long amount) {
        this.amount = amount;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
