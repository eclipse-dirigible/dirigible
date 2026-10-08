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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.engine.document.PrintTemplateException.Reason;
import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateSeed;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateSeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The print template catalogue (#7755) over an in-memory CMS: seeding versions, migrating the
 * legacy single template, resolving the active one and the tenant-template operations.
 */
class PrintTemplateCatalogTest {

    private static final String ENTITY = "SalesInvoice";
    private static final String LANG = "en";
    private static final String FOLDER = "/Templates/SalesInvoice/Print/en";

    private static final String V1 = template("one");
    private static final String V2 = template("two");

    private final InMemoryCmsStore cms = new InMemoryCmsStore();
    private final Map<String, String> configuration = new HashMap<>();
    private final List<PrintTemplateSeed> shipped = new ArrayList<>();
    private PrintTemplateCatalog catalog;

    @BeforeEach
    void setUp() throws Exception {
        PrintTemplateSelection selection = mock(PrintTemplateSelection.class);
        when(selection.get(anyString(), anyString())).thenAnswer(
                invocation -> Optional.ofNullable(configuration.get(invocation.getArgument(0) + "/" + invocation.getArgument(1))));
        doAnswer(invocation -> configuration.put(invocation.getArgument(0) + "/" + invocation.getArgument(1),
                invocation.getArgument(2))).when(selection)
                                           .select(anyString(), anyString(), anyString());
        PrintTemplateSeedService seedService = mock(PrintTemplateSeedService.class);
        when(seedService.findShipped(ENTITY, LANG)).thenReturn(shipped);
        catalog = new PrintTemplateCatalog(cms, selection, seedService);
    }

    @Test
    void aShippedTemplateIsAddedAsAVersion() throws Exception {
        catalog.seed(ENTITY, LANG, "standard", "1.28.0", bytes(V1));

        assertEquals(Set.of("standard@1.28.0.print"), cms.names(FOLDER));
    }

    @Test
    void aNewReleaseIsAddedBesideThePreviousVersionWhichStays() throws Exception {
        catalog.seed(ENTITY, LANG, "standard", "1.28.0", bytes(V1));
        catalog.seed(ENTITY, LANG, "standard", "1.30.0", bytes(V2));

        assertEquals(Set.of("standard@1.28.0.print", "standard@1.30.0.print"), cms.names(FOLDER));
        assertEquals(V1, catalog.read(ENTITY, LANG, "standard@1.28.0"));
    }

    @Test
    void aReleaseThatDidNotChangeTheLayoutAddsNothing() throws Exception {
        catalog.seed(ENTITY, LANG, "standard", "1.28.0", bytes(V1));
        catalog.seed(ENTITY, LANG, "standard", "1.30.0", bytes(V1));

        assertEquals(Set.of("standard@1.28.0.print"), cms.names(FOLDER));
    }

    @Test
    void aDriftedShippedVersionIsConvergedBackToItsShippedBytes() throws Exception {
        catalog.seed(ENTITY, LANG, "standard", "1.28.0", bytes(V1));
        cms.write(FOLDER + "/standard@1.28.0.print", bytes(V2));

        catalog.seed(ENTITY, LANG, "standard", "1.28.0", bytes(V1));

        assertEquals(V1, catalog.read(ENTITY, LANG, "standard@1.28.0"));
    }

    @Test
    void anUntouchedLegacyTemplateBecomesTheShippedVersion() throws Exception {
        cms.write(FOLDER + "/standard.print", bytes(V1));

        catalog.seed(ENTITY, LANG, "standard", "1.28.0", bytes(V1));

        assertEquals(Set.of("standard@1.28.0.print"), cms.names(FOLDER));
        assertTrue(configuration.isEmpty(), "an unchanged layout needs no selection");
    }

    @Test
    void aCustomisedLegacyTemplateIsKeptAndSelectedSoTheOutputDoesNotChange() throws Exception {
        cms.write(FOLDER + "/standard.print", bytes(V2));

        catalog.seed(ENTITY, LANG, "standard", "1.28.0", bytes(V1));

        assertEquals(Set.of("standard-custom.print", "standard@1.28.0.print"), cms.names(FOLDER));
        assertEquals("standard-custom", configuration.get(ENTITY + "/" + LANG));
        assertEquals(V2, catalog.resolve(ENTITY, LANG, null)
                                .source());
    }

    @Test
    void aTenantTemplateNamedLikeTheShippedOneIsNotMigratedOnceVersionsExist() throws Exception {
        catalog.seed(ENTITY, LANG, "standard", "1.28.0", bytes(V1));
        cms.write(FOLDER + "/standard.print", bytes(V2));

        catalog.seed(ENTITY, LANG, "standard", "1.30.0", bytes(V2));

        assertEquals(Set.of("standard.print", "standard@1.28.0.print", "standard@1.30.0.print"), cms.names(FOLDER));
        assertTrue(configuration.isEmpty());
    }

    @Test
    void withoutASelectionTheNewestReleaseVersionPrints() throws Exception {
        cms.write(FOLDER + "/standard@1.9.0.print", bytes(V1));
        cms.write(FOLDER + "/standard@1.10.0.print", bytes(V2));
        cms.write(FOLDER + "/acme.print", bytes(V1));

        PrintTemplateCatalog.Resolved resolved = catalog.resolve(ENTITY, LANG, null);

        assertEquals("standard@1.10.0", resolved.name()
                                                .reference());
    }

    @Test
    void aReleaseVersionRanksAboveAContentHashAndThePreReleaseBelowItsRelease() throws Exception {
        cms.write(FOLDER + "/standard@12345678.print", bytes(V1));
        cms.write(FOLDER + "/standard@2.0.0-rc.1.print", bytes(V1));
        cms.write(FOLDER + "/standard@1.0.0.print", bytes(V1));

        assertEquals("standard@2.0.0-rc.1", catalog.resolve(ENTITY, LANG, null)
                                                   .name()
                                                   .reference());
        cms.write(FOLDER + "/standard@2.0.0.print", bytes(V1));
        assertEquals("standard@2.0.0", catalog.resolve(ENTITY, LANG, null)
                                              .name()
                                              .reference());
    }

    @Test
    void amongContentHashesTheOneTheRegistryShipsNowIsTheNewest() throws Exception {
        cms.write(FOLDER + "/standard@aaaaaaaa.print", bytes(V1));
        cms.write(FOLDER + "/standard@ffffffff.print", bytes(V2));
        PrintTemplateSeed current = new PrintTemplateSeed();
        current.setTemplateName("standard");
        current.setVersion("aaaaaaaa");
        shipped.add(current);

        assertEquals("standard@aaaaaaaa", catalog.resolve(ENTITY, LANG, null)
                                                 .name()
                                                 .reference());
    }

    @Test
    void theSelectionWinsOverTheNewestAndTheRequestOverTheSelection() throws Exception {
        cms.write(FOLDER + "/standard@1.28.0.print", bytes(V1));
        cms.write(FOLDER + "/standard@1.30.0.print", bytes(V2));
        configuration.put(ENTITY + "/" + LANG, "standard@1.28.0");

        assertEquals("standard@1.28.0", catalog.resolve(ENTITY, LANG, null)
                                               .name()
                                               .reference());
        assertEquals("standard@1.30.0", catalog.resolve(ENTITY, LANG, "standard@1.30.0")
                                               .name()
                                               .reference());
    }

    @Test
    void aSelectionNamingAMissingTemplateFallsBackToTheNewest() throws Exception {
        cms.write(FOLDER + "/standard@1.28.0.print", bytes(V1));
        configuration.put(ENTITY + "/" + LANG, "gone");

        assertEquals("standard@1.28.0", catalog.resolve(ENTITY, LANG, null)
                                               .name()
                                               .reference());
    }

    @Test
    void aFolderWithoutShippedVersionsPrintsItsTenantTemplate() throws Exception {
        cms.write("/Templates/SalesInvoice/Print/bg/standard.print", bytes(V2));

        assertEquals(V2, catalog.resolve(ENTITY, "bg", null)
                                .source());
    }

    @Test
    void anUnknownRequestedTemplateOrAnEmptyFolderIsNotFound() {
        assertEquals(Reason.NOT_FOUND, assertThrows(PrintTemplateException.class, () -> catalog.resolve(ENTITY, LANG, null)).getReason());
        assertEquals(Reason.NOT_FOUND, assertThrows(PrintTemplateException.class, () -> catalog.resolve(ENTITY, LANG, "nope")).getReason());
    }

    @Test
    void aShippedVersionCannotBeWrittenOrDeleted() throws Exception {
        cms.write(FOLDER + "/standard@1.28.0.print", bytes(V1));
        cms.write(FOLDER + "/acme.print", bytes(V1));

        assertEquals(Reason.CONFLICT,
                assertThrows(PrintTemplateException.class, () -> catalog.write(ENTITY, LANG, "standard@1.28.0", V2)).getReason());
        assertEquals(Reason.CONFLICT,
                assertThrows(PrintTemplateException.class, () -> catalog.delete(ENTITY, LANG, "standard@1.28.0")).getReason());
        assertEquals(V1, catalog.read(ENTITY, LANG, "standard@1.28.0"));
    }

    @Test
    void aTenantTemplateIsWrittenOnlyWhenItParses() throws Exception {
        catalog.write(ENTITY, LANG, "acme-blue", V2);

        assertEquals(V2, catalog.read(ENTITY, LANG, "acme-blue"));
        assertEquals(Reason.INVALID,
                assertThrows(PrintTemplateException.class, () -> catalog.write(ENTITY, LANG, "acme-blue", "<document><page>")).getReason());
        assertEquals(Reason.INVALID,
                assertThrows(PrintTemplateException.class, () -> catalog.write(ENTITY, LANG, "../escape", V2)).getReason());
    }

    @Test
    void theActiveTemplateCannotBeDeletedButAnotherTenantTemplateCan() throws Exception {
        cms.write(FOLDER + "/standard@1.28.0.print", bytes(V1));
        cms.write(FOLDER + "/acme.print", bytes(V1));
        cms.write(FOLDER + "/draft.print", bytes(V1));
        configuration.put(ENTITY + "/" + LANG, "acme");

        assertEquals(Reason.CONFLICT, assertThrows(PrintTemplateException.class, () -> catalog.delete(ENTITY, LANG, "acme")).getReason());
        catalog.delete(ENTITY, LANG, "draft");

        assertEquals(Set.of("acme.print", "standard@1.28.0.print"), cms.names(FOLDER));
    }

    @Test
    void aDuplicateRecordsTheShippedVersionItDerivesFromEvenThroughAnotherDuplicate() throws Exception {
        cms.write(FOLDER + "/standard@1.28.0.print", bytes(V1));

        catalog.duplicate(ENTITY, LANG, "standard@1.28.0", "acme");
        catalog.duplicate(ENTITY, LANG, "acme", "acme-2");

        String copy = catalog.read(ENTITY, LANG, "acme-2");
        assertTrue(copy.startsWith("<!-- derived-from: standard@1.28.0 -->\n"), copy);
        assertEquals(1, copy.split("derived-from", -1).length - 1, "the header is not stacked");
        assertEquals(Reason.CONFLICT,
                assertThrows(PrintTemplateException.class, () -> catalog.duplicate(ENTITY, LANG, "acme", "acme-2")).getReason());
        assertEquals(Reason.INVALID,
                assertThrows(PrintTemplateException.class, () -> catalog.duplicate(ENTITY, LANG, "acme", "x@1.0.0")).getReason());
    }

    @Test
    void theCatalogueListsVersionsNewestFirstThenTenantTemplatesAndMarksTheActiveAndTheNewer() throws Exception {
        cms.write(FOLDER + "/standard@1.28.0.print", bytes(V1));
        catalog.duplicate(ENTITY, LANG, "standard@1.28.0", "acme");
        cms.write(FOLDER + "/standard@1.30.0.print", bytes(V2));
        configuration.put(ENTITY + "/" + LANG, "acme");

        List<PrintTemplateCatalog.Entry> entries = catalog.list(ENTITY, LANG);

        assertEquals(List.of(new PrintTemplateCatalog.Entry("standard@1.30.0", "shipped", "1.30.0", null, false, true),
                new PrintTemplateCatalog.Entry("standard@1.28.0", "shipped", "1.28.0", null, false, false),
                new PrintTemplateCatalog.Entry("acme", "tenant", null, "standard@1.28.0", true, false)), entries);
    }

    @Test
    void documentTypesAreTheEntityFoldersWithPrintLanguages() throws Exception {
        cms.write(FOLDER + "/standard@1.28.0.print", bytes(V1));
        cms.write("/Templates/SalesInvoice/Print/bg/standard.print", bytes(V1));
        cms.write("/Templates/Print/logo.png", bytes("png"));

        List<PrintTemplateCatalog.DocumentType> types = catalog.documentTypes();

        assertEquals(1, types.size());
        assertEquals(ENTITY, types.get(0)
                                  .entity());
        assertEquals(Set.of("en", "bg"), Set.copyOf(types.get(0)
                                                         .languages()));
        assertFalse(catalog.languages(ENTITY)
                           .isEmpty());
    }

    private static String template(String marker) {
        return "<document id=\"" + marker
                + "\"><page><section><field label=\"Number\">{{document.number}}</field></section></page></document>\n";
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
