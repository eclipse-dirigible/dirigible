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

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.engine.document.domain.CmsSeed;
import org.eclipse.dirigible.components.engine.document.service.CmsSeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds every file placed under a project's {@code doc/} folder into the tenant-scoped CMS,
 * mirroring the path the file has under {@code doc/} (e.g.
 * {@code <project>/doc/Templates/SalesInvoice/Print/en/
 * standard.print} → CMS {@code /Templates/SalesInvoice/Print/en/standard.print}). Seeding is
 * <b>create-if-absent</b> — an existing CMS document is a user customization and is never
 * overwritten; on delete the database row is removed and the CMS copy stays.
 *
 * <p>
 * Matching is <b>folder-scoped</b> (any file under {@code doc/}), not extension-scoped — the folder
 * is a raw CMS staging area, so do not place model artefacts ({@code .csvim}, {@code .bpmn}, …)
 * there expecting their normal engines to run; under {@code doc/} they are treated as opaque CMS
 * content.
 */
@Component
@Order(SynchronizersOrder.CMS_SEED)
class CmsSeedSynchronizer extends MultitenantBaseSynchronizer<CmsSeed, Long> {

    /** The project-relative folder whose contents are mirrored into the CMS. */
    private static final String DOC_SEGMENT = "/doc/";

    private static final Logger logger = LoggerFactory.getLogger(CmsSeedSynchronizer.class);

    private final CmsSeedService cmsSeedService;
    private final CmsStore cmsStore;

    private SynchronizerCallback callback;

    CmsSeedSynchronizer(CmsSeedService cmsSeedService, CmsStore cmsStore) {
        this.cmsSeedService = cmsSeedService;
        this.cmsStore = cmsStore;
    }

    /** Folder-scoped: accept any regular file under a {@code doc/} folder, whatever its extension. */
    @Override
    public boolean isAccepted(Path file, BasicFileAttributes attrs) {
        return attrs.isRegularFile() && file.toString()
                                            .replace('\\', '/')
                                            .contains(DOC_SEGMENT);
    }

    @Override
    public boolean isAccepted(String type) {
        return CmsSeed.ARTEFACT_TYPE.equals(type);
    }

    @Override
    protected List<CmsSeed> parseImpl(String location, byte[] content) throws ParseException {
        CmsSeed cmsSeed = new CmsSeed();
        Configuration.configureObject(cmsSeed);
        cmsSeed.setLocation(location);
        cmsSeed.setName(FilenameUtils.getName(location));
        cmsSeed.setType(CmsSeed.ARTEFACT_TYPE);
        cmsSeed.setCmsPath(toCmsPath(location));
        cmsSeed.setContent(content);
        cmsSeed.updateKey();
        try {
            CmsSeed maybe = getService().findByKey(cmsSeed.getKey());
            if (maybe != null) {
                cmsSeed.setId(maybe.getId());
            }
            cmsSeed = getService().save(cmsSeed);
        } catch (Exception e) {
            logger.error("Failed to save CMS seed [{}]", cmsSeed, e);
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(cmsSeed);
    }

    /** The CMS path a {@code doc/} file mirrors to: its location from the {@code doc/} folder down. */
    private static String toCmsPath(String location) {
        String normalized = location.replace('\\', '/');
        int index = normalized.indexOf(DOC_SEGMENT);
        // Keep the leading slash of the path that follows the doc folder (index + length of "/doc").
        return normalized.substring(index + DOC_SEGMENT.length() - 1);
    }

    @Override
    public ArtefactService<CmsSeed, Long> getService() {
        return cmsSeedService;
    }

    @Override
    public List<CmsSeed> retrieve(String location) {
        return getService().findByLocation(location);
    }

    @Override
    public void setStatus(CmsSeed artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    @Override
    protected boolean completeImpl(TopologyWrapper<CmsSeed> wrapper, ArtefactPhase flow) {
        CmsSeed cmsSeed = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(cmsSeed.getLifecycle())) {
                    seed(wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(cmsSeed.getLifecycle())) {
                    seed(wrapper, ArtefactLifecycle.UPDATED);
                }
                if (ArtefactLifecycle.FAILED.equals(cmsSeed.getLifecycle())) {
                    return false;
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(cmsSeed.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(cmsSeed.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(cmsSeed.getLifecycle())) {
                    // remove the database row only - the seeded CMS document may carry user customizations
                    try {
                        getService().delete(cmsSeed);
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
     * Seeds the file into the CMS (create-if-absent) and registers the given lifecycle state.
     */
    private void seed(TopologyWrapper<CmsSeed> wrapper, ArtefactLifecycle lifecycle) {
        CmsSeed cmsSeed = wrapper.getArtefact();
        try {
            cmsStore.seed(cmsSeed.getCmsPath(), cmsSeed.getContent());
            callback.registerState(this, wrapper, lifecycle);
        } catch (Exception e) {
            logger.error("Failed to seed CMS document [{}]", cmsSeed.getCmsPath(), e);
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
        }
    }

    @Override
    public void cleanupImpl(CmsSeed cmsSeed) {
        // never delete from the CMS - the seeded document may carry user customizations
        try {
            getService().delete(cmsSeed);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, cmsSeed, ArtefactLifecycle.DELETED, e);
        }
    }

    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getFileExtension() {
        // Matching is folder-scoped (see isAccepted); no single extension applies.
        return "";
    }

    @Override
    public String getArtefactType() {
        return CmsSeed.ARTEFACT_TYPE;
    }
}
