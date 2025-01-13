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
        SelenideElement element = getElementByAttributePattern(elementType, attribute, pattern);
        handleElementInAllFrames(element, e -> {
            e.click();
            LOGGER.info("Entering [{}] in [{}]", text, e);
            e.setValue(text);
        });
    }

    private SelenideElement getElementByAttributePattern(HtmlElementType elementType, HtmlAttribute attribute,
            String attributeValuePattern) {
        By selector = constructCssSelectorByTypeAndAttribute(elementType, attribute, attributeValuePattern);
        return Selenide.$(selector);
    }

    private By constructCssSelectorByTypeAndAttribute(HtmlElementType elementType, HtmlAttribute attribute, String attributePattern) {
        String cssSelector = elementType.getType() + "[" + attribute.getAttribute() + "*='" + attributePattern + "']";
        return Selectors.byCssSelector(cssSelector);
    }

    private void handleElementInAllFrames(SelenideElement element, Consumer<SelenideElement> elementHandler) {
        handleElementInAllFrames(30 * 1000, element, elementHandler);
    }

    private void handleElementInAllFrames(long waitMillis, SelenideElement element, Consumer<SelenideElement> elementHandler) {
        long maxWaitTime = System.currentTimeMillis() + waitMillis;
        try {
            do {
                boolean handled = tryToHandleElementInAllFrames(element, elementHandler);
                if (handled) {
                    LOGGER.info("Element [{}] was NOT found. Will try again.", element);
                    return;
                }
            } while (System.currentTimeMillis() < maxWaitTime);

            LOGGER.info("Element [{}] was NOT found. Will try last time to reload the page and find it.", element);
            reload();
            SleepUtil.sleepSeconds(3);
            boolean handled = tryToHandleElementInAllFrames(element, elementHandler);
            if (handled) {
                LOGGER.info("Element [{}] was FOUND and HANDLED after page reload.", element);
                return;
            }

            String screenshot = createScreenshot();
            fail("Element [" + element + "] cannot be found in any iframe. Screenshot path: " + screenshot);
        } finally {
            Selenide.switchTo()
                    .defaultContent();
        }
    }

    private boolean tryToHandleElementInAllFrames(SelenideElement element, Consumer<SelenideElement> elementHandler) {
        Selenide.switchTo()
                .defaultContent();
        LOGGER.info("Checking element [{}] in the default frame...", element);
        if (elementExists(element, 600L)) {
            LOGGER.info("Element [{}] was FOUND in the default frame.", element);
            elementHandler.accept(element);
            return true;
        }
        ElementsCollection iframes = getElements(HtmlElementType.IFRAME);
        LOGGER.info("Found [{}] iframes", iframes.size());

        for (SelenideElement iframe : iframes) {
            Selenide.switchTo()
                    .frame(iframe);

            LOGGER.info("Checking element [{}] in iframe [{}]...", element, iframe);
            if (elementExists(element, 600L)) {
                LOGGER.info("Element [{}] was FOUND in frame [{}].", element, iframe);
                elementHandler.accept(element);
                return true;
            }

            // without this, the frame cannot be switched in the next iteration
            Selenide.switchTo()
                    .defaultContent();
        }
        return false;
    }

    private boolean elementExists(SelenideElement element, long millis) {
        LOGGER.info("Checking element [{}] existence in the current frame for [{}] millis", element, millis);
        long until = System.currentTimeMillis() + (millis);
        do {
            if (element.exists()) {
                LOGGER.info("Element [{}] EXISTS in the current frame.", element);
                return true;
            }
            LOGGER.info("Element [{}] does NOT exist in the current frame.", element);
            SleepUtil.sleepMillis(200);
        } while (System.currentTimeMillis() < until);
        return false;
    }

    private ElementsCollection getElements(HtmlElementType elementType) {
        By selector = constructCssSelectorByType(elementType);
        return Selenide.$$(selector);
    }

    private By constructCssSelectorByType(HtmlElementType elementType) {
        String cssSelector = elementType.getType();
        return Selectors.byCssSelector(cssSelector);
    }

    @Override
    public String createScreenshot() {
        return Selenide.screenshot(UUID.randomUUID()
                                       .toString());
    }

    @Override
    public void reload() {
        Selenide.refresh();
    }

    @Override
    public void clearCookies() {
        Selenide.clearBrowserCookies();
    }

    @Override
    public void rightClickOnElementById(String id) {
        SelenideElement element = getElementById(id);
        handleElementInAllFrames(element, this::rightClickElement);
    }

    private SelenideElement getElementById(String id) {
        return Selenide.$("#" + id);
    }

    private void rightClickElement(SelenideElement element) {
        element.shouldBe(Condition.visible, Condition.enabled)
               .scrollIntoView(false)
               .contextClick();
    }

    @Override
    public void clickOnElementByAttributePatternAndText(HtmlElementType elementType, HtmlAttribute attribute, String pattern, String text) {
        SelenideElement element = getElementByAttributePatternAndText(elementType, attribute, pattern, text);
        handleElementInAllFrames(element, this::clickElement);
    }

    private void clickElement(SelenideElement element) {
        element.shouldBe(Condition.visible, Condition.enabled)
               .scrollIntoView(false)
               .click();
    }

    private SelenideElement getElementByAttributePatternAndText(HtmlElementType elementType, HtmlAttribute attribute, String pattern,
            String text) {
        By selector = constructCssSelectorByTypeAndAttribute(elementType, attribute, pattern);
        ElementsCollection options = Selenide.$$(selector);

        return options.findBy(Condition.text(text));
    }

    @Override
    public void clickOnElementByAttributeValue(HtmlElementType htmlElementType, HtmlAttribute htmlAttribute, String attributeValue) {
        SelenideElement element = getElementByAttributePattern(htmlElementType, htmlAttribute, attributeValue);
        handleElementInAllFrames(element, this::clickElement);
    }

    @Override
    public void doubleClickOnElementContainingText(HtmlElementType elementType, String text) {
        String textPattern = Pattern.quote(text);
        SelenideElement element = getElementByAttributeAndTextPattern(elementType, textPattern);
        handleElementInAllFrames(element, SelenideElement::doubleClick);
    }

    @Override
    public void clickOnElementContainingText(HtmlElementType elementType, String text) {
        String textPattern = Pattern.quote(text);
        SelenideElement element = getElementByAttributeAndTextPattern(elementType, textPattern);
        handleElementInAllFrames(element, SelenideElement::click);
    }

    @Override
    public void clickOnElementByAttributePattern(HtmlElementType elementType, HtmlAttribute attribute, String pattern) {
        SelenideElement element = getElementByAttributePattern(elementType, attribute, pattern);
        handleElementInAllFrames(element, SelenideElement::click);
    }

    @Override
    public void assertElementExistsByTypeAndTextPattern(HtmlElementType htmlElementType, String textPattern) {
        SelenideElement element = getElementByAttributeAndTextPattern(htmlElementType, textPattern);
        handleElementInAllFrames(element, e -> e.should(Condition.exist));
    }

    private SelenideElement getElementByAttributeAndTextPattern(HtmlElementType htmlElementType, String textPattern) {
        ElementsCollection elements = getElements(htmlElementType);
        return elements.findBy(Condition.matchText(textPattern));
    }

    @Override
    public void assertElementExistsByTypeAndText(HtmlElementType elementType, String text) {
        SelenideElement element = getElementByAttributeAndText(elementType, text);
        handleElementInAllFrames(element, e -> e.should(Condition.exist));
    }

    private SelenideElement getElementByAttributeAndText(HtmlElementType elementType, String text) {
        By selector = constructCssSelectorByType(elementType);
        ElementsCollection options = Selenide.$$(selector);

        return options.findBy(Condition.text(text));
    }

    @Override
    public String getPageTitle() {
        return Selenide.title();
    }

}
