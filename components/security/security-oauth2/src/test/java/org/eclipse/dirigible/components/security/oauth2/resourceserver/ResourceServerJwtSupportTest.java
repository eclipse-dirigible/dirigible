/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.resourceserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.access.AuthenticatedBearerToken;
import org.eclipse.dirigible.components.security.oauth.ScopeRoleJwtAuthoritiesConverter;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantAwareAuthoritiesMapper;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantGroupsClaim;
import org.eclipse.dirigible.components.security.service.ScopeService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

/**
 * The kind-aware bearer rules, end to end through a JWKS-backed decoder: RS256 tokens minted here
 * and served from an in-process JWKS endpoint, exactly as the chain sees them.
 *
 * <p>
 * The first test is the regression guard: the machine-to-machine access token accepted before this
 * class existed - scopes to roles, {@code sub} as the name, no audience - must pass unchanged.
 */
class ResourceServerJwtSupportTest {

    private static final String KEY_ID = "test-signing-key";
    private static final String COGNITO_ISSUER = "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_TEST";
    private static final String KEYCLOAK_ISSUER = "https://keycloak.example.org/realms/dirigible";
    private static final String CLIENT_ID = "the-client";
    private static final String M2M_SCOPES =
            "sample-resource-server/ADMINISTRATOR sample-resource-server/sample-app.Orders.OrderFullAccess openid";

    private static RSAKey rsaKey;
    private static HttpServer jwksServer;
    private static String jwkSetUri;

    @BeforeAll
    static void startJwks() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID(KEY_ID)
                                          .algorithm(JWSAlgorithm.RS256)
                                          .generate();
        byte[] jwks = new JWKSet(rsaKey.toPublicJWK()).toString()
                                                      .getBytes(StandardCharsets.UTF_8);
        jwksServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        jwksServer.createContext("/.well-known/jwks.json", exchange -> {
            exchange.getResponseHeaders()
                    .add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(jwks);
            }
        });
        jwksServer.start();
        jwkSetUri = "http://localhost:" + jwksServer.getAddress()
                                                    .getPort()
                + "/.well-known/jwks.json";
    }

    @AfterAll
    static void stopJwks() {
        jwksServer.stop(0);
    }

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_TOKEN_KINDS.getKey());
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_PRINCIPAL_CLAIM.getKey());
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_AUDIENCES.getKey());
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_REQUIRE_VERIFIED_EMAIL.getKey());
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
        Configuration.remove(DirigibleConfig.TENANT_GROUPS_CLAIM.getKey());
        Configuration.remove(DirigibleConfig.APP_ID.getKey());
    }

    // --- access tokens: the rules that applied before -----------------------------------------

    @Test
    void theMachineToMachineAccessTokenAcceptedSoFarStillPasses() throws Exception {
        String token = sign(COGNITO_ISSUER, claims -> claims.subject("m2m-client")
                                                            .claim("token_use", "access")
                                                            .claim("scope", M2M_SCOPES));

        Authentication authentication = cognito().authenticate(token)
                                                 .authentication();

        assertEquals("m2m-client", authentication.getName());
        assertEquals(Set.of("ADMINISTRATOR", "sample-app.Orders.OrderFullAccess"), roles(authentication));
    }

    @Test
    void anAccessTokenOfAnotherIssuerIsRefused() throws Exception {
        String token =
                sign("https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_OTHER", claims -> claims.claim("token_use", "access")
                                                                                                          .claim("scope", M2M_SCOPES));

        assertThrows(InvalidBearerTokenException.class, () -> cognito().authenticate(token));
    }

    @Test
    void aCognitoAccessTokenIsHeldToItsClientIdOnceAudiencesAreConfigured() throws Exception {
        DirigibleConfig.OAUTH2_JWT_AUDIENCES.setStringValue(CLIENT_ID);
        String ours = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "access")
                                                           .claim("client_id", CLIENT_ID)
                                                           .claim("scope", M2M_SCOPES));
        String foreign = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "access")
                                                              .claim("client_id", "another-app")
                                                              .claim("scope", M2M_SCOPES));

        assertEquals(Set.of("ADMINISTRATOR", "sample-app.Orders.OrderFullAccess"), roles(cognito().authenticate(ours)
                                                                                                  .authentication()));
        assertThrows(InvalidBearerTokenException.class, () -> cognito().authenticate(foreign));
    }

    @Test
    void aKeycloakServiceAccountTokenPassesUntilAudiencesExcludeIt() throws Exception {
        String token = sign(KEYCLOAK_ISSUER, claims -> claims.subject("service-account-platform")
                                                             .claim("typ", "Bearer")
                                                             .audience("account")
                                                             .claim("azp", CLIENT_ID)
                                                             .claim("scope", "sample-resource-server/OPERATOR"));

        Authentication authentication = keycloak().authenticate(token)
                                                  .authentication();
        assertEquals("service-account-platform", authentication.getName());
        assertEquals(Set.of("OPERATOR"), roles(authentication));

        DirigibleConfig.OAUTH2_JWT_AUDIENCES.setStringValue(CLIENT_ID);
        assertThrows(InvalidBearerTokenException.class, () -> keycloak().authenticate(token));
    }

    // --- ID tokens: the identity of a user -----------------------------------------------------

    @Test
    void aCognitoIdTokenIsNamedByTheEmailAndGrantsTheGroupRoles() throws Exception {
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("email", "jane.doe@example.org")
                                                            .claim("email_verified", true)
                                                            .claim("cognito:groups", List.of("DEVELOPER", "OPERATOR")));

        AuthenticatedBearerToken authenticated = cognito().authenticate(token);

        assertEquals("jane.doe@example.org", authenticated.authentication()
                                                          .getName());
        assertEquals(Set.of("DEVELOPER", "OPERATOR"), roles(authenticated.authentication()));
        assertInstanceOf(JwtAuthenticationToken.class, authenticated.authentication());
        assertEquals(((JwtAuthenticationToken) authenticated.authentication()).getToken()
                                                                              .getExpiresAt(),
                authenticated.expiresAt());
    }

    @Test
    void aKeycloakIdTokenIsNamedByThePreferredUsername() throws Exception {
        String token = sign(KEYCLOAK_ISSUER, claims -> claims.claim("typ", "ID")
                                                             .audience(CLIENT_ID)
                                                             .claim("preferred_username", "jane")
                                                             .claim("groups", List.of("DEVELOPER")));

        Authentication authentication = keycloak().authenticate(token)
                                                  .authentication();

        assertEquals("jane", authentication.getName());
        assertEquals(Set.of("DEVELOPER"), roles(authentication));
    }

    @Test
    void anIdTokenAlsoGrantsWhatItsScopesMapTo() throws Exception {
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("email", "jane.doe@example.org")
                                                            .claim("email_verified", true)
                                                            .claim("cognito:groups", List.of("DEVELOPER"))
                                                            .claim("scope", "sample-resource-server/OPERATOR"));

        assertEquals(Set.of("DEVELOPER", "OPERATOR"), roles(cognito().authenticate(token)
                                                                     .authentication()));
    }

    @Test
    void anIdTokenOfAnotherApplicationOfThePoolIsRefused() throws Exception {
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience("another-app")
                                                            .claim("email", "jane.doe@example.org")
                                                            .claim("email_verified", true)
                                                            .claim("cognito:groups", List.of("ADMINISTRATOR")));

        assertRefused(cognito(), token, "not intended for this client");
    }

    @Test
    void anIdTokenOfAnotherIssuerIsRefused() throws Exception {
        String token = sign(KEYCLOAK_ISSUER, claims -> claims.claim("token_use", "id")
                                                             .audience(CLIENT_ID)
                                                             .claim("email", "jane.doe@example.org")
                                                             .claim("email_verified", true));

        assertRefused(cognito(), token, "iss");
    }

    @Test
    void anExpiredIdTokenIsRefused() throws Exception {
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("email", "jane.doe@example.org")
                                                            .claim("email_verified", true)
                                                            .issueTime(Date.from(Instant.now()
                                                                                        .minusSeconds(3600)))
                                                            .expirationTime(Date.from(Instant.now()
                                                                                             .minusSeconds(120))));

        assertRefused(cognito(), token, "expired");
    }

    @Test
    void anIdTokenWithoutThePrincipalClaimIsRefused() throws Exception {
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("cognito:groups", List.of("DEVELOPER")));

        assertRefused(cognito(), token, "email");
    }

    @Test
    void anIdTokenWithAnUnverifiedEmailIsRefused() throws Exception {
        String unverified = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                                 .audience(CLIENT_ID)
                                                                 .claim("email", "jane.doe@example.org")
                                                                 .claim("email_verified", false));
        String unknown = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                              .audience(CLIENT_ID)
                                                              .claim("email", "jane.doe@example.org"));

        assertRefused(cognito(), unverified, "email_verified");
        assertRefused(cognito(), unknown, "email_verified");
    }

    @Test
    void theVerifiedEmailRequirementCanBeSwitchedOff() throws Exception {
        DirigibleConfig.OAUTH2_JWT_REQUIRE_VERIFIED_EMAIL.setBooleanValue(false);
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("email", "jane.doe@example.org"));

        assertEquals("jane.doe@example.org", cognito().authenticate(token)
                                                      .authentication()
                                                      .getName());
    }

    @Test
    void thePrincipalClaimCanBeChosen() throws Exception {
        DirigibleConfig.OAUTH2_JWT_PRINCIPAL_CLAIM.setStringValue("cognito:username");
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("cognito:username", "jane")
                                                            .claim("email", "jane.doe@example.org"));

        assertEquals("jane", cognito().authenticate(token)
                                      .authentication()
                                      .getName());
    }

    @Test
    void anIdTokenGetsNoRolesFromAGroupsClaimThatIsNotAList() throws Exception {
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("email", "jane.doe@example.org")
                                                            .claim("email_verified", true)
                                                            .claim("cognito:groups", "ADMINISTRATOR"));

        assertTrue(roles(cognito().authenticate(token)
                                  .authentication()).isEmpty());
    }

    // --- token kinds ---------------------------------------------------------------------------

    @Test
    void aRefreshTokenAndATokenOfUnknownUseAreRefused() throws Exception {
        String refresh = sign(KEYCLOAK_ISSUER, claims -> claims.claim("typ", "Refresh")
                                                               .audience(CLIENT_ID));
        String unknown = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "refresh")
                                                              .audience(CLIENT_ID));
        String untyped = sign(COGNITO_ISSUER, claims -> claims.audience(CLIENT_ID)
                                                              .claim("scope", M2M_SCOPES));
        // what an ID token of another issuer the profile was pointed at looks like: no typ, an
        // audience and a user - read as an access token it would pass on signature and issuer alone
        String untypedKeycloak = sign(KEYCLOAK_ISSUER, claims -> claims.audience(CLIENT_ID)
                                                                       .claim("preferred_username", "jane")
                                                                       .claim("scope", M2M_SCOPES));

        assertRefused(keycloak(), refresh, "kind");
        assertRefused(cognito(), unknown, "kind");
        assertRefused(cognito(), untyped, "kind");
        assertRefused(keycloak(), untypedKeycloak, "kind");
    }

    @Test
    void theAcceptedKindsCanBeRestricted() throws Exception {
        DirigibleConfig.OAUTH2_JWT_TOKEN_KINDS.setStringValue("id");
        String access = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "access")
                                                             .claim("scope", M2M_SCOPES));
        String id = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                         .audience(CLIENT_ID)
                                                         .claim("email", "jane.doe@example.org")
                                                         .claim("email_verified", true));

        assertRefused(cognito(), access, "kind");
        assertEquals("jane.doe@example.org", cognito().authenticate(id)
                                                      .authentication()
                                                      .getName());
    }

    // --- the token groups strategy -------------------------------------------------------------

    @Test
    void underTheTokenGroupsStrategyAnIdTokenGrantsTheGlobalRolesOnly() throws Exception {
        useTokenGroups();
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("email", "jane.doe@example.org")
                                                            .claim("email_verified", true)
                                                            .claim("cognito:groups", List.of("DEVELOPER", "acme.library.Owner")));

        assertEquals(Set.of("DEVELOPER"), roles(cognito().authenticate(token)
                                                         .authentication()));
    }

    @Test
    void underTheTokenGroupsStrategyAnIdTokenWithoutAGlobalRoleIsRefused() throws Exception {
        useTokenGroups();
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("email", "jane.doe@example.org")
                                                            .claim("email_verified", true)
                                                            .claim("cognito:groups", List.of("acme.library.Owner")));

        assertRefused(cognito(), token, "no role");
    }

    // --- the chain path and the frame path agree ----------------------------------------------

    @Test
    void theDecoderAndTheConverterYieldWhatAuthenticateYields() throws Exception {
        String token = sign(COGNITO_ISSUER, claims -> claims.claim("token_use", "id")
                                                            .audience(CLIENT_ID)
                                                            .claim("email", "jane.doe@example.org")
                                                            .claim("email_verified", true)
                                                            .claim("cognito:groups", List.of("DEVELOPER")));
        ResourceServerJwtSupport support = cognito();

        Jwt jwt = support.decoder()
                         .decode(token);
        AbstractAuthenticationToken viaChain = support.authenticationConverter()
                                                      .convert(jwt);
        Authentication viaFrame = support.authenticate(token)
                                         .authentication();

        assertEquals(viaFrame.getName(), viaChain.getName());
        assertEquals(roles(viaFrame), roles(viaChain));
    }

    @Test
    void anEmptyTokenIsBadCredentials() {
        assertThrows(BadCredentialsException.class, () -> cognito().authenticate(" "));
    }

    private static ResourceServerJwtSupport cognito() {
        return support(ResourceServerJwtSettings.cognito(jwkSetUri, COGNITO_ISSUER, CLIENT_ID, "email"), "cognito:groups");
    }

    private static ResourceServerJwtSupport keycloak() {
        return support(ResourceServerJwtSettings.keycloak(jwkSetUri, KEYCLOAK_ISSUER, CLIENT_ID, "preferred_username"), "groups");
    }

    private static ResourceServerJwtSupport support(ResourceServerJwtSettings settings, String providerGroupsClaim) {
        ScopeService scopeService = mock(ScopeService.class);
        when(scopeService.getScopeRolesMappings()).thenReturn(Collections.emptyMap());
        return new ResourceServerJwtSupport(settings, new ScopeRoleJwtAuthoritiesConverter(scopeService),
                new TenantAwareAuthoritiesMapper(new TenantGroupsClaim(), providerGroupsClaim));
    }

    private static void useTokenGroups() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue("cognito:groups");
        DirigibleConfig.APP_ID.setStringValue("library");
    }

    private static void assertRefused(ResourceServerJwtSupport support, String token, String reason) {
        InvalidBearerTokenException exception = assertThrows(InvalidBearerTokenException.class, () -> support.authenticate(token));
        assertTrue(exception.getMessage()
                            .toLowerCase()
                            .contains(reason.toLowerCase()),
                "expected the refusal to mention [" + reason + "] but was: " + exception.getMessage());
    }

    private static Set<String> roles(Authentication authentication) {
        return roles(authentication.getAuthorities());
    }

    private static Set<String> roles(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                          .map(GrantedAuthority::getAuthority)
                          .filter(authority -> authority.startsWith("ROLE_"))
                          .map(authority -> authority.substring("ROLE_".length()))
                          .collect(Collectors.toSet());
    }

    private static String sign(String issuer, Consumer<JWTClaimsSet.Builder> claims) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(issuer)
                                                                 .subject("3b3f18f4-1c3d-4b6e-9f9a-0e2f4c1a5d77")
                                                                 .issueTime(Date.from(now))
                                                                 .expirationTime(Date.from(now.plusSeconds(3600)));
        claims.accept(builder);
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID)
                                                                                     .build(),
                builder.build());
        signedJWT.sign(new RSASSASigner(rsaKey));
        return signedJWT.serialize();
    }
}
