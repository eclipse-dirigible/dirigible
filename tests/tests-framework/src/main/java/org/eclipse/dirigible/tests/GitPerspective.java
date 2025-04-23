/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.util.SleepUtil;

import java.util.Optional;

public class GitPerspective {
    private final Browser browser;

    public GitPerspective(Browser browser) {
        this.browser = browser;
    }

    public void cloneRepository(String repositoryUrl) {
        cloneRepository(repositoryUrl, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void cloneRepository(String repositoryUrl, Optional<String> user, Optional<String> pass, Optional<String> branch) {
        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.TITLE, "Clone");

        browser.enterTextInElementById("curli", repositoryUrl);

        user.ifPresent(u -> browser.enterTextInElementById("cuni", u));
        pass.ifPresent(p -> browser.enterTextInElementById("cpwi", p));
        branch.ifPresent(b -> browser.enterTextInElementById("cbi", b));

        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.LABEL, "Clone");

        SleepUtil.sleepMillis(2000);

        assertClonedRepository();
    }

    private void assertClonedRepository() {
        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER4, "Repository cloned");
    }
}

