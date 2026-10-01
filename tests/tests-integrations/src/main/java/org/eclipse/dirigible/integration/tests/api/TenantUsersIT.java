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

import static org.eclipse.dirigible.integration.tests.support.TenantUsersTestSupport.drainQueue;
import static org.eclipse.dirigible.integration.tests.support.TenantUsersTestSupport.pushSnapshot;
import static org.eclipse.dirigible.integration.tests.support.TenantUsersTestSupport.receive;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.eclipse.dirigible.integration.tests.support.TenantUsersTestSupport;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tenant users, the Owner's side, end to end on one instance: the three commands each publish one
 * change request in the message contract's shape, the list shows what the provisioning system wrote
 * through the users snapshot endpoint, and the advisory rules answer from that list.
 *
 * <p>
 * Driven with MockMvc under {@code TOKEN_GROUPS}: a fabricated OIDC person selects the tenant, the
 * way the picker does, and the embedded broker carries the {@code global:} queue the test reads.
 * The provisioning system is played by a {@code TENANT_PROVISIONER} client.
 */
@Tag("slow")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenantUsersIT extends IntegrationTest {

    private static final String USERS = "/services/security/tenant-users";
    private static final String SELECTION = "/services/security/tenant-selection";
    private static final String QUEUE = "global:it.tenant-users.changes";
    private static final String APP_ID = "library";
    private static final String GROUPS_CLAIM = "groups";
    private static final String OWNER = "owner@example.com";
    private static final String MEMBER = "member@example.com";
    private static final String ANN = "ann@example.com";

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
        DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.setStringValue(QUEUE);
    }

    @BeforeEach
    void provisionTheTenantOnce() throws Exception {
        if (tenant != null) {
            return;
        }
        DirigibleTestTenant created = new DirigibleTestTenant("tenant-users-it");
        createTenants(created);
        waitForTenantProvisioning(created);
        tenant = created;
        drainQueue(QUEUE);
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
           .andExpect(jsonPath("$.caller").value(OWNER))
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
    }

    @Test
    @Order(2)
    void anInviteWithTwoRolesIsOneMessageInTheContractsShape() throws Exception {
        MockHttpSession owner = enter(OWNER, "Owner");
        String accepted = mvc.perform(post(USERS).session(owner)
                                                 .with(authentication(person(OWNER, "Owner")))
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 // a tenant in the body must never choose the tenant
                                                 .content(
                                                         "{\"email\":\"Ann@Example.com\",\"roles\":[\"User\",\"Owner\"],\"tenantId\":\"elsewhere\"}"))
                             .andExpect(status().isAccepted())
                             .andReturn()
                             .getResponse()
                             .getContentAsString();
        String requestId = json.readTree(accepted)
                               .get("requestId")
                               .asText();

        JsonNode message = receive(QUEUE, 10_000);
        assertEquals(requestId, message.get("requestId")
                                       .asText());
        assertEquals("tenant.user.change.requested", message.get("type")
                                                            .asText());
        assertEquals(1, message.get("version")
                               .asInt());
        assertEquals("INVITE", message.get("action")
                                      .asText());
        assertEquals(tenant.getId(), message.get("tenantId")
                                            .asText());
        assertEquals(APP_ID, message.get("appId")
                                    .asText());
        assertEquals(ANN, message.get("email")
                                 .asText());
        assertEquals(List.of("Owner", "User"), json.convertValue(message.get("roles"), List.class));
        assertFalse(message.has("expectedRevision"), "an invite carries no expected revision");
        assertEquals(OWNER, message.get("requestedBy")
                                   .asText());
        Instant.parse(message.get("requestedAt")
                             .asText());
        assertThrows(TimeoutException.class, () -> MessagingFacade.receiveFromQueue(QUEUE, 500), "exactly one message per action");
    }

    @Test
    @Order(3)
    void theListShowsWhatTheProvisioningSystemWrote() throws Exception {
        pushSnapshot(mvc, tenant.getId(), 1, TenantUsersTestSupport.user(OWNER)
                                                                   .revision(1)
                                                                   .role("Owner", "GRANTED"),
                TenantUsersTestSupport.user(ANN)
                                      .revision(1)
                                      .status("PENDING")
                                      .role("Owner", "ADDING")
                                      .role("User", "ADDING"));

        MockHttpSession owner = enter(OWNER, "Owner");
        JsonNode ann = userNamed(owner, ANN);
        assertEquals("ADDING", roleOf(ann, "Owner").get("state")
                                                   .asText());
        assertEquals("ADDING", roleOf(ann, "User").get("state")
                                                  .asText());

        mvc.perform(post(USERS).session(owner)
                               .with(authentication(person(OWNER, "Owner")))
                               .contentType(MediaType.APPLICATION_JSON)
                               .content("{\"email\":\"" + ANN + "\",\"roles\":[\"User\"]}"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.reason").value("REQUEST_PENDING"));
    }

    @Test
    @Order(4)
    void aGrantedSnapshotShowsTheRolesGranted() throws Exception {
        pushSnapshot(mvc, tenant.getId(), 2, TenantUsersTestSupport.user(ANN)
                                                                   .revision(2)
                                                                   .role("Owner", "GRANTED")
                                                                   .role("User", "GRANTED"));

        JsonNode ann = userNamed(enter(OWNER, "Owner"), ANN);
        assertEquals("ASSIGNED", ann.get("status")
                                    .asText());
        assertEquals("GRANTED", roleOf(ann, "Owner").get("state")
                                                    .asText());
        assertEquals("GRANTED", roleOf(ann, "User").get("state")
                                                   .asText());
    }

    @Test
    @Order(5)
    void aRoleChangeChecksTheRevisionAndPublishesTheDesiredSet() throws Exception {
        MockHttpSession owner = enter(OWNER, "Owner");
        long id = userNamed(owner, ANN).get("id")
                                       .asLong();

        mvc.perform(put(USERS + "/" + id + "/roles").session(owner)
                                                    .with(authentication(person(OWNER, "Owner")))
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content("{\"roles\":[\"User\"],\"expectedRevision\":1}"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.reason").value("STALE_REVISION"));

        mvc.perform(put(USERS + "/" + id + "/roles").session(owner)
                                                    .with(authentication(person(OWNER, "Owner")))
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content("{\"roles\":[\"User\"],\"expectedRevision\":2}"))
           .andExpect(status().isAccepted());
        JsonNode message = receive(QUEUE, 10_000);
        assertEquals("UPDATE_ROLES", message.get("action")
                                            .asText());
        assertEquals(List.of("User"), json.convertValue(message.get("roles"), List.class));
        assertEquals(2, message.get("expectedRevision")
                               .asLong());

        // the provisioning system applies it
        pushSnapshot(mvc, tenant.getId(), 3, TenantUsersTestSupport.user(ANN)
                                                                   .revision(3)
                                                                   .role("User", "GRANTED"));
    }

    @Test
    @Order(6)
    void theOnlyOwnerCannotBeRemoved() throws Exception {
        MockHttpSession owner = enter(OWNER, "Owner");
        JsonNode me = userNamed(owner, OWNER);

        mvc.perform(delete(USERS + "/" + me.get("id")
                                           .asLong()).session(owner)
                                                     .with(authentication(person(OWNER, "Owner")))
                                                     .contentType(MediaType.APPLICATION_JSON)
                                                     .content("{\"expectedRevision\":" + me.get("revision")
                                                                                           .asLong()
                                                             + "}"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.reason").value("LAST_OWNER"));
    }

    @Test
    @Order(7)
    void aRemovalIsOneMessageWithoutRolesAndTheTombstoneLeavesTheList() throws Exception {
        MockHttpSession owner = enter(OWNER, "Owner");
        JsonNode ann = userNamed(owner, ANN);

        mvc.perform(delete(USERS + "/" + ann.get("id")
                                            .asLong()).session(owner)
                                                      .with(authentication(person(OWNER, "Owner")))
                                                      .contentType(MediaType.APPLICATION_JSON)
                                                      .content("{\"expectedRevision\":3}"))
           .andExpect(status().isAccepted());
        JsonNode message = receive(QUEUE, 10_000);
        assertEquals("REMOVE", message.get("action")
                                      .asText());
        assertFalse(message.has("roles"), "a removal carries no roles");
        assertEquals(3, message.get("expectedRevision")
                               .asLong());

        pushSnapshot(mvc, tenant.getId(), 4, TenantUsersTestSupport.user(ANN)
                                                                   .revision(4)
                                                                   .status("REMOVED"));
        mvc.perform(get(USERS).session(owner)
                              .with(authentication(person(OWNER, "Owner"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[*].email", containsInAnyOrder(OWNER)));
    }

    @Test
    @Order(8)
    void aFormPostAndTheDefaultTenantAreRefusedInTheEndpointsShape() throws Exception {
        MockHttpSession owner = enter(OWNER, "Owner");
        mvc.perform(post(USERS).session(owner)
                               .with(authentication(person(OWNER, "Owner")))
                               .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                               .content("email=x%40example.com&roles=User"))
           .andExpect(status().isUnsupportedMediaType())
           .andExpect(jsonPath("$.reason").value("UNSUPPORTED_MEDIA_TYPE"));

        mvc.perform(post(USERS).with(user("admin").roles("ADMINISTRATOR"))
                               .contentType(MediaType.APPLICATION_JSON)
                               .content("{\"email\":\"x@example.com\",\"roles\":[\"User\"]}"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.reason").value("DEFAULT_TENANT"));
    }

    @Test
    @Order(9)
    void enteringTheTenantStampsAnExistingRowOnly() throws Exception {
        enter("stranger@example.com", "User");

        MockHttpSession owner = enter(OWNER, "Owner");
        assertNotNull(userNamed(owner, OWNER).get("lastSignInAt"), "the owner's own row records the sign-in");
        String body = mvc.perform(get(USERS).session(owner)
                                            .with(authentication(person(OWNER, "Owner"))))
                         .andReturn()
                         .getResponse()
                         .getContentAsString();
        assertFalse(body.contains("stranger@example.com"), "a sign-in never creates a row: " + body);
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

    private static JsonNode roleOf(JsonNode user, String role) {
        for (JsonNode row : user.get("roles")) {
            if (role.equals(row.get("role")
                               .asText())) {
                return row;
            }
        }
        throw new AssertionError("[" + user.get("email") + "] has no role [" + role + "]");
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

}
