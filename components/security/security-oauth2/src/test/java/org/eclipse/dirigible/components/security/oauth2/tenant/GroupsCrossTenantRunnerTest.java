/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.CrossTenantRunner.CrossTenantResult;
import org.eclipse.dirigible.components.base.tenant.CrossTenantRunner.GrantedTenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.tenant.CrossTenantRunEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * A user who holds roles in several tenants runs work across them: each tenant is visited in its
 * own scope and with its own roles, the roles of the selected tenant never ride along, a failure
 * stays in its tenant, the caller is restored afterwards, and every visit is announced for the
 * audit trail.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupsCrossTenantRunnerTest {

    private static final String GROUPS_CLAIM = "groups";
    private static final String APP_ID = "ledger";
    private static final String USER = "accountant@practice.example";
    private static final String ACME = "acme";
    private static final String GLOBEX = "globex";
    private static final String INITECH = "initech";

    @Mock
    private TenantService tenantService;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private ApplicationEventPublisher events;

    @Mock
    private TenantContext tenantContext;

    /** The tenant the fake tenant context has open, per visit. */
    private final List<String> openedTenants = new ArrayList<>();

    private GroupsCrossTenantRunner runner;

    @BeforeEach
    void setUp() throws Exception {
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(GROUPS_CLAIM);
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        TenantSelectionManager manager = new TenantSelectionManager(new TenantGroupsClaim(), tenantService, securityContextRepository,
                events, org.mockito.Mockito.mock(TenantContext.class));
        runner = new GroupsCrossTenantRunner(manager, tenantContext, events);
        when(tenantContext.execute(anyString(), any(CallableResultAndException.class))).thenAnswer(invocation -> {
            openedTenants.add(invocation.getArgument(0));
            return ((CallableResultAndException<?, ?>) invocation.getArgument(1)).call();
        });
        when(tenantService.findById(ACME)).thenReturn(Optional.of(tenant(ACME, "Acme Ltd", TenantStatus.PROVISIONED)));
        when(tenantService.findById(GLOBEX)).thenReturn(Optional.of(tenant(GLOBEX, "Globex", TenantStatus.PROVISIONED)));
        when(tenantService.findById(INITECH)).thenReturn(Optional.of(tenant(INITECH, "Initech", TenantStatus.INITIAL)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        Configuration.remove(DirigibleConfig.TENANT_GROUPS_CLAIM.getKey());
        Configuration.remove(DirigibleConfig.APP_ID.getKey());
    }

    @Test
    void theGrantedTenantsAreTheEnterableOnesWithTheRolesHeldInEach() {
        authenticate(Set.of("Accountant"), "acme.ledger.Accountant", "globex.ledger.Viewer", "initech.ledger.Accountant");

        List<GrantedTenant> granted = runner.grantedTenants();

        assertThat(granted).containsExactly(new GrantedTenant(ACME, "Acme Ltd", Set.of("Accountant")),
                new GrantedTenant(GLOBEX, "Globex", Set.of("Viewer")));
    }

    @Test
    void eachTenantIsVisitedInItsScopeWithItsOwnRolesOnly() {
        // The session has acme selected, so the caller carries acme's Accountant role.
        authenticate(Set.of("Accountant"), "acme.ledger.Accountant", "globex.ledger.Viewer", "DEVELOPER");
        Map<String, Set<String>> rolesSeen = new java.util.LinkedHashMap<>();

        List<CrossTenantResult<String>> results = runner.forEachGrantedTenant("period-close", tenantId -> {
            rolesSeen.put(tenantId, currentRoleNames());
            return "closed " + tenantId;
        });

        assertThat(openedTenants).containsExactly(ACME, GLOBEX);
        assertThat(rolesSeen.get(ACME)).containsExactlyInAnyOrder("Accountant", "DEVELOPER");
        assertThat(rolesSeen.get(GLOBEX)).containsExactlyInAnyOrder("Viewer", "DEVELOPER");
        assertThat(results).containsExactly(new CrossTenantResult<>(ACME, true, "closed acme", null),
                new CrossTenantResult<>(GLOBEX, true, "closed globex", null));
    }

    @Test
    void theCallerIsRestoredAfterTheRun() {
        Authentication caller = authenticate(Set.of("Accountant"), "acme.ledger.Accountant", "globex.ledger.Viewer");

        runner.forEachGrantedTenant("period-close", tenantId -> null);

        assertThat(SecurityContextHolder.getContext()
                                        .getAuthentication()).isSameAs(caller);
    }

    @Test
    void aFailureStaysInItsTenantAndTheRunCarriesOn() {
        Authentication caller = authenticate(Set.of(), "acme.ledger.Accountant", "globex.ledger.Viewer");

        List<CrossTenantResult<String>> results = runner.forEachGrantedTenant("period-close", tenantId -> {
            if (ACME.equals(tenantId)) {
                throw new IllegalStateException("period 2026-09 is locked");
            }
            return "ok";
        });

        assertThat(results).containsExactly(new CrossTenantResult<>(ACME, false, null, "period 2026-09 is locked"),
                new CrossTenantResult<>(GLOBEX, true, "ok", null));
        assertThat(SecurityContextHolder.getContext()
                                        .getAuthentication()).isSameAs(caller);
    }

    @Test
    void everyVisitIsAnnouncedForTheAuditTrail() {
        authenticate(Set.of(), "acme.ledger.Accountant", "globex.ledger.Viewer");

        runner.forEachGrantedTenant("vat-return", tenantId -> {
            if (GLOBEX.equals(tenantId)) {
                throw new IllegalArgumentException("no VAT registration");
            }
            return null;
        });

        ArgumentCaptor<CrossTenantRunEvent> event = ArgumentCaptor.forClass(CrossTenantRunEvent.class);
        verify(events, times(2)).publishEvent(event.capture());
        assertThat(event.getAllValues())
                                        .extracting(CrossTenantRunEvent::tenantId, CrossTenantRunEvent::principal,
                                                CrossTenantRunEvent::operation, CrossTenantRunEvent::succeeded, CrossTenantRunEvent::error)
                                        .containsExactly(org.assertj.core.groups.Tuple.tuple(ACME, USER, "vat-return", true, null),
                                                org.assertj.core.groups.Tuple.tuple(GLOBEX, USER, "vat-return", false,
                                                        "no VAT registration"));
    }

    @Test
    void aBearerUserIsVisitedWithTheTenantsRolesNotTheRolesOfTheTenantTheRequestNamed() {
        // A bearer request that named acme carries acme's Accountant role on top of the token's scope.
        Jwt jwt = Jwt.withTokenValue("token")
                     .header("alg", "RS256")
                     .subject(USER)
                     .claim(GROUPS_CLAIM, List.of("acme.ledger.Accountant", "globex.ledger.Viewer"))
                     .issuedAt(Instant.now())
                     .expiresAt(Instant.now()
                                       .plusSeconds(300))
                     .build();
        List<org.springframework.security.core.GrantedAuthority> authorities = new ArrayList<>(AuthoritiesUtil.toAuthorities("Accountant"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_openid"));
        setAuthentication(new JwtAuthenticationToken(jwt, authorities, USER));
        Map<String, Set<String>> rolesSeen = new java.util.LinkedHashMap<>();

        runner.forEachGrantedTenant("period-close", tenantId -> rolesSeen.put(tenantId, currentRoleNames()));

        assertThat(rolesSeen.get(GLOBEX)).containsExactlyInAnyOrder("Viewer", "SCOPE_openid");
    }

    @Test
    void aRunWithoutASignedInUserIsRefused() {
        setAuthentication(new UsernamePasswordAuthenticationToken("system", null, List.of()));

        assertThatThrownBy(() -> runner.forEachGrantedTenant("period-close", tenantId -> null)).isInstanceOf(IllegalStateException.class);
        verify(events, never()).publishEvent(any());
    }

    @Test
    void aRunCannotBeStartedFromWithinARun() {
        authenticate(Set.of(), "acme.ledger.Accountant");

        List<CrossTenantResult<Object>> results =
                runner.forEachGrantedTenant("outer", tenantId -> runner.forEachGrantedTenant("inner", inner -> null));

        assertThat(results).singleElement()
                           .satisfies(result -> {
                               assertThat(result.succeeded()).isFalse();
                               assertThat(result.error()).contains("within a cross-tenant run");
                           });
    }

    @Test
    void aRunNamesItsOperation() {
        authenticate(Set.of(), "acme.ledger.Accountant");

        assertThatThrownBy(() -> runner.forEachGrantedTenant(" ", tenantId -> null)).isInstanceOf(IllegalArgumentException.class);
    }

    private Authentication authenticate(Set<String> currentRoles, String... groups) {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now()
                                                                                .plusSeconds(300),
                Map.of("sub", USER, GROUPS_CLAIM, List.of(groups)));
        OidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        Authentication authentication = new OAuth2AuthenticationToken(oidcUser, AuthoritiesUtil.toAuthorities(currentRoles), "keycloak");
        setAuthentication(authentication);
        return authentication;
    }

    private static void setAuthentication(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private static Set<String> currentRoleNames() {
        return Set.copyOf(AuthoritiesUtil.toRoleNames(SecurityContextHolder.getContext()
                                                                           .getAuthentication()
                                                                           .getAuthorities()));
    }

    private static Tenant tenant(String id, String name, TenantStatus status) {
        Tenant tenant = new Tenant("-", name, "The " + name + " tenant", id, status);
        tenant.setId(id);
        return tenant;
    }
}
