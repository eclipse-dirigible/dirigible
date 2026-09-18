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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.base.http.access.AuthenticatedBearerToken;
import org.eclipse.dirigible.components.base.http.access.BearerTokenAuthenticator;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
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
 */
public class ResourceServerJwtSupport implements BearerTokenAuthenticator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServerJwtSupport.class);

    /** The claim an ID token carries the user's e-mail address in. */
    static final String EMAIL_CLAIM = "email";

    /** The claim stating whether the e-mail address of an ID token was verified by the provider. */
    static final String EMAIL_VERIFIED_CLAIM = "email_verified";

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
        this.decoder = NimbusJwtDecoder.withJwkSetUri(settings.jwkSetUri())
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
            Set<GrantedAuthority> groupAuthorities = groupAuthoritiesMapper.authoritiesOf(jwt);
            if (groupAuthorities.isEmpty() && TenantResolutionStrategy.fromConfiguration() == TenantResolutionStrategy.TOKEN_GROUPS) {
                // the tenant roles of such a user apply only once a tenant is selected, which a bearer
                // request never does - a user with no global role has nothing here, as the tenant
                // selection filter concludes for a session
                throw new InvalidBearerTokenException("The token grants no role of this deployment");
            }
            Set<GrantedAuthority> authorities = new LinkedHashSet<>(groupAuthorities);
            Collection<GrantedAuthority> scopeAuthorities = scopeAuthoritiesConverter.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }
            return authorities;
        }
    }
}
