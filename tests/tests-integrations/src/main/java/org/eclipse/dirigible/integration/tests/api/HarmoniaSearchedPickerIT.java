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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * A relation picker lists a bounded first page of its target and searches the rest on the server
 * (dirigible #8343). The shared {@code baseFormPage} helpers decide it: {@code pickerRows} asks for
 * one row more than the page, so an answer that fills it means there is more to search, and
 * {@code searchPicker} adds the server's matches for the typed text to the listed options.
 *
 * <p>
 * The helpers are exercised by evaluating the shipped {@code basePage.js} and
 * {@code baseFormPage.js} in a polyglot context over a REST stub; {@code PickerSearchIT} runs the
 * generated controller and checks the generated pages call them.
 */
class HarmoniaSearchedPickerIT {

    private static final String SHELL = "/META-INF/dirigible/application-core/shell/js/";

    @Test
    void aTargetThatFitsThePageIsCompleteAndNeverSearched() {
        try (Context context = load(3)) {
            context.eval("js", "page.pickerRows('Customer', '/customers', { text: 'Name' })"
                    + ".then(rows => { page.optionsCustomer = page.optionsFromCustomer(rows); return page.searchPicker('Customer', 'zz'); })"
                    + ".then(() => { __result = page.pickers.Customer.complete + '|' + page.optionsCustomer.length; });");
            assertEquals("true|3", result(context));
            assertEquals("/customers/search {\"conditions\":[],\"$limit\":5}", requests(context),
                    "the first page asks for one row more than it lists, and a complete list is never searched");
        }
    }

    @Test
    void aLargerTargetListsThePageAndSearchesTheServer() {
        try (Context context = load(10)) {
            context.eval("js",
                    "page.pickerRows('Customer', '/customers', { text: 'Name', conditions: [{ propertyName: 'Active', operator: 'EQ', value: true }] })"
                            + ".then(rows => { page.optionsCustomer = page.optionsFromCustomer(rows); return page.searchPicker('Customer', ' c9 '); })"
                            + ".then(() => { __result = page.pickers.Customer.complete + '|' + page.optionsCustomer.map(o => o.text).join(','); });");
            assertEquals("false|C1,C2,C3,C4,C9", result(context), "the page lists four rows, the match is added beside them");
            assertEquals(
                    "/customers/search {\"conditions\":[{\"propertyName\":\"Active\",\"operator\":\"EQ\",\"value\":true}],\"$limit\":5}\n"
                            + "/customers/search {\"conditions\":[{\"propertyName\":\"Active\",\"operator\":\"EQ\",\"value\":true},"
                            + "{\"propertyName\":\"Name\",\"operator\":\"CONTAINS\",\"value\":\"c9\"}],\"$limit\":4}",
                    requests(context), "a search keeps the picker's own conditions and adds the typed text");
        }
    }

    @Test
    void anEndpointIgnoringTheLimitIsTheWholeSet() {
        try (Context context = load(10)) {
            context.eval("js", "__ignoresLimit = true; page.pickerRows('Customer', '/customers', { text: 'Name' })"
                    + ".then(rows => { __result = page.pickers.Customer.complete + '|' + rows.length; });");
            assertEquals("true|10", result(context));
        }
    }

    @Test
    void aControllerWithoutContainsIsAskedWithLike() {
        try (Context context = load(10)) {
            context.eval("js", "__refusesContains = true; page.pickerRows('Customer', '/customers', { text: 'Name' })"
                    + ".then(rows => { page.optionsCustomer = page.optionsFromCustomer(rows); return page.searchPicker('Customer', 'C1'); })"
                    + ".then(() => { __result = page.optionsCustomer.map(o => o.text).join(','); });");
            assertEquals("C1,C2,C3,C4,C10", result(context));
            assertEquals("{\"propertyName\":\"Name\",\"operator\":\"LIKE\",\"value\":\"%C1%\"}",
                    context.eval("js", "JSON.stringify(__bodies.pop().conditions.pop())")
                           .asString());
        }
    }

    @Test
    void anAnswerSupersededByANewerSearchIsDropped() {
        try (Context context = load(10)) {
            context.eval("js",
                    "page.pickerRows('Customer', '/customers', { text: 'Name' })"
                            + ".then(rows => { page.optionsCustomer = page.optionsFromCustomer(rows);"
                            + "  return Promise.all([page.searchPicker('Customer', 'C9'), page.searchPicker('Customer', 'C8')]); })"
                            + ".then(() => { __result = page.optionsCustomer.map(o => o.text).join(','); });");
            assertEquals("C1,C2,C3,C4,C8", result(context), "C8 was typed before the answer to C9 arrived");
        }
    }

    private static String result(Context context) {
        return context.eval("js", "String(__result)")
                      .asString();
    }

    private static String requests(Context context) {
        return context.eval("js", "__bodies.map((b, i) => __urls[i] + ' ' + JSON.stringify(b)).join('\\n')")
                      .asString();
    }

    /**
     * Evaluate the shipped basePage.js and baseFormPage.js over a REST stub serving {@code count}
     * customers C1..Cn at {@code /customers/search}, with a page size of 4; promises settle between
     * evaluations.
     */
    private static Context load(int count) {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", "var window = this; var __result = null; var __urls = []; var __bodies = [];"
                + " var __ignoresLimit = false; var __refusesContains = false; var __count = " + count + ";"
                + " var localStorage = { getItem: () => null }; window.localStorage = localStorage;" + " var console = { error: () => {} };"
                + " var App = { services: { api: { post: (url, body) => { __urls.push(url); __bodies.push(body);"
                + "   const rows = Array.from({ length: __count }, (_, i) => ({ Id: i + 1, Name: 'C' + (i + 1) }));"
                + "   const contains = (body.conditions || []).find(c => c.operator === 'CONTAINS' || c.operator === 'LIKE');"
                + "   if (contains && contains.operator === 'CONTAINS' && __refusesContains) return Promise.reject({ httpStatus: 400 });"
                + "   const typed = contains ? String(contains.value).replace(/%/g, '').toLowerCase() : null;"
                + "   const found = typed == null ? rows : rows.filter(r => r.Name.toLowerCase().includes(typed));"
                + "   return Promise.resolve(__ignoresLimit ? found : found.slice(0, body.$limit));" + " } } } };");
        for (String script : List.of("components/pages/basePage.js", "components/pages/baseFormPage.js")) {
            try {
                context.eval("js", read(SHELL + script));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to evaluate " + script, ex);
            }
        }
        context.eval("js", "var page = baseFormPage(); page.pickerPageSize = 4;"
                + " page.optionsFromCustomer = function (rows) { return this.sortOptions(rows.map(e => ({ value: e.Id, text: e.Name }))); };");
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = HarmoniaSearchedPickerIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
