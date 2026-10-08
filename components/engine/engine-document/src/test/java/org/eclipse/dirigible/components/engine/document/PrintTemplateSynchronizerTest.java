/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateSeed;
import org.eclipse.dirigible.components.engine.document.service.CmsSeedService;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateSeedService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.Test;

/**
 * Which files the print template synchronizer seeds (#7755) - and therefore which the generic CMS
 * seed leaves alone - the content-hash version, and a failure that sticks until every tenant's seed
 * has succeeded.
 */
class PrintTemplateSynchronizerTest {

    @Test
    void aTemplateUnderTheTemplatesConventionIsShipped() {
        PrintTemplateSynchronizer.ShippedLocation location =
                PrintTemplateSynchronizer.ShippedLocation.of("/sales/doc/Templates/SalesInvoice/Print/en/standard.print")
                                                         .orElseThrow();

        assertEquals("sales", location.project());
        assertEquals("SalesInvoice", location.entity());
        assertEquals("en", location.language());
        assertEquals("standard", location.name());
        assertTrue(PrintTemplateSynchronizer.isShippedTemplate("C:\\r\\sales\\doc\\Templates\\X\\Print\\bg\\standard.print"));
    }

    @Test
    void aTemplateWhoseNameIsNotAValidTemplateNameShipsUnderItsSanitisedName() {
        PrintTemplateSynchronizer.ShippedLocation location =
                PrintTemplateSynchronizer.ShippedLocation.of("/sales/doc/Templates/SalesInvoice/Print/en/Invoice template.print")
                                                         .orElseThrow();

        assertEquals("Invoice-template", location.name());
    }

    @Test
    void otherDocFilesStayPlainCmsSeeds() {
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/doc/Templates/Print/logo.png"));
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/doc/Templates/SalesInvoice/Print/en/notes.txt"));
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/doc/Templates/SalesInvoice/Print/standard.print"));
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/doc/Other/SalesInvoice/Print/en/standard.print"));
        assertFalse(PrintTemplateSynchronizer.isShippedTemplate("/sales/SalesInvoice.print"));
    }

    @Test
    void theContentVersionIsTheFirstEightHexDigitsOfTheSha256() {
        // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        assertEquals("ba7816bf", PrintTemplateName.shortHash("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void aSeedThatFailedForOneTenantStaysFailedAfterAnotherTenantSucceededUntilItSucceedsToo() throws Exception {
        PrintTemplateCatalog catalog = mock(PrintTemplateCatalog.class);
        TenantContext tenantContext = mock(TenantContext.class);
        SynchronizerCallback callback = mock(SynchronizerCallback.class);
        PrintTemplateSynchronizer synchronizer = new PrintTemplateSynchronizer(mock(PrintTemplateSeedService.class),
                mock(CmsSeedService.class), catalog, mock(PrintTemplateReleases.class), mock(IRepository.class), tenantContext);
        synchronizer.setCallback(callback);
        PrintTemplateSeed seed = new PrintTemplateSeed();
        seed.setLocation("/sales/doc/Templates/SalesInvoice/Print/en/standard.print");
        seed.setName("standard.print");
        seed.setType(PrintTemplateSeed.ARTEFACT_TYPE);
        seed.setEntityName("SalesInvoice");
        seed.setLanguage("en");
        seed.setTemplateName("standard");
        seed.setVersion("1.28.0");
        seed.setContent(new byte[0]);
        seed.updateKey();
        TopologyWrapper<PrintTemplateSeed> wrapper = new TopologyWrapper<>(seed, new HashMap<>(), synchronizer);

        // The tenants are completed one after another on the one shared artefact row.
        seed.setLifecycle(ArtefactLifecycle.NEW);
        actAs(tenantContext, "t1");
        doThrow(new IOException("CMS not reachable")).when(catalog)
                                                     .seed(anyString(), anyString(), anyString(), anyString(), any());
        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE));
        reset(catalog);
        seed.setLifecycle(ArtefactLifecycle.NEW);
        actAs(tenantContext, "t2");
        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE));
        verify(callback).registerState(eq(synchronizer), eq(wrapper), eq(ArtefactLifecycle.FAILED),
                eq("The seed has not succeeded for the tenant(s) [t1] yet"));

        // The START phase retries a FAILED artefact - once t1 succeeds, it is CREATED.
        seed.setLifecycle(ArtefactLifecycle.FAILED);
        actAs(tenantContext, "t1");
        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.START));
        verify(callback).registerState(synchronizer, wrapper, ArtefactLifecycle.CREATED);
    }

    private static void actAs(TenantContext tenantContext, String tenantId) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(tenantId);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
    }

    @Test
    void theSelectionKeyIsTheEntityAndLanguageUpperCased() {
        assertEquals("DIRIGIBLE_PRINT_TEMPLATE_SALESINVOICE_EN", PrintTemplateSelection.key("SalesInvoice", "en"));
        assertEquals("DIRIGIBLE_PRINT_TEMPLATE_SALESINVOICE_PT_BR", PrintTemplateSelection.key("SalesInvoice", "pt-BR"));
    }
}
