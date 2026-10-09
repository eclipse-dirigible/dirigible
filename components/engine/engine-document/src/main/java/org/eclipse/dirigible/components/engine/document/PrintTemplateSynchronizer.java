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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.document.domain.CmsSeed;
import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateSeed;
import org.eclipse.dirigible.components.engine.document.service.CmsSeedService;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateSeedService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Seeds the print templates a project ships at
 * {@code doc/Templates/<Entity>/Print/<lang>/<name>.print} into every tenant's CMS as immutable
 * versions, {@code Templates/<Entity>/Print/<lang>/<name>@<version>.print}, beside the versions
 * shipped before - see {@link PrintTemplateCatalog#seed} for what a tenant's catalogue receives.
 * The generic {@link CmsSeedSynchronizer} leaves these files alone, so the two never both seed one
 * file.
 *
 * <p>
 * The version is the module's release version - the {@code version} field of the project's
 * {@code project.json}, else of its {@code package.json} - when one declares a valid one, else the
 * first 8 hex digits of the content's SHA-256; {@link PrintTemplateReleases} keeps it immutable
 * (bytes shipped before get their earlier version back, changed bytes under a recorded label get
 * its next revision, {@code <label>_v1}, {@code <label>_v2}, ...). Deleting the file removes the
 * artefact row only; the versions it shipped stay in every tenant's catalogue.
 *
 * <p>
 * Taking a file over from {@link CmsSeedSynchronizer} retires its {@code cms-seed} row, recording
 * the bytes that row last shipped as the previous version - which is how a tenant's unedited copy
 * of the previous release is recognised during the migration.
 *
 * <p>
 * A seed that fails for one tenant stays FAILED until it has succeeded for every tenant: the
 * artefact row is shared by all tenants, and the tenants are completed one after another, so a
 * later tenant's success would otherwise overwrite the earlier tenant's failure and nothing would
 * retry it.
 */
@Component
@Order(SynchronizersOrder.PRINT_TEMPLATE)
class PrintTemplateSynchronizer extends MultitenantBaseSynchronizer<PrintTemplateSeed, Long> {

    private static final Logger logger = LoggerFactory.getLogger(PrintTemplateSynchronizer.class);

    /** The project folder whose contents are mirrored into the CMS. */
    private static final String DOC_FOLDER = "doc";

    private static final String TEMPLATES_SEGMENT = "Templates";
    private static final String PRINT_SEGMENT = "Print";
    /** The project descriptors that may declare the module's release version, in precedence order. */
    private static final List<String> VERSION_DESCRIPTORS = List.of("project.json", "package.json");
    private static final String VERSION_PROPERTY = "version";
    private static final int HASH_VERSION_LENGTH = 8;

    private final PrintTemplateSeedService printTemplateSeedService;
    private final CmsSeedService cmsSeedService;
    private final PrintTemplateCatalog catalog;
    private final PrintTemplateReleases releases;
    private final IRepository repository;
    private final TenantContext tenantContext;

    /**
     * The tenants whose seed of an artefact failed in the current fan-out, by artefact key: a later
     * tenant's success must not hide an earlier tenant's failure. Reset by every {@link #complete},
     * which fans out over the tenants provisioned now - a tenant that failed before is either retried
     * in it or no longer exists, and neither may pin the artefact FAILED.
     */
    private final Map<String, Set<String>> failedTenants = new ConcurrentHashMap<>();

    private SynchronizerCallback callback;

    PrintTemplateSynchronizer(PrintTemplateSeedService printTemplateSeedService, CmsSeedService cmsSeedService,
            PrintTemplateCatalog catalog, PrintTemplateReleases releases, IRepository repository, TenantContext tenantContext) {
        this.printTemplateSeedService = printTemplateSeedService;
        this.cmsSeedService = cmsSeedService;
        this.catalog = catalog;
        this.releases = releases;
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    /**
     * A shipped print template's place:
     * {@code <project>/doc/Templates/<Entity>/Print/<lang>/<file>.print}.
     *
     * @param project the project the file ships in
     * @param entity the document type
     * @param language the language code
     * @param name the template name - the file name without the extension (and without any
     *        {@code @version} the file name carries), sanitised when it is not a valid template name
     */
    record ShippedLocation(String project, String entity, String language, String name) {

        /**
         * Parses a registry-relative location ({@code /<project>/.../doc/Templates/...}) or an absolute
         * file path. The shape is matched on the tail - the last six segments are
         * {@code doc/Templates/<Entity>/Print/<lang>/<file>.print} - so a {@code /doc/} anywhere above it
         * (in the repository root, in the project's name) does not matter; the project is the first
         * segment, which is meaningful for a registry-relative location only (the one {@code parseImpl} is
         * handed), and is where the project's {@code project.json} lives however deep the {@code doc/}
         * folder is.
         *
         * @param location the location
         * @return the shipped location, empty when the file is not a shipped print template
         */
        static Optional<ShippedLocation> of(String location) {
            String[] segments = Arrays.stream(location.replace('\\', '/')
                                                      .split("/"))
                                      .filter(segment -> !segment.isEmpty())
                                      .toArray(String[]::new);
            int doc = segments.length - 6;
            if (doc < 1 || !DOC_FOLDER.equals(segments[doc]) || !TEMPLATES_SEGMENT.equals(segments[doc + 1])
                    || !PRINT_SEGMENT.equals(segments[doc + 3]) || !PrintTemplateCatalog.isSegment(segments[doc + 2])
                    || !PrintTemplateCatalog.isLanguage(segments[doc + 4])) {
                return Optional.empty();
            }
            String fileName = segments[doc + 5];
            if (!fileName.toLowerCase(Locale.ROOT)
                         .endsWith(PrintTemplateName.EXTENSION)) {
                return Optional.empty();
            }
            // A module may ship a file whose name is not a valid template name (Invoice template.print):
            // it is still a print template, and ships under its sanitised name.
            String name = PrintTemplateName.fromFileName(fileName)
                                           .map(PrintTemplateName::name)
                                           .orElseGet(() -> PrintTemplateName.sanitize(PrintTemplateName.templateBase(fileName)
                                                                                                        .orElseThrow()));
            return Optional.of(new ShippedLocation(segments[0], segments[doc + 2], segments[doc + 4], name));
        }
    }

    /**
     * Whether a location is a shipped print template - also how {@link CmsSeedSynchronizer} knows to
     * leave it alone.
     *
     * @param location the registry-relative location or file path
     * @return true for {@code <project>/doc/Templates/<Entity>/Print/<lang>/<file>.print}
     */
    static boolean isShippedTemplate(String location) {
        return ShippedLocation.of(location)
                              .isPresent();
    }

    @Override
    public boolean isAccepted(Path file, BasicFileAttributes attrs) {
        return attrs.isRegularFile() && isShippedTemplate(file.toString());
    }

    @Override
    public boolean isAccepted(String type) {
        return PrintTemplateSeed.ARTEFACT_TYPE.equals(type);
    }

    @Override
    protected List<PrintTemplateSeed> parseImpl(String location, byte[] content) throws ParseException {
        ShippedLocation shipped = ShippedLocation.of(location)
                                                 .orElseThrow(() -> new ParseException("Not a shipped print template: " + location, 0));
        PrintTemplateSeed seed = new PrintTemplateSeed();
        Configuration.configureObject(seed);
        seed.setLocation(location);
        seed.setName(FilenameUtils.getName(location));
        seed.setType(PrintTemplateSeed.ARTEFACT_TYPE);
        seed.setEntityName(shipped.entity());
        seed.setLanguage(shipped.language());
        seed.setTemplateName(shipped.name());
        seed.setContent(content);
        try {
            retireCmsSeeds(location, shipped, content);
            String label = releaseVersion(shipped.project()).orElseGet(() -> PrintTemplateName.shortHash(content));
            seed.setVersion(releases.assign(shipped.entity(), shipped.language(), shipped.name(), label, content));
            seed.updateKey();
            PrintTemplateSeed maybe = getService().findByKey(seed.getKey());
            if (maybe != null) {
                seed.setId(maybe.getId());
            }
            seed = getService().save(seed);
        } catch (Exception e) {
            logger.error("Failed to save shipped print template [{}]", seed, e);
            throw new ParseException(e.getMessage(), 0);
        } finally {
            releases.invalidate(shipped.entity(), shipped.language());
        }
        return List.of(seed);
    }

    /**
     * Retires the {@code cms-seed} rows of a file this synchronizer took over: the generic seed handled
     * every file under {@code doc/} before print templates were versioned, and its rows would otherwise
     * stay behind - a FAILED one forever reported by the health check and the tenant initialization
     * status. The bytes such a row last shipped are recorded as the version before the current one -
     * unless they are the current bytes, which then ship under the module's own version, not a hash.
     */
    private void retireCmsSeeds(String location, ShippedLocation shipped, byte[] current) {
        for (CmsSeed legacy : cmsSeedService.findByLocation(location)) {
            byte[] previous = legacy.getContent();
            if (previous != null && previous.length > 0 && !Arrays.equals(previous, current)) {
                String version = releases.assign(shipped.entity(), shipped.language(), shipped.name(),
                        PrintTemplateName.shortHash(previous), previous);
                logger.info("The print template [{}] was seeded as a plain CMS file before - recorded as version [{}@{}]", location,
                        shipped.name(), version);
            }
            cmsSeedService.delete(legacy);
        }
    }

    /**
     * The {@code version} of the project's {@code project.json}, else of its {@code package.json}, when
     * one declares a valid one.
     */
    private Optional<String> releaseVersion(String project) {
        if (project.isEmpty()) {
            return Optional.empty();
        }
        for (String descriptorName : VERSION_DESCRIPTORS) {
            Optional<String> version = declaredVersion(project, descriptorName);
            if (version.isPresent()) {
                return version;
            }
        }
        return Optional.empty();
    }

    /** The {@code version} one descriptor of the project declares, when it is a valid one. */
    private Optional<String> declaredVersion(String project, String descriptorName) {
        IResource descriptor = repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + project + "/" + descriptorName);
        if (!descriptor.exists()) {
            return Optional.empty();
        }
        try {
            JsonElement json = JsonParser.parseString(new String(descriptor.getContent(), StandardCharsets.UTF_8));
            if (json.isJsonObject()) {
                JsonObject object = json.getAsJsonObject();
                if (object.has(VERSION_PROPERTY) && object.get(VERSION_PROPERTY)
                                                          .isJsonPrimitive()) {
                    String version = object.get(VERSION_PROPERTY)
                                           .getAsString()
                                           .trim();
                    if (PrintTemplateName.isValidVersion(version)) {
                        return Optional.of(version);
                    }
                    logger.warn("The version [{}] in [{}] of project [{}] cannot name a print template version", version, descriptorName,
                            project);
                }
            }
        } catch (RuntimeException e) {
            logger.warn("Cannot read the version from [{}] of project [{}]", descriptorName, project, e);
        }
        return Optional.empty();
    }

    @Override
    public ArtefactService<PrintTemplateSeed, Long> getService() {
        return printTemplateSeedService;
    }

    @Override
    public List<PrintTemplateSeed> retrieve(String location) {
        return getService().findByLocation(location);
    }

    @Override
    public void setStatus(PrintTemplateSeed artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    @Override
    public boolean complete(TopologyWrapper<PrintTemplateSeed> wrapper, ArtefactPhase flow) {
        newFanOut(wrapper.getArtefact()
                         .getKey());
        return super.complete(wrapper, flow);
    }

    /**
     * Forgets the failures of the previous fan-out over the tenants: the one starting now retries them.
     */
    void newFanOut(String key) {
        failedTenants.remove(key);
    }

    @Override
    protected boolean completeImpl(TopologyWrapper<PrintTemplateSeed> wrapper, ArtefactPhase flow) {
        PrintTemplateSeed seed = wrapper.getArtefact();
        ArtefactLifecycle lifecycle = seed.getLifecycle();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(lifecycle)) {
                    return seed(wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(lifecycle)) {
                    return seed(wrapper, ArtefactLifecycle.UPDATED);
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(lifecycle) || ArtefactLifecycle.UPDATED.equals(lifecycle)
                        || ArtefactLifecycle.FAILED.equals(lifecycle)) {
                    // remove the database row only - the shipped versions stay in every tenant's catalogue
                    try {
                        getService().delete(seed);
                        failedTenants.remove(seed.getKey());
                        releases.invalidate(seed.getEntityName(), seed.getLanguage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, e);
                    }
                }
                break;
            case START:
                // A seed that failed for some tenant (the CMS or the tenant configuration not reachable
                // yet) is retried - for every tenant, which is safe: seeding is idempotent.
                if (ArtefactLifecycle.FAILED.equals(lifecycle)) {
                    return seed(wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case PREPARE:
            case STOP:
                break;
        }
        return true;
    }

    /**
     * Seeds the template into the current tenant's catalogue and registers the given lifecycle state -
     * or FAILED while any tenant's seed of it is still failing.
     */
    private boolean seed(TopologyWrapper<PrintTemplateSeed> wrapper, ArtefactLifecycle lifecycle) {
        PrintTemplateSeed seed = wrapper.getArtefact();
        String tenant = tenantContext.getCurrentTenant()
                                     .getId();
        Set<String> failing = failedTenants.computeIfAbsent(seed.getKey(), key -> ConcurrentHashMap.newKeySet());
        try {
            catalog.seed(seed.getEntityName(), seed.getLanguage(), seed.getTemplateName(), seed.getVersion(), seed.getContent());
            failing.remove(tenant);
            if (failing.isEmpty()) {
                failedTenants.remove(seed.getKey(), failing);
                callback.registerState(this, wrapper, lifecycle);
            } else {
                String error = "The seed has not succeeded for the tenant(s) " + failing + " yet";
                callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, error);
            }
            return true;
        } catch (Exception e) {
            failing.add(tenant);
            logger.error("Failed to seed print template [{}@{}] of [{}/{}] for tenant [{}]", seed.getTemplateName(), seed.getVersion(),
                    seed.getEntityName(), seed.getLanguage(), tenant, e);
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
            return false;
        }
    }

    @Override
    public void cleanupImpl(PrintTemplateSeed seed) {
        // never delete from the CMS - shipped versions stay in every tenant's catalogue
        try {
            getService().delete(seed);
            failedTenants.remove(seed.getKey());
            releases.invalidate(seed.getEntityName(), seed.getLanguage());
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, seed, ArtefactLifecycle.DELETED, e);
        }
    }

    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getFileExtension() {
        return PrintTemplateName.EXTENSION;
    }

    @Override
    public String getArtefactType() {
        return PrintTemplateSeed.ARTEFACT_TYPE;
    }
}
