/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.endpoint;

import org.eclipse.dirigible.components.api.security.ActAsFacade;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The act-as (delegated entry) session control: arm/disarm/report the acting identity behind
 * {@link ActAsFacade}. GET is open to every authenticated user (the shells ask "am I entitled, am I
 * armed" to decide what to render); arming is entitlement-gated in the facade itself and never
 * trusts anything but the server-side session.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "actas")
public class ActAsEndpoint extends BaseEndpoint {

    /**
     * The state the shells render from: may this user arm at all, who is armed right now, and when that
     * arming expires on its own (epoch milliseconds; null when nothing is armed).
     */
    public record ActAsState(boolean entitled, String actingAs, Long expiresAt) {

        static ActAsState current() {
            return new ActAsState(ActAsFacade.isEntitled(), ActAsFacade.actingAs(), ActAsFacade.expiresAt());
        }
    }

    /** The arm request: the acting identity's username (e.g. the employee's e-mail). */
    public record ArmRequest(String username) {
    }

    /**
     * The current session's act-as state.
     *
     * @return entitled + the armed acting identity (null when none) + its expiry
     */
    @GetMapping
    public ResponseEntity<ActAsState> state() {
        return ResponseEntity.ok(ActAsState.current());
    }

    /**
     * Arms the acting identity for the current session.
     *
     * @param request the acting identity's username
     * @return the new state
     */
    @PutMapping
    public ResponseEntity<ActAsState> arm(@RequestBody ArmRequest request) {
        try {
            ActAsFacade.arm(request == null ? null : request.username());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return ResponseEntity.ok(ActAsState.current());
    }

    /**
     * Disarms the acting identity for the current session.
     *
     * @return the new state
     */
    @DeleteMapping
    public ResponseEntity<ActAsState> disarm() {
        ActAsFacade.disarm();
        return ResponseEntity.ok(ActAsState.current());
    }
}
