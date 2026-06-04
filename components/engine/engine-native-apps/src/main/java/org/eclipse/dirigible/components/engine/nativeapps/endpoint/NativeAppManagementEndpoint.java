/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.endpoint;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;
import org.eclipse.dirigible.components.engine.nativeapps.registry.NativeAppRegistry;
import org.eclipse.dirigible.components.engine.nativeapps.service.NativeAppService;
import org.eclipse.dirigible.components.engine.nativeapps.synchronizer.NativeAppParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Management REST surface for native apps.
 *
 * <p>
 * Lists all registered apps, returns details of a single app (credentials omitted), and supports
 * start / stop / delete. Used today for diagnostics; the upcoming monitoring UI is the primary
 * client.
 *
 * <p>
 * Access is restricted to operations roles ({@code DEVELOPER}, {@code ADMINISTRATOR},
 * {@code OPERATOR}) via {@code HttpSecurityURIConfigurator.NATIVE_APPS_MANAGEMENT_PATTERNS}.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURED + "native-apps")
class NativeAppManagementEndpoint extends BaseEndpoint {

    private final NativeAppService service;
    private final NativeAppRegistry registry;
    private final NativeAppProcessManager processManager;

    NativeAppManagementEndpoint(NativeAppService service, NativeAppRegistry registry, NativeAppProcessManager processManager) {
        this.service = service;
        this.registry = registry;
        this.processManager = processManager;
    }

    @GetMapping
    public List<NativeAppDto> list() {
        return service.getAll()
                      .stream()
                      .map(app -> NativeAppDto.from(app, processManager))
                      .toList();
    }

    @GetMapping("{id}")
    public NativeAppDto get(@PathVariable Long id) {
        NativeApp app = service.findOptionalById(id)
                               .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Native app [" + id + "] not found."));
        return NativeAppDto.from(app, processManager);
    }

    @PostMapping("{id}/start")
    public ResponseEntity<NativeAppDto> start(@PathVariable Long id) {
        NativeApp app = service.findOptionalById(id)
                               .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Native app [" + id + "] not found."));
        if (app.getKind() != NativeAppKind.LOCAL) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Only LOCAL native apps can be started; [" + app.getName() + "] is " + app.getKind() + ".");
        }
        NativeAppParser.rehydrateConfig(app);
        processManager.startAndAwaitReady(app);
        return ResponseEntity.ok(NativeAppDto.from(app, processManager));
    }

    @PostMapping("{id}/stop")
    public ResponseEntity<NativeAppDto> stop(@PathVariable Long id) {
        NativeApp app = service.findOptionalById(id)
                               .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Native app [" + id + "] not found."));
        if (app.getKind() == NativeAppKind.LOCAL) {
            NativeAppParser.rehydrateConfig(app);
            processManager.stop(app);
        }
        return ResponseEntity.ok(NativeAppDto.from(app, processManager));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        NativeApp app = service.findOptionalById(id)
                               .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Native app [" + id + "] not found."));
        if (app.getKind() == NativeAppKind.LOCAL) {
            NativeAppParser.rehydrateConfig(app);
            processManager.stop(app);
        }
        registry.unregister(app);
        service.delete(app);
        return ResponseEntity.noContent()
                             .build();
    }
}
