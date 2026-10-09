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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.engine.cms.documents.DocumentAccessEvaluator;
import org.eclipse.dirigible.components.engine.document.PrintTemplateException.Reason;
import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateVersion;
import org.eclipse.dirigible.components.engine.document.repository.PrintTemplateSeedRepository.ShippedVersion;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateSeedService;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The print template catalogue (#7755) over an in-memory CMS without a rename (as on S3), with the
 * version ledger and the artefact rows in memory: seeding versions, migrating what the
 * create-if-absent seed left, resolving the active template and the tenant-template operations.
 */
class PrintTemplateCatalogTest {

    private static final String ENTITY = "SalesInvoice";
    private static final String LANG = "en";
    private static final String FOLDER = "/Templates/SalesInvoice/Print/en";

    private static final String V0 = template("zero");
    private static final String V1 = template("one");
    private static final String V2 = template("two");
    private static final String V3 = template("three");

    private final InMemoryCmsStore cms = new InMemoryCmsStore();
    /** The tenant configuration as stored. */
    private final Map<String, String> configuration = new HashMap<>();
    /** The version the registry ships now, per template name - the artefact rows. */
    private final Map<String, String> current = new HashMap<>();
    /** The ledger of shipped versions. */
    private final List<PrintTemplateVersion> ledger = new ArrayList<>();
    /** Whether the request's copy of the configuration is empty - another node, or a failed read. */
    private boolean staleRequestConfiguration;

    private PrintTemplateSeedService seedService;
    private DocumentAccessEvaluator access;
    private PrintTemplateReleases releases;
    private PrintTemplateCatalog catalog;

    @BeforeEach
    void setUp() throws Exception {
        PrintTemplateSelection selection = mock(PrintTemplateSelection.class);
        when(selection.get(anyString(), anyString())).thenAnswer(invocation -> staleRequestConfiguration ? Optional.empty()
                : Optional.ofNullable(configuration.get(invocation.getArgument(0) + "/" + invocation.getArgument(1))));
        when(selection.getStored(anyString(), anyString())).thenAnswer(
                invocation -> Optional.ofNullable(configuration.get(invocation.getArgument(0) + "/" + invocation.getArgument(1))));
        when(selection.getEffective(anyString(), anyString())).thenAnswer(
                invocation -> Optional.ofNullable(configuration.get(invocation.getArgument(0) + "/" + invocation.getArgument(1))));
        doAnswer(invocation -> configuration.put(invocation.getArgument(0) + "/" + invocation.getArgument(1),
                invocation.getArgument(2))).when(selection)
                                           .select(anyString(), anyString(), anyString());

        seedService = mock(PrintTemplateSeedService.class);
        when(seedService.findShippedVersions(ENTITY, LANG)).thenAnswer(invocation -> current.entrySet()
                                                                                            .stream()
                                                                                            .map(entry -> shippedVersion(entry.getKey(),
                                                                                                    entry.getValue()))
                                                                                            .toList());
        PrintTemplateVersionService versionService = mock(PrintTemplateVersionService.class);
        when(versionService.findShipped(ENTITY, LANG)).thenAnswer(invocation -> List.copyOf(ledger));
        when(versionService.record(any())).thenAnswer(invocation -> {
            PrintTemplateVersion version = invocation.getArgument(0);
            Field id = PrintTemplateVersion.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(version, ledger.size() + 1L);
            ledger.add(version);
            return version;
        });
        releases = new PrintTemplateReleases(seedService, versionService);

        access = mock(DocumentAccessEvaluator.class);
        when(access.isReadable(anyString(), any())).thenReturn(true);
        when(access.isWritable(anyString(), any())).thenReturn(true);
        catalog = new PrintTemplateCatalog(cms, selection, releases, access);
    }

    // ===== seeding

    @Test
    void aShippedTemplateIsAddedAsAVersion() throws Exception {
        ship("standard", "1.28.0", V1);

        assertEquals(Set.of("standard@1.28.0.print"), cms.names(FOLDER));
    }

    @Test
    void aNewReleaseIsAddedBesideThePreviousVersionWhichStays() throws Exception {
        ship("standard", "1.28.0", V1);
        ship("standard", "1.30.0", V2);

        assertEquals(Set.of("standard@1.28.0.print", "standard@1.30.0.print"), cms.names(FOLDER));
        assertEquals(V1, read("standard@1.28.0"));
        assertEquals("standard@1.30.0", resolved());
    }

    @Test
    void aReleaseThatDidNotChangeTheLayoutAddsNothing() throws Exception {
        ship("standard", "1.28.0", V1);

        assertEquals("1.28.0", ship("standard", "1.30.0", V1), "the same bytes keep their version");
        assertEquals(Set.of("standard@1.28.0.print"), cms.names(FOLDER));
        assertEquals(1, ledger.size());
    }

    @Test
    void changedBytesUnderARecordedVersionGetANewVersionAndTheRecordedOneIsNeverOverwritten() throws Exception {
        ship("standard", "1.28.0", V1);
        configuration.put(ENTITY + "/" + LANG, "standard@1.28.0");

        String hotfix = ship("standard", "1.28.0", V2);

        assertEquals("1.28.0_v1", hotfix);
        assertEquals(V1, read("standard@1.28.0"), "a pinned tenant keeps printing what it pinned");
        assertEquals(V2, read("standard@" + hotfix));
        configuration.clear();
        assertEquals("standard@" + hotfix, resolved());
    }

    @Test
    void everyFurtherChangeUnderOneVersionGetsTheNextRevision() throws Exception {
        ship("standard", "1.28.0", V1);
        ship("standard", "1.28.0", V2);

        assertEquals("1.28.0_v2", ship("standard", "1.28.0", V3));
        assertEquals("1.28.0_v1", ship("standard", "1.28.0", V2), "bytes shipped before keep their revision");
        assertEquals(Set.of("standard@1.28.0.print", "standard@1.28.0_v1.print", "standard@1.28.0_v2.print"), cms.names(FOLDER));
    }

    @Test
    void aRollbackMakesTheEarlierVersionTheDefaultAgain() throws Exception {
        ship("standard", "1.28.0", V1);
        ship("standard", "1.30.0", V2);

        assertEquals("1.28.0", ship("standard", "1.28.0", V1));

        assertEquals(Set.of("standard@1.28.0.print", "standard@1.30.0.print"), cms.names(FOLDER));
        assertEquals("standard@1.28.0", resolved(), "the version the registry ships now prints, not the newest one present");
    }

    @Test
    void aDriftedShippedVersionIsConvergedBackToItsShippedBytes() throws Exception {
        ship("standard", "1.28.0", V1);
        cms.write(FOLDER + "/standard@1.28.0.print", bytes(V2));

        ship("standard", "1.28.0", V1);

        assertEquals(V1, read("standard@1.28.0"));
    }

    // ===== migrating what the create-if-absent seed left

    @Test
    void anUntouchedLegacyTemplateBecomesTheShippedVersion() throws Exception {
        cms.write(FOLDER + "/standard.print", bytes(V1));

        ship("standard", "1.28.0", V1);

        assertEquals(Set.of("standard@1.28.0.print"), cms.names(FOLDER));
        assertTrue(configuration.isEmpty(), "an unchanged layout needs no selection");
    }

    @Test
    void aStaleUneditedCopyOfAnEarlierReleaseBecomesThatVersionAndTheTenantGetsTheCurrentOne() throws Exception {
        // The bytes the generic CMS seed shipped last, recorded when the print template synchronizer
        // retired its row - the copy this tenant got create-if-absent and never edited.
        String previous = releases.assign(ENTITY, LANG, "standard", PrintTemplateName.shortHash(bytes(V0)), bytes(V0));
        cms.write(FOLDER + "/standard.print", bytes(V0));

        ship("standard", "1.28.0", V1);

        assertEquals(Set.of("standard@" + previous + ".print", "standard@1.28.0.print"), cms.names(FOLDER));
        assertTrue(configuration.isEmpty(), "an unedited copy is not a customisation");
        assertEquals("standard@1.28.0", resolved());
        List<PrintTemplateCatalog.Entry> entries = catalog.list(ENTITY, LANG, true);
        assertEquals(List.of("standard@1.28.0", "standard@" + previous), entries.stream()
                                                                                .map(PrintTemplateCatalog.Entry::name)
                                                                                .toList());
    }

    @Test
    void aCustomisedLegacyTemplateIsKeptSelectedAndEveryShippedVersionIsFlaggedNewer() throws Exception {
        cms.write(FOLDER + "/standard.print", bytes(V2));

        ship("standard", "1.28.0", V1);

        assertEquals(Set.of("standard-custom.print", "standard@1.28.0.print"), cms.names(FOLDER));
        assertEquals("standard-custom", configuration.get(ENTITY + "/" + LANG));
        String kept = catalog.resolve(ENTITY, LANG, null)
                             .source();
        assertEquals("<!-- derived-from: standard@legacy -->\n" + V2, kept);
        assertEquals(
                List.of(new PrintTemplateCatalog.Entry("standard@1.28.0", "shipped", "1.28.0", null, false, true, true),
                        new PrintTemplateCatalog.Entry("standard-custom", "tenant", null, "standard@legacy", true, false, false)),
                catalog.list(ENTITY, LANG, true));
    }

    @Test
    void theByteOrderMarkOfAMigratedCustomisationGoesBeforeNotAfterTheHeader() throws Exception {
        cms.write(FOLDER + "/standard.print", bytes("﻿" + V2));

        ship("standard", "1.28.0", V1);

        assertEquals("<!-- derived-from: standard@legacy -->\n" + V2, read("standard-custom"));
    }

    @Test
    void anotherTemplateThatPrintedBeforeVersionsKeepsPrinting() throws Exception {
        // The resolution before versions printed the first .print document in CMS order: acme.print.
        cms.write(FOLDER + "/acme.print", bytes(V2));
        cms.write(FOLDER + "/standard.print", bytes(V1));

        ship("standard", "1.28.0", V1);

        assertEquals("acme", configuration.get(ENTITY + "/" + LANG));
        assertEquals("acme", resolved());
        assertEquals(Set.of("acme.print", "standard@1.28.0.print"), cms.names(FOLDER));
    }

    @Test
    void anUneditedSecondTemplateThatPrintedStaysSelectedWhenItSeedsFirst() throws Exception {
        assertTheTemplateThatPrintedStaysSelected(List.of("compact", "standard"));
    }

    @Test
    void anUneditedSecondTemplateThatPrintedStaysSelectedWhenTheStandardOneSeedsFirst() throws Exception {
        assertTheTemplateThatPrintedStaysSelected(List.of("standard", "compact"));
    }

    /**
     * A module ships compact and standard; the tenant has unedited copies of both, and the resolution
     * before versions printed compact, the first in CMS order. Whichever seeds first, compact keeps
     * printing.
     */
    private void assertTheTemplateThatPrintedStaysSelected(List<String> seedOrder) throws Exception {
        Map<String, String> contents = Map.of("compact", V2, "standard", V1);
        contents.forEach((name, content) -> cms.write(FOLDER + "/" + name + ".print", bytes(content)));
        // Every shipped template is parsed - recorded and given its artefact row - before any is seeded.
        contents.forEach((name, content) -> current.put(name, releases.assign(ENTITY, LANG, name, "1.28.0", bytes(content))));
        releases.invalidate(ENTITY, LANG);

        for (String name : seedOrder) {
            catalog.seed(ENTITY, LANG, name, "1.28.0", bytes(contents.get(name)));
        }

        assertEquals("compact@1.28.0", configuration.get(ENTITY + "/" + LANG));
        assertEquals("compact@1.28.0", resolved());
        assertEquals(Set.of("compact@1.28.0.print", "standard@1.28.0.print"), cms.names(FOLDER));
    }

    @Test
    void theOnlyTenantTemplateKeepsPrintingWhenTheStandardOneWasDeleted() throws Exception {
        cms.write(FOLDER + "/acme.print", bytes(V2));

        ship("standard", "1.28.0", V1);

        assertEquals("acme", resolved());
    }

    @Test
    void aMigrationNeverOverridesASelectionTheTenantAlreadyMade() throws Exception {
        cms.write(FOLDER + "/acme.print", bytes(V2));
        cms.write(FOLDER + "/beta.print", bytes(V3));
        configuration.put(ENTITY + "/" + LANG, "beta");

        ship("standard", "1.28.0", V1);

        assertEquals("beta", resolved());
    }

    @Test
    void aTemplateWithAnInvalidNameStaysVisibleSelectableAndPrintable() throws Exception {
        cms.write(FOLDER + "/Invoice template.print", bytes(V2));
        cms.write(FOLDER + "/фактура.print", bytes(V3));

        ship("standard", "1.28.0", V1);

        // The first .print document in CMS order printed before versions: it stays selected, moved to
        // its sanitised name so the selection names a stable document.
        assertEquals("Invoice-template", configuration.get(ENTITY + "/" + LANG));
        assertTrue(cms.names(FOLDER)
                      .contains("Invoice-template.print"));
        String cyrillic = "template-" + PrintTemplateName.shortHash("фактура");
        assertEquals(List.of("standard@1.28.0", "Invoice-template", cyrillic), catalog.list(ENTITY, LANG, false)
                                                                                      .stream()
                                                                                      .map(PrintTemplateCatalog.Entry::name)
                                                                                      .toList());
        assertEquals(V3, catalog.resolve(ENTITY, LANG, cyrillic)
                                .source());
    }

    @Test
    void theLeftoverOfAnOverwriteOnS3IsTheTenantsLegacyTemplate() throws Exception {
        // CmsService.updateDocument uploads under a temporary name, deletes the original and renames
        // the upload back - and the S3 rename does nothing.
        cms.write(FOLDER + "/standard.print-1696000000000", bytes(V2));

        ship("standard", "1.28.0", V1);

        assertEquals(Set.of("standard-custom.print", "standard@1.28.0.print"), cms.names(FOLDER));
        assertEquals("standard-custom", configuration.get(ENTITY + "/" + LANG));
    }

    @Test
    void aTenantTemplateNamedLikeTheShippedOneIsNotMigratedOnceVersionsExist() throws Exception {
        ship("standard", "1.28.0", V1);
        cms.write(FOLDER + "/standard.print", bytes(V2));

        ship("standard", "1.30.0", V3);

        assertEquals(Set.of("standard.print", "standard@1.28.0.print", "standard@1.30.0.print"), cms.names(FOLDER));
        assertTrue(configuration.isEmpty());
    }

    // ===== resolution

    @Test
    void contentHashVersionsAreOrderedByWhenTheyWereShipped() throws Exception {
        String r1 = ship("standard", PrintTemplateName.shortHash(bytes(V1)), V1);
        String r2 = ship("standard", PrintTemplateName.shortHash(bytes(V2)), V2);
        String r3 = ship("standard", PrintTemplateName.shortHash(bytes(V3)), V3);
        configuration.put(ENTITY + "/" + LANG, "standard@" + r2);

        List<PrintTemplateCatalog.Entry> entries = catalog.list(ENTITY, LANG, false);

        assertEquals(List.of("standard@" + r3, "standard@" + r2, "standard@" + r1), entries.stream()
                                                                                           .map(PrintTemplateCatalog.Entry::name)
                                                                                           .toList());
        assertEquals(List.of(true, false, false), entries.stream()
                                                         .map(PrintTemplateCatalog.Entry::newer)
                                                         .toList());
    }

    @Test
    void whenTheCurrentVersionIsMissingTheNewestPresentOnePrints() throws Exception {
        ship("standard", PrintTemplateName.shortHash(bytes(V1)), V1);
        String r2 = ship("standard", PrintTemplateName.shortHash(bytes(V2)), V2);
        // The next release was recorded, but its seed failed for this tenant.
        current.put("standard", releases.assign(ENTITY, LANG, "standard", PrintTemplateName.shortHash(bytes(V3)), bytes(V3)));
        releases.invalidate(ENTITY, LANG);

        assertEquals("standard@" + r2, resolved());
    }

    @Test
    void aSecondLayoutShippedLaterDoesNotBecomeTheDefault() throws Exception {
        ship("standard", "1.28.0", V1);
        ship("compact", "1.30.0", V2);

        assertEquals("standard@1.28.0", resolved());
        List<PrintTemplateCatalog.Entry> entries = catalog.list(ENTITY, LANG, false);
        assertTrue(entries.stream()
                          .noneMatch(PrintTemplateCatalog.Entry::newer),
                "another layout is not a newer version of this one");
    }

    @Test
    void withoutARecordAReleaseVersionRanksAboveAContentHashAndAPreReleaseBelowItsRelease() throws Exception {
        cms.write(FOLDER + "/standard@12345678.print", bytes(V1));
        cms.write(FOLDER + "/standard@2.0.0-rc.10.print", bytes(V1));
        cms.write(FOLDER + "/standard@2.0.0-rc.2.print", bytes(V1));
        cms.write(FOLDER + "/standard@1.0.0.print", bytes(V1));

        assertEquals("standard@2.0.0-rc.10", resolved());
        cms.write(FOLDER + "/standard@2.0.0.print", bytes(V1));
        assertEquals("standard@2.0.0", resolved());
    }

    @Test
    void theSelectionWinsOverTheDefaultAndTheRequestOverTheSelection() throws Exception {
        ship("standard", "1.28.0", V1);
        ship("standard", "1.30.0", V2);
        configuration.put(ENTITY + "/" + LANG, "standard@1.28.0");

        assertEquals("standard@1.28.0", resolved());
        assertEquals("standard@1.30.0", catalog.resolve(ENTITY, LANG, "standard@1.30.0")
                                               .name()
                                               .reference());
    }

    @Test
    void aSelectionNamingAMissingTemplateFallsBackToTheDefault() throws Exception {
        ship("standard", "1.28.0", V1);
        configuration.put(ENTITY + "/" + LANG, "gone");

        assertEquals("standard@1.28.0", resolved());
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
    void aPrintReadsTheShippedStateOnceAndThenFromTheCache() throws Exception {
        ship("standard", "1.28.0", V1);

        for (int i = 0; i < 5; i++) {
            resolved();
        }

        verify(seedService, times(1)).findShippedVersions(ENTITY, LANG);
    }

    // ===== tenant templates

    @Test
    void aShippedVersionCannotBeWrittenOrDeleted() throws Exception {
        ship("standard", "1.28.0", V1);
        cms.write(FOLDER + "/acme.print", bytes(V1));

        assertEquals(Reason.CONFLICT,
                assertThrows(PrintTemplateException.class, () -> catalog.write(ENTITY, LANG, "standard@1.28.0", V2, null)).getReason());
        assertEquals(Reason.CONFLICT,
                assertThrows(PrintTemplateException.class, () -> catalog.delete(ENTITY, LANG, "standard@1.28.0", null)).getReason());
        assertEquals(V1, read("standard@1.28.0"));
    }

    @Test
    void aTenantTemplateIsWrittenOnlyWhenItParses() throws Exception {
        catalog.write(ENTITY, LANG, "acme-blue", V2, null);

        assertEquals(V2, read("acme-blue"));
        assertEquals(Reason.INVALID, assertThrows(PrintTemplateException.class,
                () -> catalog.write(ENTITY, LANG, "acme-blue", "<document><page>", null)).getReason());
        assertEquals(Reason.INVALID,
                assertThrows(PrintTemplateException.class, () -> catalog.write(ENTITY, LANG, "../escape", V2, null)).getReason());
    }

    @Test
    void theCmsAccessGrantsApplyToEveryTemplateOperation() throws Exception {
        ship("standard", "1.28.0", V1);
        cms.write(FOLDER + "/acme.print", bytes(V1));
        when(access.isReadable(anyString(), any())).thenReturn(false);
        when(access.isWritable(anyString(), any())).thenReturn(false);

        assertEquals(Reason.FORBIDDEN,
                assertThrows(PrintTemplateException.class, () -> catalog.read(ENTITY, LANG, "acme", null)).getReason());
        assertEquals(Reason.FORBIDDEN,
                assertThrows(PrintTemplateException.class, () -> catalog.write(ENTITY, LANG, "acme", V2, null)).getReason());
        assertEquals(Reason.FORBIDDEN,
                assertThrows(PrintTemplateException.class, () -> catalog.duplicate(ENTITY, LANG, "acme", "copy", null)).getReason());
        assertEquals(Reason.FORBIDDEN,
                assertThrows(PrintTemplateException.class, () -> catalog.delete(ENTITY, LANG, "acme", null)).getReason());
        assertEquals(Set.of("acme.print", "standard@1.28.0.print"), cms.names(FOLDER));
    }

    @Test
    void theActiveTemplateCannotBeDeletedEvenWhenTheRequestsCopyOfTheSelectionIsStale() throws Exception {
        ship("standard", "1.28.0", V1);
        cms.write(FOLDER + "/acme.print", bytes(V1));
        cms.write(FOLDER + "/draft.print", bytes(V1));
        configuration.put(ENTITY + "/" + LANG, "acme");
        staleRequestConfiguration = true;

        assertEquals(Reason.CONFLICT,
                assertThrows(PrintTemplateException.class, () -> catalog.delete(ENTITY, LANG, "acme", null)).getReason());
        catalog.delete(ENTITY, LANG, "draft", null);

        assertEquals(Set.of("acme.print", "standard@1.28.0.print"), cms.names(FOLDER));
    }

    @Test
    void aDuplicateRecordsTheShippedVersionItDerivesFromEvenThroughAnotherDuplicate() throws Exception {
        ship("standard", "1.28.0", V1);

        catalog.duplicate(ENTITY, LANG, "standard@1.28.0", "acme", null);
        catalog.duplicate(ENTITY, LANG, "acme", "acme-2", null);

        String copy = read("acme-2");
        assertTrue(copy.startsWith("<!-- derived-from: standard@1.28.0 -->\n"), copy);
        assertEquals(1, copy.split("derived-from", -1).length - 1, "the header is not stacked");
        assertEquals(Reason.CONFLICT,
                assertThrows(PrintTemplateException.class, () -> catalog.duplicate(ENTITY, LANG, "acme", "acme-2", null)).getReason());
        assertEquals(Reason.INVALID,
                assertThrows(PrintTemplateException.class, () -> catalog.duplicate(ENTITY, LANG, "acme", "x@1.0.0", null)).getReason());
    }

    @Test
    void aDuplicateOfASourceWithAByteOrderMarkStillParses() throws Exception {
        ship("standard", "1.28.0", V1);
        cms.write(FOLDER + "/uploaded.print", bytes("﻿" + V2));

        catalog.duplicate(ENTITY, LANG, "uploaded", "copy", null);

        assertEquals("<!-- derived-from: uploaded -->\n" + V2, read("copy"));
    }

    @Test
    void theCatalogueListsVersionsNewestFirstThenTenantTemplatesAndMarksTheActiveAndTheNewer() throws Exception {
        ship("standard", "1.28.0", V1);
        catalog.duplicate(ENTITY, LANG, "standard@1.28.0", "acme", null);
        ship("standard", "1.30.0", V2);
        configuration.put(ENTITY + "/" + LANG, "acme");

        assertEquals(
                List.of(new PrintTemplateCatalog.Entry("standard@1.30.0", "shipped", "1.30.0", null, false, true, true),
                        new PrintTemplateCatalog.Entry("standard@1.28.0", "shipped", "1.28.0", null, false, false, false),
                        new PrintTemplateCatalog.Entry("acme", "tenant", null, "standard@1.28.0", true, false, false)),
                catalog.list(ENTITY, LANG, true));
        List<PrintTemplateCatalog.Entry> light = catalog.list(ENTITY, LANG, false);
        assertNull(light.get(2)
                        .derivedFrom(),
                "without details no tenant template is read");
        assertFalse(light.get(0)
                         .newer());
    }

    @Test
    void documentTypesAreTheEntityFoldersAndLanguagesAreReadPerType() throws Exception {
        ship("standard", "1.28.0", V1);
        cms.write("/Templates/SalesInvoice/Print/bg/standard.print", bytes(V1));
        cms.write("/Templates/Print/logo.png", bytes("png"));

        assertEquals(List.of(new PrintTemplateCatalog.DocumentType(ENTITY)), catalog.documentTypes());
        assertEquals(Set.of("en", "bg"), Set.copyOf(catalog.languages(ENTITY)));
    }

    /** Ships a template as the synchronizer does: assign the version, record the artefact row, seed. */
    private String ship(String name, String label, String content) throws Exception {
        String version = releases.assign(ENTITY, LANG, name, label, bytes(content));
        current.put(name, version);
        releases.invalidate(ENTITY, LANG);
        catalog.seed(ENTITY, LANG, name, version, bytes(content));
        return version;
    }

    private String resolved() throws Exception {
        return catalog.resolve(ENTITY, LANG, null)
                      .name()
                      .reference();
    }

    private String read(String reference) throws Exception {
        return catalog.read(ENTITY, LANG, reference, null);
    }

    private static ShippedVersion shippedVersion(String name, String version) {
        return new ShippedVersion() {
            @Override
            public String getTemplateName() {
                return name;
            }

            @Override
            public String getVersion() {
                return version;
            }
        };
    }

    private static String template(String marker) {
        return "<document id=\"" + marker
                + "\"><page><section><field label=\"Number\">{{document.number}}</field></section></page></document>\n";
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
