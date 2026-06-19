/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.agent;

import jakarta.annotation.security.RolesAllowed;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST surface for the Intent Editor's AI assistant.
 *
 * <p>
 * {@code POST /services/ide/intent/agent} - body is the current intent YAML, the developer's
 * message and the prior transcript; returns {@code {reply, proposedYaml}}. The assistant only
 * proposes a complete YAML for the editor to diff; it never writes to the workspace or runs the
 * generators (the developer accepts the diff, then Saves and Generates as usual). Returns
 * {@code 412} when no API key is configured and {@code 502} when the upstream model call fails.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "intent")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER"})
class IntentAgentEndpoint {

    private final IntentAgentService agentService;

    IntentAgentEndpoint(IntentAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping(value = "/agent", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AgentReply> agent(@RequestBody AgentRequest request) {
        try {
            return ResponseEntity.ok(agentService.chat(request));
        } catch (IntentAgentNotConfiguredException ex) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, ex.getMessage(), ex);
        } catch (IntentAgentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage(), ex);
        }
    }
}
