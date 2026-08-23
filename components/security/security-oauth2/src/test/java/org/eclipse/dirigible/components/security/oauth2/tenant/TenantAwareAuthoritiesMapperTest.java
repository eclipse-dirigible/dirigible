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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

/**
 * The subdomain strategy keeps mapping every group of the identity provider's own claim, byte for
 * byte as before this class existed. The token groups strategy maps only the global roles, because
 * which tenant's roles apply is not known until the user picks one.
 */
class TenantAwareAuthoritiesMapperTest {

    private static final String COGNITO_CLAIM = "cognito:groups";
    private static final String KEYCLOAK_CLAIM = "groups";
    private static final String APP_ID = "library";

    @BeforeEach
    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
        Configuration.remove(DirigibleConfig.TENANT_GROUPS_CLAIM.getKey());
        Configuration.remove(DirigibleConfig.APP_ID.getKey());
    }

    @Test
    void subdomainStrategyMapsEveryGroupOfTheProviderClaim() {
        Collection<? extends GrantedAuthority> mapped =
                mapper(COGNITO_CLAIM).mapAuthorities(List.of(oidcAuthority(COGNITO_CLAIM, "DEVELOPER", "acme.library.Owner")));

        assertThat(roleNames(mapped)).containsExactlyInAnyOrder("DEVELOPER", "acme.library.Owner");
    }

    @Test
    void subdomainStrategyIsIdenticalToTheLegacyMapping() {
        List<String> groups = List.of("DEVELOPER", "OPERATOR", "acme.library.Owner");

        Collection<GrantedAuthority> mapped =
                new LinkedHashSet<>(mapper(COGNITO_CLAIM).mapAuthorities(List.of(oidcAuthority(COGNITO_CLAIM, groups))));

        assertThat(mapped).containsExactlyInAnyOrderElementsOf(AuthoritiesUtil.toAuthorities(groups));
    }

    @Test
    void subdomainStrategyReadsTheProviderClaimAndNotTheConfiguredOne() {
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(COGNITO_CLAIM);

        Collection<? extends GrantedAuthority> mapped =
                mapper(KEYCLOAK_CLAIM).mapAuthorities(List.of(oidcAuthority(KEYCLOAK_CLAIM, "DEVELOPER")));

        assertThat(roleNames(mapped)).containsExactly("DEVELOPER");
    }

    @Test
    void tokenGroupsStrategyMapsGlobalRolesOnly() {
        useTokenGroups();

        Collection<? extends GrantedAuthority> mapped = mapper(COGNITO_CLAIM).mapAuthorities(
                List.of(oidcAuthority(COGNITO_CLAIM, "DEVELOPER", "acme.library.Owner", "globex.library.User", "acme.bi.Owner")));

        assertThat(roleNames(mapped)).containsExactly("DEVELOPER");
    }

    @Test
    void tokenGroupsStrategyGrantsNothingToAUserWithTenantGroupsOnly() {
        useTokenGroups();

        Collection<? extends GrantedAuthority> mapped =
                mapper(COGNITO_CLAIM).mapAuthorities(List.of(oidcAuthority(COGNITO_CLAIM, "acme.library.Owner")));

        assertThat(mapped).isEmpty();
    }

    @Test
    void tokenGroupsStrategyReadsTheConfiguredClaim() {
        useTokenGroups();
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(KEYCLOAK_CLAIM);

        // The provider default is the Cognito claim, but a Keycloak realm was configured.
        Collection<? extends GrantedAuthority> mapped =
                mapper(COGNITO_CLAIM).mapAuthorities(List.of(oidcAuthority(KEYCLOAK_CLAIM, "OPERATOR")));

        assertThat(roleNames(mapped)).containsExactly("OPERATOR");
    }

    @Test
    void aMissingClaimGrantsNothing() {
        useTokenGroups();

        assertThat(mapper(COGNITO_CLAIM).mapAuthorities(List.of(oidcAuthority("some-other-claim", "DEVELOPER")))).isEmpty();
        assertThat(mapper(COGNITO_CLAIM).mapAuthorities(List.of())).isEmpty();
        assertThat(mapper(COGNITO_CLAIM).mapAuthorities(null)).isEmpty();
    }

    @Test
    void anAuthorityThatIsNotAnOidcUserAuthorityIsIgnoredInsteadOfFailing() {
        useTokenGroups();

        Collection<? extends GrantedAuthority> mapped =
                mapper(COGNITO_CLAIM).mapAuthorities(List.of(new SimpleGrantedAuthority("SCOPE_read")));

        assertThat(mapped).isEmpty();
    }

    @Test
    void trialModeGrantsEverySystemRole() {
        TenantAwareAuthoritiesMapper trialMapper =
                new TenantAwareAuthoritiesMapper(new TenantGroupsClaim(COGNITO_CLAIM), COGNITO_CLAIM, true);

        Collection<? extends GrantedAuthority> mapped = trialMapper.mapAuthorities(List.of());

        assertThat(roleNames(mapped)).containsExactlyInAnyOrderElementsOf(Arrays.stream(Roles.values())
                                                                                .map(Roles::getRoleName)
                                                                                .collect(Collectors.toSet()));
    }

    private static void useTokenGroups() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
    }

    private static TenantAwareAuthoritiesMapper mapper(String providerClaim) {
        return new TenantAwareAuthoritiesMapper(new TenantGroupsClaim(), providerClaim, false);
    }

    private static OidcUserAuthority oidcAuthority(String claimName, String... groups) {
        return oidcAuthority(claimName, List.of(groups));
    }

    private static OidcUserAuthority oidcAuthority(String claimName, List<String> groups) {
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now()
                                                                             .plusSeconds(300),
                Map.of("sub", "user@example.com", claimName, groups));
        return new OidcUserAuthority(idToken);
    }

    private static Set<String> roleNames(Collection<? extends GrantedAuthority> authorities) {
        return Set.copyOf(AuthoritiesUtil.toRoleNames(authorities));
    }
}
