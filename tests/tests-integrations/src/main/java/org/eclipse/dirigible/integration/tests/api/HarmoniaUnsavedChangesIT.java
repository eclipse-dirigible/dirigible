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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Every generated form guards its unsaved changes (dirigible #7359).
 *
 * <p>
 * The guard is one mechanism in the shared runtime - a snapshot of the save payload plus a veto on
 * the route change - but it only protects a form that actually takes the snapshot and routes its
 * exits through it. A template that does neither renders a page which looks exactly like a guarded
 * one and still drops the edit on Back, which is how all six surfaces came to behave that way at
 * once. The full journey is driven in a browser by {@code DependsOnHarmoniaIT}; this is the sweep
 * that keeps a surface from being left behind (or written back to a direct navigation), including
 * the self-service forms nobody generates in the browser suite.
 */
class HarmoniaUnsavedChangesIT {

    private static final String UI_BASE = "/META-INF/dirigible/template-application-ui-harmonia-java/ui/";

    /** Every generated page component that owns a header form the user can edit. */
    private static final List<String> PAGES = List.of("perspective/manage/form-page.js.template",
            "perspective/document/document-page.js.template", "my/my-form-page.js.template", "my/my-document-page.js.template",
            "partner/partner-form-page.js.template", "partner/partner-document-page.js.template");

    /** Their views - where the marker and the one guard dialog are rendered. */
    private static final List<String> VIEWS = List.of("perspective/manage/form-view.html.template",
            "perspective/document/document-view.html.template", "my/my-form-view.html.template", "my/my-document-view.html.template",
            "partner/partner-form-view.html.template", "partner/partner-document-view.html.template");

    /**
     * Both halves are needed and neither implies the other: a page that never snapshots is never dirty
     * (the guard is silently inert), and a page that snapshots but navigates directly is worse than
     * before - the shared router guard vetoes its own exit and the button does nothing.
     */
    @Test
    void everyEditableFormPageSnapshotsAndGuardsItsExits() throws Exception {
        for (String page : PAGES) {
            String content = read(UI_BASE + page);
            assertTrue(content.contains("this.markPristine()"), page + " never snapshots what it loaded - it can never report an edit");
            assertTrue(content.contains("this.guardExit(") || content.contains("this.confirmLeave("),
                    page + " leaves the page without asking - an unsaved edit is dropped silently");
        }
    }

    /**
     * The dialog is what the guard calls; a page whose view does not render it vetoes the exit and then
     * shows nothing, which traps the user on the form.
     */
    @Test
    void everyFormViewRendersTheGuardDialogAndTheMarker() throws Exception {
        for (String view : VIEWS) {
            String content = read(UI_BASE + view);
            assertTrue(content.contains(":data-open=\"leaveOpen\""), view + " renders no unsaved-changes dialog for the guard to open");
            assertTrue(content.contains("leaveKeepEditing()") && content.contains("leaveDiscard()") && content.contains("leaveSave()"),
                    view + " does not offer all three answers (keep editing / discard / save)");
            assertTrue(content.contains("x-show=\"isDirty()\""), view + " never marks the form as having unsaved changes");
        }
    }

    /**
     * The shared half: the registry the pages register with and the veto itself. Restating either in a
     * template is how a second, drifting mechanism starts.
     */
    @Test
    void theSharedRuntimeCarriesTheGuard() throws Exception {
        String app = read("/META-INF/dirigible/application-core/shell/js/app.js");
        assertTrue(app.contains("App.leaveGuard"), "the shared runtime declares no leave guard");
        assertTrue(app.contains("globalHandlers"),
                "the guard does not hook Pinecone's global handler - an in-app route change is not vetoed");
        assertTrue(app.contains("beforeunload"), "a reload / closed tab is not guarded");

        String base = read("/META-INF/dirigible/application-core/shell/js/components/pages/basePage.js");
        assertTrue(base.contains("markPristine()") && base.contains("isDirty()"), "the shared page mixin carries no dirty state");
    }

    private static String read(String resource) throws IOException {
        try (InputStream content = HarmoniaUnsavedChangesIT.class.getResourceAsStream(resource)) {
            assertNotNull(content, "Missing resource " + resource);
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
