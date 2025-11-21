/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.synchronizer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.engine.bpm.flowable.domain.Bpmn;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmnService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The Class BpmnSynchronizer.
 */
@Component
@Order(SynchronizersOrder.BPMN)
public class BpmnSynchronizer extends MultitenantBaseSynchronizer<Bpmn, Long> {

    /** The Constant FILE_EXTENSION_BPMN. */
    public static final String FILE_EXTENSION_BPMN = ".bpmn";
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(BpmnSynchronizer.class);
    /** The bpmn service. */
    private final BpmnService bpmnService;
    private final BpmService bpmService;

    /** The synchronization callback. */
    private SynchronizerCallback callback;

    @Autowired
    public BpmnSynchronizer(BpmnService bpmnService, BpmService bpmService) {
        this.bpmnService = bpmnService;
        this.bpmService = bpmService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the artefact
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Bpmn.ARTEFACT_TYPE.equals(type);
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
    protected List<Bpmn> parseImpl(String location, byte[] content) throws ParseException {
        String bpmnContent = new String(content, StandardCharsets.UTF_8);
        Bpmn bpmn = new Bpmn();
        bpmn.setLocation(location);
        bpmn.setName(Paths.get(location)
                          .getFileName()
                          .toString());
        bpmn.setType(Bpmn.ARTEFACT_TYPE);
        bpmn.updateKey();
        bpmn.setContent(bpmnContent);
        try {
            Bpmn maybe = getService().findByKey(bpmn.getKey());
            if (maybe != null) {
                bpmn.setId(maybe.getId());
            }
            bpmn = getService().save(bpmn);
        } catch (Exception e) {
            String errMsg = "Failed to parse bpmn [" + bpmn + "] due to error [" + e.getMessage() + "] with content: " + content;
            throw new ParseException(errMsg, 0);
        }
        return List.of(bpmn);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Bpmn, Long> getService() {
        return bpmnService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Bpmn> retrieve(String location) {
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
    public void setStatus(Bpmn artefact, ArtefactLifecycle lifecycle, String error) {
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
    protected boolean completeImpl(TopologyWrapper<Bpmn> wrapper, ArtefactPhase flow) {

        try {
            Bpmn bpmn = wrapper.getArtefact();

            switch (flow) {
                case CREATE:
                    if (ArtefactLifecycle.NEW.equals(bpmn.getLifecycle())) {
                        deployProcess(bpmn);
                        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                    }
                    break;
                case UPDATE:
                    if (ArtefactLifecycle.MODIFIED.equals(bpmn.getLifecycle())) {
                        deployProcess(bpmn);
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    }
                    if (ArtefactLifecycle.FAILED.equals(bpmn.getLifecycle())) {
                        return false;
                    }
                    break;
                case DELETE:
                    if (ArtefactLifecycle.CREATED.equals(bpmn.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(bpmn.getLifecycle())
                            || ArtefactLifecycle.FAILED.equals(bpmn.getLifecycle())) {
                        removeFromProcessEngine(bpmn);
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    }
                    break;
            }
            return true;
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
            return false;
        }
    }

    private void deployProcess(Bpmn bpmn) {
        try {
            Deployment deployment = bpmService.deployProcess(bpmn.getLocation(), bpmn.getLocation(), bpmn.getContent());
            ProcessDefinition processDefinition = bpmService.getProcessDefinitionByDeploymentId(deployment.getId());

            bpmn.setDeploymentId(processDefinition.getDeploymentId());
            bpmn.setProcessDefinitionId(processDefinition.getId());
            bpmn.setProcessDefinitionKey(processDefinition.getKey());
            bpmn.setProcessDefinitionName(processDefinition.getName());
            bpmn.setProcessDefinitionVersion(processDefinition.getVersion());
            bpmn.setProcessDefinitionTenantId(processDefinition.getTenantId());
            bpmn.setProcessDefinitionCategory(processDefinition.getCategory());
            bpmn.setProcessDefinitionDescription(processDefinition.getDescription());
            logger.info("BPMN [{}] has been deployed : id [{}], key: [{}], tenant [{}]", bpmn, deployment.getId(), deployment.getKey(),
                    deployment.getTenantId());
        } catch (RuntimeException ex) {
            String errorMessage = "Failed to deploy BPMN: " + bpmn;
            throw new IllegalStateException(errorMessage, ex);
        }
    }

    /**
     * Removes the from process engine.
     *
     * @param bpmn the bpmn
     */
    private void removeFromProcessEngine(Bpmn bpmn) {
        List<Deployment> deployments = bpmService.getDeploymentsByKey(bpmn.getLocation());
        for (Deployment deployment : deployments) {
            bpmService.deleteDeployment(deployment.getId());
            logger.info("Deleted deployment: [{}] with key: [{}] for tenant [{}] on the Flowable BPMN Engine.", deployment.getId(),
                    deployment.getKey(), deployment.getTenantId());
        }

    }

    /**
     * Cleanup.
     *
     * @param bpmn the bpmn
     */
    @Override
    public void cleanupImpl(Bpmn bpmn) {
        try {
            removeFromProcessEngine(bpmn);
            getService().delete(bpmn);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, bpmn, ArtefactLifecycle.DELETED, e);
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
     * Gets the file bpmn.
     *
     * @return the file bpmn
     */
    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_BPMN;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Bpmn.ARTEFACT_TYPE;
    }

}
