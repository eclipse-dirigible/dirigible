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

import java.util.List;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tells a page which tenant its request is running in, and whether the user could be in another.
 *
 * <p>
 * The shells render this in their header: the current tenant is otherwise invisible to a browser,
 * while everything on the page - the data, the menu, the roles - silently depends on it. Unlike
 * {@link TenantSelectionEndpoint}, which exists only where tenants are selected, this answers on
 * every resolution strategy: under {@code SUBDOMAIN} the tenant is the host's and cannot be changed
 * from here, on a single-tenant instance it is the default tenant and the page hides the widget.
 *
 * <p>
 * The tenants offered for a switch are the ones the user's groups grant, as the picker lists them,
 * and only for an interactive session under {@code TOKEN_GROUPS}: a bearer request names its tenant
 * per request and has no session to keep a choice in, and under {@code SUBDOMAIN} the identity
 * provider's groups say nothing about tenants.
 *
 * <p>
 * Deliberately without a role gate, for the same reason as the selection endpoint: a user of a
 * single tenant has no roles at all, staff have only global ones, and every one of them is entitled
 * to know where they are. It is read-only and the URL is gated as authenticated. Its path sits
 * under the selection endpoint's, which the selection filter leaves unfiltered - so a user who has
 * not chosen yet is told they are in the default tenant instead of being answered with the filter's
 * 409.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "tenant-selection/current")
public class CurrentTenantEndpoint {

    private final TenantSelectionManager tenantSelectionManager;

    private final TenantContext tenantContext;

    /**
     * Instantiates a new current tenant endpoint.
     *
     * @param tenantSelectionManager the tenant selection manager
     * @param tenantContext the tenant scope of the current execution
     */
    public CurrentTenantEndpoint(TenantSelectionManager tenantSelectionManager, TenantContext tenantContext) {
        this.tenantSelectionManager = tenantSelectionManager;
        this.tenantContext = tenantContext;
    }

    /**
     * The tenant of this request, and what the user could switch to.
     *
     * @return the current tenant state
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CurrentTenantState> current() {
        Tenant tenant = tenantContext.getCurrentTenant();
        TenantResolutionStrategy strategy = TenantResolutionStrategy.fromConfiguration();
        boolean multitenant = DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getBooleanValue();
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        List<TenantOption> tenants = offeredTenants(strategy, authentication);
        boolean switchable = tenants.stream()
                                    .anyMatch(option -> option.state() == TenantOption.State.READY && !option.id()
                                                                                                             .equals(tenant.getId()));
        return ResponseEntity.ok(new CurrentTenantState(new TenantInfo(tenant.getId(), tenant.getName(), tenant.isDefault()), multitenant,
                strategy.name(), switchable, tenants));
    }

    /**
     * The tenants the user may enter, where entering one is something a user does.
     *
     * <p>
     * Asked of the manager only where the answer can be non-empty. On a {@code SUBDOMAIN} instance
     * nothing in the groups names a tenant, and the manager's warning about a tenant this instance
     * never registered must not be raised by a page load.
     *
     * @param strategy how this instance resolves tenants
     * @param authentication the authentication of the request; may be {@code null}
     * @return the tenants offered for a switch, never {@code null}
     */
    private List<TenantOption> offeredTenants(TenantResolutionStrategy strategy, Authentication authentication) {
        if (strategy != TenantResolutionStrategy.TOKEN_GROUPS || !(authentication instanceof OAuth2AuthenticationToken)) {
            return List.of();
        }
        return tenantSelectionManager.availableTenants(authentication);
    }

    /**
     * Where the request runs, and whether the user could be elsewhere.
     *
     * @param tenant the tenant of this request
     * @param multitenant whether this instance serves more than the default tenant at all; a page hides
     *        the tenant widget where it does not
     * @param resolutionStrategy how this instance resolves tenants - {@code SUBDOMAIN} or
     *        {@code TOKEN_GROUPS}
     * @param switchable whether at least one of the offered tenants other than the current one can be
     *        entered right now
     * @param tenants the tenants the user's groups grant, each with its local state; empty where a
     *        tenant is not something a user selects
     */
    public record CurrentTenantState(TenantInfo tenant, boolean multitenant, String resolutionStrategy, boolean switchable,
            List<TenantOption> tenants) {
    }

    /**
     * A tenant, as a page names it.
     *
     * @param id the tenant id
     * @param name the tenant name; the id where the tenant has no other name
     * @param defaultTenant whether this is the default tenant
     */
    public record TenantInfo(String id, String name, boolean defaultTenant) {
    }
}
