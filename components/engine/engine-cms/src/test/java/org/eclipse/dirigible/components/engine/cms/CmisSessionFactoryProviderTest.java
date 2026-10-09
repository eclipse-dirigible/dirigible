/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * The CMS provider named by DIRIGIBLE_CMS_PROVIDER, including an add-on provider that is not in the
 * default bundle (#7780).
 */
class CmisSessionFactoryProviderTest {

    private final CmsProviderFactory internal = () -> null;

    private StaticListableBeanFactory defaultBundle() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("cms-provider-internal", internal);
        beanFactory.addBean("cms-provider-s3", (CmsProviderFactory) () -> null);
        return beanFactory;
    }

    @Test
    void anAvailableProviderIsResolved() {
        assertSame(internal, CmisSessionFactory.resolveProviderFactory(defaultBundle(), "cms-provider-internal"));
    }

    @Test
    void sharePointWithoutTheAddOnNamesTheArtifactToAdd() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CmisSessionFactory.resolveProviderFactory(defaultBundle(), "cms-provider-ms-sharepoint"));

        assertTrue(e.getMessage()
                    .contains("dirigible-components-engine-cms-sharepoint"),
                e.getMessage());
    }

    @Test
    void anUnknownProviderListsTheAvailableOnes() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CmisSessionFactory.resolveProviderFactory(defaultBundle(), "cms-provider-ftp"));

        assertTrue(e.getMessage()
                    .contains("cms-provider-internal, cms-provider-s3"),
                e.getMessage());
    }
}
