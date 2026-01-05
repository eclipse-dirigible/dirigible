/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.wiki.synchronizer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.List;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.engine.wiki.domain.Confluence;
import org.eclipse.dirigible.components.engine.wiki.service.ConfluenceService;
import org.eclipse.dirigible.components.engine.wiki.service.WikiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The Class ConfluenceSynchronizer.
 */
@Component
@Order(SynchronizersOrder.CONFLUENCE)
public class ConfluenceSynchronizer extends BaseSynchronizer<Confluence, Long> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ConfluenceSynchronizer.class);

    /**
     * The Constant FILE_EXTENSION_CONFLUENCE.
     */
    private static final String FILE_EXTENSION_CONFLUENCE = ".confluence";

    /** The confluence service. */
    private final ConfluenceService confluenceService;

    /** The wiki service. */
    private final WikiService wikiService;

    /** The synchronization callback. */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new confluence synchronizer.
     *
     * @param confluenceService the confluence service
     * @param wikiService the wiki service
     */
    @Autowired
    public ConfluenceSynchronizer(ConfluenceService confluenceService, WikiService wikiService) {
        this.confluenceService = confluenceService;
        this.wikiService = wikiService;
    }

    /**
     * Gets the wiki service.
     *
     * @return the wiki service
     */
    public WikiService getWikiService() {
        return wikiService;
    }

    /**
     * Checks if is accepted.
     *
     * @param file the file
     * @param attrs the attrs
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(Path file, BasicFileAttributes attrs) {
        return file.toString()
                   .endsWith(FILE_EXTENSION_CONFLUENCE)
                && file.toString()
                       .indexOf("webjars") == -1
                && file.toString()
                       .indexOf("node_modules") == -1;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the artefact
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Confluence.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Load.
     *
     * @param location the location
     * @param content the content
     * @return the list
     * @throws ParseException the parse exception
     */
    @Override
    protected List<Confluence> parseImpl(String location, byte[] content) throws ParseException {
        String confluenceContent = new String(content, StandardCharsets.UTF_8);
        Confluence wiki = new Confluence();
        Configuration.configureObject(wiki);
        wiki.setLocation(location);
        wiki.setName(Paths.get(location)
                          .getFileName()
                          .toString());
        wiki.setType(Confluence.ARTEFACT_TYPE);
        wiki.updateKey();
        wiki.setContent(confluenceContent);
        try {
            Confluence maybe = getService().findByKey(wiki.getKey());
            if (maybe != null) {
                wiki.setId(maybe.getId());
            }
            wiki = getService().save(wiki);
            wiki.setContent(confluenceContent);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("wiki: {}", wiki);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(wiki);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Confluence, Long> getService() {
        return confluenceService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Confluence> retrieve(String location) {
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
    public void setStatus(Confluence artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<Confluence> wrapper, ArtefactPhase flow) {
        Confluence wiki = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(wiki.getLifecycle())) {
                    wikiService.generateContent(wiki.getLocation(), wiki.getContent());
                    callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(wiki.getLifecycle())) {
                    wikiService.generateContent(wiki.getLocation(), wiki.getContent());
                    callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                }
                if (ArtefactLifecycle.FAILED.equals(wiki.getLifecycle())) {
                    return false;
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(wiki.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(wiki.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(wiki.getLifecycle())) {
                    wikiService.removeGenerated(wiki.getLocation());
                    getService().delete(wiki);
                    callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                }
                break;
            case START:
            case STOP:
        }

        return true;
    }

    /**
     * Cleanup.
     *
     * @param wiki the wiki
     */
    @Override
    public void cleanupImpl(Confluence wiki) {
        try {
            wikiService.removeGenerated(wiki.getLocation());
            getService().delete(wiki);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, wiki, ArtefactLifecycle.DELETED, e);
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
        return FILE_EXTENSION_CONFLUENCE;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Confluence.ARTEFACT_TYPE;
    }

}
