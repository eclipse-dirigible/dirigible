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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.engine.document.PrintTemplateException.Reason;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateSeedService;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The print templates of a tenant as a versioned, selectable catalogue. Everything the platform
 * knows about print templates lives here; the CMS underneath is a plain file store
 * ({@link CmsStore}) and the selection a plain tenant configuration
 * ({@link PrintTemplateSelection}).
 *
 * <p>
 * A language folder {@code Templates/<Entity>/Print/<lang>/} holds two kinds of templates, told
 * apart by their names alone ({@link PrintTemplateName}): <b>shipped versions</b>
 * ({@code standard@1.28.0.print}), added by {@link PrintTemplateSynchronizer} beside the earlier
 * ones and never edited or deleted here, and <b>tenant templates</b> ({@code acme-blue.print}), the
 * tenant's own, editable. "Updating" a layout is not an operation: a release adds a version, and
 * the tenant switches to it - or, having never chosen, gets it automatically.
 *
 * <p>
 * <b>Resolution</b>, for a print: the explicit {@code template} request parameter, else the tenant
 * configuration, else the newest shipped version, else (a folder with no shipped version - a
 * language the tenant uploaded itself) the first tenant template by name.
 */
@Component
class PrintTemplateCatalog {

    private static final Logger logger = LoggerFactory.getLogger(PrintTemplateCatalog.class);

    private static final String TEMPLATES_ROOT = "/Templates";
    private static final String PRINT_SEGMENT = "Print";
    private static final String SEPARATOR = "/";

    /** The suffix a migrated, customised legacy template is renamed with. */
    private static final String CUSTOM_SUFFIX = "-custom";

    /**
     * A folder segment (a document type, a language code): a plain name, never a path or a dot segment.
     */
    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,254}");

    /**
     * The header a duplicate records its origin in - plain template content (the parser skips
     * comments), not CMS metadata.
     */
    private static final Pattern DERIVED_FROM = Pattern.compile("\\A\\s*<!--\\s*derived-from:\\s*(\\S+)\\s*-->[ \\t]*\\R?");

    private final CmsStore cmsStore;
    private final PrintTemplateSelection selection;
    private final PrintTemplateSeedService seedService;

    PrintTemplateCatalog(CmsStore cmsStore, PrintTemplateSelection selection, PrintTemplateSeedService seedService) {
        this.cmsStore = cmsStore;
        this.selection = selection;
        this.seedService = seedService;
    }

    /**
     * A catalogue entry.
     *
     * @param name the reference ({@code standard@1.28.0}, {@code acme-blue})
     * @param kind {@code shipped} or {@code tenant}
     * @param version the shipped version, {@code null} for a tenant template
     * @param derivedFrom the reference a tenant template was duplicated from, when it records one
     * @param active whether this template prints when no other is requested
     * @param newer whether this shipped version is newer than the active layout's version
     */
    record Entry(String name, String kind, String version, String derivedFrom, boolean active, boolean newer) {
    }

    /**
     * A document type and the languages it has print templates in.
     *
     * @param entity the document type
     * @param languages the language codes
     */
    record DocumentType(String entity, List<String> languages) {
    }

    /**
     * A resolved template.
     *
     * @param name the template's name
     * @param source the template source
     */
    record Resolved(PrintTemplateName name, String source) {
    }

    /** A validated language folder and the templates it holds, keyed by name, valued by file name. */
    private record Folder(String entity, String language, String path, Map<PrintTemplateName, String> files) {

        String filePath(PrintTemplateName name) {
            String fileName = files.get(name);
            return path + SEPARATOR + (fileName == null ? name.fileName() : fileName);
        }
    }

    /**
     * Lists the document types that have print templates, with their languages.
     *
     * @return the document types, in CMS order
     * @throws IOException on CMS access failure
     */
    List<DocumentType> documentTypes() throws IOException {
        List<DocumentType> types = new ArrayList<>();
        for (String entity : cmsStore.listFolders(TEMPLATES_ROOT)) {
            if (isSegment(entity)) {
                List<String> languages = cmsStore.listFolders(printFolderPath(entity));
                if (!languages.isEmpty()) {
                    types.add(new DocumentType(entity, languages));
                }
            }
        }
        return types;
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
     * @return the entries, empty when the folder holds no template
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException when the document type or language is not a valid name
     */
    List<Entry> list(String entity, String language) throws IOException, PrintTemplateException {
        Folder folder = folder(entity, language);
        Comparator<PrintTemplateName> order = versionOrder(folder);
        Optional<PrintTemplateName> active = active(folder, order);

        Map<PrintTemplateName, String> derivedFrom = new LinkedHashMap<>();
        for (PrintTemplateName tenant : tenantTemplates(folder)) {
            derivedFrom.put(tenant, derivedFrom(readSource(folder, tenant)).orElse(null));
        }
        Optional<PrintTemplateName> baseline = active.flatMap(name -> name.isShipped() ? Optional.of(name)
                : Optional.ofNullable(derivedFrom.get(name))
                          .flatMap(PrintTemplateName::parse)
                          .filter(PrintTemplateName::isShipped));

        List<Entry> entries = new ArrayList<>();
        for (PrintTemplateName shipped : shippedNewestFirst(folder, order)) {
            boolean newer = baseline.filter(base -> base.name()
                                                        .equals(shipped.name()))
                                    .map(base -> order.compare(shipped, base) > 0)
                                    .orElse(false);
            entries.add(new Entry(shipped.reference(), "shipped", shipped.version(), null, active.filter(shipped::equals)
                                                                                                 .isPresent(),
                    newer));
        }
        derivedFrom.forEach(
                (tenant, origin) -> entries.add(new Entry(tenant.reference(), "tenant", null, origin, active.filter(tenant::equals)
                                                                                                            .isPresent(),
                        false)));
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
            name = active(folder, versionOrder(folder)).orElseThrow(() -> new PrintTemplateException(Reason.NOT_FOUND,
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
     * @return the source
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException {@link Reason#NOT_FOUND} when the template is missing
     */
    String read(String entity, String language, String reference) throws IOException, PrintTemplateException {
        Folder folder = folder(entity, language);
        return readSource(folder, existing(folder, reference));
    }

    /**
     * Writes a tenant template's source, creating the template when it does not exist yet.
     *
     * @param entity the document type
     * @param language the language code
     * @param reference the tenant template's name
     * @param source the template source, which must parse
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException {@link Reason#CONFLICT} for a shipped version,
     *         {@link Reason#INVALID} for a malformed name or a source that does not parse
     */
    void write(String entity, String language, String reference, String source) throws IOException, PrintTemplateException {
        Folder folder = folder(entity, language);
        PrintTemplateName name = PrintTemplateName.parse(reference)
                                                  .orElseThrow(() -> invalidName(reference));
        if (name.isShipped()) {
            throw new PrintTemplateException(Reason.CONFLICT, "The print template [" + reference
                    + "] is a shipped version and cannot be changed - duplicate it into a tenant template to customise it");
        }
        requireParses(source);
        cmsStore.write(folder.filePath(name), source.getBytes(StandardCharsets.UTF_8));
        logger.info("Print template [{}] of [{}/{}] written", name.reference(), entity, language);
    }

    /**
     * Copies a shipped version or a tenant template into a new tenant template, recording the shipped
     * version it derives from in a header comment.
     *
     * @param entity the document type
     * @param language the language code
     * @param reference the template to copy
     * @param target the new tenant template's name
     * @return the new template's name
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException {@link Reason#NOT_FOUND} when the source is missing,
     *         {@link Reason#INVALID} for a target that is not a tenant template name,
     *         {@link Reason#CONFLICT} when the target exists
     */
    PrintTemplateName duplicate(String entity, String language, String reference, String target)
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
        String content = readSource(folder, source);
        String origin = source.isShipped() ? source.reference() : derivedFrom(content).orElse(source.reference());
        String body = DERIVED_FROM.matcher(content)
                                  .replaceFirst("");
        String duplicate = "<!-- derived-from: " + origin + " -->\n" + body;
        cmsStore.write(folder.filePath(copy), duplicate.getBytes(StandardCharsets.UTF_8));
        logger.info("Print template [{}] of [{}/{}] duplicated as [{}]", source.reference(), entity, language, copy.reference());
        return copy;
    }

    /**
     * Deletes a tenant template.
     *
     * @param entity the document type
     * @param language the language code
     * @param reference the tenant template's name
     * @throws IOException on CMS access failure
     * @throws PrintTemplateException {@link Reason#NOT_FOUND} when the template is missing,
     *         {@link Reason#CONFLICT} for a shipped version or the active template
     */
    void delete(String entity, String language, String reference) throws IOException, PrintTemplateException {
        Folder folder = folder(entity, language);
        PrintTemplateName name = existing(folder, reference);
        if (name.isShipped()) {
            throw new PrintTemplateException(Reason.CONFLICT,
                    "The print template [" + reference + "] is a shipped version - shipped versions are never deleted");
        }
        if (active(folder, versionOrder(folder)).filter(name::equals)
                                                .isPresent()) {
            throw new PrintTemplateException(Reason.CONFLICT,
                    "The print template [" + reference + "] is the active one - select another template before deleting it");
        }
        cmsStore.delete(folder.filePath(name));
        logger.info("Print template [{}] of [{}/{}] deleted", name.reference(), entity, language);
    }

    /**
     * Brings the current tenant's catalogue up to a shipped template - the seeding side, called by
     * {@link PrintTemplateSynchronizer} once per tenant.
     * <ul>
     * <li>The first time, a legacy {@code <name>.print} (seeded create-if-absent before versions
     * existed) is migrated: unchanged, it becomes this version; customised, it is kept as
     * {@code <name>-custom} and selected, so the tenant's output does not change.</li>
     * <li>An existing {@code <name>@<version>.print} whose bytes drifted is converged back to the
     * shipped bytes - immutability is enforced here, not by CMS permissions.</li>
     * <li>Otherwise the version is added, unless the newest shipped version of the template already
     * carries the same bytes (a release that did not change the layout adds nothing).</li>
     * </ul>
     * Tenant templates are never touched.
     *
     * @param entity the document type
     * @param language the language code
     * @param name the shipped template's name
     * @param version the version it ships as
     * @param content the shipped bytes
     * @throws IOException on CMS access failure
     * @throws SQLException when the migration cannot select the kept customisation
     * @throws PrintTemplateException when the document type or language is not a valid name
     */
    void seed(String entity, String language, String name, String version, byte[] content)
            throws IOException, SQLException, PrintTemplateException {
        PrintTemplateName target = PrintTemplateName.shipped(name, version);
        migrateLegacy(folder(entity, language), target, content);

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
        Optional<PrintTemplateName> newest = shippedNewestFirst(folder, versionOrder(folder)).stream()
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

    private void migrateLegacy(Folder folder, PrintTemplateName target, byte[] content) throws IOException, SQLException {
        PrintTemplateName legacy = PrintTemplateName.tenant(target.name());
        boolean versioned = folder.files()
                                  .keySet()
                                  .stream()
                                  .anyMatch(name -> name.isShipped() && name.name()
                                                                            .equals(target.name()));
        if (!folder.files()
                   .containsKey(legacy)
                || versioned) {
            // Nothing to migrate, or migrated already: a tenant template of the same name is the tenant's own.
            return;
        }
        String legacyPath = folder.filePath(legacy);
        if (Arrays.equals(cmsStore.read(legacyPath)
                                  .orElse(null),
                content)) {
            cmsStore.rename(legacyPath, target.fileName());
            logger.info("Print template [{}] of [{}/{}] migrated to the shipped version [{}]", legacy.reference(), folder.entity(),
                    folder.language(), target.reference());
            return;
        }
        PrintTemplateName custom = PrintTemplateName.tenant(target.name() + CUSTOM_SUFFIX);
        for (int i = 2; folder.files()
                              .containsKey(custom); i++) {
            custom = PrintTemplateName.tenant(target.name() + CUSTOM_SUFFIX + "-" + i);
        }
        // Select before renaming: should the selection fail, the legacy file is still in place and the
        // next attempt migrates it, instead of the tenant silently printing the shipped layout.
        Optional<String> selected = selection.get(folder.entity(), folder.language());
        if (selected.isEmpty() || selected.get()
                                          .equals(legacy.reference())) {
            selection.select(folder.entity(), folder.language(), custom.reference());
        }
        cmsStore.rename(legacyPath, custom.fileName());
        logger.info("Print template [{}] of [{}/{}] differs from the shipped one - kept as the tenant template [{}]", legacy.reference(),
                folder.entity(), folder.language(), custom.reference());
    }

    /**
     * The template that prints when none is requested: the tenant configuration when it names an
     * existing template, else the newest shipped version, else the first tenant template.
     */
    private Optional<PrintTemplateName> active(Folder folder, Comparator<PrintTemplateName> order) {
        Optional<String> configured = selection.get(folder.entity(), folder.language());
        if (configured.isPresent()) {
            Optional<PrintTemplateName> selected = PrintTemplateName.parse(configured.get())
                                                                    .filter(folder.files()::containsKey);
            if (selected.isPresent()) {
                return selected;
            }
            logger.warn("The print template [{}] selected by [{}] does not exist - printing with the default one", configured.get(),
                    PrintTemplateSelection.key(folder.entity(), folder.language()));
        }
        List<PrintTemplateName> shipped = shippedNewestFirst(folder, order);
        if (!shipped.isEmpty()) {
            return Optional.of(shipped.get(0));
        }
        return tenantTemplates(folder).stream()
                                      .findFirst();
    }

    /**
     * The order of shipped versions, oldest first. A release version ranks above any other version and
     * release versions compare by their numbers; versions without one (content hashes) rank by seed
     * order, which is known only for the one the registry ships now - it ranks above the rest.
     */
    private Comparator<PrintTemplateName> versionOrder(Folder folder) {
        Set<String> current = seedService.findShipped(folder.entity(), folder.language())
                                         .stream()
                                         .map(seed -> seed.getTemplateName() + "@" + seed.getVersion())
                                         .collect(Collectors.toSet());
        return (left, right) -> {
            if (left.hasReleaseVersion() != right.hasReleaseVersion()) {
                return left.hasReleaseVersion() ? 1 : -1;
            }
            if (left.hasReleaseVersion()) {
                int compared = left.compareReleaseVersion(right);
                return compared != 0 ? compared
                        : left.reference()
                              .compareTo(right.reference());
            }
            boolean leftCurrent = current.contains(left.reference());
            if (leftCurrent != current.contains(right.reference())) {
                return leftCurrent ? 1 : -1;
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

    private Folder folder(String entity, String language) throws IOException, PrintTemplateException {
        requireSegment("document type", entity);
        requireSegment("language", language);
        String path = printFolderPath(entity) + SEPARATOR + language;
        Map<PrintTemplateName, String> files = new LinkedHashMap<>();
        for (String document : cmsStore.listDocuments(path)) {
            PrintTemplateName.fromFileName(document)
                             .ifPresent(name -> files.putIfAbsent(name, document));
        }
        return new Folder(entity, language, path, files);
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

    /** The shipped version a template records it derives from, when its header names a valid one. */
    private static Optional<String> derivedFrom(String source) {
        Matcher matcher = DERIVED_FROM.matcher(source);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return PrintTemplateName.parse(matcher.group(1))
                                .map(PrintTemplateName::reference);
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

    private static String printFolderPath(String entity) {
        return TEMPLATES_ROOT + SEPARATOR + entity + SEPARATOR + PRINT_SEGMENT;
    }
}
