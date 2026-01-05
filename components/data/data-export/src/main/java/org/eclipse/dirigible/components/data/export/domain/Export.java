/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.domain;

import java.sql.Timestamp;

import com.google.gson.annotations.Expose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "DIRIGIBLE_EXPORTS")
public class Export {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXPORT_ID", nullable = false)
    private Long id;

    /**
     * The name.
     */
    @Column(name = "EXPORT_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String name;

    /** The status. */
    @Column(name = "EXPORT_STATUS", nullable = true)
    @Enumerated(EnumType.STRING)
    private ExportStatus status = ExportStatus.UNKNOWN;

    /**
     * The started at.
     */
    @Column(name = "EXPORT_STARTED_AT", columnDefinition = "TIMESTAMP", nullable = true)
    @Expose
    private Timestamp startedAt;

    /**
     * The finished at.
     */
    @Column(name = "EXPORT_FINISHED_AT", columnDefinition = "TIMESTAMP", nullable = true)
    @Expose
    private Timestamp finishedAt;

    /**
     * The message.
     */
    @Column(name = "EXPORT_MESSAGE", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    @Expose
    private String message;

    public Export(String name, ExportStatus status, Timestamp startedAt, Timestamp finishedAt, String message) {
        super();
        this.name = name;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.message = message;
    }

    public Export() {
        super();
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the status
     */
    public ExportStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(ExportStatus status) {
        this.status = status;
    }

    /**
     * @return the startedAt
     */
    public Timestamp getStartedAt() {
        return startedAt;
    }

    /**
     * @param startedAt the startedAt to set
     */
    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * @return the finishedAt
     */
    public Timestamp getFinishedAt() {
        return finishedAt;
    }

    /**
     * @param finishedAt the finishedAt to set
     */
    public void setFinishedAt(Timestamp finishedAt) {
        this.finishedAt = finishedAt;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Export [id=" + id + ", name=" + name + ", status=" + status + ", startedAt=" + startedAt + ", finishedAt=" + finishedAt
                + ", message=" + message + "]";
    }



}
