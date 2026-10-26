/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.aggregation;

import static org.eclipse.dirigible.components.extensions.aggregation.Modules.json;
import static org.eclipse.dirigible.components.extensions.aggregation.Modules.stub;
import static org.eclipse.dirigible.components.extensions.aggregation.Modules.values;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.context.ThreadContextFacade;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.script.ScriptEngineAvailability;
import org.eclipse.dirigible.components.extensions.modules.ExtensionModuleReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The view validation, de-duplication, focus and ordering rules, the views offered only with the
 * JavaScript engine, and the views offered only to the roles they name.
 */
@ExtendWith(MockitoExtension.class)
class ViewsAggregatorTest {

    private static final String POINT = "test-views";

    @Mock
    private ExtensionModuleReader reader;

    @Mock
    private ScriptEngineAvailability scriptEngine;

    @Test
    void skips_a_view_without_id_label_or_target() {
        stub(reader, POINT, json("{'label': 'No id', 'path': '/a'}"), json("{'id': 'no-label', 'path': '/b'}"),
                json("{'id': 'no-target', 'label': 'No target'}"), json("{'id': 'page', 'label': 'Page', 'path': '/c'}"),
                json("{'id': 'action', 'label': 'Action', 'endpoint': '/d'}"));

        assertEquals(List.of("action", "page"), values(aggregate(), "id"));
    }

    @Test
    void keeps_the_first_of_two_views_with_one_id() {
        stub(reader, POINT, json("{'id': 'v', 'label': 'First', 'path': '/a'}"), json("{'id': 'v', 'label': 'Second', 'path': '/b'}"));

        assertEquals(List.of("/a"), values(aggregate(), "path"));
    }

    @Test
    void sets_the_focus_by_region_unless_the_view_says_otherwise() {
        stub(reader, POINT, json("{'id': 'none', 'label': 'a', 'path': '/', 'autoFocusTab': true}"),
                json("{'id': 'center', 'label': 'b', 'path': '/', 'region': 'center'}"),
                json("{'id': 'bottom', 'label': 'c', 'path': '/', 'region': 'bottom', 'autoFocusTab': false}"),
                json("{'id': 'left', 'label': 'd', 'path': '/', 'region': 'left'}"),
                json("{'id': 'right', 'label': 'e', 'path': '/', 'region': 'right', 'autoFocusTab': true}"),
                json("{'id': 'other', 'label': 'f', 'path': '/', 'region': 'top'}"));

        List<Map<String, Object>> views = aggregate();

        // A view without a region never takes the focus, whatever it declares.
        assertEquals(Arrays.asList(false, true, false, false, true, null), values(views, "autoFocusTab"));
    }

    @Test
    void orders_by_order_then_label() {
        stub(reader, POINT, json("{'id': 'b', 'label': 'beta', 'path': '/'}"), json("{'id': 'a', 'label': 'Alpha', 'path': '/'}"),
                json("{'id': 'o', 'label': 'Omega', 'path': '/', 'order': 5}"));

        assertEquals(List.of("o", "a", "b"), values(aggregate(), "id"));
    }

    @Test
    void offers_a_view_requiring_the_script_engine_only_where_it_is_deployed() {
        stub(reader, POINT, json("{'id': 'debugger', 'label': 'Debugger', 'path': '/d', 'requiresScriptEngine': true}"),
                json("{'id': 'console', 'label': 'Console', 'path': '/c'}"));

        when(scriptEngine.isAvailable()).thenReturn(false);
        assertEquals(List.of("console"), values(aggregate(), "id"));

        when(scriptEngine.isAvailable()).thenReturn(true);
        assertEquals(List.of("console", "debugger"), values(aggregate(), "id"));
    }

    @Test
    void aCustomActionWithRolesIsDroppedForACallerWithoutThem() throws Exception {
        stub(reader, POINT, json("{'id': 'escalate', 'label': 'Escalate', 'call': {'path': '/services/e'}, 'roles': ['SupportLead']}"),
                json("{'id': 'retry', 'label': 'Retry', 'call': {'path': '/services/r'}, 'roles': ['Clerk', 'SupportLead']}"),
                json("{'id': 'open', 'label': 'Open', 'call': {'path': '/services/o'}}"));

        assertEquals(List.of("open", "retry"), values(aggregateAs("Clerk"), "id"));
    }

    @Test
    void aCustomActionWithRolesIsKeptForAnAdministrator() throws Exception {
        stub(reader, POINT, json("{'id': 'escalate', 'label': 'Escalate', 'call': {'path': '/services/e'}, 'roles': ['SupportLead']}"));

        assertEquals(List.of("escalate"), values(aggregateAs(Roles.ADMINISTRATOR.getRoleName()), "id"));
    }

    /** Aggregates as a signed-in caller holding exactly the given roles. */
    private List<Map<String, Object>> aggregateAs(String... roles) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        for (String role : roles) {
            lenient().when(request.isUserInRole(role))
                     .thenReturn(true);
        }
        ThreadContextFacade.setUp();
        try {
            ThreadContextFacade.set(HttpServletRequest.class.getCanonicalName(), request);
            return aggregate();
        } finally {
            ThreadContextFacade.tearDown();
        }
    }

    private List<Map<String, Object>> aggregate() {
        return new ViewsAggregator(reader, scriptEngine).aggregate(List.of(POINT));
    }
}
