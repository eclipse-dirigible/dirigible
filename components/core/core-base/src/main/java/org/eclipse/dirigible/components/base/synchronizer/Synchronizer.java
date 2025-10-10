/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.synchronizer;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.List;

/**
 * The Interface Synchronizer.
 *
 */
public interface Synchronizer<A extends Artefact, ID> {

    /**
     * Gets the service.
     *
     * @return the service
     */
    ArtefactService<A, ID> getService();

    /**
     * Checks if is accepted.
     *
     * @param file the file
     * @param attrs the attrs
     * @return true, if is accepted
     */
    boolean isAccepted(Path file, BasicFileAttributes attrs);

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    boolean isAccepted(String type);

    /**
     * Parse the definition and produce artefacts.
     *
     * @param location the location
     * @param content the content
     * @return the list
     * @throws ParseException the parse exception
     */
    List<A> parse(String location, byte[] content) throws ParseException;

    /**
     * Retrieve all the processed artefacts by the definition location.
     *
     * @param location the location
     * @return the list
     */
    List<A> retrieve(String location);

    /**
     * Retrieve all the processed artefacts by the definition location.
     *
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param message the message
     */
    void setStatus(A artefact, ArtefactLifecycle lifecycle, String message);

    /**
     * Complete.
     *
     * @param wrapper the topology wrapper
     * @param flow the flow
     * @return true, if successful
     */
    boolean complete(TopologyWrapper<A> wrapper, ArtefactPhase flow);

    /**
     * Cleanup.
     *
     * @param artefact the artefact
     */
    void cleanup(A artefact);

    /**
     * Set the callback.
     *
     * @param callback the new callback
     */
    void setCallback(SynchronizerCallback callback);

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    String getFileExtension();

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    String getArtefactType();

    /**
     * Checks if current syncronizer must have multitenant processing.
     *
     * @return true, if the execution must be multitenant
     */
    boolean multitenantExecution();

    /**
     * Perform finalization activities, if any
     */
    default void finishing() {}

}
