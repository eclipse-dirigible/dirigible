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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import jakarta.jms.Connection;

/**
 * Two applications on one broker no longer claim the same shared client id (#8655).
 *
 * <p>
 * In the shared listener mode a scope's client-Java listeners ride one connection whose client id
 * used to derive from the tenant alone - {@code dirigible-java-tenant-<tenant>} - and every
 * application has the same tenants' ids to claim (a {@code default-tenant} above all). A JMS client
 * id is unique per broker, so the application that connected second was refused for as long as the
 * other ran, and none of its listeners in that tenant was ever subscribed. This test plays the
 * other application: it holds a tenant's unprefixed client id on the embedded broker before the
 * tenant is provisioned, then sets {@code DIRIGIBLE_MESSAGING_CLIENT_ID_PREFIX} and proves a
 * message published in that tenant still reaches this instance's handler - on a connection under
 * the prefixed id.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ListenerClientIdPrefixIT extends IntegrationTest {

    private static final String PROJECT = "listener-client-id-prefix-it";
    private static final String PREFIX = "client-id-prefix-it";
    private static final int ROUND_TRIP_TIMEOUT_SECONDS = 90;
    private static final long RECEIVE_TIMEOUT_MILLIS = 2000;

    @Autowired
    private ActiveMQConnectionArtifactsFactory connectionFactory;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private TenantContext tenantContext;

    private Connection otherApplication;

    @AfterEach
    void releaseTheClientIdAndThePrefix() throws Exception {
        Configuration.remove(DirigibleConfig.MESSAGING_CLIENT_ID_PREFIX.getKey());
        if (otherApplication != null) {
            otherApplication.close();
        }
    }

    @Test
    void aTenantsListenersSubscribeWhileAnotherApplicationHoldsTheUnprefixedClientId() {
        DirigibleTestTenant tenant = new DirigibleTestTenant(PROJECT);
        otherApplication = connectionFactory.createConnection(error -> {
        }, ("dirigible-java-tenant-" + tenant.getId()).replaceAll("[^A-Za-z0-9._-]", "_"));
        DirigibleConfig.MESSAGING_CLIENT_ID_PREFIX.setStringValue(PREFIX);

        deployEchoHandler();
        synchronizationProcessor.forceProcessSynchronizers();
        createTenants(tenant);
        waitForTenantProvisioning(tenant);

        String message = "ping-" + System.currentTimeMillis();
        String[] received = new String[1];
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(ROUND_TRIP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> tenantContext.execute(tenant.getId(), () -> attempt(message, received)));
        assertEquals("echo:" + message, received[0],
                "the tenant's handler must be subscribed under the prefixed client id although the unprefixed one is held");
    }

    private static boolean attempt(String message, String[] received) {
        MessagingFacade.sendToTopic(PROJECT + "-in", message);
        try {
            received[0] = MessagingFacade.receiveFromQueue(PROJECT + "-out", RECEIVE_TIMEOUT_MILLIS);
            return true;
        } catch (RuntimeException notYet) {
            return false;
        }
    }

    /** A client listener echoing its topic onto its queue. */
    private void deployEchoHandler() {
        String source = """
                package demo;
                import org.eclipse.dirigible.sdk.component.Component;
                import org.eclipse.dirigible.sdk.messaging.ListenerKind;
                import org.eclipse.dirigible.sdk.messaging.MessageHandler;
                import org.eclipse.dirigible.sdk.messaging.Producer;
                @Component
                public class PrefixEcho implements MessageHandler {
                    @Override
                    public String destination() {
                        return "%s-in";
                    }
                    @Override
                    public ListenerKind kind() {
                        return ListenerKind.TOPIC;
                    }
                    @Override
                    public void onMessage(String message) {
                        Producer.sendToQueue("%s-out", "echo:" + message);
                    }
                }
                """.formatted(PROJECT, PROJECT);
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/demo/PrefixEcho.java",
                source.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
    }
}
