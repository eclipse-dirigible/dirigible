/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth.domain;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Class OAuthClientRegistration.
 */
@Entity
@Table(name = "DIRIGIBLE_CLIENT_REGISTRATIONS")
public class ClientRegistration extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "client-registration";

    /** The id. */
    @Id
    @Column(name = "CLIENT_REGISTRATION_ID", nullable = false)
    private String id;

    /** The OAuth Client Id. */
    @Column(name = "CLIENT_REGISTRATION_CLIENT_ID", nullable = false)
    private String clientId;

    /** The OAuth Client Secret. */
    @Column(name = "CLIENT_REGISTRATION_CLIENT_SECRET", nullable = false)
    private String clientSecret;

    /** The OAuth Redirect URI. */
    @Column(name = "CLIENT_REGISTRATION_REDIRECT_URI", nullable = false)
    private String redirectUri;

    /** The OAuth Authorization Grant Type. */
    @Column(name = "CLIENT_REGISTRATION_AUTHORIZATION_GRANT_TYPE", nullable = false)
    private String authorizationGrantType;

    /** The OAuth Scope. */
    @Column(name = "CLIENT_REGISTRATION_SCOPE", nullable = false)
    private String scope;

    /** The OAuth Token URI. */
    @Column(name = "CLIENT_REGISTRATION_TOKEN_URI", nullable = false)
    private String tokenUri;

    /** The OAuth Authorization URI. */
    @Column(name = "CLIENT_REGISTRATION_AUTHORIZATION_URI", nullable = false)
    private String authorizationUri;

    /** The OAuth User Info URI. */
    @Column(name = "CLIENT_REGISTRATION_USER_INFO_URI", nullable = false)
    private String userInfoUri;

    /** The OAuth Issuer URI. */
    @Column(name = "CLIENT_REGISTRATION_ISSUER_URI", nullable = false)
    private String issuerUri;

    /** The OAuth JWK Set URI. */
    @Column(name = "CLIENT_REGISTRATION_JWK_SET_URI", nullable = false)
    private String jwkSetUri;

    /** The OAuth User Name Attribute Name. */
    @Column(name = "CLIENT_REGISTRATION_USER_NAME_ATTRIBUTE_NAME", nullable = false)
    private String userNameAttributeName;


    /**
     * Instantiates a new oauth client registration.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param clientId
     * @param clientSecret
     * @param redirectUri
     * @param authorizationGrantType
     * @param scope
     * @param tokenUri
     * @param authorizationUri
     * @param userInfoUri
     * @param issuerUri
     * @param jwkSetUri
     * @param userNameAttributeName
     */
    public ClientRegistration(String location, String name, String description, String clientId, String clientSecret, String redirectUri,
            String authorizationGrantType, String scope, String tokenUri, String authorizationUri, String userInfoUri, String issuerUri,
            String jwkSetUri, String userNameAttributeName) {
        super(location, name, ARTEFACT_TYPE, description, null);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizationGrantType = authorizationGrantType;
        this.scope = scope;
        this.tokenUri = tokenUri;
        this.authorizationUri = authorizationUri;
        this.userInfoUri = userInfoUri;
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri;
        this.userNameAttributeName = userNameAttributeName;
    }

    /**
     * Instantiates a new oauth client registration.
     */
    public ClientRegistration() {
        super();
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the client id.
     *
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the client id.
     *
     * @param clientId the new client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets the client secret.
     *
     * @return the client secret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Sets the client secret.
     *
     * @param clientSecret the new client secret
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Gets the redirect uri.
     *
     * @return the redirect uri
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Sets the redirect uri.
     *
     * @param redirectUri the new redirect uri
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * Gets the authorization grant type.
     *
     * @return the authorization grant type
     */
    public String getAuthorizationGrantType() {
        return authorizationGrantType;
    }

    /**
     * Sets the authorization grant type.
     *
     * @param authorizationGrantType the new authorization grant type
     */
    public void setAuthorizationGrantType(String authorizationGrantType) {
        this.authorizationGrantType = authorizationGrantType;
    }

    /**
     * Gets the scope.
     *
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope.
     *
     * @param scope the new scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Gets the token uri.
     *
     * @return the token uri
     */
    public String getTokenUri() {
        return tokenUri;
    }

    /**
     * Sets the token uri.
     *
     * @param tokenUri the new token uri
     */
    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    /**
     * Gets the authorization uri.
     *
     * @return the authorization uri
     */
    public String getAuthorizationUri() {
        return authorizationUri;
    }

    /**
     * Sets the authorization uri.
     *
     * @param authorizationUri the new authorization uri
     */
    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }


    /**
     * Gets the user info uri.
     *
     * @return the user info uri
     */
    public String getUserInfoUri() {
        return userInfoUri;
    }

    /**
     * Sets the user info uri.
     *
     * @param userInfoUri the new user info uri
     */
    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    /**
     * Gets the issuer uri.
     *
     * @return the issuer uri
     */
    public String getIssuerUri() {
        return issuerUri;
    }

    /**
     * Sets the issuer uri.
     *
     * @param issuerUri the new issuer uri
     */
    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    /**
     * Gets the jwk set uri.
     *
     * @return the jwk set uri
     */
    public String getJwkSetUri() {
        return jwkSetUri;
    }

    /**
     * Sets the jwk set uri.
     *
     * @param jwkSetUri the new jwk set uri
     */
    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    /**
     * Gets the user name attribute name.
     *
     * @return the user name attribute name
     */
    public String getUserNameAttributeName() {
        return userNameAttributeName;
    }

    /**
     * Sets the user name attribute name.
     *
     * @param userNameAttributeName the user name attribute name
     */
    public void setUserNameAttributeName(String userNameAttributeName) {
        this.userNameAttributeName = userNameAttributeName;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "OAuth Client Registration [id=" + id + ", clientId=" + clientId + ", redirectUri=" + redirectUri
                + ", authorizationGrantType=" + authorizationGrantType + ", scope=" + scope + ", tokenUri=" + tokenUri
                + ", authorizationUri=" + authorizationUri + ", userInfoUri=" + userInfoUri + ", issuerUri=" + issuerUri + ", jwkSetUri="
                + jwkSetUri + ", userNameAttributeName=" + userNameAttributeName + "]";
    }

}
