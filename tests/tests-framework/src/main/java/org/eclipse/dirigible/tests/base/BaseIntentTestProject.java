/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.ide.IntentEditorView;

/**
 * A fixture project whose single source of truth is its {@code app.intent}, configured exactly as a
 * developer would in the browser IDE: copy it into the workspace, open the intent in the Intent
 * Editor and press its Generate, then Publish all from the Workbench.
 *
 * <p>
 * That prologue is the whole reason this class exists. An intent project's model files and code are
 * NOT produced by publishing - the intent has no synchronizer, by design - so a fixture that only
 * copies and publishes deploys an empty application and every assertion after it fails for a reason
 * nowhere near the cause. Driving the editor's own Generate is also what keeps the fixtures honest:
 * they exercise the button a developer presses, not a service call a test invented.
 *
 * <p>
 * A subclass supplies only its {@link #verify()}; override {@link #intentFileName()} for a project
 * whose intent is not the conventional {@code app.intent}.
 */
public abstract class BaseIntentTestProject extends BaseTestProject {

    /** The conventional name of a project's intent file. */
    private static final String DEFAULT_INTENT_FILE = "app.intent";

    private final IntentEditorView intentEditorView;

    protected BaseIntentTestProject(String projectResourcesFolder, IDE ide, ProjectUtil projectUtil, EdmView edmView,
            IntentEditorView intentEditorView) {
        super(projectResourcesFolder, ide, projectUtil, edmView);
        this.intentEditorView = intentEditorView;
    }

    @Override
    public final void configure() {
        copyToWorkspace();
        // Opening the workbench logs into the IDE and binds the browser session - the same prologue
        // BaseTestProject.generateEDM performs before driving its editor view.
        getIde().openWorkbench();
        intentEditorView.generate(getProjectResourcesFolder(), intentFileName());
        publish();
    }

    /**
     * The project-relative name of the intent to generate from.
     *
     * @return {@code app.intent} unless a subclass names another
     */
    protected String intentFileName() {
        return DEFAULT_INTENT_FILE;
    }
}
