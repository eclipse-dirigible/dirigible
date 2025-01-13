/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.google.gson.annotations.Expose;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import java.util.Date;

import static jakarta.persistence.TemporalType.TIMESTAMP;

/**
 * The Class Auditable.
 *
 * @param <U> the generic type
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable<U> {

    /** The created by. */
    @CreatedBy
    @Column(name = "CREATED_BY", columnDefinition = "VARCHAR", nullable = true, length = 128)
    @Expose
    protected U createdBy;

    /** The created at. */
    @CreatedDate
    @Temporal(TIMESTAMP)
    @Column(name = "CREATED_AT", columnDefinition = "TIMESTAMP", nullable = true)
    @Expose
    protected Date createdAt;

    /** The updated by. */
    @LastModifiedBy
    @Column(name = "UPDATED_BY", columnDefinition = "VARCHAR", nullable = true, length = 128)
    @Expose
    protected U updatedBy;

    /** The updated at. */
    @LastModifiedDate
    @Temporal(TIMESTAMP)
    @Column(name = "UPDATED_AT", columnDefinition = "TIMESTAMP", nullable = true)
    @Expose
    protected Date updatedAt;

    /**
     * Gets the created by.
     *
     * @return the createdBy
     */
    public U getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the created by.
     *
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(U createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the created at.
     *
     * @return the createdAt
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the created at.
     *
     * @param createdAt the createdAt to set
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the updated by.
     *
     * @return the updatedBy
     */
    public U getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the updated by.
     *
     * @param updatedBy the updatedBy to set
     */
    public void setUpdatedBy(U updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Gets the updated at.
     *
     * @return the updatedAt
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the updated at.
     *
     * @param updatedAt the updatedAt to set
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

}
