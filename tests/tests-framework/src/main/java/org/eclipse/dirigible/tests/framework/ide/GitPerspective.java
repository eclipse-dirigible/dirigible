/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.ide;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.util.SleepUtil;

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
        cloneRepository(repositoryUrl, user, pass, branch, 2000);
    }

    public void cloneRepository(String repositoryUrl, Optional<String> user, Optional<String> pass, Optional<String> branch,
            long waitForCloneMillis) {
        asyncCloneRepository(repositoryUrl, user, pass, branch);

        SleepUtil.sleepMillis(waitForCloneMillis);

        assertClonedRepository(repositoryUrl);
    }

    private void assertClonedRepository(String repositoryUrl) {
        String repositoryName = GitUtil.extractRepoName(repositoryUrl);

        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, repositoryName);
    }

    /**
     * Trigger clone of a repository
     *
     * @param repositoryUrl repo URL
     * @param user user
     * @param pass password
     * @param branch branch
     */
    public void asyncCloneRepository(String repositoryUrl, Optional<String> user, Optional<String> pass, Optional<String> branch) {
        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.TITLE, "Clone");

        browser.enterTextInElementById("curli", repositoryUrl);

        user.ifPresent(u -> browser.enterTextInElementById("cuni", u));
        pass.ifPresent(p -> browser.enterTextInElementById("cpwi", p));
        branch.ifPresent(b -> browser.enterTextInElementById("cbi", b));

        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.LABEL, "Clone");
    }

    private void assertClonedRepositoryNotification() {
        // note that this notification is shown for only a few seconds (~4-5s)
        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER4, "Repository cloned");
    }

    public void asyncCloneRepository(String repositoryUrl) {
        asyncCloneRepository(repositoryUrl, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void asyncCloneRepository(String repositoryUrl, String branch) {
        asyncCloneRepository(repositoryUrl, Optional.empty(), Optional.empty(), Optional.of(branch));
    }

    public void asyncCloneRepository(String repositoryUrl, String user, String password) {
        asyncCloneRepository(repositoryUrl, Optional.of(user), Optional.of(password), Optional.empty());
    }
}
