/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Lazy
@Component
class DependsOnScenariosTestProject extends BaseTestProject {

    private static final String PROJECT_RESOURCES_PATH = "DependsOnScenariosTestProject";
    private static final String VERIFICATION_URI_CY = "/services/web/" + PROJECT_RESOURCES_PATH + "/gen/sales-order/ui/Customer/index.html";
    private static final String VERIFICATION_URI_PM =
            "/services/web/" + PROJECT_RESOURCES_PATH + "/gen/sales-order/ui/SalesOrder/index.html";
    private final Browser browser;

    DependsOnScenariosTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, Browser browser) {
        super(PROJECT_RESOURCES_PATH, ide, projectUtil, edmView);
        this.browser = browser;
    }

    @Override
    public void configure() {
        copyToWorkspace();
        publish();
    }

    @Override
    public void verify() {
        browser.openPath(VERIFICATION_URI_CY);

        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.GLYPH, "sap-icon--add", HtmlAttribute.CLASS, "fd-button fd-button--transparent fd-button--compact"));
        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.CLASS, "fd-button", HtmlAttribute.NGCLICK, "refreshCountry()"));

        asserCountryCity("Bulgaria", "Sofia");
        asserCountryCity("Italy", "Rome");

        // 2
        browser.openPath(VERIFICATION_URI_PM);
        browser.clickOnElementByAttributePattern(HtmlElementType.ANCHOR, HtmlAttribute.CLASS,
                "fd-list__link fd-list__link--navigation-indicator"); // customer A

        browser.clickOnElementById("SalesOrderItem");
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");

        assertProductUom("Product A", "Kg");
        assertProductUom("Product B", "Liter");

        // 3
        browser.openPath(VERIFICATION_URI_PM);
        browser.clickOnElementByAttributePattern(HtmlElementType.ANCHOR, HtmlAttribute.CLASS,
                "fd-list__link fd-list__link--navigation-indicator"); // customer A

        browser.clickOnElementById("SalesOrderItem");
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");

        assertProductPrice("Product A", "11");
        assertProductPrice("Product B ", "20");

        // 4
        // browser.openPath(VERIFICATION_URI_PM);
        // browser.clickOnElementByAttributePattern(HtmlElementType.ANCHOR, HtmlAttribute.CLASS,
        // "fd-list__link fd-list__link--navigation-indicator"); // customer A
        //
        // browser.clickOnElementById("SalesOrderPayment");
        // browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");
        //
        // SleepUtil.sleepMillis(1000);
        //
        // browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER,
        // "Search Customer ...");
        // browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Customer A");

        // 5
        // browser.openPath(VERIFICATION_URI_PM);
        // browser.clickOnElementByAttributePattern(HtmlElementType.ANCHOR, HtmlAttribute.CLASS,
        // "fd-list__link fd-list__link--navigation-indicator"); // customer A
        //
        // browser.clickOnElementById("SalesOrderPayment");
        // browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");
        //
        // assertCustomerPayment("Customer A", "Payment 1");
        // assertCustomerPayment("Customer B", "Payment 2");

        // 6
        // browser.openPath(VERIFICATION_URI_PM);
        // browser.clickOnElementByAttributePattern(HtmlElementType.ANCHOR, HtmlAttribute.CLASS,
        // "fd-list__link fd-list__link--navigation-indicator"); // customer A
        //
        // browser.clickOnElementById("SalesOrderPayment");
        // browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");
        //
        // assertCustomerPaymentAmount("Customer A", "101");
        // assertCustomerPaymentAmount("Customer B", "201");
    }

    private void assertCustomerPaymentAmount(String customer, String amount) {
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Customer ...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", customer);
        browser.assertElementValueByAttributes(HtmlElementType.INPUT, Map.of(HtmlAttribute.ID, "idAmount"), amount);

        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, amount);
    }

    private void assertCustomerPayment(String customer, String payment) {
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Customer ...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", customer);
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search CustomerPayment ...");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, payment);
    }

    private void assertProductPrice(String product, String price) {
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Product ...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", product);
        browser.assertElementValueByAttributes(HtmlElementType.INPUT, Map.of(HtmlAttribute.ID, "idPrice"), price);
    }

    private void assertProductUom(String product, String uom) {
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Product ...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", product);
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search UoM ...");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, uom);
    }

    private void asserCountryCity(String country, String city) {
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Country ...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", country);
        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.CLASS, "fd-button", HtmlAttribute.NGCLICK, "refreshCity()"));
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search City ...");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, city);
    }
}
