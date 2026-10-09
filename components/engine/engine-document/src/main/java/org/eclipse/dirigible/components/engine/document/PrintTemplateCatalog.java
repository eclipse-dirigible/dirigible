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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.engine.cms.documents.DocumentAccessEvaluator;
import org.eclipse.dirigible.components.engine.document.PrintTemplateException.Reason;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The print templates of a tenant as a versioned, selectable catalogue. Everything the platform
 * knows about print templates lives here; the CMS underneath is a plain file store
 * ({@link CmsStore}), the selection a plain tenant configuration ({@link PrintTemplateSelection})
 * and what was shipped when the instance's ledger ({@link PrintTemplateReleases}).
 *
 * <p>
 * A language folder {@code Templates/<Entity>/Print/<lang>/} holds two kinds of templates, told
 * apart by their names alone ({@link PrintTemplateName}): <b>shipped versions</b>
 * ({@code standard@1.28.0.print}), added by {@link PrintTemplateSynchronizer} beside the earlier
 * ones and never edited or deleted here, and <b>tenant templates</b> ({@code acme-blue.print}), the
 * tenant's own, editable. "Updating" a layout is not an operation: a release adds a version, and
 * the tenant switches to it - or, having never chosen, gets it automatically. A document whose name
 * is not a valid template name ({@code Invoice template.print}) is a tenant template under a
 * sanitised name, never dropped.
 *
 * <p>
 * <b>Resolution</b>, for a print: the explicit {@code template} request parameter, else the tenant
 * configuration, else the default - the version the registry ships now of the primary template
 * ({@code standard}, else the first shipped name), else that template's newest version present -
 * else (a folder with no shipped version - a language the tenant uploaded itself) the first tenant
 * template by name.
 */
@Component
class PrintTemplateCatalog {

    private static final Logger logger = LoggerFactory.getLogger(PrintTemplateCatalog.class);

    private static final String TEMPLATES_ROOT = "/Templates";
    private static final String PRINT_SEGMENT = "Print";
    private static final String SEPARATOR = "/";

    /** The template that prints by default when a document type ships several. */
    static final String PRIMARY_NAME = "standard";

    /** The suffix a migrated, customised legacy template is kept under. */
    private static final String CUSTOM_SUFFIX = "-custom";

    /**
     * The version a migrated customisation records it derives from: one of the copies seeded before
     * versions existed, which ranks below every recorded version.
     */
    static final String LEGACY_VERSION = "legacy";

    /** The byte order mark, which must not end up after the derived-from header. */
    private static final char BYTE_ORDER_MARK = '\uFEFF';

    /** Room left for the disambiguating hash suffix of a sanitised name. */
    private static final int SANITISED_PREFIX_LENGTH = 90;

    /**
     * A folder segment (a document type, a language code): a plain name, never a path or a dot segment.
     */
    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,254}");

    /** A language code: a segment no longer than the ledger's language column. */
    private static final Pattern LANGUAGE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    /**
     * The header a duplicate records its origin in - plain template content (the parser skips
     * comments), not CMS metadata.
     */
    private static final Pattern DERIVED_FROM = Pattern.compile("\\A\\s*<!--\\s*derived-from:\\s*(\\S+)\\s*-->[ \\t]*\\R?");

    private final CmsStore cmsStore;
    private final PrintTemplateSelection selection;
    private final PrintTemplateReleases releases;
    private final DocumentAccessEvaluator accessEvaluator;

    PrintTemplateCatalog(CmsStore cmsStore, PrintTemplateSelection selection, PrintTemplateReleases releases,
            DocumentAccessEvaluator accessEvaluator) {
        this.cmsStore = cmsStore;
        this.selection = selection;
        this.releases = releases;
        this.accessEvaluator = accessEvaluator;
    }

    /**
     * A catalogue entry.
     *
     * @param name the reference ({@code standard@1.28.0}, {@code acme-blue})
     * @param kind {@code shipped} or {@code tenant}
     * @param version the shipped version, {@code null} for a tenant template
     * @param derivedFrom the reference a tenant template was duplicated from, when it records one and
     *        the listing reads the tenant templates
     * @param active whether this template prints when no other is requested
     * @param newer whether this shipped version is newer than the active layout's version
     * @param defaultTemplate whether this template prints when the tenant has no selection - the
     *        shipped default a print dialog offers beside the tenant's own templates
     */
    record Entry(String name, String kind, String version, String derivedFrom, boolean active, boolean newer, boolean defaultTemplate) {
    }

    /**
     * A document type that may have print templates.
     *
     * @param entity the document type
     */
    record DocumentType(String entity) {
    }

    /**
     * A resolved template.
     *
     * @param name the template's name
     * @param source the template source
     */
    record Resolved(PrintTemplateName name, String source) {
    }

    /**
     * A validated language folder: its print templates keyed by name, valued by document name, and
     * every document name in CMS order - the order the resolution before versions read.
     */
    private record Folder(String entity, String language, String path, Map<PrintTemplateName, String> files, List<String> documents) {

        String filePath(PrintTemplateName name) {
            String fileName = files.get(name);
            return path + SEPARATOR + (fileName == null ? name.fileName() : fileName);
        }

        boolean hasShipped() {
            return files.keySet()
                        .stream()
                        .anyMatch(PrintTemplateName::isShipped);
        }

        boolean hasShipped(String name) {
            return files.keySet()
                        .stream()
                        .anyMatch(each -> each.isShipped() && each.name()
                                                                  .equals(name));
        }
    }

    /**
     * Lists the document types that may have print templates - the entity folders under
     * {@code Templates/}, read in one listing (their languages are read when one is chosen), except
     * {@code Templates/Print/}, which holds the assets the templates share (the logo).
     *
     * @return the document types, in CMS order
     * @throws IOException on CMS access failure
     */
    List<DocumentType> documentTypes() throws IOException {
        return cmsStore.listFolders(TEMPLATES_ROOT)
                       .stream()
                       .filter(entity -> isSegment(entity) && !PRINT_SEGMENT.equals(entity))
                       .map(DocumentType::new)
                       .toList();
    }

    /**
     * Lists the languages a document type has print templates in.
     *
     * @param entity the document type
     * @return the language codes, empty when the document type has none
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException when the document type is not a valid name
     */
    List<String> languages(String entity) throws IOException, PrintTemplateException {
        requireSegment("document type", entity);
        return cmsStore.listFolders(printFolderPath(entity));
    }

    /**
     * Lists the catalogue of a document type and language: the shipped versions newest first, then the
     * tenant templates by name.
     *
     * @param entity the document type
     * @param language the language code
     * @param details whether to read the tenant templates for the version each derives from - what the
     *        Settings page shows, and what {@code newer} is measured against while a tenant template is
     *        active; a print dialog needs neither and reads no template
     * @return the entries, empty when the folder holds no template
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException when the document type or language is not a valid name
     */
    List<Entry> list(String entity, String language, boolean details) throws IOException, PrintTemplateException {
        Folder folder = folder(entity, language);
        PrintTemplateReleases.Snapshot shipped = releases.snapshot(entity, language);
        Comparator<PrintTemplateName> order = versionOrder(shipped);
        Optional<PrintTemplateName> active = active(folder, shipped, order, selection.get(entity, language));
        Optional<PrintTemplateName> fallback = defaultTemplate(folder, shipped, order);

        Map<PrintTemplateName, String> derivedFrom = new LinkedHashMap<>();
        for (PrintTemplateName tenant : tenantTemplates(folder)) {
            derivedFrom.put(tenant, details ? derivedFrom(readSource(folder, tenant)).orElse(null) : null);
        }
        Optional<PrintTemplateName> baseline = active.flatMap(name -> name.isShipped() ? Optional.of(name)
                : Optional.ofNullable(derivedFrom.get(name))
                          .flatMap(PrintTemplateName::parse)
                          .filter(PrintTemplateName::isShipped));

        List<Entry> entries = new ArrayList<>();
        for (PrintTemplateName version : shippedNewestFirst(folder, order)) {
            boolean newer = baseline.filter(base -> base.name()
                                                        .equals(version.name()))
                                    .map(base -> order.compare(version, base) > 0)
                                    .orElse(false);
            entries.add(new Entry(version.reference(), "shipped", version.version(), null, active.filter(version::equals)
                                                                                                 .isPresent(),
                    newer, fallback.filter(version::equals)
                                   .isPresent()));
        }
        derivedFrom.forEach((tenant,
                origin) -> entries.add(new Entry(tenant.reference(), "tenant", null, origin, active.filter(tenant::equals)
                                                                                                   .isPresent(),
                        false, fallback.filter(tenant::equals)
                                       .isPresent())));
        return entries;
    }

    /**
     * Resolves the template a print uses.
     *
     * @param entity the document type
     * @param language the language code
     * @param reference an explicitly requested template reference, or {@code null} for the active one
     * @return the template
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException {@link Reason#NOT_FOUND} when the requested template or, without
     *         one, any template is missing
     */
    Resolved resolve(String entity, String language, String reference) throws IOException, PrintTemplateException {
        Folder folder = folder(entity, language);
        PrintTemplateName name;
        if (reference != null && !reference.isBlank()) {
            name = existing(folder, reference);
        } else {
            PrintTemplateReleases.Snapshot shipped = releases.snapshot(entity, language);
            name = active(folder, shipped, versionOrder(shipped), selection.get(entity, language)).orElseThrow(
                    () -> new PrintTemplateException(Reason.NOT_FOUND,
                            "No print template found for entity [" + entity + "] and language [" + language + "]"));
        }
        return new Resolved(name, readSource(folder, name));
    }

    /**
     * Reads a template's source.
     *
     * @param entity the document type
     * @param language the language code
     * @param reference the template reference
     * @param request the request the caller's CMS access grants are checked against
     * @return the source
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException {@link Reason#NOT_FOUND} when the template is missing,
     *         {@link Reason#FORBIDDEN} when the caller may not read it
     */
    String read(String entity, String language, String reference, HttpServletRequest request) throws IOException, PrintTemplateException {
        Folder folder = folder(entity, language);
        PrintTemplateName name = existing(folder, reference);
        requireReadable(folder, name, request);
        return readSource(folder, name);
    }

    /**
     * Writes a tenant template's source, creating the template when it does not exist yet.
     *
     * @param entity the document type
     * @param language the language code
     * @param reference the tenant template's name
     * @param source the template source, which must parse
     * @param request the request the caller's CMS access grants are checked against
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException {@link Reason#CONFLICT} for a shipped version,
     *         {@link Reason#INVALID} for a malformed name or a source that does not parse,
     *         {@link Reason#FORBIDDEN} when the caller may not write it
     */
    void write(String entity, String language, String reference, String source, HttpServletRequest request)
            throws IOException, PrintTemplateException {
        Folder folder = folder(entity, language);
        PrintTemplateName name = PrintTemplateName.parse(reference)
                                                  .orElseThrow(() -> invalidName(reference));
        if (name.isShipped()) {
            throw new PrintTemplateException(Reason.CONFLICT, "The print template [" + reference
                    + "] is a shipped version and cannot be changed - duplicate it into a tenant template to customise it");
        }
        requireWritable(folder, name, request);
        requireParses(source);
        cmsStore.write(folder.filePath(name), source.getBytes(StandardCharsets.UTF_8));
        logger.info("Print template [{}] written", LoggedPath.of(folder.filePath(name)));
    }

    /**
     * Copies a shipped version or a tenant template into a new tenant template, recording the shipped
     * version it derives from in a header comment.
     *
     * @param entity the document type
     * @param language the language code
     * @param reference the template to copy
     * @param target the new tenant template's name
     * @param request the request the caller's CMS access grants are checked against
     * @return the new template's name
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException {@link Reason#NOT_FOUND} when the source is missing,
     *         {@link Reason#INVALID} for a target that is not a tenant template name or a copy that
     *         does not parse, {@link Reason#CONFLICT} when the target exists, {@link Reason#FORBIDDEN}
     *         when the caller may not read the source or write the target
     */
    PrintTemplateName duplicate(String entity, String language, String reference, String target, HttpServletRequest request)
            throws IOException, PrintTemplateException {
        Folder folder = folder(entity, language);
        PrintTemplateName source = existing(folder, reference);
        PrintTemplateName copy = PrintTemplateName.parse(target)
                                                  .filter(name -> !name.isShipped())
                                                  .orElseThrow(() -> invalidName(target));
        if (folder.files()
                  .containsKey(copy)) {
            throw new PrintTemplateException(Reason.CONFLICT, "The print template [" + copy.reference() + "] already exists");
        }
        requireReadable(folder, source, request);
        requireWritable(folder, copy, request);
        String content = readSource(folder, source);
        String origin = source.isShipped() ? source.reference() : derivedFrom(content).orElse(source.reference());
        String duplicate = withDerivedFrom(content, origin);
        requireParses(duplicate);
        cmsStore.write(folder.filePath(copy), duplicate.getBytes(StandardCharsets.UTF_8));
        logger.info("Print template [{}] duplicated as [{}]", LoggedPath.of(folder.filePath(source)), LoggedPath.of(copy.reference()));
        return copy;
    }

    /**
     * Deletes a tenant template.
     *
     * @param entity the document type
     * @param language the language code
     * @param reference the tenant template's name
     * @param request the request the caller's CMS access grants are checked against
     * @throws IOException on CMS access failure
     * @throws SQLException when the tenant's selection cannot be read - the active template is never
     *         deleted on a guess
     * @throws PrintTemplateException {@link Reason#NOT_FOUND} when the template is missing,
     *         {@link Reason#CONFLICT} for a shipped version or the active template,
     *         {@link Reason#FORBIDDEN} when the caller may not delete it
     */
    void delete(String entity, String language, String reference, HttpServletRequest request)
            throws IOException, SQLException, PrintTemplateException {
        Folder folder = folder(entity, language);
        PrintTemplateName name = existing(folder, reference);
        if (name.isShipped()) {
            throw new PrintTemplateException(Reason.CONFLICT,
                    "The print template [" + reference + "] is a shipped version - shipped versions are never deleted");
        }
        requireWritable(folder, name, request);
        // The selection read fresh, past this node's configuration cache: on another node the cached
        // copy can be stale, and the guard would let the active one go.
        PrintTemplateReleases.Snapshot shipped = releases.snapshot(entity, language);
        if (active(folder, shipped, versionOrder(shipped), selection.getEffective(entity, language)).filter(name::equals)
                                                                                                    .isPresent()) {
            throw new PrintTemplateException(Reason.CONFLICT,
                    "The print template [" + reference + "] is the active one - select another template before deleting it");
        }
        cmsStore.delete(folder.filePath(name));
        logger.info("Print template [{}] deleted", LoggedPath.of(folder.filePath(name)));
    }

    /**
     * Brings the current tenant's catalogue up to a shipped template - the seeding side, called by
     * {@link PrintTemplateSynchronizer} once per tenant.
     * <ul>
     * <li>The first time, the folder as the create-if-absent seed left it is migrated (see
     * {@link #migrate}), so that no tenant loses a layout and none prints a different one.</li>
     * <li>An existing {@code <name>@<version>.print} whose bytes drifted is converged back to the
     * shipped bytes - immutability is enforced here and by refusing such writes in the Documents
     * perspective, not by CMS permissions.</li>
     * <li>Otherwise the version is added, unless the newest shipped version of the template already
     * carries the same bytes.</li>
     * </ul>
     * Tenant templates are never touched.
     *
     * @param entity the document type
     * @param language the language code
     * @param name the shipped template's name
     * @param version the version it ships as
     * @param content the shipped bytes
     * @throws IOException on CMS access failure
     * @throws SQLException when the migration cannot read or write the tenant's selection
     * @throws PrintTemplateException when the document type or language is not a valid name
     */
    void seed(String entity, String language, String name, String version, byte[] content)
            throws IOException, SQLException, PrintTemplateException {
        PrintTemplateName target = PrintTemplateName.shipped(name, version);
        migrate(folder(entity, language), name);

        Folder folder = folder(entity, language);
        if (folder.files()
                  .containsKey(target)) {
            String path = folder.filePath(target);
            if (!Arrays.equals(cmsStore.read(path)
                                       .orElse(null),
                    content)) {
                cmsStore.write(path, content);
                logger.info("Print template version [{}] of [{}/{}] converged back to its shipped content", target.reference(), entity,
                        language);
            }
            return;
        }
        Optional<PrintTemplateName> newest = shippedNewestFirst(folder, versionOrder(releases.snapshot(entity, language))).stream()
                                                                                                                          .filter(shipped -> shipped.name()
                                                                                                                                                    .equals(name))
                                                                                                                          .findFirst();
        if (newest.isPresent() && Arrays.equals(cmsStore.read(folder.filePath(newest.get()))
                                                        .orElse(null),
                content)) {
            logger.debug("Print template [{}] of [{}/{}] is unchanged since [{}] - no version added", target.reference(), entity, language,
                    newest.get()
                          .reference());
            return;
        }
        cmsStore.write(folder.filePath(target), content);
        logger.info("Print template version [{}] of [{}/{}] added", target.reference(), entity, language);
    }

    /**
     * Migrates what the create-if-absent seed left in a folder, before the first version of a template
     * lands in it.
     * <ul>
     * <li>While the folder holds no shipped version at all, the resolution before versions still
     * decides what prints: the first {@code .print} document in CMS order. When the tenant has no
     * selection and that document is not the legacy copy of this template, it is selected - an uploaded
     * {@code acme.print} keeps printing. A document whose name is not a valid template name is first
     * moved to its sanitised name, so the selection names a stable file.</li>
     * <li>The legacy copy, {@code <name>.print}, whose bytes this instance shipped as some version,
     * becomes that version: an unedited copy, however old, is not a customisation, and a tenant who
     * never chose gets the current version. When it printed and is not the primary template, that
     * version is selected, so the outcome does not depend on which template seeds first.</li>
     * <li>Any other legacy copy is kept as {@code <name>-custom}, with a header recording that it
     * derives from {@code <name>@legacy} (so every shipped version is flagged as newer), and is
     * selected when it is what printed.</li>
     * </ul>
     * Everything is a copy followed by a delete, never a CMS rename (see {@link CmsStore#move}).
     */
    private void migrate(Folder folder, String name) throws IOException, SQLException {
        PrintTemplateName legacy = PrintTemplateName.tenant(name);
        boolean legacyPending = folder.files()
                                      .containsKey(legacy)
                && !folder.hasShipped(name);
        boolean preCatalogue = !folder.hasShipped();
        if (!legacyPending && !preCatalogue) {
            return;
        }
        Optional<String> stored = selection.getStored(folder.entity(), folder.language());
        Optional<PrintTemplateName> printed = preCatalogue && stored.isEmpty() ? printedBeforeVersions(folder) : Optional.empty();

        if (printed.isPresent() && !printed.get()
                                           .equals(legacy)) {
            PrintTemplateName kept = printed.get();
            // Select before moving: should the move fail, the selection still resolves - folder() maps the
            // sanitised name to the document under its original name, which it then simply keeps.
            selection.select(folder.entity(), folder.language(), kept.reference());
            String document = folder.files()
                                    .get(kept);
            if (!document.equals(kept.fileName())) {
                String path = folder.filePath(kept);
                cmsStore.move(path, kept.fileName(), cmsStore.read(path)
                                                             .orElseThrow(() -> new IOException("CMS document [" + path + "] vanished")));
            }
            logger.info("Print template [{}] of [{}/{}] printed before versions existed - it stays selected",
                    LoggedPath.of(kept.reference()), folder.entity(), folder.language());
        }
        if (!legacyPending) {
            return;
        }

        String legacyPath = folder.filePath(legacy);
        byte[] legacyContent = cmsStore.read(legacyPath)
                                       .orElseThrow(() -> new IOException("CMS document [" + legacyPath + "] vanished"));
        boolean selectedLegacy = stored.filter(legacy.reference()::equals)
                                       .isPresent();
        Optional<String> shippedVersion = releases.versionOf(folder.entity(), folder.language(), name, legacyContent);
        if (shippedVersion.isPresent()) {
            PrintTemplateName version = PrintTemplateName.shipped(name, shippedVersion.get());
            // An unedited copy of the primary template needs no selection: the default is that template's
            // current version. Any other one that printed keeps printing - the default would switch the
            // tenant to the primary template, depending on which template happened to seed first.
            boolean printedOther = printed.filter(legacy::equals)
                                          .isPresent()
                    && !name.equals(primaryName(folder));
            if (selectedLegacy || printedOther) {
                selection.select(folder.entity(), folder.language(), version.reference());
            }
            cmsStore.move(legacyPath, version.fileName(), legacyContent);
            logger.info("Print template [{}] of [{}/{}] is the unedited shipped version [{}] - migrated to it", legacy.reference(),
                    folder.entity(), folder.language(), version.reference());
            return;
        }

        PrintTemplateName custom = PrintTemplateName.tenant(name + CUSTOM_SUFFIX);
        for (int i = 2; folder.files()
                              .containsKey(custom); i++) {
            custom = PrintTemplateName.tenant(name + CUSTOM_SUFFIX + "-" + i);
        }
        // Select before moving: should the selection fail, the legacy file is still in place and the
        // next attempt migrates it, instead of the tenant silently printing the shipped layout.
        if (selectedLegacy || printed.filter(legacy::equals)
                                     .isPresent()) {
            selection.select(folder.entity(), folder.language(), custom.reference());
        }
        String source = new String(legacyContent, StandardCharsets.UTF_8);
        String kept = derivedFrom(source).isPresent() ? source
                : withDerivedFrom(source, PrintTemplateName.shipped(name, LEGACY_VERSION)
                                                           .reference());
        cmsStore.move(legacyPath, custom.fileName(), kept.getBytes(StandardCharsets.UTF_8));
        logger.info("Print template [{}] of [{}/{}] matches no shipped version - kept as the tenant template [{}]", legacy.reference(),
                folder.entity(), folder.language(), custom.reference());
    }

    /**
     * The template the resolution before versions printed: the first {@code .print} document in CMS
     * order, else the first document that is a print template at all.
     */
    private static Optional<PrintTemplateName> printedBeforeVersions(Folder folder) {
        Map<String, PrintTemplateName> byDocument = new LinkedHashMap<>();
        folder.files()
              .forEach((name, document) -> byDocument.put(document, name));
        Optional<PrintTemplateName> first = folder.documents()
                                                  .stream()
                                                  .filter(document -> document.toLowerCase(Locale.ROOT)
                                                                              .endsWith(PrintTemplateName.EXTENSION))
                                                  .map(byDocument::get)
                                                  .filter(name -> name != null)
                                                  .findFirst();
        if (first.isPresent()) {
            return first;
        }
        return folder.documents()
                     .stream()
                     .map(byDocument::get)
                     .filter(name -> name != null)
                     .findFirst();
    }

    /**
     * The template that prints when none is requested: the configured one when it names an existing
     * template, else the default.
     */
    private Optional<PrintTemplateName> active(Folder folder, PrintTemplateReleases.Snapshot shipped, Comparator<PrintTemplateName> order,
            Optional<String> configured) {
        if (configured.isPresent()) {
            Optional<PrintTemplateName> selected = PrintTemplateName.parse(configured.get())
                                                                    .filter(folder.files()::containsKey);
            if (selected.isPresent()) {
                return selected;
            }
            logger.warn("The print template [{}] selected by [{}] does not exist - printing with the default one",
                    LoggedPath.of(configured.get()), LoggedPath.of(PrintTemplateSelection.key(folder.entity(), folder.language())));
        }
        return defaultTemplate(folder, shipped, order);
    }

    /**
     * The default: of the primary template - {@code standard}, else the first name the registry ships,
     * else the first name present - the version the registry ships now, else its newest version
     * present; else the first tenant template. Scoped by name, so a release that adds a second layout
     * does not make it the default.
     */
    private static Optional<PrintTemplateName> defaultTemplate(Folder folder, PrintTemplateReleases.Snapshot shipped,
            Comparator<PrintTemplateName> order) {
        Set<String> present = new TreeSet<>();
        folder.files()
              .keySet()
              .stream()
              .filter(PrintTemplateName::isShipped)
              .forEach(name -> present.add(name.name()));
        Set<String> candidates = new LinkedHashSet<>();
        primary(new TreeSet<>(shipped.current()
                                     .keySet())).ifPresent(candidates::add);
        primary(present).ifPresent(candidates::add);
        for (String candidate : candidates) {
            String current = shipped.current()
                                    .get(candidate);
            if (current != null) {
                PrintTemplateName now = PrintTemplateName.shipped(candidate, current);
                if (folder.files()
                          .containsKey(now)) {
                    return Optional.of(now);
                }
            }
            Optional<PrintTemplateName> newest = shippedNewestFirst(folder, order).stream()
                                                                                  .filter(name -> name.name()
                                                                                                      .equals(candidate))
                                                                                  .findFirst();
            if (newest.isPresent()) {
                return newest;
            }
        }
        return tenantTemplates(folder).stream()
                                      .findFirst();
    }

    /**
     * The primary template of a folder: of the names the registry ships now, else of the shipped names
     * present - the name {@link #defaultTemplate} prints by default.
     */
    private String primaryName(Folder folder) {
        Set<String> shipped = new TreeSet<>(releases.snapshot(folder.entity(), folder.language())
                                                    .current()
                                                    .keySet());
        if (shipped.isEmpty()) {
            folder.files()
                  .keySet()
                  .stream()
                  .filter(PrintTemplateName::isShipped)
                  .forEach(name -> shipped.add(name.name()));
        }
        return primary(shipped).orElse(null);
    }

    private static Optional<String> primary(Set<String> sortedNames) {
        if (sortedNames.contains(PRIMARY_NAME)) {
            return Optional.of(PRIMARY_NAME);
        }
        return sortedNames.stream()
                          .findFirst();
    }

    /**
     * The order of shipped versions, oldest first: a version this instance recorded ranks by when it
     * was first shipped, above every version it never recorded; among those, a release version ranks
     * above any other and release versions compare by their numbers. {@code @legacy}, the origin a
     * migrated customisation records, ranks below everything.
     */
    static Comparator<PrintTemplateName> versionOrder(PrintTemplateReleases.Snapshot shipped) {
        return (left, right) -> {
            boolean leftLegacy = LEGACY_VERSION.equals(left.version());
            if (leftLegacy != LEGACY_VERSION.equals(right.version())) {
                return leftLegacy ? -1 : 1;
            }
            Optional<Long> leftPosition = shipped.position(left);
            Optional<Long> rightPosition = shipped.position(right);
            if (leftPosition.isPresent() != rightPosition.isPresent()) {
                return leftPosition.isPresent() ? 1 : -1;
            }
            if (leftPosition.isPresent()) {
                return Long.compare(leftPosition.get(), rightPosition.get());
            }
            if (left.hasReleaseVersion() != right.hasReleaseVersion()) {
                return left.hasReleaseVersion() ? 1 : -1;
            }
            if (left.hasReleaseVersion()) {
                int compared = left.compareReleaseVersion(right);
                if (compared != 0) {
                    return compared;
                }
            }
            return left.reference()
                       .compareTo(right.reference());
        };
    }

    private static List<PrintTemplateName> shippedNewestFirst(Folder folder, Comparator<PrintTemplateName> order) {
        return folder.files()
                     .keySet()
                     .stream()
                     .filter(PrintTemplateName::isShipped)
                     .sorted(order.reversed())
                     .toList();
    }

    private static List<PrintTemplateName> tenantTemplates(Folder folder) {
        return folder.files()
                     .keySet()
                     .stream()
                     .filter(name -> !name.isShipped())
                     .sorted(Comparator.comparing(PrintTemplateName::reference))
                     .toList();
    }

    /**
     * Reads a language folder. Valid template names are taken as they are; then every other document
     * that is a print template ({@link PrintTemplateName#templateBase}), in name order, as a tenant
     * template under its sanitised name - qualified by a hash of its document name when that name is
     * taken, so the mapping does not depend on listing order.
     */
    private Folder folder(String entity, String language) throws IOException, PrintTemplateException {
        requireSegment("document type", entity);
        if (!isLanguage(language)) {
            throw new PrintTemplateException(Reason.INVALID, "[" + language + "] is not a valid language");
        }
        String path = printFolderPath(entity) + SEPARATOR + language;
        List<String> documents = cmsStore.listDocuments(path);
        Map<PrintTemplateName, String> files = new LinkedHashMap<>();
        List<String> others = new ArrayList<>();
        for (String document : documents) {
            Optional<PrintTemplateName> name = PrintTemplateName.fromFileName(document);
            if (name.isPresent()) {
                files.putIfAbsent(name.get(), document);
            } else if (PrintTemplateName.templateBase(document)
                                        .isPresent()) {
                others.add(document);
            }
        }
        others.sort(Comparator.naturalOrder());
        for (String document : others) {
            String sanitised = PrintTemplateName.sanitize(PrintTemplateName.templateBase(document)
                                                                           .orElseThrow());
            PrintTemplateName name = PrintTemplateName.tenant(sanitised);
            if (files.containsKey(name)) {
                String prefix = sanitised.length() > SANITISED_PREFIX_LENGTH ? sanitised.substring(0, SANITISED_PREFIX_LENGTH) : sanitised;
                name = PrintTemplateName.tenant(prefix + "-" + PrintTemplateName.shortHash(document));
            }
            files.putIfAbsent(name, document);
        }
        return new Folder(entity, language, path, files, documents);
    }

    private static PrintTemplateName existing(Folder folder, String reference) throws PrintTemplateException {
        return PrintTemplateName.parse(reference)
                                .filter(folder.files()::containsKey)
                                .orElseThrow(() -> new PrintTemplateException(Reason.NOT_FOUND, "No print template [" + reference
                                        + "] found for entity [" + folder.entity() + "] and language [" + folder.language() + "]"));
    }

    private String readSource(Folder folder, PrintTemplateName name) throws IOException, PrintTemplateException {
        byte[] content = cmsStore.read(folder.filePath(name))
                                 .orElseThrow(() -> new PrintTemplateException(Reason.NOT_FOUND, "No print template [" + name.reference()
                                         + "] found for entity [" + folder.entity() + "] and language [" + folder.language() + "]"));
        return new String(content, StandardCharsets.UTF_8);
    }

    private void requireReadable(Folder folder, PrintTemplateName name, HttpServletRequest request) throws PrintTemplateException {
        String path = folder.filePath(name);
        if (!accessEvaluator.isReadable(path, request)) {
            throw new PrintTemplateException(Reason.FORBIDDEN, "Reading [" + path + "] is not allowed by the CMS access grants");
        }
    }

    private void requireWritable(Folder folder, PrintTemplateName name, HttpServletRequest request) throws PrintTemplateException {
        String path = folder.filePath(name);
        if (!accessEvaluator.isWritable(path, request)) {
            throw new PrintTemplateException(Reason.FORBIDDEN, "Writing [" + path + "] is not allowed by the CMS access grants");
        }
    }

    /** The shipped version a template records it derives from, when its header names a valid one. */
    private static Optional<String> derivedFrom(String source) {
        Matcher matcher = DERIVED_FROM.matcher(withoutByteOrderMark(source));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return PrintTemplateName.parse(matcher.group(1))
                                .map(PrintTemplateName::reference);
    }

    /**
     * The source with its header replaced by one recording the given origin, and no byte order mark.
     */
    private static String withDerivedFrom(String source, String origin) {
        String body = DERIVED_FROM.matcher(withoutByteOrderMark(source))
                                  .replaceFirst("");
        return "<!-- derived-from: " + origin + " -->\n" + body;
    }

    private static String withoutByteOrderMark(String source) {
        return !source.isEmpty() && source.charAt(0) == BYTE_ORDER_MARK ? source.substring(1) : source;
    }

    private static void requireParses(String source) throws PrintTemplateException {
        if (source == null || source.isBlank()) {
            throw new PrintTemplateException(Reason.INVALID, "The print template is empty");
        }
        try {
            new DocumentParser().parse(source);
        } catch (RuntimeException ex) {
            throw new PrintTemplateException(Reason.INVALID, "The print template does not parse: " + ex.getMessage(), ex);
        }
    }

    private static PrintTemplateException invalidName(String reference) {
        return new PrintTemplateException(Reason.INVALID, "[" + reference
                + "] is not a tenant template name - use letters, digits, '.', '-' and '_', starting with a letter or digit, and no '@'");
    }

    private static void requireSegment(String what, String value) throws PrintTemplateException {
        if (!isSegment(value)) {
            throw new PrintTemplateException(Reason.INVALID, "[" + value + "] is not a valid " + what);
        }
    }

    /**
     * Whether a value is a valid folder segment - a document type or a language code.
     *
     * @param value the value
     * @return true for a plain name, never a path or a dot segment
     */
    static boolean isSegment(String value) {
        return value != null && SEGMENT.matcher(value)
                                       .matches();
    }

    /**
     * Whether a value is a valid language code - a segment short enough for the ledger's column.
     *
     * @param value the value
     * @return true for a plain name of at most 64 characters
     */
    static boolean isLanguage(String value) {
        return value != null && LANGUAGE.matcher(value)
                                        .matches();
    }

    private static String printFolderPath(String entity) {
        return TEMPLATES_ROOT + SEPARATOR + entity + SEPARATOR + PRINT_SEGMENT;
    }
}
