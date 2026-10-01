/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.api.messaging.TimeoutException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * What the tenant users ITs share: playing the external provisioning system - its machine
 * authentication, the users snapshot it writes, and the change requests it reads off the queue.
 */
public final class TenantUsersTestSupport {

    /** Who grants and changes, unless a test says otherwise. */
    public static final String GRANTOR = "owner@example.com";

    /** When, unless a test says otherwise. */
    public static final String AT = "2026-09-30T10:00:00Z";

    private static final ObjectMapper JSON = new ObjectMapper();

    private TenantUsersTestSupport() {}

    /**
     * The provisioning system's machine client, holding only {@code TENANT_PROVISIONER}.
     *
     * @return the authentication
     */
    public static Authentication provisioner() {
        return new UsernamePasswordAuthenticationToken("provisioner", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_TENANT_PROVISIONER")));
    }

    /**
     * A user of a snapshot, to build.
     *
     * @param email the email
     * @return the builder
     */
    public static UserSnapshot user(String email) {
        return new UserSnapshot(email);
    }

    /**
     * Writes users into a tenant's replica, as the provisioning system does.
     *
     * @param mvc the MockMvc of the test
     * @param tenantId the tenant id
     * @param revision the tenant's revision sequence
     * @param users the users
     * @throws Exception when the request cannot be sent
     */
    public static void pushSnapshot(MockMvc mvc, String tenantId, long revision, UserSnapshot... users) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("complete", false);
        body.put("revision", revision);
        List<Map<String, Object>> entries = new ArrayList<>();
        for (UserSnapshot user : users) {
            entries.add(user.build());
        }
        body.put("users", entries);
        mvc.perform(put("/services/tenant-provisioning/tenants/" + tenantId + "/users").with(authentication(provisioner()))
                                                                                       .contentType(MediaType.APPLICATION_JSON)
                                                                                       .content(JSON.writeValueAsString(body)))
           .andExpect(status().isOk());
    }

    /**
     * The next change request on a queue.
     *
     * @param queue the queue
     * @param timeoutMillis how long to wait
     * @return the envelope
     * @throws Exception when none arrives or it is not JSON
     */
    public static JsonNode receive(String queue, long timeoutMillis) throws Exception {
        return JSON.readTree(MessagingFacade.receiveFromQueue(queue, timeoutMillis));
    }

    /**
     * Takes every change request waiting on a queue.
     *
     * @param queue the queue
     * @return the envelopes, in arrival order
     */
    public static List<JsonNode> drainQueue(String queue) {
        List<JsonNode> envelopes = new ArrayList<>();
        try {
            while (true) {
                envelopes.add(JSON.readTree(MessagingFacade.receiveFromQueue(queue, 250)));
            }
        } catch (TimeoutException drained) {
            return envelopes;
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the queue [" + queue + "]", e);
        }
    }

    /**
     * One user of a snapshot, in the users snapshot contract's shape. Who and when default to
     * {@link #GRANTOR} and {@link #AT}.
     */
    public static final class UserSnapshot {

        private final String email;
        private long revision = 1;
        private String status = "ASSIGNED";
        private final List<Map<String, Object>> roles = new ArrayList<>();
        private Map<String, Object> lastError;

        private UserSnapshot(String email) {
            this.email = email;
        }

        public UserSnapshot revision(long revision) {
            this.revision = revision;
            return this;
        }

        public UserSnapshot status(String status) {
            this.status = status;
            return this;
        }

        /** A role; one not being added carries its grantor and grant time. */
        public UserSnapshot role(String role, String state) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("role", role);
            entry.put("state", state);
            if (!"ADDING".equals(state)) {
                entry.put("grantedBy", GRANTOR);
                entry.put("grantedAt", AT);
            }
            roles.add(entry);
            return this;
        }

        public UserSnapshot lastError(String code, String message) {
            lastError = new LinkedHashMap<>();
            lastError.put("code", code);
            lastError.put("message", message);
            return this;
        }

        Map<String, Object> build() {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("email", email);
            user.put("revision", revision);
            user.put("status", status);
            user.put("roles", roles);
            user.put("invitedBy", GRANTOR);
            user.put("invitedAt", AT);
            user.put("lastChangedBy", GRANTOR);
            user.put("lastChangedAt", AT);
            user.put("lastError", lastError);
            return user;
        }
    }
}
