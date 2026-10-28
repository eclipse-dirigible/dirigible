/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.dirigible.components.base.publisher.PublisherHandler;
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublisherServiceTest {

    private static final String USER = "admin";

    private static final String WORKSPACE = "workspace";

    private static final String WORKSPACE_LOCATION = "/users/admin/workspace";

    private static final String REGISTRY = "/registry/public";

    @TempDir
    Path root;

    private IRepository repository;

    private final PublisherHandler handler = mock(PublisherHandler.class);

    private PublisherService publisher;

    @BeforeEach
    void setUp() {
        repository = new LocalRepository(root.toString(), true);
        publisher = new PublisherService(repository, List.of(handler), new RegistryMutationTracker());
    }

    @Test
    void aRepublishUnpublishesTheFilesDeletedFromTheWorkspace() {
        write(WORKSPACE_LOCATION + "/shop/jobs/conv.job");
        write(WORKSPACE_LOCATION + "/shop/jobs/handler.js");
        publisher.publish(USER, WORKSPACE, "shop", "");

        repository.removeResource(WORKSPACE_LOCATION + "/shop/jobs/conv.job");
        repository.removeResource(WORKSPACE_LOCATION + "/shop/jobs/handler.js");
        write(WORKSPACE_LOCATION + "/shop/shop/ConvJob.java");
        publisher.publish(USER, WORKSPACE, "shop", "");

        assertThat(repository.hasResource(REGISTRY + "/shop/jobs/conv.job")).isFalse();
        assertThat(repository.hasResource(REGISTRY + "/shop/jobs/handler.js")).isFalse();
        assertThat(repository.hasResource(REGISTRY + "/shop/shop/ConvJob.java")).isTrue();
        verify(handler).afterUnpublish(REGISTRY + "/shop/jobs/conv.job");
        verify(handler).afterUnpublish(REGISTRY + "/shop/jobs/handler.js");
    }

    @Test
    void aRepublishKeepsTheRegistryFilesNoPublishCopied() {
        write(WORKSPACE_LOCATION + "/shop/service.ts");
        publisher.publish(USER, WORKSPACE, "shop", "");
        write(REGISTRY + "/shop/service.js");
        write(REGISTRY + "/shop/node_modules/lib/index.js");

        publisher.publish(USER, WORKSPACE, "shop", "");

        assertThat(repository.hasResource(REGISTRY + "/shop/service.js")).isTrue();
        assertThat(repository.hasResource(REGISTRY + "/shop/node_modules/lib/index.js")).isTrue();
        verify(handler, never()).afterUnpublish(anyString());
    }

    @Test
    void aFolderPublishUnpublishesOnlyWithinThatFolder() {
        write(WORKSPACE_LOCATION + "/shop/jobs/conv.job");
        write(WORKSPACE_LOCATION + "/shop/forms/order.form");
        publisher.publish(USER, WORKSPACE, "shop", "");

        repository.removeResource(WORKSPACE_LOCATION + "/shop/jobs/conv.job");
        repository.removeResource(WORKSPACE_LOCATION + "/shop/forms/order.form");
        write(WORKSPACE_LOCATION + "/shop/jobs/other.job");
        publisher.publish(USER, WORKSPACE, "shop", "jobs");

        assertThat(repository.hasResource(REGISTRY + "/shop/jobs/conv.job")).isFalse();
        assertThat(repository.hasResource(REGISTRY + "/shop/forms/order.form")).isTrue();

        publisher.publish(USER, WORKSPACE, "shop", "");

        assertThat(repository.hasResource(REGISTRY + "/shop/forms/order.form")).isFalse();
        assertThat(repository.hasResource(REGISTRY + "/shop/jobs/other.job")).isTrue();
    }

    @Test
    void aPublishOfTheWholeWorkspaceUnpublishesPerProject() {
        write(WORKSPACE_LOCATION + "/shop/jobs/conv.job");
        write(WORKSPACE_LOCATION + "/billing/billing.table");
        publisher.publish(USER, WORKSPACE, "*", "");

        repository.removeResource(WORKSPACE_LOCATION + "/shop/jobs/conv.job");
        write(WORKSPACE_LOCATION + "/shop/shop/ConvJob.java");
        publisher.publish(USER, WORKSPACE, "*", "");

        assertThat(repository.hasResource(REGISTRY + "/shop/jobs/conv.job")).isFalse();
        assertThat(repository.hasResource(REGISTRY + "/billing/billing.table")).isTrue();
    }

    @Test
    void anUnpublishedFileIsNotUnpublishedAgainByTheNextPublish() {
        write(WORKSPACE_LOCATION + "/shop/jobs/conv.job");
        publisher.publish(USER, WORKSPACE, "shop", "");
        publisher.unpublish("shop/jobs/conv.job");
        repository.removeResource(WORKSPACE_LOCATION + "/shop/jobs/conv.job");
        write(WORKSPACE_LOCATION + "/shop/shop/ConvJob.java");

        publisher.publish(USER, WORKSPACE, "shop", "");

        verify(handler).afterUnpublish(REGISTRY + "/shop/jobs/conv.job");
        assertThat(repository.hasResource(REGISTRY + "/shop/shop/ConvJob.java")).isTrue();
    }

    private void write(String location) {
        repository.createResource(location, location.getBytes(StandardCharsets.UTF_8));
    }

}
