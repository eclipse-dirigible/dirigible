/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.impl;

import com.codeborne.selenide.*;
import com.codeborne.selenide.ex.ListSizeMismatch;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;

@Lazy
@Component
class BrowserImpl implements Browser {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserImpl.class);

    private static final String BROWSER = "chrome";
    private static final long SELENIDE_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(15);
    private static final String PATH_SEPARATOR = "/";
    private static final int TOTAL_ELEMENT_SEARCH_TIMEOUT = 60 * 1000;
    private static final long ELEMENT_SEARCH_IN_FRAME_MILLIS = 100;

    static {
        Configuration.timeout = SELENIDE_TIMEOUT_MILLIS;
        Configuration.browser = BROWSER;
        Configuration.browserCapabilities = new ChromeOptions().addArguments("--remote-allow-origins=*");
    }

    private final String protocol;
    private final int port;
    private final String host;

    @Autowired
    BrowserImpl(@LocalServerPort int port) {
        this(ProtocolType.HTTP, "localhost", port);
    }

    BrowserImpl(ProtocolType protocolType, String host, int port) {
        this.protocol = protocolType.protocol;
        this.host = host;
        this.port = port;
    }

    enum ProtocolType {
        HTTP("http"), HTTPS("https");

        private final String protocol;

        ProtocolType(String protocol) {
            this.protocol = protocol;
        }
    }

    @Override
    public void openPath(String path) {
        String url = createAppUrl(path);
        LOGGER.info("Opening path [{}] using URL [{}]", path, url);
        Selenide.open(url);
        maximizeBrowser();
    }

    private String createAppUrl(String path) {
        if (StringUtils.isBlank(path)) {
            return createAppUrlByAbsolutePath("");
        }
        String absolutePath = path.startsWith(PATH_SEPARATOR) ? path : PATH_SEPARATOR + path;
        return createAppUrlByAbsolutePath(absolutePath);
    }

    private String createAppUrlByAbsolutePath(String absolutePath) {
        return protocol + "://" + host + ":" + port + absolutePath;
    }

    private void maximizeBrowser() {
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .maximize();
    }

    @Override
    public void enterTextInElementById(String elementId, String text) {
        enterTextInElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.ID, elementId, text);
    }

    @Override
    public void enterTextInElementByAttributePattern(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text) {
        enterTextInElementByAttributePattern(elementType.getType(), attribute.getAttribute(), pattern, text);
    }

    @Override
    public void enterTextInElementByAttributePattern(String elementType, String attribute, String pattern, String text) {
        By by = constructCssSelectorByTypeAndAttribute(elementType, attribute, pattern);
        handleElementInAllFrames(by, e -> {
            e.click();
            LOGGER.info("Entering [{}] in [{}]", text, e);
            e.setValue(text);
        }, Condition.visible);
    }

    private By constructCssSelectorByTypeAndAttribute(String elementType, String attribute, String attributePattern) {
        String cssSelector = elementType + "[" + attribute + "*='" + attributePattern + "']";
        return Selectors.byCssSelector(cssSelector);
    }

    @Override
    public void handleElementInAllFrames(By by, Consumer<SelenideElement> elementHandler, WebElementCondition... conditions) {
        SelenideElement element = findElementInAllFrames(by, conditions);
        elementHandler.accept(element);
    }

    @Override
    public String createScreenshot() {
        return Selenide.screenshot(UUID.randomUUID()
                                       .toString());
    }

    @Override
    public SelenideElement findElementInAllFrames(By by, WebElementCondition... conditions) {
        long maxWaitTime = System.currentTimeMillis() + TOTAL_ELEMENT_SEARCH_TIMEOUT;

        do {
            Optional<SelenideElement> element = findSingleElementInAllFrames(by, conditions);
            if (element.isEmpty()) {
                LOGGER.debug("Element by [{}] and conditions [{}] was NOT found. Will try again.", by, conditions);
            } else {
                LOGGER.debug("Element by [{}] and conditions [{}] was FOUND.", by, conditions);
                return element.get();
            }
        } while (System.currentTimeMillis() < maxWaitTime);

        LOGGER.debug("Element by [{}] and conditions [{}] was NOT found. Will try last time to reload the page and find it.", by,
                conditions);

        reload();
        SleepUtil.sleepSeconds(3);

        Optional<SelenideElement> element = findSingleElementInAllFrames(by);
        if (element.isPresent()) {
            LOGGER.debug("Element [{}] was FOUND after page reload.", element);
            return element.get();
        } else {
            String screenshot = createScreenshot();
            fail("Element by [" + by + "] and conditions [" + Arrays.toString(conditions)
                    + "] cannot be found in any iframe OR found multiple matches. Check logs for more details. Screenshot path: "
                    + screenshot);
            throw new IllegalStateException("Will not be thrown");
        }
    }

    @Override
    public void reload() {
        Selenide.refresh();
    }

    private Optional<SelenideElement> findSingleElementInAllFrames(By by, WebElementCondition... conditions) {
        Selenide.switchTo()
                .defaultContent();
        LOGGER.debug("Checking element by [{}] and conditions [{}] in the DEFAULT frame...", by, conditions);

        Optional<SelenideElement> element = findSingleElement(by, conditions);
        if (element.isPresent()) {
            LOGGER.debug("Element by selector [{}] and conditions [{}] was FOUND in the DEFAULT frame.", by, conditions);
            return element;
        }

        LOGGER.debug(
                "Element by selector [{}] and conditions [{}] was NOT FOUND in the DEFAULT frame. Will search for it recursively in all iframes...",
                by, conditions);
        return findElementInFramesRecursively(by, conditions);
    }

    private Optional<SelenideElement> findElementInFramesRecursively(By by, WebElementCondition... conditions) {
        By iframeSelector = constructCssSelectorByType(HtmlElementType.IFRAME);
        ElementsCollection iframes = Selenide.$$(iframeSelector);
        LOGGER.debug("Found [{}] iframes", iframes.size());

        for (SelenideElement iframe : iframes) {
            Selenide.switchTo()
                    .frame(iframe);
            LOGGER.debug("Switched to iframe [{}]. Searching for element by [{}] and conditions [{}]...", iframe, by, conditions);

            Optional<SelenideElement> element = findSingleElement(by, conditions);
            if (element.isPresent()) {
                LOGGER.debug("Element with selector [{}] and conditions [{}] was FOUND in iframe [{}].", by, conditions, iframe);
                return element;
            }

            element = findElementInFramesRecursively(by, conditions);
            if (element.isPresent()) {
                return element;
            }

            Selenide.switchTo()
                    .parentFrame();
        }

        LOGGER.debug("Element with selector [{}] and conditions [{}] was NOT FOUND in [{}] iframes.", by, conditions, iframes.size());
        return Optional.empty();
    }

    private By constructCssSelectorByType(HtmlElementType elementType) {
        return constructCssSelectorByType(elementType.getType());
    }

    private Optional<SelenideElement> findSingleElement(By by, WebElementCondition... conditions) {
        LOGGER.debug("Searching for element by [{}] and conditions [{}] in the current frame for [{}] millis", by, conditions,
                ELEMENT_SEARCH_IN_FRAME_MILLIS);

        ElementsCollection foundElements = Selenide.$$(by);

        List<WebElementCondition> allConditions = new ArrayList<>();
        allConditions.add(Condition.exist);
        allConditions.addAll(Arrays.asList(conditions));

        for (WebElementCondition condition : allConditions) {
            foundElements = foundElements.filterBy(condition);
        }

        try {
            foundElements.shouldHave(CollectionCondition.size(1), Duration.ofMillis(ELEMENT_SEARCH_IN_FRAME_MILLIS));

            return Optional.of(foundElements.first());
        } catch (ListSizeMismatch ex) {
            LOGGER.debug(
                    "Element with selector [{}] and conditions [{}] does NOT exist in the current frame or MULTIPLE found. Consider using more precise selector and conditions.\nFound elements: {}.\nCause error message: {}",
                    by, allConditions, describeCollection(by, foundElements, conditions), ex.getMessage());
            return Optional.empty();
        }
    }

    private String describeCollection(By by, ElementsCollection foundElements, WebElementCondition... conditions) {
        try {
            return foundElements.describe();
        } catch (StaleElementReferenceException ex) {
            LOGGER.debug("Element with selector [{}] and conditions [{}] cannot be described", by, conditions, ex);

            return "Failed to describe elements by [" + by + "] and conditions [" + Arrays.toString(conditions) + "] due to error: "
                    + ex.getMessage();
        }
    }

    @Override
    public void clearCookies() {
        Selenide.clearBrowserCookies();
    }

    @Override
    public void rightClickOnElementById(String id) {
        By by = Selectors.byId(id);
        handleElementInAllFrames(by, this::rightClickElement, Condition.visible, Condition.enabled);
    }

    private void rightClickElement(SelenideElement element) {
        element.scrollIntoView(false)
               .contextClick();
    }

    @Override
    public void clickOnElementByAttributePatternAndText(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text) {
        clickOnElementByAttributePatternAndText(elementType.getType(), attribute.getAttribute(), pattern, text);
    }

    @Override
    public void clickOnElementByAttributePatternAndText(String elementType, String attribute, String pattern, String text) {
        By by = constructCssSelectorByTypeAndAttribute(elementType, attribute, pattern);

        WebElementCondition[] conditions = {Condition.visible, Condition.enabled, Condition.exactText(text)};

        SelenideElement element = findElementInAllFrames(by, conditions);
        clickElement(element);
    }

    private static WebElementCondition containsText(String text) {
        return Condition.text(text);
    }

    private void clickElement(SelenideElement element) {
        element.scrollIntoView(false)
               .click();
    }

    @Override
    public void clickOnElementByAttributeValue(HtmlElementType htmlElementType, HtmlAttribute htmlAttribute, String attributeValue) {
        clickOnElementByAttributeValue(htmlElementType.getType(), htmlAttribute.getAttribute(), attributeValue);
    }

    @Override
    public void clickOnElementByAttributeValue(String htmlElementType, String htmlAttribute, String attributeValue) {
        By by = constructCssSelectorByTypeAndAttribute(htmlElementType, htmlAttribute, attributeValue);
        handleElementInAllFrames(by, this::clickElement, Condition.visible, Condition.enabled);
    }

    @Override
    public void doubleClickOnElementContainingText(HtmlElementType elementType, String text) {
        doubleClickOnElementContainingText(elementType.getType(), text);
    }

    @Override
    public void doubleClickOnElementContainingText(String elementType, String text) {
        SelenideElement element = getElementByAttributeAndContainsText(elementType, text);

        element.doubleClick();
    }

    private SelenideElement getElementByAttributeAndContainsText(String elementType, String text) {
        By selector = constructCssSelectorByType(elementType);

        WebElementCondition[] conditions = {Condition.exist, Condition.matchText(Pattern.quote(text))};
        return findElementInAllFrames(selector, conditions);
    }

    private By constructCssSelectorByType(String elementType) {
        return Selectors.byTagName(elementType);
    }

    private SelenideElement getElementByAttributeAndText(String elementType, String text) {
        By selector = constructCssSelectorByType(elementType);
        return findElementInAllFrames(selector, Condition.exist, Condition.exactText(text), Condition.visible, Condition.clickable);
    }

    @Override
    public void clickOnElementContainingText(HtmlElementType elementType, String text) {
        clickOnElementContainingText(elementType.getType(), text);
    }

    @Override
    public void clickOnElementContainingText(String elementType, String text) {
        SelenideElement element = getElementByAttributeAndContainsText(elementType, text);

        element.shouldBe(Condition.visible);

        element.click();
    }

    @Override
    public void clickOnElementWithText(HtmlElementType elementType, String text) {
        clickOnElementWithText(elementType.getType(), text);
    }

    @Override
    public void clickOnElementWithText(String elementType, String text) {
        SelenideElement element = getElementByAttributeAndText(elementType, text);

        element.click();
    }

    @Override
    public void clickOnElementByAttributePattern(HtmlElementType elementType, HtmlAttribute attribute, String pattern) {
        clickOnElementByAttributePattern(elementType.getType(), attribute.getAttribute(), pattern);
    }

    @Override
    public void clickOnElementByAttributePattern(String elementType, String attribute, String pattern) {
        By by = constructCssSelectorByTypeAndAttribute(elementType, attribute, pattern);

        handleElementInAllFrames(by, SelenideElement::click);
    }

    @Override
    public void assertElementExistsByTypeAndContainsText(HtmlElementType htmlElementType, String text) {
        assertElementExistsByTypeAndContainsText(htmlElementType.getType(), text);
    }

    @Override
    public void assertElementExistsByTypeAndContainsText(String elementType, String text) {
        getElementByAttributeAndContainsText(elementType, text);
    }

    @Override
    public void clickOnElementById(String id) {
        By by = Selectors.byId(id);
        handleElementInAllFrames(by, this::clickElement, Condition.visible, Condition.enabled);
    }

    @Override
    public void assertElementExistsByTypeAndText(HtmlElementType elementType, String text) {
        assertElementExistsByTypeAndText(elementType.getType(), text);
    }

    @Override
    public void assertElementExistsByTypeAndText(String elementType, String text) {
        By selector = constructCssSelectorByType(elementType);

        findElementInAllFrames(selector, Condition.exist, Condition.exactText(text));
    }

    @Override
    public String getPageTitle() {
        return Selenide.title();
    }

}
