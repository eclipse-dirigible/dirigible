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

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.engine.document.domain.PrintTemplate;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

/**
 * Synchronizes {@code .print} files into {@link PrintTemplate} artefacts and seeds each template
 * into the tenant-scoped CMS at {@code Templates/<EntityName>/Print/en/standard.print} —
 * create-if-absent only, an existing CMS document is a user customization and is never overwritten.
 * On delete the database row is removed; the CMS copy stays.
 */
@Component
@Order(SynchronizersOrder.PRINT)
class PrintTemplateSynchronizer extends MultitenantBaseSynchronizer<PrintTemplate, Long> {

    /** The file extension of print templates. */
    private static final String FILE_EXTENSION_PRINT = ".print";

    private static final Logger logger = LoggerFactory.getLogger(PrintTemplateSynchronizer.class);

    private final PrintTemplateService printTemplateService;
    private final PrintTemplateCmsStore cmsStore;

    private SynchronizerCallback callback;

    PrintTemplateSynchronizer(PrintTemplateService printTemplateService, PrintTemplateCmsStore cmsStore) {
        this.printTemplateService = printTemplateService;
        this.cmsStore = cmsStore;
    }

    @Override
    public boolean isAccepted(String type) {
        return PrintTemplate.ARTEFACT_TYPE.equals(type);
    }

    @Override
    protected List<PrintTemplate> parseImpl(String location, byte[] content) throws ParseException {
        PrintTemplate printTemplate = new PrintTemplate();
        Configuration.configureObject(printTemplate);
        printTemplate.setLocation(location);
        printTemplate.setName(FilenameUtils.getBaseName(location));
        printTemplate.setType(PrintTemplate.ARTEFACT_TYPE);
        printTemplate.setContent(new String(content, StandardCharsets.UTF_8));
        printTemplate.updateKey();
        try {
            PrintTemplate maybe = getService().findByKey(printTemplate.getKey());
            if (maybe != null) {
                printTemplate.setId(maybe.getId());
            }
            printTemplate = getService().save(printTemplate);
        } catch (Exception e) {
            logger.error("Failed to save print template [{}]", printTemplate, e);
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(printTemplate);
    }

    @Override
    public ArtefactService<PrintTemplate, Long> getService() {
        return printTemplateService;
    }

    @Override
    public List<PrintTemplate> retrieve(String location) {
        return getService().findByLocation(location);
    }

    @Override
    public void setStatus(PrintTemplate artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    @Override
    protected boolean completeImpl(TopologyWrapper<PrintTemplate> wrapper, ArtefactPhase flow) {
        PrintTemplate printTemplate = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(printTemplate.getLifecycle())) {
                    seed(wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(printTemplate.getLifecycle())) {
                    seed(wrapper, ArtefactLifecycle.UPDATED);
                }
                if (ArtefactLifecycle.FAILED.equals(printTemplate.getLifecycle())) {
                    return false;
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(printTemplate.getLifecycle())
                        || ArtefactLifecycle.UPDATED.equals(printTemplate.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(printTemplate.getLifecycle())) {
                    // remove the database row only - the seeded CMS document may carry user customizations
                    try {
                        getService().delete(printTemplate);
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, e);
                    }
                }
                break;
            case START:
            case STOP:
                break;
        }
        return true;
    }

    /**
     * Seeds the template into the CMS (create-if-absent) and registers the given lifecycle state.
     */
    private void seed(TopologyWrapper<PrintTemplate> wrapper, ArtefactLifecycle lifecycle) {
        PrintTemplate printTemplate = wrapper.getArtefact();
        try {
            cmsStore.seedDefaultTemplate(printTemplate.getName(), printTemplate.getContent());
            callback.registerState(this, wrapper, lifecycle);
        } catch (Exception e) {
            logger.error("Failed to seed print template [{}] into the CMS", printTemplate.getKey(), e);
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
        }
    }

    @Override
    public void cleanupImpl(PrintTemplate printTemplate) {
        // never delete from the CMS - the seeded document may carry user customizations
        try {
            getService().delete(printTemplate);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, printTemplate, ArtefactLifecycle.DELETED, e);
        }
    }

    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_PRINT;
    }

    @Override
    public String getArtefactType() {
        return PrintTemplate.ARTEFACT_TYPE;
    }
}
