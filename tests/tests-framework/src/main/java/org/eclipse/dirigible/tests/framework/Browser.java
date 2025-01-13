/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework;

public interface Browser {

    void openPath(String path);

    void enterTextInElementByAttributePattern(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text);

    void enterTextInElementById(String elementId, String text);

    void clickOnElementByAttributePatternAndText(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text);

    void assertElementExistsByTypeAndText(HtmlElementType elementType, String text);

    String getPageTitle();

    void clickOnElementByAttributeValue(HtmlElementType htmlElementType, HtmlAttribute htmlAttribute, String attributeValue);

    void doubleClickOnElementContainingText(HtmlElementType htmlElementType, String text);

    void clickOnElementContainingText(HtmlElementType htmlElementType, String text);

    void clickOnElementByAttributePattern(HtmlElementType htmlElementType, HtmlAttribute htmlAttribute, String pattern);

    void assertElementExistsByTypeAndTextPattern(HtmlElementType htmlElementType, String textPattern);

    void reload();

    String createScreenshot();

    void clearCookies();

    void rightClickOnElementById(String id);
}
