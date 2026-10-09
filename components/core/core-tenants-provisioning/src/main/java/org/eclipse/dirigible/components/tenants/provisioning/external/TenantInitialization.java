/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import java.time.Instant;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The initialization an activation asked for, one per tenant.
 *
 * <p>
 * Written in the request thread, before the activation is answered, which is what makes the tenant
 * read {@code IN_PROGRESS} from that moment until its own initialization ends - on every node,
 * since it is a row of the system database, and across a restart, which picks the open ones up
 * again.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANT_INITIALIZATIONS")
public class TenantInitialization {

    /** Where an initialization is. */
    public enum State {
        /** Asked for, not started yet. */
        REQUESTED,
        /** Running. */
        RUNNING,
        /** Over - the outcome per artefact is in the ledger, a failure of the run itself in the error. */
        FINISHED
    }

    @Id
    @Column(name = "TENINIT_TENANT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "TENINIT_STATE", columnDefinition = "VARCHAR", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "TENINIT_REQUESTED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Instant requestedAt;

    @Column(name = "TENINIT_STARTED_AT", columnDefinition = "TIMESTAMP")
    private Instant startedAt;

    @Column(name = "TENINIT_FINISHED_AT", columnDefinition = "TIMESTAMP")
    private Instant finishedAt;

    @Column(name = "TENINIT_ERROR", columnDefinition = "VARCHAR", length = Artefact.ERROR_LENGTH)
    private String error;

    /** Required by JPA. */
    protected TenantInitialization() {}

    TenantInitialization(String tenantId) {
        this.tenantId = tenantId;
    }

    String getTenantId() {
        return tenantId;
    }

    String getError() {
        return error;
    }

    /**
     * Whether the initialization has yet to end.
     *
     * @return true, if it is requested or running
     */
    boolean isOpen() {
        return State.FINISHED != state;
    }

    void request(Instant now) {
        state = State.REQUESTED;
        requestedAt = now;
        startedAt = null;
        finishedAt = null;
        error = null;
    }

    void start(Instant now) {
        state = State.RUNNING;
        startedAt = now;
    }

    void finish(Instant now, String failure) {
        state = State.FINISHED;
        finishedAt = now;
        error = failure != null && failure.length() > Artefact.ERROR_LENGTH ? failure.substring(0, Artefact.ERROR_LENGTH) : failure;
    }

    @Override
    public String toString() {
        return "TenantInitialization [tenantId=" + tenantId + ", state=" + state + "]";
    }
}
