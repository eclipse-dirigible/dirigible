/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.endpoint;

import java.util.List;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.engine.java.runtime.ClientBeanResolver;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.eclipse.dirigible.sdk.bpm.ProcessTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.annotation.security.RolesAllowed;

/**
 * Re-runs a generated process trigger for one record - the repair for a record whose stamped
 * process instance the engine no longer has (#7599).
 *
 * <p>
 * {@code POST /services/bpm/bpm-processes/restart?process=<definition key>&id=<record id>} locates
 * the client bean implementing {@link ProcessTrigger} for the process and calls its
 * {@link ProcessTrigger#restart(String)}: the same variables and the same stamping the create path
 * performs, so the two cannot drift. It is refused while the stamped instance is still running - a
 * restart then would be a duplicate approval, not a repair. Lives under the monitoring surface of
 * {@code /services/bpm/bpm-processes/**} (operator, developer, administrator), next to the retry
 * and skip actions.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_BPM + "/bpm-processes")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER", "OPERATOR"})
public class ProcessTriggerRestartEndpoint extends BaseEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTriggerRestartEndpoint.class);

    private final ClientBeansHolder clientBeans;

    public ProcessTriggerRestartEndpoint(ClientBeansHolder clientBeans) {
        this.clientBeans = clientBeans;
    }

    /**
     * Restarts the process for the record.
     *
     * @param process the process definition key
     * @param id the record's primary key
     * @return the started process-instance id
     */
    @PostMapping("/restart")
    public ResponseEntity<String> restart(@RequestParam("process") String process, @RequestParam("id") String id) {
        ProcessTrigger trigger = triggerOf(process);
        LOGGER.info("Restarting process [{}] for {} [{}] on request", process, trigger.entity(), id);
        try {
            String processInstanceId = trigger.restart(id);
            LOGGER.info("Restarted process [{}] for {} [{}] as instance [{}]", process, trigger.entity(), id, processInstanceId);
            return ResponseEntity.ok(processInstanceId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    private ProcessTrigger triggerOf(String process) {
        ClientBeanResolver resolver = clientBeans.current();
        List<ProcessTrigger> triggers = resolver == null ? List.of() : resolver.getAll(ProcessTrigger.class);
        return triggers.stream()
                       .filter(trigger -> process.equals(trigger.process()))
                       .findFirst()
                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                               "No process trigger starts [" + process + "] - the process is not started by a record event"));
    }
}
