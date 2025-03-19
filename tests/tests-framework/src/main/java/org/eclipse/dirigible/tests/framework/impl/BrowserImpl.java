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
import org.eclipse.dirigible.tests.IntegrationTest;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.util.FileUtil;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Lazy
@Component
class BrowserImpl implements Browser {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserImpl.class);

    private static final String PATH_SEPARATOR = "/";

    private static final int FRAME_SEARCH_TOTAL_SECONDS = 45;
    private static final int ELEMENT_EXISTENCE_SEARCH_TIME_SECONDS = 5;
    private static final int ELEMENT_SEARCH_IN_FRAME_MILLIS = 50;

    static {
        configureSelenide();
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

    private static void configureSelenide() {
        Configuration.timeout = TimeUnit.SECONDS.toMillis(15);
        Configuration.headless = IntegrationTest.isHeadlessExecution();
        Configuration.browser = "chrome";
        Configuration.browserCapabilities = new ChromeOptions().addArguments("--remote-allow-origins=*");
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
        handleElementInAllFrames(by, enterTextInElement(text), Condition.visible);
    }

    private Consumer<SelenideElement> enterTextInElement(String text) {
        return element -> {
            element.click();
            LOGGER.debug("Entering [{}] in [{}]", text, element);
            element.setValue(text);
        };
    }

    @Override
    public By constructCssSelectorByTypeAndAttribute(HtmlElementType elementType, HtmlAttribute attribute, String attributePattern) {
        return constructCssSelectorByTypeAndAttribute(elementType.getType(), attribute.getAttribute(), attributePattern);
    }

    @Override
    public By constructCssSelectorByTypeAndAttribute(String elementType, String attribute, String attributePattern) {
        String cssSelector = elementType + "[" + attribute + "*='" + attributePattern + "']";
        return Selectors.byCssSelector(cssSelector);
    }

    public By constructCssSelectorByTypeAndExactAttribute(String elementType, String attribute, String value) {
        String cssSelector = elementType + "[" + attribute + "='" + value + "']";
        return Selectors.byCssSelector(cssSelector);
    }

    public By constructCssSelectorByTypeAndExactAttribute(HtmlElementType elementType, HtmlAttribute attribute, String value) {
        return constructCssSelectorByTypeAndExactAttribute(elementType.getType(), attribute.getAttribute(), value);
    }

    @Override
    public void handleElementInAllFrames(By by, Consumer<SelenideElement> elementHandler, WebElementCondition... conditions) {
        SelenideElement element = findElementInAllFrames(by, conditions);
        elementHandler.accept(element);
    }

    @Override
    public SelenideElement findElementInAllFrames(By by, WebElementCondition... conditions) {
        Optional<SelenideElement> element = findOptionalElementInAllFrames(by, FRAME_SEARCH_TOTAL_SECONDS, conditions);
        if (element.isEmpty()) {
            failWithScreenshot("Element by [" + by + "] and conditions [" + Arrays.toString(conditions)
                    + "] cannot be found in any iframe OR found multiple matches. Check logs for more details.");
        }
        return element.get();
    }

    @Override
    public void close() {
        Selenide.closeWebDriver();
    }

    @Override
    public void assertAlertWithMessage(String message) {
        String alertMessage = getAlertMessage();
        assertThat(alertMessage).contains(message);
    }

    @Override
    public String getAlertMessage() {
        Alert alert = Selenide.switchTo()
                              .alert();
        return alert.getText();
    }

    @Override
    public void switchToLatestTab() {
        Selenide.switchTo()
                .window(Selenide.webdriver()
                                .object()
                                .getWindowHandles()
                                .size()
                        - 1);
    }

    @Override
    public void pressEnter() {
        pressKey(Keys.ENTER);
    }

    @Override
    public void pressKey(Keys key) {
        Selenide.actions()
                .sendKeys(key)
                .perform();
    }

    @Override
    public void pressMultipleKeys(Keys modifier, CharSequence charSequence) {
        Selenide.actions()
                .keyDown(modifier)
                .sendKeys(charSequence)
                .keyUp(modifier)
                .perform();
    }

    @Override
    public void type(String text) {
        Selenide.actions()
                .sendKeys(text)
                .perform();
    }

    @Override
    public void closeWindow() {
        Selenide.closeWindow();
    }

    private void failWithScreenshot(String message) {
        String screenshot = createScreenshot();
        fail(message + "\nScreenshot path: " + screenshot);
    }

    @Override
    public String createScreenshot() {
        return Selenide.screenshot(UUID.randomUUID()
                                       .toString());
    }

    @Override
    public Optional<SelenideElement> findOptionalElementInAllFrames(By by, long totalSearchTimeoutSeconds,
            WebElementCondition... conditions) {
        long maxWaitTime = System.currentTimeMillis() + (totalSearchTimeoutSeconds * 1000);

        do {
            Optional<SelenideElement> element = findSingleElementInAllFrames(by, conditions);
            if (element.isEmpty()) {
                LOGGER.debug("Element by [{}] and conditions [{}] was NOT found. Will try again.", by, conditions);
            } else {
                LOGGER.debug("Element by [{}] and conditions [{}] was FOUND.", by, conditions);
                return element;
            }
        } while (System.currentTimeMillis() < maxWaitTime);

        LOGGER.debug("Element by [{}] and conditions [{}] was NOT found. Will try last time to reload the page and find it.", by,
                conditions);

        reload();
        SleepUtil.sleepSeconds(5);

        Optional<SelenideElement> element = findSingleElementInAllFrames(by);
        if (element.isPresent()) {
            LOGGER.debug("Element [{}] was FOUND after page reload.", element);
            return element;
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void reload() {
        Selenide.refresh();
    }

    @Override
    public Optional<SelenideElement> findOptionalElementInAllFrames(By by, WebElementCondition... conditions) {
        return findOptionalElementInAllFrames(by, FRAME_SEARCH_TOTAL_SECONDS, conditions);
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

        if (iframes.size() == 0) {
            LOGGER.debug("Found zero iframes");
            return Optional.empty();
        }

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
            Serializable matchedElements = ex.getActual()
                                             .getValue();
            if (matchedElements instanceof Integer matchedElementsCount) {
                if (matchedElementsCount == 0) {
                    LOGGER.debug(
                            "Found ZERO elements with selector [{}] and conditions [{}] but expected ONLY ONE. Consider using more precise selector and conditions.\nFound elements: {}.\nCause error message: {}",
                            by, allConditions, describeCollection(by, foundElements, conditions), ex.getMessage());

                    FileUtil.deleteFile(ex.getScreenshot()
                                          .getImage());
                    FileUtil.deleteFile(ex.getScreenshot()
                                          .getSource());
                }
                if (matchedElementsCount > 1) {
                    LOGGER.error(
                            "Found MORE THAN ONE elements [{}] with selector [{}] and conditions [{}] but expected ONLY ONE. Consider using more precise selector and conditions.\nFound elements: {}.\nCause error message: {}",
                            matchedElementsCount, by, allConditions, describeCollection(by, foundElements, conditions), ex.getMessage());
                }
            }

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

    @Override
    public void rightClickOnElementByText(HtmlElementType elementType, String text) {
        rightClickOnElementByText(elementType.getType(), text);
    }

    @Override
    public void rightClickOnElementByText(String elementType, String text) {
        SelenideElement element = getElementByAttributeAndExactText(elementType, text);

        element.shouldBe(Condition.visible);
        element.shouldBe(Condition.exactText(text));

        rightClickElement(element);
    }

    private void rightClickElement(SelenideElement element) {
        element.scrollIntoView(true)
               .contextClick();
    }

    @Override
    public void rightClickOnElementContainingText(HtmlElementType elementType, String text) {
        rightClickOnElementContainingText(elementType.getType(), text);
    }

    @Override
    public void rightClickOnElementContainingText(String elementType, String text) {
        SelenideElement element = getElementByAttributeAndContainsText(elementType, text);
        element.shouldBe(Condition.visible);
        rightClickElement(element);
    }

    @Override
    public void clickOnElementByAttributePatternAndText(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text) {
        clickOnElementByAttributePatternAndText(elementType.getType(), attribute.getAttribute(), pattern, text);
    }

    @Override
    public void assertElementExistByAttributePatternAndText(HtmlElementType elementType, HtmlAttribute attribute, String pattern,
            String text) {
        By by = constructCssSelectorByTypeAndAttribute(elementType.getType(), attribute.getAttribute(), pattern);

        findElementInAllFrames(by, Condition.visible, Condition.enabled, Condition.exactText(text), Condition.exist);
    }

    @Override
    public void clickOnElementByAttributePatternAndText(String elementType, String attribute, String pattern, String text) {
        By by = constructCssSelectorByTypeAndAttribute(elementType, attribute, pattern);

        SelenideElement element = findElementInAllFrames(by, Condition.visible, Condition.enabled, Condition.exactText(text));
        clickElement(element);
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
        element.shouldBe(Condition.visible);
        element.doubleClick();
    }

    private SelenideElement getElementByAttributeAndExactText(String elementType, String text) {
        By selector = constructCssSelectorByType(elementType);

        return findElementInAllFrames(selector, Condition.exist, Condition.exactText(text));
    }

    private SelenideElement getElementByAttributeAndContainsText(String elementType, String text) {
        By selector = constructCssSelectorByType(elementType);

        return findElementInAllFrames(selector, Condition.exist, Condition.matchText(Pattern.quote(text)));
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
    public void clickOnElementWithExactClass(HtmlElementType elementType, String className) {
        By by = constructCssSelectorByTypeAndExactAttribute(elementType, HtmlAttribute.CLASS, className);
        handleElementInAllFrames(by, this::clickElement, Condition.visible, Condition.enabled);
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
    public void clickElementByAttributes(HtmlElementType elementType, Map<HtmlAttribute, String> attributes) {
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("Attributes map cannot be empty");
        }

        StringBuilder cssSelector = new StringBuilder(elementType.getType());
        attributes.forEach((attribute, value) -> {
            cssSelector.append("[")
                       .append(attribute.getAttribute())
                       .append("='")
                       .append(value)
                       .append("']");
        });

        By by = Selectors.byCssSelector(cssSelector.toString());
        handleElementInAllFrames(by, SelenideElement::click, Condition.visible);
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
    public void assertElementDoesNotExistsByTypeAndContainsText(HtmlElementType htmlElementType, String text) {
        assertElementDoesNotExistsByTypeAndContainsText(htmlElementType.getType(), text);
    }

    @Override
    public void assertElementDoesNotExistsByTypeAndContainsText(String elementType, String text) {
        By by = constructCssSelectorByType(elementType);

        Optional<SelenideElement> element = findOptionalElementInAllFrames(by, ELEMENT_EXISTENCE_SEARCH_TIME_SECONDS);
        if (element.isPresent()) {
            failWithScreenshot("Element with selector [" + by + "] was not found");
        }
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
