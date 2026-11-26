/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A {@code hierarchy:} entity's list is a tree-TABLE (dirigible #7613).
 *
 * <p>
 * The list used to render as a label-only tree: no columns, no headers, no sort, no way back to the
 * table - and with no hierarchy filled in yet (a fresh install, a company that has not entered its
 * managers) every record was a childless root, so the page read as a broken list. The list is now
 * the same table in both modes - the tree flattened depth-first, the first column indented under an
 * expand chevron and carrying the entity's icon - the flat table shows while no record has a
 * parent, and a Tree / Table choice is remembered per user.
 *
 * <p>
 * The application is generated from an intent, and the GENERATED list page is evaluated in a
 * polyglot context over the shared {@code basePage.js}, so the tree logic under test is the code
 * the browser runs; the generated view is asserted for the markup that drives it.
 */
class HarmoniaHierarchyTreeTableIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "orgtree";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String SHELL = "/META-INF/dirigible/application-core/shell/js/";

    private static final String INTENT_YAML = """
            name: orgtree
            description: hierarchy fixture - an org chart over a self-relation

            entities:
              - name: Employee
                icon: users
                hierarchy: Manager
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 100 }
                  - { name: title, type: string, length: 100 }
                relations:
                  - { name: Manager, kind: manyToOne, to: Employee }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_hierarchy_list_is_a_tree_table_that_falls_back_to_the_table() throws Exception {
        generateProject();
        String page = contentOf("gen/orgtree/js/components/pages/Employee/EmployeeManageListPage.js");
        String view = contentOf("gen/orgtree/views/Employee/Employee-manage-list.html");

        assertTheViewIsOneTableForBothModes(view);

        try (Context context = load(page)) {
            // No record has a parent yet: a tree with no edges is a table - every record a row, paged.
            set(context, "[e(1, 'Ada', null), e(2, 'Bob', null), e(3, 'Cy', null)]");
            assertEquals("false", eval(context, "String(page.hasHierarchy)"));
            assertEquals("false", eval(context, "String(page.treeVisible)"), "no edges, no tree");
            assertEquals("1:0,2:0,3:0", rows(context), "the flat page, depth 0");
            // The view switch (#7837) presses what the page shows: Table, though the mode is Tree.
            assertEquals("table", eval(context, "page.viewChoice"), "no hierarchy, so the switch reads Table");

            // A filled-in org chart: depth-first, children under their parent, any depth, unpaged.
            set(context, "[e(1, 'Ada', null), e(2, 'Bob', 1), e(3, 'Cy', 1), e(4, 'Dee', 2), e(5, 'Eve', 4),"
                    + " e(6, 'Fay', 5), e(7, 'Gus', 6), e(8, 'Hal', 7), e(9, 'Ivy', 8), e(10, 'Jo', 9), e(11, 'Kim', 10)]");
            assertEquals("true", eval(context, "String(page.treeVisible)"));
            assertEquals("tree", eval(context, "page.viewChoice"));
            assertEquals("1:0,2:1,4:2,5:3,6:4,7:5,8:6,9:7,10:8,11:9,3:1", rows(context),
                    "depth-first, deeper than the old six fixed levels, and not cut to one page of 10");
            assertEquals("true,true,false", eval(context, "[0, 1, 10].map(i => page.visibleRows[i].hasChildren).join(',')"));

            // Siblings follow the table's sort, so the headers work in tree mode too.
            eval(context, "page.cycleSort('Name'); page.cycleSort('Name');");
            assertEquals("1:0,3:1,2:1", rows(context).substring(0, 11), "Name descending: Cy before Bob under Ada");
            eval(context, "page.cycleSort('Name');");

            // Collapsing a node hides its subtree, and only its subtree.
            eval(context, "page.toggleNode(page.items[1]);");
            assertEquals("1:0,2:1,3:1", rows(context));
            eval(context, "page.toggleNode(page.items[1]);");

            // The table on demand, picked through the view switch, which sets the mode and remembers it;
            // and for a search: a match anywhere in the tree must be findable.
            eval(context, "page.viewChoice = 'table';");
            assertEquals("table", eval(context, "page.viewMode"));
            assertEquals("table", eval(context, "Object.values(__storage).join(',')"), "the switch remembers the pick");
            assertEquals("false", eval(context, "String(page.treeVisible)"));
            assertEquals(10, rows(context).split(",").length, "the table is paged again");
            eval(context, "page.viewChoice = 'tree'; page.searchTerm = 'Dee';");
            assertEquals("4:0", rows(context));
            eval(context, "page.searchTerm = '';");

            // A parent cycle in the data renders every record once instead of looping or vanishing.
            set(context, "[e(1, 'Ada', 2), e(2, 'Bob', 1), e(3, 'Cy', null)]");
            assertEquals("3:0,1:0,2:1", rows(context));

            // The choice is remembered per user and entity, and a browser without storage is no error.
            eval(context, "page.rememberViewMode('table');");
            assertTrue(eval(context, "Object.keys(__storage).join(',')").contains("Employee.ada"), "keyed by entity and user");
            eval(context, "var other = __factory(); other.restoreViewMode();");
            assertEquals("table", eval(context, "other.viewMode"));
            eval(context, "__storageBroken = true; other.rememberViewMode('tree'); other.restoreViewMode();");
            assertEquals("table", eval(context, "other.viewMode"), "an unavailable storage keeps the current choice");
        }
    }

    /**
     * Add child (#7724): with a record in front of them, the user creates its child with the parent
     * already set, instead of finding it in the Parent picker - forgetting which put a new subaccount
     * at the top level. The create route carries the hierarchy relation as a query param, which the
     * generated form both prefills and context-locks; inline, the dialog's own flags join that query.
     */
    @Test
    void a_hierarchy_list_adds_a_child_with_its_parent_preset() throws Exception {
        generateProject();
        String page = contentOf("gen/orgtree/js/components/pages/Employee/EmployeeManageListPage.js");
        String view = contentOf("gen/orgtree/views/Employee/Employee-manage-list.html");
        String form = contentOf("gen/orgtree/views/Employee/Employee-form.html");

        assertTrue(view.contains("@click=\"newChild(row)\""), "Add child on every row's menu");
        assertTrue(view.contains("@click=\"newChild(selected)\""), "Add child on the selected record's sheet");
        assertTrue(form.contains("isContextLocked('Manager')"), "the preset parent renders locked on the create form");

        try (Context context = load(page)) {
            set(context, "[e(1, 'Ada', null), e(4, 'Dee', 1)]");
            eval(context, "page.newChild(page.items[1]);");
            assertEquals("/Employee/create?Manager=4", eval(context, "__navigated"), "the create route names the parent");

            // Inline (the Settings detail pane) the form opens in a dialog: one query string, not two,
            // or the form would read the parent as "4?embedded=1".
            eval(context, "window.location.hash = '#/Settings/Employee'; page.newChild(page.items[1]);");
            String dialog = eval(context, "__dialogUrl");
            assertTrue(dialog.endsWith("#/Employee/create?Manager=4&embedded=1&dialog=1"), dialog);
        }
    }

    /**
     * Previous / Next on the record sheet (#8869): the user walks the records the table shows, in its
     * order, without closing the sheet and clicking the next row. In Tree mode that is the visible tree
     * (a collapsed branch is skipped, and the tree is not paged); in Table mode it is every page of the
     * sorted list, and a step past the page's edge turns the page so the highlighted row stays in view.
     * The ends disable the buttons rather than wrapping around.
     */
    @Test
    void the_record_sheet_steps_to_the_previous_and_next_record() throws Exception {
        generateProject();
        String page = contentOf("gen/orgtree/js/components/pages/Employee/EmployeeManageListPage.js");
        String view = contentOf("gen/orgtree/views/Employee/Employee-manage-list.html");

        assertTrue(view.contains("data-record-previous :disabled=\"!hasPreviousRecord\" @click=\"stepRecord(-1)\""),
                "Previous on the sheet's footer");
        assertTrue(view.contains("data-record-next :disabled=\"!hasNextRecord\" @click=\"stepRecord(1)\""), "Next on the sheet's footer");

        try (Context context = load(page)) {
            // Tree mode, depth-first: Ada > (Bob > Dee), Cy.
            set(context, "[e(1, 'Ada', null), e(2, 'Bob', 1), e(3, 'Cy', 1), e(4, 'Dee', 2)]");
            eval(context, "page.selectRow(page.items[0]);");
            assertEquals("1;prev=false;next=true;open=true", sheet(context), "the first record has no previous one");
            eval(context, "page.stepRecord(1); page.stepRecord(1);");
            assertEquals("4;prev=true;next=true;open=true", sheet(context), "Next follows the tree, not the load order");
            eval(context, "page.stepRecord(1);");
            assertEquals("3;prev=true;next=false;open=true", sheet(context), "the last record has no next one");
            eval(context, "page.stepRecord(1);");
            assertEquals("3;prev=true;next=false;open=true", sheet(context), "a step past the end stays put");

            // A collapsed branch is skipped: from Bob, with his subtree folded, Next is Cy.
            eval(context, "page.toggleNode(page.items[1]); page.selectRow(page.items[1]); page.stepRecord(1);");
            assertEquals("3;prev=true;next=false;open=true", sheet(context), "Dee is hidden under Bob, so Next skips him");

            // Table mode is paged by 10: Next from the page's last row turns to page 2, Previous back.
            set(context, "[" + IntStream.rangeClosed(1, 12)
                                        .mapToObj(i -> "e(" + i + ", 'E" + (char) ('A' + i) + "', null)")
                                        .collect(Collectors.joining(","))
                    + "]");
            eval(context, "page.viewChoice = 'table'; page.selectRow(page.items[9]);");
            assertEquals("10;prev=true;next=true;open=true", sheet(context));
            eval(context, "page.stepRecord(1);");
            assertEquals("11;prev=true;next=true;open=true", sheet(context), "Next crosses the page boundary");
            assertEquals("2", eval(context, "String(page.currentPage)"), "the list turns to the page holding the record");
            eval(context, "page.stepRecord(-1);");
            assertEquals("1", eval(context, "String(page.currentPage)"), "and Previous turns back");

            // The order is the table's: sorted by Name descending, Next from the first row is the
            // second-highest name, not the next id.
            eval(context, "page.cycleSort('Name'); page.cycleSort('Name'); page.selectRow(page.sortedItems[0]); page.stepRecord(1);");
            assertEquals("11;prev=true;next=true;open=true", sheet(context), "Next follows the sort");

            // A closed sheet has nothing to step from.
            eval(context, "page.closeDetails();");
            assertEquals("false,false", eval(context, "[page.hasPreviousRecord, page.hasNextRecord].join(',')"));
        }
    }

    /** The open record's id, whether Previous / Next are enabled, and whether the sheet is open. */
    private static String sheet(Context context) {
        return eval(context,
                "page.selected.Id + ';prev=' + page.hasPreviousRecord + ';next=' + page.hasNextRecord" + " + ';open=' + page.detailsOpen");
    }

    /** The markup the page drives: one table, a tree column, the toggle - and no label tree. */
    private static void assertTheViewIsOneTableForBothModes(String view) {
        assertFalse(view.contains("x-h-tree"), "the label-only tree is gone");
        assertTrue(view.contains("x-for=\"{ row, depth, hasChildren } in visibleRows\""), "one table body for both modes");
        assertTrue(view.contains("x-h-button-group x-model=\"viewChoice\"") && view.contains("x-h-button-group-radio=\"'tree'\"")
                && view.contains("x-h-button-group-radio=\"'table'\""), "the Tree / Table toggle");
        assertTrue(view.contains("data-lucide=\"users\""), "the node icon is the entity's icon, not a generic file");
        // The chevron is the row's own control: the row ignores it rather than the chevron stopping the
        // click, which kept an open menu from closing (#8259).
        assertTrue(view.contains("data-row-control @click=\"toggleNode(row)\""), "the chevron expands and collapses");
        for (String line : view.split("\n")) {
            assertFalse(line.contains("${") || line.contains("$property") || line.contains("$hierarchy"),
                    "an unresolved template reference survived: " + line);
        }
    }

    /** {@code id:depth} of every row the table body renders, in order. */
    private static String rows(Context context) {
        return eval(context, "page.visibleRows.map(r => r.row.Id + ':' + r.depth).join(',')");
    }

    private static void set(Context context, String items) {
        eval(context, "page.items = " + items + "; page.collapsed = {}; page.currentPage = 1;");
    }

    private static String eval(Context context, String expression) {
        return context.eval("js", expression)
                      .toString();
    }

    private static Context load(String page) throws Exception {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js",
                """
                        var window = this;
                        var __documentListeners = {};
                        var document = { addEventListener: function (type, fn) { (__documentListeners[type] = __documentListeners[type] || []).push(fn); } };
                        window.addEventListener = function () {};
                        window.removeEventListener = function () {};
                        var __storage = {};
                        var __storageBroken = false;
                        window.localStorage = {
                          getItem: function (k) { if (__storageBroken) { throw new Error('denied'); } return k in __storage ? __storage[k] : null; },
                          setItem: function (k, v) { if (__storageBroken) { throw new Error('denied'); } __storage[k] = String(v); }
                        };
                        window.location = { hash: '#/Employee', pathname: '/index.html' };
                        var __navigated = null;
                        window.PineconeRouter = { context: {}, navigate: function (route) { __navigated = route; } };
                        var __dialogUrl = null;
                        var __stores = { currentUser: { name: 'ada' }, related: { create: function (url) { __dialogUrl = url; } } };
                        var __factory = null;
                        var Alpine = {
                          store: function (name, value) { if (value !== undefined) { __stores[name] = value; } return __stores[name]; },
                          data: function (name, factory) { __factory = factory; }
                        };
                        var console = { log: function () {}, warn: function () {}, error: function () {} };
                        var App = { config: {}, services: {} };
                        function T(key, fallback) { return fallback; }
                        function e(id, name, manager) { return { Id: id, Name: name, Title: '', Manager: manager }; }
                        """);
        // The cells - and the search, which matches what they show (#8561) - format through the shipped
        // format.js, as in the browser.
        context.eval("js", read(SHELL + "services/format.js"));
        context.eval("js", read(SHELL + "components/pages/basePage.js"));
        context.eval("js", page);
        context.eval("js", "__documentListeners['alpine:init'].forEach(function (fn) { fn(); }); var page = __factory();");
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = HarmoniaHierarchyTreeTableIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void generateProject() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    @AfterEach
    void cleanup() {
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
    }
}
