package org.eclipse.dirigible.components.security.oauth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KeycloakDefaultTenantProperties {

    @Value("${spring.security.oauth2.client.registration.keycloak.client-name:unknown}")
    private String clientName;

    @Value("${spring.security.oauth2.client.registration.keycloak.authorization-grant-type:unknown}")
    private String authorizationGrantType;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id:unknown}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret:unknown}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.keycloak.scope:unknown}")
    private String scope;

    @Value("${spring.security.oauth2.client.registration.keycloak.redirect-uri:unknown}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.keycloak.authorization-uri:unknown}")
    private String authorizationUri;

    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri:unknown}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.keycloak.user-info-uri:unknown}")
    private String userInfoUri;

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri:unknown}")
    private String issuerUri;

    @Value("${spring.security.oauth2.client.provider.keycloak.user-name-attribute:unknown}")
    private String userNameAttribute;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:unknown}")
    private String jwkSetUri;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getAuthorizationGrantType() {
        return authorizationGrantType;
    }

    public void setAuthorizationGrantType(String authorizationGrantType) {
        this.authorizationGrantType = authorizationGrantType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public boolean hasAllProperties() {
        return !clientName.equals("unknown") && !authorizationGrantType.equals("unknown") && !clientId.equals("unknown")
        // && !clientSecret.equals("unknown")
                && !scope.equals("unknown") && !redirectUri.equals("unknown") && !authorizationUri.equals("unknown")
                && !tokenUri.equals("unknown") && !userInfoUri.equals("unknown") && !issuerUri.equals("unknown")
                && !userNameAttribute.equals("unknown") && !jwkSetUri.equals("unknown");
    }
}
