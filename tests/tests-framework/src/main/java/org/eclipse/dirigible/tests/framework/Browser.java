/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.By;

import java.util.Optional;
import java.util.function.Consumer;

public interface Browser {

    void openPath(String path);

    String getPageTitle();

    void enterTextInElementByAttributePattern(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text);

    void enterTextInElementByAttributePattern(String elementType, String attribute, String pattern, String text);

    void enterTextInElementById(String elementId, String text);

    void assertElementExistsByTypeAndText(HtmlElementType elementType, String text);

    void assertElementExistsByTypeAndText(String elementType, String text);

    void assertElementExistsByTypeAndContainsText(HtmlElementType htmlElementType, String text);

    void assertElementExistsByTypeAndContainsText(String htmlElementType, String text);

    void assertElementDoesNotExistsByTypeAndContainsText(HtmlElementType htmlElementType, String text);

    void assertElementDoesNotExistsByTypeAndContainsText(String htmlElementType, String text);

    void clickOnElementById(String id);

    void clickOnElementByAttributeValue(HtmlElementType htmlElementType, HtmlAttribute htmlAttribute, String attributeValue);

    void clickOnElementByAttributeValue(String htmlElementType, String htmlAttribute, String attributeValue);

    void clickOnElementByAttributePatternAndText(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text);

    void assertElementExistByAttributePatternAndText(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text);

    void clickOnElementByAttributePatternAndText(String elementType, String attribute, String pattern, String text);

    void clickOnElementContainingText(HtmlElementType htmlElementType, String text);

    void clickOnElementContainingText(String htmlElementType, String text);

    void clickOnElementWithText(HtmlElementType htmlElementType, String text);

    void clickOnElementWithText(String htmlElementType, String text);

    void clickOnElementByAttributePattern(HtmlElementType htmlElementType, HtmlAttribute htmlAttribute, String pattern);

    void clickOnElementByAttributePattern(String htmlElementType, String htmlAttribute, String pattern);

    void doubleClickOnElementContainingText(HtmlElementType htmlElementType, String text);

    void doubleClickOnElementContainingText(String htmlElementType, String text);

    void rightClickOnElementById(String id);

    void rightClickOnElementContainingText(HtmlElementType htmlElementType, String text);

    void rightClickOnElementContainingText(String htmlElementType, String text);

    void reload();

    String createScreenshot();

    void clearCookies();

    SelenideElement findElementInAllFrames(By by, WebElementCondition... conditions);

    Optional<SelenideElement> findOptionalElementInAllFrames(By by, WebElementCondition... conditions);

    Optional<SelenideElement> findOptionalElementInAllFrames(By by, long totalTimeoutSeconds, WebElementCondition... conditions);

    void handleElementInAllFrames(By by, Consumer<SelenideElement> elementHandler, WebElementCondition... conditions);

}
