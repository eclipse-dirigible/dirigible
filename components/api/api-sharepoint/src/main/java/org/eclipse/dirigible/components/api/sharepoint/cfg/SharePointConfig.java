/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.sharepoint.cfg;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.sharepoint.SharePointService;
import org.eclipse.dirigible.components.api.sharepoint.SharepointFacade;
import org.eclipse.dirigible.components.engine.cms.TenantPathResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("'${DIRIGIBLE_CMS_PROVIDER:cms-provider-internal}'.equalsIgnoreCase('cms-provider-ms-sharepoint')")
class SharePointConfig {

    @Bean
    @ConditionalOnProperty(name = "DIRIGIBLE_MS_SHAREPOINT_TENANT_ID")
    @ConditionalOnProperty(name = "DIRIGIBLE_MS_SHAREPOINT_CLIENT_ID")
    @ConditionalOnProperty(name = "DIRIGIBLE_MS_SHAREPOINT_CLIENT_SECRET")
    ClientSecretCredential provideClientSecretCredential() {
        String tenantId = DirigibleConfig.MS_SHAREPOINT_TENANT_ID.getStringValue();
        String clientId = DirigibleConfig.MS_SHAREPOINT_CLIENT_ID.getStringValue();
        String clientSecret = DirigibleConfig.MS_SHAREPOINT_CLIENT_SECRET.getStringValue();

        return new ClientSecretCredentialBuilder().tenantId(tenantId)
                                                  .clientId(clientId)
                                                  .clientSecret(clientSecret)
                                                  .build();
    }

    @Bean
    @ConditionalOnProperty(name = "DIRIGIBLE_MS_SHAREPOINT_TOKEN")
    TokenCredential provideTokenCredential() {
        String token = DirigibleConfig.MS_SHAREPOINT_TOKEN.getStringValue();
        return new StaticTokenCredential(token);
    }

    @Bean
    GraphServiceClient provideGraphServiceClient(TokenCredential credential) {
        return new GraphServiceClient(credential, "https://graph.microsoft.com/.default");
    }

    @Bean
    SharePointService provideSharePointService(GraphServiceClient graphClient) {
        return new SharePointService(graphClient);
    }

    @Bean
    SharepointFacade provideSharepointFacade(TenantPathResolver tenantPathResolver, SharePointService sharePointService) {
        return new SharepointFacade(tenantPathResolver, sharePointService);
    }

}
