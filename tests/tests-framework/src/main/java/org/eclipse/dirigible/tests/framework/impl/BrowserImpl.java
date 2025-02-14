/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.impl;

import com.codeborne.selenide.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
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
        By by = constructCssSelectorByTypeAndAttribute(elementType, attribute, pattern);
        handleElementInAllFrames(by, e -> {
            e.click();
            LOGGER.info("Entering [{}] in [{}]", text, e);
            e.setValue(text);
        });
    }

    private By constructCssSelectorByTypeAndAttribute(HtmlElementType elementType, HtmlAttribute attribute, String attributePattern) {
        String cssSelector = elementType.getType() + "[" + attribute.getAttribute() + "*='" + attributePattern + "']";
        return Selectors.byCssSelector(cssSelector);
    }

    private void handleElementInAllFrames(By by, Consumer<SelenideElement> elementHandler) {
        Optional<SelenideElement> element = findElementInAllFrames(by);
        if (element.isPresent()) {
            elementHandler.accept(element.get());
            return;
        }
        String screenshot = createScreenshot();
        fail("Element by [" + by + "] cannot be found in any iframe. Screenshot path: " + screenshot);
    }

    @Override
    public String createScreenshot() {
        return Selenide.screenshot(UUID.randomUUID()
                                       .toString());
    }

    private Optional<SelenideElement> findElementInAllFrames(By by, WebElementCondition... conditions) {
        return findElementInAllFrames(30 * 1000, by, conditions);
    }

    private Optional<SelenideElement> findElementInAllFrames(long waitMillis, By by, WebElementCondition... conditions) {
        long maxWaitTime = System.currentTimeMillis() + waitMillis;

        do {
            Optional<SelenideElement> element = findSingleElementInAllFrames(by, conditions);
            if (element.isEmpty()) {
                LOGGER.info("Element by [{}] and conditions [{}] was NOT found. Will try again.", by, conditions);
            } else {
                LOGGER.info("Element by [{}] and conditions [{}] was FOUND.", by, conditions);
                return element;
            }
        } while (System.currentTimeMillis() < maxWaitTime);

        LOGGER.info("Element by [{}] was NOT found. Will try last time to reload the page and find it.", by);

        reload();
        SleepUtil.sleepSeconds(3);

        Optional<SelenideElement> element = findSingleElementInAllFrames(by);
        if (element.isPresent()) {
            LOGGER.info("Element [{}] was FOUND after page reload.", element);
            return element;
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void reload() {
        Selenide.refresh();
    }

    private Optional<SelenideElement> findSingleElementInAllFrames(By by, WebElementCondition... conditions) {
        Selenide.switchTo()
                .defaultContent();
        LOGGER.info("Checking element by [{}] and conditions [{}] in the default frame...", by, conditions);

        Optional<SelenideElement> element = findSingleElement(by, conditions);
        if (element.isPresent()) {
            LOGGER.info("Element by selector [{}] and conditions [{}] was FOUND in the default frame.", by, conditions);
            return element;
        }

        return findElementInFramesRecursively(by, conditions);
    }

    private Optional<SelenideElement> findElementInFramesRecursively(By by, WebElementCondition... conditions) {
        By iframeSelector = constructCssSelectorByType(HtmlElementType.IFRAME);
        ElementsCollection iframes = Selenide.$$(iframeSelector);
        LOGGER.info("Found [{}] iframes", iframes.size());

        for (SelenideElement iframe : iframes) {
            Selenide.switchTo()
                    .frame(iframe);
            LOGGER.info("Switched to iframe [{}]. Searching for element by [{}] and conditions[{}]...", iframe, by, conditions);

            Optional<SelenideElement> element = findSingleElement(by, conditions);
            if (element.isPresent()) {
                LOGGER.info("Element with selector [{}] was FOUND in iframe [{}].", by, iframe);
                return element;
            }

            element = findElementInFramesRecursively(by, conditions);
            if (element.isPresent()) {
                return element;
            }

            Selenide.switchTo()
                    .parentFrame();
        }

        return Optional.empty();
    }

    private By constructCssSelectorByType(HtmlElementType elementType) {
        String cssSelector = elementType.getType();
        return Selectors.byCssSelector(cssSelector);
    }

    private Optional<SelenideElement> findSingleElement(By by, WebElementCondition... conditions) {
        long searchMillis = 1000;
        LOGGER.info("Searching for element by [{}] and conditions [{}] in the current frame for [{}] millis", by, conditions, searchMillis);
        long until = System.currentTimeMillis() + searchMillis;
        do {
            ElementsCollection foundElements = Selenide.$$(by);
            for (WebElementCondition condition : conditions) {
                foundElements = foundElements.filterBy(condition);
            }

            int foundElementsCount = foundElements.size();
            if (foundElements.size() == 1) {
                return Optional.of(foundElements.first());
            }

            if (foundElementsCount > 1) {
                String screenshot = createScreenshot();
                fail("Expected only one element with selector [" + by + "] and conditions [{" + Arrays.toString(conditions)
                        + "}] but found [" + foundElementsCount + "]. Consider using more precises selector. Elements: "
                        + foundElements.describe() + "\nScreenshot path: " + screenshot);
                throw new IllegalStateException("Will not be actually thrown");
            }

            LOGGER.info("Element with selector [{}] and conditions [{}] does NOT exist in the current frame.", by, conditions);
            SleepUtil.sleepMillis(200);
        } while (System.currentTimeMillis() < until);
        return Optional.empty();
    }

    @Override
    public void clearCookies() {
        Selenide.clearBrowserCookies();
    }

    @Override
    public void rightClickOnElementById(String id) {
        By by = By.cssSelector("#" + id);
        handleElementInAllFrames(by, this::rightClickElement);
    }

    private void rightClickElement(SelenideElement element) {
        element.shouldBe(Condition.visible, Condition.enabled)
               .scrollIntoView(false)
               .contextClick();
    }

    @Override
    public void clickOnElementByAttributePatternAndText(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text) {
        By by = constructCssSelectorByTypeAndAttribute(elementType, attribute, pattern);
        SelenideElement element = findElementInAllFrames(by, Condition.text(text)).orElseThrow(
                () -> new IllegalStateException("Element by [" + by + "] cannot be found in any iframe."));
        clickElement(element);
    }

    private void clickElement(SelenideElement element) {
        element.shouldBe(Condition.visible, Condition.enabled)
               .scrollIntoView(false)
               .click();
    }

    @Override
    public void clickOnElementByAttributeValue(HtmlElementType htmlElementType, HtmlAttribute htmlAttribute, String attributeValue) {
        By by = constructCssSelectorByTypeAndAttribute(htmlElementType, htmlAttribute, attributeValue);
        handleElementInAllFrames(by, this::clickElement);
    }

    @Override
    public void doubleClickOnElementContainingText(HtmlElementType elementType, String text) {
        String textPattern = Pattern.quote(text);
        SelenideElement element = getElementByAttributeAndTextPattern(elementType, textPattern);

        element.doubleClick();
    }

    private SelenideElement getElementByAttributeAndTextPattern(HtmlElementType elementType, String textPattern) {
        By selector = constructCssSelectorByType(elementType);
        SelenideElement element = findElementInAllFrames(selector, Condition.matchText(textPattern), Condition.exist).orElseThrow(
                () -> new IllegalStateException("Element by [" + selector + "] cannot be found in any iframe."));

        return element;
    }

    private SelenideElement getElementByAttributeAndText(HtmlElementType elementType, String text) {
        By selector = constructCssSelectorByType(elementType);
        SelenideElement element = findElementInAllFrames(selector, Condition.exist, Condition.exactText(text), Condition.visible,
                Condition.clickable).orElseThrow(
                        () -> new IllegalStateException("Element by [" + selector + "] cannot be found in any iframe."));

        return element;
    }

    @Override
    public void clickOnElementContainingText(HtmlElementType elementType, String text) {
        String textPattern = Pattern.quote(text);
        SelenideElement element = getElementByAttributeAndTextPattern(elementType, textPattern);

        element.click();
    }

    @Override
    public void clickOnElementWithText(HtmlElementType elementType, String text) {
        SelenideElement element = getElementByAttributeAndText(elementType, text);

        element.click();
    }

    @Override
    public void clickOnElementByAttributePattern(HtmlElementType elementType, HtmlAttribute attribute, String pattern) {
        By by = constructCssSelectorByTypeAndAttribute(elementType, attribute, pattern);

        handleElementInAllFrames(by, SelenideElement::click);
    }

    @Override
    public void assertElementExistsByTypeAndTextPattern(HtmlElementType htmlElementType, String textPattern) {
        getElementByAttributeAndTextPattern(htmlElementType, textPattern);
    }

    @Override
    public void assertElementExistsByTypeAndText(HtmlElementType elementType, String text) {
        By selector = constructCssSelectorByType(elementType);

        findElementInAllFrames(selector, Condition.exist, Condition.exactText(text)).orElseThrow(
                () -> new IllegalStateException("Element by [" + selector + "] cannot be found in any iframe."));
    }

    @Override
    public String getPageTitle() {
        return Selenide.title();
    }

}
