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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.eclipse.dirigible.components.base.http.access.AuthenticatedBearerToken;
import org.eclipse.dirigible.components.base.http.access.BearerTokenAuthenticator;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantAwareAuthoritiesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;

/**
 * Everything a login profile needs to accept bearer tokens by the rules of its
 * {@link ResourceServerJwtSettings}: the decoder the HTTP chain validates with, the converter that
 * turns a validated token into the platform identity, and the same two applied outside the chain
 * for a STOMP CONNECT frame. One instance per chain, so the identity a bearer token yields is the
 * same wherever it is presented.
 *
 * <p>
 * The decoder is pinned on the chain rather than published as a {@link JwtDecoder} bean: the
 * embedded Spring Authorization Server publishes its own (backed by its in-memory keys), and the
 * Spring Boot resource-server auto-configuration is not on the classpath, so a decoder looked up by
 * type would be the authorization server's and reject every token of the identity provider.
 *
 * <p>
 * Validation is kind-aware. Every token is verified against the JWKS and must carry the issuer of
 * the provider. An ID token must further be issued for this deployment (audience), name its user in
 * the principal claim, and - identified by an e-mail address - carry a verified one; its
 * authorities are the roles of its groups plus whatever its scopes map to. An access token keeps
 * the rules that applied before: identified by {@code sub}, authorities from its scopes, audience
 * checked only where audiences are configured. A token of any other kind, or of a kind the
 * deployment does not accept, is refused.
 *
 * <p>
 * Under the {@code TOKEN_GROUPS} tenant resolution strategy the groups of an ID token yield its
 * global roles only, exactly as a login's do: the roles of a tenant apply once a tenant is entered,
 * which for a bearer request the tenant selection filter does per request, from the
 * {@code X-Tenant-Id} header. A token whose groups grant tenants but no global role is therefore
 * accepted here and refused by that filter when the request names no tenant.
 *
 * <p>
 * The signing keys come from the provider's JWKS endpoint through Nimbus: cached for five minutes,
 * refreshed when a token names a key the cache does not hold, and at most once every thirty seconds
 * - a burst of tokens carrying unknown key ids costs one call to the provider, not one per token,
 * while a genuine key rotation is picked up within that interval. Every asymmetric signature
 * algorithm is accepted, a token being verified only against a key the provider publishes for it.
 */
public class ResourceServerJwtSupport implements BearerTokenAuthenticator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServerJwtSupport.class);

    /** The claim an ID token carries the user's e-mail address in. */
    static final String EMAIL_CLAIM = "email";

    /** The claim stating whether the e-mail address of an ID token was verified by the provider. */
    static final String EMAIL_VERIFIED_CLAIM = "email_verified";

    /**
     * The signature algorithms accepted: every asymmetric one, since a token is verified only against a
     * key the provider publishes for it. HMAC has no place with a public key set - a shared-secret
     * algorithm over a published key is the classic algorithm-confusion attack.
     */
    static final Set<SignatureAlgorithm> ASYMMETRIC_ALGORITHMS = Set.of(SignatureAlgorithm.RS256, SignatureAlgorithm.RS384,
            SignatureAlgorithm.RS512, SignatureAlgorithm.PS256, SignatureAlgorithm.PS384, SignatureAlgorithm.PS512,
            SignatureAlgorithm.ES256, SignatureAlgorithm.ES384, SignatureAlgorithm.ES512);

    /**
     * Connect and read timeout of a JWKS fetch. Nimbus's half-second default is too tight for a cold
     * TLS handshake to a cloud provider; Spring's own source had no bound at all.
     */
    private static final int JWKS_TIMEOUT_MILLIS = 5_000;

    private static final OAuth2Error KIND_NOT_ACCEPTED = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Token kind is not accepted",
            "https://tools.ietf.org/html/rfc6750#section-3.1");

    private final ResourceServerJwtSettings settings;
    private final NimbusJwtDecoder decoder;
    private final Converter<Jwt, AbstractAuthenticationToken> authenticationConverter;
    private final JwtAuthenticationProvider authenticationProvider;

    /**
     * Instantiates the support of one login profile.
     *
     * @param settings the rules of the profile
     * @param scopeAuthoritiesConverter the converter deriving roles from the scopes of a token
     * @param groupAuthoritiesMapper the mapper deriving roles from the groups of an ID token
     */
    public ResourceServerJwtSupport(ResourceServerJwtSettings settings,
            Converter<Jwt, Collection<GrantedAuthority>> scopeAuthoritiesConverter, TenantAwareAuthoritiesMapper groupAuthoritiesMapper) {
        this.settings = settings;
        this.decoder = NimbusJwtDecoder.withJwkSource(jwkSource(settings.jwkSetUri()))
                                       .jwsAlgorithms(algorithms -> algorithms.addAll(ASYMMETRIC_ALGORITHMS))
                                       .build();
        this.decoder.setJwtValidator(kindAwareValidator());
        this.authenticationConverter = new KindAwareAuthenticationConverter(scopeAuthoritiesConverter, groupAuthoritiesMapper);
        this.authenticationProvider = new JwtAuthenticationProvider(this.decoder);
        this.authenticationProvider.setJwtAuthenticationConverter(this.authenticationConverter);
    }

    /**
     * Gets the rules the tokens are held to.
     *
     * @return the settings
     */
    public ResourceServerJwtSettings settings() {
        return settings;
    }

    /**
     * Gets the decoder the HTTP chain validates bearer tokens with.
     *
     * @return the decoder
     */
    public JwtDecoder decoder() {
        return decoder;
    }

    /**
     * Gets the converter turning a validated token into the platform identity.
     *
     * @return the authentication converter
     */
    public Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        return authenticationConverter;
    }

    /**
     * Authenticates a bearer token exactly as the HTTP chain would.
     *
     * @param bearerToken the token, without the scheme prefix
     * @return the authenticated identity and the expiry of the token
     * @throws AuthenticationException when the token is not acceptable
     */
    @Override
    public AuthenticatedBearerToken authenticate(String bearerToken) throws AuthenticationException {
        if (!StringUtils.hasText(bearerToken)) {
            throw new BadCredentialsException("Empty bearer token");
        }
        Authentication authentication = authenticationProvider.authenticate(new BearerTokenAuthenticationToken(bearerToken));
        Instant expiresAt = authentication instanceof JwtAuthenticationToken jwtAuthentication ? jwtAuthentication.getToken()
                                                                                                                  .getExpiresAt()
                : null;
        return new AuthenticatedBearerToken(authentication, expiresAt);
    }

    /**
     * The keys the signatures are verified against, fetched from the JWKS endpoint: cached for five
     * minutes, refreshed when a token names a key the cache does not hold - at most once every thirty
     * seconds, which Spring's own source switches off - and retried once when a fetch fails.
     */
    private static JWKSource<SecurityContext> jwkSource(String jwkSetUri) {
        URL url;
        try {
            url = new URI(jwkSetUri).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException ex) {
            throw new InvalidConfigException("The JWKS endpoint [" + jwkSetUri + "] of the bearer tokens is not a URL: " + ex.getMessage(),
                    DirigibleConfig.OAUTH2_JWT_JWK_SET_URI.getKey());
        }
        return JWKSourceBuilder.<SecurityContext>create(url,
                new DefaultResourceRetriever(JWKS_TIMEOUT_MILLIS, JWKS_TIMEOUT_MILLIS, JWKSourceBuilder.DEFAULT_HTTP_SIZE_LIMIT))
                               .cache(true)
                               .refreshAheadCache(false)
                               .rateLimited(true)
                               .retrying(true)
                               .build();
    }

    private OAuth2TokenValidator<Jwt> kindAwareValidator() {
        OAuth2TokenValidator<Jwt> issuedByProvider = JwtValidators.createDefaultWithIssuer(settings.issuer());
        OAuth2TokenValidator<Jwt> idToken = new DelegatingOAuth2TokenValidator<>(idTokenValidators(issuedByProvider));
        OAuth2TokenValidator<Jwt> accessToken = new DelegatingOAuth2TokenValidator<>(accessTokenValidators(issuedByProvider));
        return jwt -> {
            Optional<TokenKind> kind = settings.kindOf(jwt)
                                               .filter(settings.acceptedKinds()::contains);
            if (kind.isEmpty()) {
                LOGGER.debug("Refusing a bearer token of [{}] whose kind is not accepted, accepted are {}", settings.provider(),
                        settings.acceptedKinds());
                return OAuth2TokenValidatorResult.failure(KIND_NOT_ACCEPTED);
            }
            return kind.get() == TokenKind.ID ? idToken.validate(jwt) : accessToken.validate(jwt);
        };
    }

    private List<OAuth2TokenValidator<Jwt>> idTokenValidators(OAuth2TokenValidator<Jwt> issuedByProvider) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(issuedByProvider);
        // a session minted from the token ends when the token does, so an ID token without an expiry
        // cannot be accepted (the specification requires the claim anyway)
        validators.add(new JwtClaimValidator<Object>(JwtClaimNames.EXP, Objects::nonNull));
        validators.add(AudienceValidator.ofAudienceClaim(settings.idTokenAudiences()));
        validators.add(new JwtClaimValidator<Object>(settings.principalClaim(), ResourceServerJwtSupport::isText));
        if (settings.requireVerifiedEmail() && EMAIL_CLAIM.equals(settings.principalClaim())) {
            validators.add(new JwtClaimValidator<Object>(EMAIL_VERIFIED_CLAIM, ResourceServerJwtSupport::isTrue));
        }
        return validators;
    }

    private List<OAuth2TokenValidator<Jwt>> accessTokenValidators(OAuth2TokenValidator<Jwt> issuedByProvider) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(issuedByProvider);
        if (!settings.accessTokenAudiences()
                     .isEmpty()) {
            validators.add(
                    settings.provider() == IdentityProvider.COGNITO ? AudienceValidator.ofCognitoClientId(settings.accessTokenAudiences())
                            : AudienceValidator.ofAudienceClaim(settings.accessTokenAudiences()));
        }
        return validators;
    }

    private static boolean isText(Object claimValue) {
        return claimValue instanceof String text && StringUtils.hasText(text);
    }

    private static boolean isTrue(Object claimValue) {
        return Boolean.TRUE.equals(claimValue) || "true".equals(claimValue);
    }

    /**
     * Turns a validated token into the platform identity by the rules of its kind.
     */
    private final class KindAwareAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

        private final JwtAuthenticationConverter idTokenConverter;
        private final JwtAuthenticationConverter accessTokenConverter;

        KindAwareAuthenticationConverter(Converter<Jwt, Collection<GrantedAuthority>> scopeAuthoritiesConverter,
                TenantAwareAuthoritiesMapper groupAuthoritiesMapper) {
            idTokenConverter = new JwtAuthenticationConverter();
            idTokenConverter.setPrincipalClaimName(settings.principalClaim());
            idTokenConverter.setJwtGrantedAuthoritiesConverter(
                    jwt -> idTokenAuthorities(jwt, scopeAuthoritiesConverter, groupAuthoritiesMapper));
            accessTokenConverter = new JwtAuthenticationConverter();
            accessTokenConverter.setJwtGrantedAuthoritiesConverter(scopeAuthoritiesConverter);
        }

        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            TokenKind kind = settings.kindOf(jwt)
                                     .orElseThrow(() -> new InvalidBearerTokenException(KIND_NOT_ACCEPTED.getDescription()));
            return kind == TokenKind.ID ? idTokenConverter.convert(jwt) : accessTokenConverter.convert(jwt);
        }

        private Collection<GrantedAuthority> idTokenAuthorities(Jwt jwt,
                Converter<Jwt, Collection<GrantedAuthority>> scopeAuthoritiesConverter,
                TenantAwareAuthoritiesMapper groupAuthoritiesMapper) {
            // under the token groups strategy these are the global roles only: the roles of a tenant
            // are granted per request by the tenant selection filter, once the request names the
            // tenant - the token itself cannot say which of the user's tenants a request is about
            Set<GrantedAuthority> authorities = new LinkedHashSet<>(groupAuthoritiesMapper.authoritiesOf(jwt));
            Collection<GrantedAuthority> scopeAuthorities = scopeAuthoritiesConverter.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }
            return authorities;
        }
    }
}
