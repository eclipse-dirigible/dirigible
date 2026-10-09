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

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateVersion;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateSeedService;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * What this instance has shipped of the print templates of a document type and language, from two
 * system tables: the artefact rows ({@code DIRIGIBLE_PRINT_TEMPLATE_SEEDS}) know the version the
 * registry ships <b>now</b>, and the ledger ({@code DIRIGIBLE_PRINT_TEMPLATE_VERSIONS}) every
 * version ever shipped, in shipping order, with the hash of its bytes.
 * <ul>
 * <li>A version label always names the same bytes: changed bytes published under a label already
 * recorded get its next revision, {@code <label>_v1}, {@code <label>_v2}, ... instead, and bytes
 * shipped before get their earlier version back (a rollback adds nothing).</li>
 * <li>Versions are ordered by when they were first shipped, which is the only order a content hash
 * has - and, for release versions published in order, their release order.</li>
 * <li>A tenant copy is recognised as an earlier version by its bytes.</li>
 * </ul>
 * A print reads this through a short-lived cache, so it costs no statement in the steady state; the
 * synchronizer invalidates what it changes, and the expiry covers the other nodes of a cluster.
 */
@Component
class PrintTemplateReleases {

    private static final Logger logger = LoggerFactory.getLogger(PrintTemplateReleases.class);

    /** How long another node's publish may take to reach this node's prints. */
    static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final PrintTemplateSeedService seedService;
    private final PrintTemplateVersionService versionService;
    private final Map<String, Snapshot> cache = new ConcurrentHashMap<>();
    /** Bumped by every invalidation: a snapshot loaded across one is returned but not cached. */
    private final AtomicLong generation = new AtomicLong();

    PrintTemplateReleases(PrintTemplateSeedService seedService, PrintTemplateVersionService versionService) {
        this.seedService = seedService;
        this.versionService = versionService;
    }

    /**
     * The shipped state of a document type and language.
     *
     * @param current the version the registry ships now, per template name
     * @param order the shipping order of every recorded version, by reference ({@code name@version})
     * @param loadedAt when it was read
     */
    record Snapshot(Map<String, String> current, Map<String, Long> order, Instant loadedAt) {

        /**
         * The shipping position of a version.
         *
         * @param name the shipped version
         * @return its position, empty when this instance never recorded it
         */
        Optional<Long> position(PrintTemplateName name) {
            return Optional.ofNullable(order.get(name.reference()));
        }
    }

    /**
     * The shipped state of a document type and language, cached.
     *
     * @param entity the document type
     * @param language the language code
     * @return the snapshot
     */
    Snapshot snapshot(String entity, String language) {
        Instant now = Instant.now();
        String key = key(entity, language);
        Snapshot cached = cache.get(key);
        if (cached != null && cached.loadedAt()
                                    .plus(CACHE_TTL)
                                    .isAfter(now)) {
            return cached;
        }
        // Read outside the map's locks: two concurrent misses read twice, which is cheaper than a
        // print waiting on another key's database round trip.
        long before = generation.get();
        Snapshot loaded = load(entity, language, now);
        if (generation.get() == before) {
            cache.put(key, loaded);
        }
        return loaded;
    }

    /**
     * Assigns the version a shipped template's bytes ship as, recording it in the ledger when it is
     * new.
     *
     * @param entity the document type
     * @param language the language code
     * @param name the template name
     * @param label the version the module declares - its release version, else the content hash
     * @param content the shipped bytes
     * @return the earlier version of the same bytes; else the label, unless it already names other
     *         bytes, in which case its next revision ({@code <label>_v1}, ...)
     */
    String assign(String entity, String language, String name, String label, byte[] content) {
        String hash = PrintTemplateName.contentHash(content);
        Optional<String> known = versionOf(entity, language, name, hash);
        if (known.isPresent()) {
            return known.get();
        }
        OptionalInt revision = latestRevision(entity, language, name, label);
        String version = revision.isPresent() ? PrintTemplateName.revise(label, revision.getAsInt() + 1) : label;
        try {
            versionService.record(new PrintTemplateVersion(entity, language, name, version, hash));
        } catch (DataIntegrityViolationException e) {
            // Another node recorded it first: its record is the answer.
            logger.debug("Print template version [{}@{}] of [{}/{}] was recorded concurrently", name, version, entity, language, e);
            return versionOf(entity, language, name, hash).orElseThrow(() -> e);
        } finally {
            invalidate(entity, language);
        }
        if (!version.equals(label)) {
            logger.warn("The print template [{}] of [{}/{}] changed without a new version [{}] - it ships as [{}@{}]", name, entity,
                    language, label, name, version);
        }
        return version;
    }

    /**
     * The version shipped with exactly these bytes.
     *
     * @param entity the document type
     * @param language the language code
     * @param name the template name
     * @param content the bytes
     * @return the version, empty when this instance never shipped these bytes
     */
    Optional<String> versionOf(String entity, String language, String name, byte[] content) {
        return versionOf(entity, language, name, PrintTemplateName.contentHash(content));
    }

    /**
     * Drops the cached state of a document type and language - after the synchronizer changed what is
     * shipped.
     *
     * @param entity the document type
     * @param language the language code
     */
    void invalidate(String entity, String language) {
        generation.incrementAndGet();
        cache.remove(key(entity, language));
    }

    private Optional<String> versionOf(String entity, String language, String name, String hash) {
        return versionService.findShipped(entity, language)
                             .stream()
                             .filter(version -> version.getTemplateName()
                                                       .equals(name)
                                     && version.getContentHash()
                                               .equals(hash))
                             .map(PrintTemplateVersion::getVersion)
                             .findFirst();
    }

    /**
     * The highest revision of a label recorded for a template: 0 for the label itself, empty when
     * neither it nor a revision of it is recorded.
     */
    private OptionalInt latestRevision(String entity, String language, String name, String label) {
        return versionService.findShipped(entity, language)
                             .stream()
                             .filter(recorded -> recorded.getTemplateName()
                                                         .equals(name))
                             .map(recorded -> PrintTemplateName.revisionOf(recorded.getVersion(), label))
                             .filter(OptionalInt::isPresent)
                             .mapToInt(OptionalInt::getAsInt)
                             .max();
    }

    private Snapshot load(String entity, String language, Instant now) {
        Map<String, String> current = new HashMap<>();
        seedService.findShippedVersions(entity, language)
                   .forEach(shipped -> current.put(shipped.getTemplateName(), shipped.getVersion()));
        Map<String, Long> order = new HashMap<>();
        List<PrintTemplateVersion> recorded = versionService.findShipped(entity, language);
        recorded.forEach(version -> order.put(version.getTemplateName() + "@" + version.getVersion(), version.getId()));
        return new Snapshot(Map.copyOf(current), Map.copyOf(order), now);
    }

    private static String key(String entity, String language) {
        return entity + "/" + language;
    }
}
