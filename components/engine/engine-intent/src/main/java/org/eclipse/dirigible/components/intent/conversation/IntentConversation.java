/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.conversation;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One AI conversation about one application, in one authoring surface.
 *
 * <p>
 * The identity is {@code (tenant, project, surface, path)} - deliberately NOT the workspace or the
 * user: a workspace is one developer's view of a project, and the whole point of persisting the
 * dialogue is that a teammate opening the same app sees the same history. Per-message authorship is
 * recorded on {@link IntentConversationMessage} instead.
 */
@Entity
@Table(name = "DIRIGIBLE_INTENT_CONVERSATIONS")
public class IntentConversation {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INTENTCONVERSATION_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The owning tenant. Every read and write is scoped by it. */
    @Column(name = "INTENTCONVERSATION_TENANT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String tenantId;

    /** The workspace project the application lives in. */
    @Column(name = "INTENTCONVERSATION_PROJECT", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String project;

    /**
     * The authoring surface the conversation happened in - {@code builder} or {@code intent-editor}.
     */
    @Column(name = "INTENTCONVERSATION_SURFACE", columnDefinition = "VARCHAR", nullable = false, length = 32)
    private String surface;

    /** The intent file path within the project, e.g. {@code app.intent}. */
    @Column(name = "INTENTCONVERSATION_PATH", columnDefinition = "VARCHAR", nullable = false, length = 512)
    private String path;

    /** When the conversation started. */
    @Column(name = "INTENTCONVERSATION_CREATED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Timestamp createdAt;

    /** When a message was last appended. */
    @Column(name = "INTENTCONVERSATION_UPDATED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Timestamp updatedAt;

    /**
     * Instantiates a new intent conversation. Required by JPA.
     */
    IntentConversation() {}

    /**
     * Instantiates a new intent conversation.
     *
     * @param tenantId the owning tenant
     * @param project the workspace project
     * @param surface the authoring surface
     * @param path the intent file path within the project
     * @param now the creation timestamp
     */
    IntentConversation(String tenantId, String project, String surface, String path, Timestamp now) {
        this.tenantId = tenantId;
        this.project = project;
        this.surface = surface;
        this.path = path;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the owning tenant.
     *
     * @return the tenant id
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Gets the workspace project.
     *
     * @return the project
     */
    public String getProject() {
        return project;
    }

    /**
     * Gets the authoring surface.
     *
     * @return the surface
     */
    public String getSurface() {
        return surface;
    }

    /**
     * Gets the intent file path within the project.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return the creation timestamp
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the timestamp of the last appended message.
     *
     * @return the update timestamp
     */
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp of the last appended message.
     *
     * @param updatedAt the update timestamp
     */
    void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
