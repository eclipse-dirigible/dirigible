/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth.endpoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.security.oauth.domain.ClientRegistration;
import org.eclipse.dirigible.components.security.oauth.service.DynamicClientRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;

/**
 * The Class OAuthClientRegistrationEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "client-registrations")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER", "OPERATOR"})
public class ClientRegistrationEndpoint {


    /** The dynamic oauth client registration repository. */
    private final DynamicClientRegistrationRepository repository;

    /**
     * Instantiates a new oauth client registration endpoint.
     *
     * @param repository the repository
     */
    @Autowired
    public ClientRegistrationEndpoint(DynamicClientRegistrationRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<ClientRegistration>> getAll() {
        return ResponseEntity.ok(repository.getAll());
    }

    /**
     * Gets the oauth client registration.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClientRegistration> get(@PathVariable("id") String id) {
        return ResponseEntity.ok(repository.findById(id)
                                           .get());
    }

    /**
     * Find by client id.
     *
     * @param clientId the client id
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<ClientRegistration> findByName(@RequestParam("clientId") String clientId) {
        return ResponseEntity.ok(repository.findByClientId(clientId)
                                           .get());
    }

    /**
     * Creates the registration.
     *
     * @param registrationParameter the registration parameter
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @PostMapping
    public ResponseEntity<URI> createRegistration(@Valid @RequestBody ClientRegistrationParameter registrationParameter)
            throws URISyntaxException {
        ClientRegistration registration = new ClientRegistration(registrationParameter.getName(), registrationParameter.getName(), "",
                registrationParameter.getClientId(), registrationParameter.getClientSecret(), registrationParameter.getRedirectUri(),
                registrationParameter.getAuthorizationGrantType(), registrationParameter.getScope(), registrationParameter.getTokenUri(),
                registrationParameter.getAuthorizationUri(), registrationParameter.getUserInfoUri(), registrationParameter.getIssuerUri(),
                registrationParameter.getJwkSetUri(), registrationParameter.getUserNameAttributeName());
        registration.setId(UUID.randomUUID()
                               .toString());
        registration.updateKey();
        registration = repository.save(registration);
        return ResponseEntity.created(new URI(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "client-registrations/" + registration.getId()))
                             .build();
    }

    /**
     * Updates the registration.
     *
     * @param id the id of the registration
     * @param registrationParameter the registration parameter
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @PutMapping("{id}")
    public ResponseEntity<URI> updateRegistration(@PathVariable("id") String id,
            @Valid @RequestBody ClientRegistrationParameter registrationParameter) throws URISyntaxException {
        ClientRegistration registration = repository.findById(id)
                                                    .get();
        registration.setName(registrationParameter.getName());
        registration.setClientId(registrationParameter.getClientId());
        registration.setClientSecret(registrationParameter.getClientSecret());
        registration.setRedirectUri(registrationParameter.getRedirectUri());
        registration.setAuthorizationGrantType(registrationParameter.getAuthorizationGrantType());
        registration.setScope(registrationParameter.getScope());
        registration.setTokenUri(registrationParameter.getTokenUri());
        registration.setAuthorizationUri(registrationParameter.getAuthorizationUri());
        registration.setUserInfoUri(registrationParameter.getUserInfoUri());
        registration.setIssuerUri(registrationParameter.getIssuerUri());
        registration.setJwkSetUri(registrationParameter.getJwkSetUri());
        registration.setUserNameAttributeName(registrationParameter.getUserNameAttributeName());

        registration.updateKey();
        registration = repository.save(registration);
        return ResponseEntity.created(new URI(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "client-registrations/" + registration.getId()))
                             .build();
    }

    /**
     * Deletes the oauth client registration.
     *
     * @param id the id of the oauth client registration
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteDataSource(@PathVariable("id") String id) throws URISyntaxException {
        ClientRegistration registration = repository.findById(id)
                                                    .get();
        repository.delete(registration);
        return ResponseEntity.noContent()
                             .build();
    }


}
