/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.api.messaging.TimeoutException;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A tenant owner manages the users of their tenant: sees who is there, invites another person, and
 * the invitation reaches the configured queue as the contract envelope; the provisioning system's
 * callback then moves the user on.
 *
 * <p>
 * Runs under {@code TOKEN_GROUPS} with fabricated OIDC authentications, the way
 * {@code TenantSelectionIT} does: the owner selects a provisioned tenant first, so every following
 * request runs in it. The request queue is a {@code global:} destination on the embedded broker.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenantUsersIT extends IntegrationTest {

    private static final String USERS = "/services/security/tenant-users";
    private static final String SELECTION = "/services/security/tenant-selection";
    private static final String QUEUE = "global:tenant-users-it.requests";
    private static final String APP_ID = "library";
    private static final String GROUPS_CLAIM = "groups";
    private static final String OWNER = "owner@example.com";
    private static final String MEMBER = "member@example.com";

    private static DirigibleTestTenant tenant;

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    static void enableTenantUsers() {
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(GROUPS_CLAIM);
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_USERS_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_USERS_REQUEST_QUEUE.setStringValue(QUEUE);
    }

    @BeforeEach
    void provisionTheTenantOnce() {
        if (tenant != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("tenant-users-it");
        createTenants(created);
        waitForTenantProvisioning(created);
        tenant = created;
        drainTheQueue();
    }

    @Test
    @Order(1)
    void anOwnerSeesTheSectionAndAMemberDoesNot() throws Exception {
        MockHttpSession owner = enter(OWNER, "Owner");
        mvc.perform(get(USERS + "/context").session(owner)
                                           .with(authentication(person(OWNER, "Owner"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.canManage").value(true))
           .andExpect(jsonPath("$.tenantId").value(tenant.getId()))
           .andExpect(jsonPath("$.roles", hasSize(2)));

        MockHttpSession member = enter(MEMBER, "User");
        mvc.perform(get(USERS + "/context").session(member)
                                           .with(authentication(person(MEMBER, "User"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.canManage").value(false))
           .andExpect(jsonPath("$.canRead").value(false));
        mvc.perform(get(USERS).session(member)
                              .with(authentication(person(MEMBER, "User"))))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.reason").value("NOT_A_TENANT_OWNER"));
        mvc.perform(post(USERS).session(member)
                               .with(authentication(person(MEMBER, "User")))
                               .contentType(MediaType.APPLICATION_JSON)
                               .content("{\"email\":\"x@example.com\",\"role\":\"User\"}"))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.reason").value("NOT_A_TENANT_OWNER"));
    }

    @Test
    @Order(2)
    void anInvitationIsRecordedAndPublishedAsTheContractEnvelope() throws Exception {
        MockHttpSession owner = enter(OWNER, "Owner");
        MvcResult invited = mvc.perform(post(USERS).session(owner)
                                                   .with(authentication(person(OWNER, "Owner")))
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   // a tenant in the body must never choose the tenant
                                                   .content(
                                                           "{\"email\":\"Invited.Person@Example.com\",\"role\":\"User\",\"tenantId\":\"elsewhere\"}"))
                               .andExpect(status().isAccepted())
                               .andExpect(jsonPath("$.status").value("PENDING"))
                               .andExpect(jsonPath("$.roles[0].role").value("User"))
                               .andExpect(jsonPath("$.roles[0].state").value("REQUESTED"))
                               .andExpect(jsonPath("$.tenantId").value(tenant.getId()))
                               .andReturn();
        String requestId = json.readTree(invited.getResponse()
                                                .getContentAsString())
                               .get("roles")
                               .get(0)
                               .get("requestId")
                               .asText();

        JsonNode message = json.readTree(MessagingFacade.receiveFromQueue(QUEUE, 10_000));
        assertEquals(requestId, message.get("messageId")
                                       .asText());
        assertEquals("user.assignment.requested", message.get("type")
                                                         .asText());
        assertEquals(tenant.getId(), message.get("tenantId")
                                            .asText());
        assertEquals(APP_ID, message.get("appId")
                                    .asText());
        assertEquals("invited.person@example.com", message.get("email")
                                                          .asText());
        assertEquals("User", message.get("role")
                                    .asText());
        assertEquals(OWNER, message.get("requestedBy")
                                   .asText());

        mvc.perform(post(USERS).session(owner)
                               .with(authentication(person(OWNER, "Owner")))
                               .contentType(MediaType.APPLICATION_JSON)
                               .content("{\"email\":\"invited.person@example.com\",\"role\":\"User\"}"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.reason").value("REQUEST_PENDING"));

        // another role is requested side by side
        mvc.perform(post(USERS).session(owner)
                               .with(authentication(person(OWNER, "Owner")))
                               .contentType(MediaType.APPLICATION_JSON)
                               .content("{\"email\":\"invited.person@example.com\",\"role\":\"Owner\"}"))
           .andExpect(status().isAccepted())
           .andExpect(jsonPath("$.roles[?(@.role == 'Owner')].state").value("REQUESTED"))
           .andExpect(jsonPath("$.roles[?(@.role == 'User')].state").value("REQUESTED"));
        drainTheQueue();
    }

    @Test
    @Order(3)
    void aResendPublishesTheSameIdAndAFormPostIsRefused() throws Exception {
        MockHttpSession owner = enter(OWNER, "Owner");
        JsonNode user = userNamed(owner, "invited.person@example.com");

        mvc.perform(post(USERS + "/" + user.get("id")
                                           .asLong()
                + "/roles/User/resend").session(owner)
                                       .with(authentication(person(OWNER, "Owner")))
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .content("{}"))
           .andExpect(status().isAccepted());
        assertEquals(requestIdOf(user, "User"), json.readTree(MessagingFacade.receiveFromQueue(QUEUE, 10_000))
                                                    .get("messageId")
                                                    .asText());

        mvc.perform(post(USERS).session(owner)
                               .with(authentication(person(OWNER, "Owner")))
                               .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                               .content("email=x%40example.com&role=User"))
           .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post(USERS + "/" + user.get("id")
                                           .asLong()
                + "/roles/User/resend").session(owner)
                                       .with(authentication(person(OWNER, "Owner")))
                                       .contentType(MediaType.APPLICATION_FORM_URLENCODED))
           .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @Order(4)
    void theProvisioningCallbackMovesTheUserOn() throws Exception {
        MockHttpSession owner = enter(OWNER, "Owner");
        JsonNode user = userNamed(owner, "invited.person@example.com");

        mvc.perform(put("/services/tenant-provisioning/tenants/" + tenant.getId() + "/users").with(authentication(provisioner()))
                                                                                             .contentType(MediaType.APPLICATION_JSON)
                                                                                             .content(json.writeValueAsString(Map.of(
                                                                                                     "email", "invited.person@example.com",
                                                                                                     "role", "User", "status", "INVITED",
                                                                                                     "requestId", requestIdOf(user, "User"),
                                                                                                     "updatedBy", OWNER))))
           .andExpect(status().isOk());

        JsonNode after = userNamed(owner, "invited.person@example.com");
        assertEquals("INVITED", after.get("status")
                                     .asText());
        assertEquals("GRANTED", roleOf(after, "User").get("state")
                                                     .asText());
        assertEquals(OWNER, roleOf(after, "User").get("requestedBy")
                                                 .asText());
        assertEquals("REQUESTED", roleOf(after, "Owner").get("state")
                                                        .asText());

        mvc.perform(post(USERS).session(owner)
                               .with(authentication(person(OWNER, "Owner")))
                               .contentType(MediaType.APPLICATION_JSON)
                               .content("{\"email\":\"invited.person@example.com\",\"role\":\"User\"}"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.reason").value("ROLE_ALREADY_GRANTED"));
        drainTheQueue();
    }

    @Test
    @Order(5)
    void theInvitedPersonEnteringTheTenantBecomesActive() throws Exception {
        enter("invited.person@example.com", "User");

        JsonNode user = userNamed(enter(OWNER, "Owner"), "invited.person@example.com");
        assertEquals("ACTIVE", user.get("status")
                                   .asText());
        assertEquals(false, user.get("lastSignInAt")
                                .isNull());
    }

    private MockHttpSession enter(String user, String role) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post(SELECTION).session(session)
                                   .with(authentication(person(user, role)))
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content("{\"tenantId\":\"" + tenant.getId() + "\"}"))
           .andExpect(status().isOk());
        return session;
    }

    private JsonNode userNamed(MockHttpSession owner, String email) throws Exception {
        String body = mvc.perform(get(USERS).session(owner)
                                            .with(authentication(person(OWNER, "Owner"))))
                         .andExpect(status().isOk())
                         .andReturn()
                         .getResponse()
                         .getContentAsString();
        for (JsonNode user : json.readTree(body)) {
            if (email.equals(user.get("email")
                                 .asText())) {
                return user;
            }
        }
        throw new AssertionError("no user [" + email + "] in " + body);
    }

    /**
     * A signed-in person whose groups make them [role] of the test tenant, as a selection leaves them.
     */
    private static Authentication person(String user, String role) {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now()
                                                                                .plusSeconds(300),
                Map.of("sub", user, "email", user, GROUPS_CLAIM, List.of(tenant.getId() + "." + APP_ID + "." + role)));
        return new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken),
                List.of(new SimpleGrantedAuthority("ROLE_" + role)), "keycloak");
    }

    private static Authentication provisioner() {
        return new UsernamePasswordAuthenticationToken("provisioner", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_TENANT_PROVISIONER")));
    }

    private static void drainTheQueue() {
        try {
            while (true) {
                MessagingFacade.receiveFromQueue(QUEUE, 50);
            }
        } catch (TimeoutException expected) {
            // drained
        }
    }

    private static JsonNode roleOf(JsonNode user, String role) {
        for (JsonNode row : user.get("roles")) {
            if (role.equals(row.get("role")
                               .asText())) {
                return row;
            }
        }
        throw new AssertionError("[" + user.get("email") + "] has no role row [" + role + "]");
    }

    private static String requestIdOf(JsonNode user, String role) {
        return roleOf(user, role).get("requestId")
                                 .asText();
    }
}
