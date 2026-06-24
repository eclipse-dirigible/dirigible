/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.lsp.java.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.security.RolesAllowed;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.lsp.java.process.JdtLsInstance;
import org.eclipse.dirigible.components.ide.lsp.java.process.JdtLsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

/**
 * HTTP facade over a workspace's JDT Language Server for IDE views that live outside the Monaco
 * editor iframe (Call Hierarchy, Type Hierarchy). Those views cannot reuse the editor's per-iframe
 * LSP WebSocket, so each request here drives the shared {@link JdtLsInstance} directly.
 *
 * <p>
 * URIs travel as the browser's virtual form ({@code file:///workspace/<ws>/...}); the bridge
 * translates to/from the real on-disk paths, so responses come back virtual and are usable by the
 * views as-is.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "java-lsp")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER"})
@ConditionalOnProperty(name = "java.lsp.enabled", havingValue = "true", matchIfMissing = true)
public class JavaLspQueryEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(JavaLspQueryEndpoint.class);

    private static final long REQUEST_TIMEOUT_SECONDS = 60;

    private final JdtLsManager manager;
    private final ObjectMapper mapper = new ObjectMapper();

    public JavaLspQueryEndpoint(JdtLsManager manager) {
        this.manager = manager;
    }

    /** Resolves the call-hierarchy item at a position ({@code textDocument/prepareCallHierarchy}). */
    @PostMapping("/call-hierarchy/prepare")
    public ResponseEntity<JsonNode> prepareCallHierarchy(@RequestBody JsonNode body, Principal principal) {
        return ResponseEntity.ok(request(principal, workspaceOf(body), "textDocument/prepareCallHierarchy", positionParams(body)));
    }

    /** Callers of the given call-hierarchy item ({@code callHierarchy/incomingCalls}). */
    @PostMapping("/call-hierarchy/incoming")
    public ResponseEntity<JsonNode> incomingCalls(@RequestBody JsonNode body, Principal principal) {
        return ResponseEntity.ok(request(principal, workspaceOf(body), "callHierarchy/incomingCalls", itemParams(body)));
    }

    /** Callees of the given call-hierarchy item ({@code callHierarchy/outgoingCalls}). */
    @PostMapping("/call-hierarchy/outgoing")
    public ResponseEntity<JsonNode> outgoingCalls(@RequestBody JsonNode body, Principal principal) {
        return ResponseEntity.ok(request(principal, workspaceOf(body), "callHierarchy/outgoingCalls", itemParams(body)));
    }

    /** Resolves the type-hierarchy item at a position ({@code textDocument/prepareTypeHierarchy}). */
    @PostMapping("/type-hierarchy/prepare")
    public ResponseEntity<JsonNode> prepareTypeHierarchy(@RequestBody JsonNode body, Principal principal) {
        return ResponseEntity.ok(request(principal, workspaceOf(body), "textDocument/prepareTypeHierarchy", positionParams(body)));
    }

    /** Supertypes of the given type-hierarchy item ({@code typeHierarchy/supertypes}). */
    @PostMapping("/type-hierarchy/supertypes")
    public ResponseEntity<JsonNode> supertypes(@RequestBody JsonNode body, Principal principal) {
        return ResponseEntity.ok(request(principal, workspaceOf(body), "typeHierarchy/supertypes", itemParams(body)));
    }

    /** Subtypes of the given type-hierarchy item ({@code typeHierarchy/subtypes}). */
    @PostMapping("/type-hierarchy/subtypes")
    public ResponseEntity<JsonNode> subtypes(@RequestBody JsonNode body, Principal principal) {
        return ResponseEntity.ok(request(principal, workspaceOf(body), "typeHierarchy/subtypes", itemParams(body)));
    }

    // -------------------------------------------------------------------------

    private JsonNode request(Principal principal, String workspace, String method, JsonNode params) {
        if (workspace == null || workspace.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Missing 'workspace'");
        }
        String username = principal != null ? principal.getName() : "anonymous";
        try {
            JdtLsInstance instance = manager.getOrStart(username, workspace);
            instance.ensureInitialized()
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            JsonNode response = instance.sendRequest(method, mapper.writeValueAsString(params))
                                        .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return response.path("result");
        } catch (IllegalStateException notReady) {
            // JDT.LS still starting up or not bundled in this build.
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, notReady.getMessage(), notReady);
        } catch (InterruptedException ie) {
            Thread.currentThread()
                  .interrupt();
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Interrupted", ie);
        } catch (Exception e) {
            logger.warn("[java-lsp] {} failed for workspace {}", method, workspace, e);
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "JDT.LS request failed", e);
        }
    }

    private static String workspaceOf(JsonNode body) {
        return body.path("workspace")
                   .asText(null);
    }

    /** Builds {@code {textDocument:{uri}, position:{line,character}}} from the request body. */
    private ObjectNode positionParams(JsonNode body) {
        ObjectNode params = mapper.createObjectNode();
        params.putObject("textDocument")
              .put("uri", body.path("uri")
                              .asText());
        params.putObject("position")
              .put("line", body.path("line")
                               .asInt())
              .put("character", body.path("character")
                                    .asInt());
        return params;
    }

    /** Builds {@code {item: <hierarchy item>}} from the request body's {@code item} field. */
    private ObjectNode itemParams(JsonNode body) {
        ObjectNode params = mapper.createObjectNode();
        params.set("item", body.path("item"));
        return params;
    }
}
