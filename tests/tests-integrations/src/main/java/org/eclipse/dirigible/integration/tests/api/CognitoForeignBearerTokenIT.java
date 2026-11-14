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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.restassured.RestAssured;

/**
 * A bearer token the identity provider did not issue is the application's, not the platform's
 * (#8592).
 *
 * <p>
 * The {@code cognito} chain used to take every {@code Authorization: Bearer} header for one of the
 * provider's, on every path: an application acting as an OIDC provider never saw the bearer call to
 * its own public {@code /userinfo}, and the second such call within thirty seconds - the key set's
 * refresh being rate limited by then - was answered with a 500 instead of a 401. Now a token naming
 * another issuer reaches the application, and one naming the provider but a key it does not publish
 * is refused with a 401 every time.
 */
@ActiveProfiles("cognito")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CognitoForeignBearerTokenIT extends IntegrationTest {

    private static final String PROJECT = "cognito-foreign-bearer-token-it";
    private static final String USERINFO = "/public/js/" + PROJECT + "/userinfo.mjs";
    private static final String PROTECTED_USERINFO = "/services/js/" + PROJECT + "/userinfo.mjs";
    private static final String APPLICATION_ISSUER = "https://the-application.example.org/oidc";

    /** The application's userinfo: answers the bearer token it received. */
    private static final String USERINFO_SOURCE = """
            import { request, response } from "@aerokit/sdk/http";

            response.setContentType("application/json");
            response.println(JSON.stringify({ authorization: request.getHeader("Authorization") }));
            """;

    /**
     * Opens the userinfo to anonymous callers, as an OIDC provider's userinfo must be - by its full
     * path, the filter strips no {@code /public/js} prefix.
     */
    private static final String PUBLIC_ACCESS = """
            {
                "constraints": [
                    {
                        "path": "/public/js/%s/userinfo.mjs",
                        "method": "GET",
                        "scope": "HTTP",
                        "roles": [
                            "public"
                        ]
                    }
                ]
            }
            """.formatted(PROJECT);

    private static ExternalFrontendIT.MockIdentityProvider identityProvider;
    /** A key the provider never published - the application's own signing key. */
    private static RSAKey unpublishedKey;

    @LocalServerPort
    private int port;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @BeforeAll
    static void startIdentityProvider() throws Exception {
        identityProvider = ExternalFrontendIT.MockIdentityProvider.start();
        unpublishedKey = new RSAKeyGenerator(2048).keyID("the-application-key")
                                                  .generate();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    static void stopIdentityProvider() {
        identityProvider.stop();
    }

    /**
     * The cognito profile with the user pool's issuer and JWKS replaced by the mock identity provider.
     */
    @DynamicPropertySource
    static void cognitoProfile(DynamicPropertyRegistry registry) {
        registry.add("DIRIGIBLE_COGNITO_CLIENT_ID", () -> ExternalFrontendIT.CLIENT_ID);
        registry.add("DIRIGIBLE_COGNITO_CLIENT_SECRET", () -> ExternalFrontendIT.CLIENT_SECRET);
        registry.add("DIRIGIBLE_COGNITO_DOMAIN", () -> identityProvider.issuer());
        registry.add("DIRIGIBLE_COGNITO_REGION_ID", () -> "eu-central-1");
        registry.add("DIRIGIBLE_COGNITO_USER_POOL_ID", () -> "eu-central-1_it");
        registry.add("spring.security.oauth2.client.provider.cognito.issuer-uri", () -> identityProvider.issuer());
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> identityProvider.issuer() + "/protocol/openid-connect/certs");
        registry.add("DIRIGIBLE_HOST", () -> "{baseUrl}");
    }

    @BeforeEach
    void publishUserinfo() {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/userinfo.mjs",
                USERINFO_SOURCE.getBytes(StandardCharsets.UTF_8), false, "application/javascript", true);
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/security/userinfo.access",
                PUBLIC_ACCESS.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    @Test
    void aTokenOfTheApplicationReachesItsPublicEndpointEveryTime() throws JOSEException {
        String token = signedWithUnpublishedKey(APPLICATION_ISSUER);

        for (int i = 0; i < 3; i++) {
            given().port(port)
                   .auth()
                   .oauth2(token)
                   .when()
                   .get(USERINFO)
                   .then()
                   .statusCode(200)
                   .body("authorization", equalTo("Bearer " + token));
        }
    }

    @Test
    void aTokenOfTheApplicationAuthenticatesNoOneOnAProtectedPath() throws JOSEException {
        String token = signedWithUnpublishedKey(APPLICATION_ISSUER);

        for (int i = 0; i < 3; i++) {
            given().port(port)
                   .auth()
                   .oauth2(token)
                   .when()
                   .get(PROTECTED_USERINFO)
                   .then()
                   .statusCode(401);
        }
    }

    @Test
    void aTokenNamingTheProviderAndAKeyItDoesNotPublishIsRefusedEveryTime() throws JOSEException {
        String token = signedWithUnpublishedKey(identityProvider.issuer());

        // past the first refresh of the key set, every lookup of the unknown key is rate limited
        for (int i = 0; i < 3; i++) {
            given().port(port)
                   .auth()
                   .oauth2(token)
                   .when()
                   .get(USERINFO)
                   .then()
                   .statusCode(401)
                   .header(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token"));
        }
    }

    private static String signedWithUnpublishedKey(String issuer) throws JOSEException {
        Instant now = Instant.now();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(unpublishedKey.getKeyID())
                                                                               .build(),
                new JWTClaimsSet.Builder().issuer(issuer)
                                          .subject("jane")
                                          .claim("token_use", "access")
                                          .issueTime(Date.from(now))
                                          .expirationTime(Date.from(now.plusSeconds(300)))
                                          .build());
        jwt.sign(new RSASSASigner(unpublishedKey));
        return jwt.serialize();
    }
}
