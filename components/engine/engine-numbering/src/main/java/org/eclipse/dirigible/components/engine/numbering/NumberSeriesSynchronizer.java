/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.numbering;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Synchronizes {@code .numbers} artefacts - a module's declared REQUIREMENT for tenant-level number
 * series, the way {@code .roles} declares roles. File shape:
 *
 * <pre>
 * {"series": [{"name": "Sales Invoice", "prefix": "SI", "size": 10}]}
 * </pre>
 *
 * <p>
 * Per tenant (multitenant execution), a declared series that the tenant does not have yet is
 * PROVISIONED with the declared prefix/size and a zero counter; a series the tenant already has is
 * left completely alone - its counter is live and its shape may have been configured by an
 * administrator, and neither is the artefact's business. Two modules may declare the same series
 * only IDENTICALLY (a shared legal range); a differing re-declaration fails that artefact loudly,
 * naming both declaring locations - it must never silently reshape or fork a tenant's counter.
 *
 * <p>
 * DELETE removes only the declaration record. It never touches {@code DIRIGIBLE_DOCUMENT_NUMBERS}:
 * allocated ranges are business history and survive any module lifecycle.
 */
@Component
@Order(SynchronizersOrder.NUMBER_SERIES)
public class NumberSeriesSynchronizer extends MultitenantBaseSynchronizer<NumberSeriesDeclaration, Long> {

    /** The Constant FILE_EXTENSION_NUMBERS. */
    public static final String FILE_EXTENSION_NUMBERS = ".numbers";

    private static final Logger LOGGER = LoggerFactory.getLogger(NumberSeriesSynchronizer.class);

    /**
     * A plain Gson: the platform's JsonHelper excludes fields without {@code @Expose}, and the wrapper
     * DTO below deliberately stays annotation-free.
     */
    private static final Gson GSON = new Gson();

    private final NumberSeriesDeclarationService declarationService;
    private final DocumentNumberService documentNumberService;

    private SynchronizerCallback callback;

    NumberSeriesSynchronizer(NumberSeriesDeclarationService declarationService, DocumentNumberService documentNumberService) {
        this.declarationService = declarationService;
        this.documentNumberService = documentNumberService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return NumberSeriesDeclaration.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Parses one {@code .numbers} file into one declaration artefact per series entry.
     *
     * @param location the location
     * @param content the content
     * @return the declarations
     * @throws ParseException on malformed JSON or an invalid declaration
     */
    @Override
    protected List<NumberSeriesDeclaration> parseImpl(String location, byte[] content) throws ParseException {
        NumbersFile file;
        try {
            file = GSON.fromJson(new String(content, StandardCharsets.UTF_8), NumbersFile.class);
        } catch (JsonSyntaxException ex) {
            LOGGER.error("Malformed .numbers artefact [{}]", location, ex);
            throw new ParseException("Malformed .numbers artefact [" + location + "]: " + ex.getMessage(), 0);
        }
        if (file == null || file.series == null || file.series.isEmpty()) {
            throw new ParseException("The .numbers artefact [" + location + "] declares no series - expected {\"series\": [...]}", 0);
        }
        List<NumberSeriesDeclaration> declarations = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SeriesEntry entry : file.series) {
            validate(location, entry, seen);
            NumberSeriesDeclaration declaration =
                    new NumberSeriesDeclaration(location, entry.name, entry.prefix == null ? "" : entry.prefix, entry.size);
            declaration.updateKey();
            try {
                NumberSeriesDeclaration existing = getService().findByKey(declaration.getKey());
                if (existing != null) {
                    declaration.setId(existing.getId());
                }
                declarations.add(getService().save(declaration));
            } catch (Exception ex) {
                LOGGER.error("Failed to save number-series declaration [{}] from [{}]", entry.name, location, ex);
                throw new ParseException(ex.getMessage(), 0);
            }
        }
        return declarations;
    }

    private static void validate(String location, SeriesEntry entry, Set<String> seen) throws ParseException {
        if (entry == null || entry.name == null || entry.name.isBlank()) {
            throw new ParseException("The .numbers artefact [" + location + "] contains a series without a name", 0);
        }
        if (!seen.add(entry.name)) {
            throw new ParseException("The .numbers artefact [" + location + "] declares series [" + entry.name + "] more than once", 0);
        }
        try {
            DocumentNumberService.validateShape(entry.prefix, entry.size);
        } catch (IllegalArgumentException ex) {
            throw new ParseException("Series [" + entry.name + "] in [" + location + "]: " + ex.getMessage(), 0);
        }
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<NumberSeriesDeclaration, Long> getService() {
        return declarationService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<NumberSeriesDeclaration> retrieve(String location) {
        return getService().findByLocation(location);
    }

    /**
     * Sets the status.
     *
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param error the error
     */
    @Override
    public void setStatus(NumberSeriesDeclaration artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete - runs once per tenant; provisions the declared series into that tenant's
     * {@code DIRIGIBLE_DOCUMENT_NUMBERS} when absent.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<NumberSeriesDeclaration> wrapper, ArtefactPhase flow) {
        NumberSeriesDeclaration declaration = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(declaration.getLifecycle())) {
                    provision(wrapper, declaration, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                // A FAILED declaration is re-evaluated too: the conflict it failed on may have been
                // resolved by the OTHER module re-declaring. Registering FAILED again (not returning
                // false) keeps the artefact depleted, so the processor does not overwrite the
                // conflict message with generic "undepleted artefact" noise every cycle.
                if (ArtefactLifecycle.MODIFIED.equals(declaration.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(declaration.getLifecycle())) {
                    provision(wrapper, declaration, ArtefactLifecycle.UPDATED);
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(declaration.getLifecycle())
                        || ArtefactLifecycle.UPDATED.equals(declaration.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(declaration.getLifecycle())) {
                    // Only the declaration record goes; the tenant's series row - shape, counter, allocated
                    // history - is a business object and outlives any module.
                    getService().delete(declaration);
                    callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                }
                break;
            case PREPARE:
            case START:
            case STOP:
                break;
        }

        return true;
    }

    private void provision(TopologyWrapper<NumberSeriesDeclaration> wrapper, NumberSeriesDeclaration declaration,
            ArtefactLifecycle lifecycle) {
        Optional<NumberSeriesDeclaration> rival = findConflictingDeclaration(declaration);
        if (rival.isPresent()) {
            NumberSeriesDeclaration other = rival.get();
            String message = "Number series [" + declaration.getName() + "] is declared as prefix [" + declaration.getPrefix() + "] size ["
                    + declaration.getSize() + "] by [" + declaration.getLocation() + "] but as prefix [" + other.getPrefix() + "] size ["
                    + other.getSize() + "] by [" + other.getLocation()
                    + "]. A series is one tenant-level object: align the declarations or use different series names.";
            LOGGER.error(message);
            callback.addError(message);
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, message);
            return;
        }
        try {
            documentNumberService.provision(declaration.getName(), declaration.getPrefix(), declaration.getSize());
            callback.registerState(this, wrapper, lifecycle);
        } catch (Exception ex) {
            LOGGER.error("Failed to provision number series [{}] declared by [{}]", declaration.getName(), declaration.getLocation(), ex);
            callback.addError(ex.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, ex);
        }
    }

    /**
     * Another module's declaration of the same series with a different shape. An identical
     * re-declaration is legal (a shared legal range provisions once, idempotently); a differing one is
     * a conflict this artefact must fail on.
     */
    private Optional<NumberSeriesDeclaration> findConflictingDeclaration(NumberSeriesDeclaration declaration) {
        return declarationService.findAllByName(declaration.getName())
                                 .stream()
                                 .filter(other -> !other.getLocation()
                                                        .equals(declaration.getLocation()))
                                 .filter(other -> !sameShape(other, declaration))
                                 .findFirst();
    }

    private static boolean sameShape(NumberSeriesDeclaration one, NumberSeriesDeclaration two) {
        return one.getSize() == two.getSize() && one.getPrefix()
                                                    .equals(two.getPrefix());
    }

    /**
     * Cleanup - reaps the orphaned declaration record only; never the tenant's series row.
     *
     * @param declaration the declaration
     */
    @Override
    public void cleanupImpl(NumberSeriesDeclaration declaration) {
        try {
            getService().delete(declaration);
        } catch (Exception ex) {
            callback.addError(ex.getMessage());
            callback.registerState(this, declaration, ArtefactLifecycle.DELETED, ex);
        }
    }

    /**
     * Sets the callback.
     *
     * @param callback the new callback
     */
    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_NUMBERS;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return NumberSeriesDeclaration.ARTEFACT_TYPE;
    }

    /** The parsed {@code .numbers} file. */
    private static class NumbersFile {
        List<SeriesEntry> series;
    }

    /** One declared series. */
    private static class SeriesEntry {
        String name;
        String prefix;
        int size;
    }
}
