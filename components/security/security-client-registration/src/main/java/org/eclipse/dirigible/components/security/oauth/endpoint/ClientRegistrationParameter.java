/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth.endpoint;

/**
 * The Class OAuthClientRegistrationParameter.
 */
public class ClientRegistrationParameter {

    /** The name. */
    private String name;

    /** The OAuth Client Id. */
    private String clientId;

    /** The OAuth Client Secret. */
    private String clientSecret;

    /** The OAuth Redirect URI. */
    private String redirectUri;

    /** The OAuth Authorization Grant Type. */
    private String authorizationGrantType;

    /** The OAuth Scope. */
    private String scope;

    /** The OAuth Token URI. */
    private String tokenUri;

    /** The OAuth Authorization URI. */
    private String authorizationUri;

    /** The OAuth User Info URI. */
    private String userInfoUri;

    /** The OAuth Issuer URI. */
    private String issuerUri;

    /** The OAuth JWK Set URI. */
    private String jwkSetUri;

    /** The OAuth User Name Attribute Name. */
    private String userNameAttributeName;

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
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

}
