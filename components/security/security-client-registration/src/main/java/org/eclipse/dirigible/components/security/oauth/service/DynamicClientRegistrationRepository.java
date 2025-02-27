package org.eclipse.dirigible.components.security.oauth.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.dirigible.components.security.oauth.client.CognitoDefaultTenantProperties;
import org.eclipse.dirigible.components.security.oauth.client.KeycloakDefaultTenantProperties;
import org.eclipse.dirigible.components.security.oauth.domain.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

@Component
public class DynamicClientRegistrationRepository
        implements ClientRegistrationRepository, Iterable<org.springframework.security.oauth2.client.registration.ClientRegistration> {

    private static final Map<String, org.springframework.security.oauth2.client.registration.ClientRegistration> REGISTRATIONS =
            new HashMap<>();

    private final ClientRegistrationService service;
    private final Optional<ClientRegistration> cognitoRegistration;
    private final Optional<ClientRegistration> keycloakRegistration;

    public DynamicClientRegistrationRepository(ClientRegistrationService service, CognitoDefaultTenantProperties cognitoClient,
            KeycloakDefaultTenantProperties keycloakClient) {
        this.service = service;
        if (cognitoClient.hasAllProperties()) {
            ClientRegistration cognitoClientRegistration = new ClientRegistration(cognitoClient.getClientName(), "cognito", "",
                    cognitoClient.getClientId(), cognitoClient.getClientSecret(), cognitoClient.getRedirectUri(),
                    cognitoClient.getAuthorizationGrantType(), cognitoClient.getScope(), cognitoClient.getTokenUri(),
                    cognitoClient.getAuthorizationUri(), cognitoClient.getUserInfoUri(), cognitoClient.getIssuerUri(),
                    cognitoClient.getJwkSetUri(), cognitoClient.getUserNameAttribute());
            cognitoClientRegistration.setId("default-tenant");
            cognitoClientRegistration.updateKey();
            cognitoRegistration = Optional.of(cognitoClientRegistration);
        } else {
            this.cognitoRegistration = Optional.empty();
        }
        if (keycloakClient.hasAllProperties()) {
            ClientRegistration keycloakClientRegistration = new ClientRegistration(keycloakClient.getClientName(), "keycloak", "",
                    keycloakClient.getClientId(), keycloakClient.getClientSecret(), keycloakClient.getRedirectUri(),
                    keycloakClient.getAuthorizationGrantType(), keycloakClient.getScope(), keycloakClient.getTokenUri(),
                    keycloakClient.getAuthorizationUri(), keycloakClient.getUserInfoUri(), keycloakClient.getIssuerUri(),
                    keycloakClient.getJwkSetUri(), keycloakClient.getUserNameAttribute());
            keycloakClientRegistration.setId("default-tenant");
            keycloakClientRegistration.updateKey();
            keycloakRegistration = Optional.of(keycloakClientRegistration);
        } else {
            this.keycloakRegistration = Optional.empty();
        }
    }

    @Override
    public Iterator<org.springframework.security.oauth2.client.registration.ClientRegistration> iterator() {
        if (cognitoRegistration.isPresent()) {
            this.save(cognitoRegistration.get());
        }
        if (keycloakRegistration.isPresent()) {
            this.save(keycloakRegistration.get());
        }
        service.getAll()
               .stream()
               .map((ClientRegistration registration) -> toClientRegistration(registration))
               .forEach(e -> REGISTRATIONS.put(e.getRegistrationId(), e));
        return REGISTRATIONS.values()
                            .iterator();
    }

    @Override
    public org.springframework.security.oauth2.client.registration.ClientRegistration findByRegistrationId(String registrationId) {
        return REGISTRATIONS.get(registrationId);
    }

    public Optional<ClientRegistration> findById(String id) {
        return service.findById(id);
    }

    public Optional<ClientRegistration> findByClientId(String clientId) {
        return service.findByClientId(clientId);
    }

    public List<ClientRegistration> getAll() {
        return service.getAll();
    }

    public ClientRegistration save(ClientRegistration registration) {
        ClientRegistration newRegistration = service.save(registration);
        org.springframework.security.oauth2.client.registration.ClientRegistration clientRegistration = toClientRegistration(registration);
        REGISTRATIONS.put(clientRegistration.getRegistrationId(), clientRegistration);
        return newRegistration;
    }

    public void delete(ClientRegistration registration) {
        service.delete(registration);
        org.springframework.security.oauth2.client.registration.ClientRegistration clientRegistration = toClientRegistration(registration);
        REGISTRATIONS.remove(clientRegistration.getRegistrationId());
    }

    private org.springframework.security.oauth2.client.registration.ClientRegistration toClientRegistration(
            ClientRegistration registration) {
        AuthorizationGrantType authorizationGrantType = AuthorizationGrantType.AUTHORIZATION_CODE;
        if (registration.getAuthorizationGrantType()
                        .equals("client_credentials")) {
            authorizationGrantType = AuthorizationGrantType.CLIENT_CREDENTIALS;
        } else if (registration.getAuthorizationGrantType()
                               .equals("refresh_token")) {
            authorizationGrantType = AuthorizationGrantType.REFRESH_TOKEN;
        }

        return org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId(registration.getName())
                                                                                         .clientId(registration.getClientId())
                                                                                         .clientSecret(registration.getClientSecret())
                                                                                         .tokenUri(registration.getTokenUri())
                                                                                         .authorizationUri(
                                                                                                 registration.getAuthorizationUri())
                                                                                         .userInfoUri(registration.getUserInfoUri())
                                                                                         .issuerUri(registration.getIssuerUri())
                                                                                         .jwkSetUri(registration.getJwkSetUri())
                                                                                         .redirectUri(registration.getRedirectUri())
                                                                                         .userNameAttributeName(
                                                                                                 registration.getUserNameAttributeName())
                                                                                         .scope(registration.getScope()
                                                                                                            .split(","))
                                                                                         .authorizationGrantType(authorizationGrantType)
                                                                                         .build();
    }

}
