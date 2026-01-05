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

import org.eclipse.dirigible.tests.base.BaseTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Lazy
@Component
class DependsOnScenariosTestProject extends BaseTestProject {

    private static final String PROJECT_RESOURCES_PATH = "DependsOnScenariosTestProject";
    private static final String VERIFICATION_URI = "/services/web/dashboard/index.html";
    private final Browser browser;

    DependsOnScenariosTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, Browser browser) {
        super(PROJECT_RESOURCES_PATH, ide, projectUtil, edmView);
        this.browser = browser;
    }

    @Override
    public void configure() {
        copyToWorkspace();
        generateEDM("sales-order.edm");
        publish();
    }

    @Override
    public void verify() {
        verifyCountryCityDependency();
        verifyProductUom();
        verifyProductPrice();
        verifyOrderCustomer();
        verifyCustomerPayment();
        verifyPaymentAmount();
    }

    private void verifyPaymentAmount() {
        browser.openPath(VERIFICATION_URI);
        browser.clickOnElementWithText(HtmlElementType.SPAN, "SalesOrder");

        browser.clickOnElementWithText(HtmlElementType.DIV, "Customer A");

        browser.clickOnElementById("SalesOrderPayment");
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");

        assertCustomerPaymentAmount();
    }

    private void assertCustomerPaymentAmount() {
        clickEmptyCustomerField();
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", "Customer A");
        browser.assertElementValueByAttributes(HtmlElementType.INPUT, Map.of(HtmlAttribute.ID, "idAmount"), "101");

        clickFilledCustomerField();
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", "Customer B");
        browser.assertElementValueByAttributes(HtmlElementType.INPUT, Map.of(HtmlAttribute.ID, "idAmount"), "201");

    }

    private void verifyCustomerPayment() {
        browser.openPath(VERIFICATION_URI);
        browser.clickOnElementWithText(HtmlElementType.SPAN, "SalesOrder");

        browser.clickOnElementWithText(HtmlElementType.DIV, "Customer A");

        browser.clickOnElementById("SalesOrderPayment");
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");

        assertCustomerPayment();
    }

    private void assertCustomerPayment() {
        clickEmptyCustomerField();
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", "Customer A");
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search CustomerPayment...");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Payment 1");

        clickFilledCustomerField();
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", "Customer B");
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search CustomerPayment...");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Payment 2");

    }

    private void clickEmptyCustomerField() {
        browser.clickElementByAttributes(HtmlElementType.INPUT, Map.of(HtmlAttribute.CLASS,
                "fd-input ng-empty ng-valid fd-input-group__input", HtmlAttribute.PLACEHOLDER, "Search Customer..."));
    }

    private void clickFilledCustomerField() {
        browser.clickElementByAttributes(HtmlElementType.INPUT, Map.of(HtmlAttribute.CLASS,
                "fd-input ng-valid fd-input-group__input ng-touched ng-not-empty", HtmlAttribute.PLACEHOLDER, "Search Customer..."));
    }

    private void verifyOrderCustomer() {
        browser.openPath(VERIFICATION_URI);
        browser.clickOnElementWithText(HtmlElementType.SPAN, "SalesOrder");

        browser.clickOnElementWithText(HtmlElementType.DIV, "Customer A");

        browser.clickOnElementById("SalesOrderPayment");
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");

        browser.clickElementByAttributes(HtmlElementType.INPUT, Map.of(HtmlAttribute.CLASS,
                "fd-input ng-empty ng-valid fd-input-group__input", HtmlAttribute.PLACEHOLDER, "Search Customer..."));

        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Customer A");
    }

    private void verifyProductPrice() {
        browser.openPath(VERIFICATION_URI);
        browser.clickOnElementWithText(HtmlElementType.SPAN, "SalesOrder");

        browser.clickOnElementWithText(HtmlElementType.DIV, "Customer A");

        browser.clickOnElementById("SalesOrderItem");
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");

        assertProductPrice("Product A", "11");
        assertProductPrice("Product B ", "20");
    }

    private void assertProductPrice(String product, String price) {
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Product...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", product);
        browser.assertElementValueByAttributes(HtmlElementType.INPUT, Map.of(HtmlAttribute.ID, "idPrice"), price);
    }

    private void verifyProductUom() {
        browser.openPath(VERIFICATION_URI);
        browser.clickOnElementWithText(HtmlElementType.SPAN, "SalesOrder");

        browser.clickOnElementWithText(HtmlElementType.DIV, "Customer A");

        browser.clickOnElementById("SalesOrderItem");
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");

        assertProductUom("Product A", "Kg");
        assertProductUom("Product B", "Liter");
    }

    private void assertProductUom(String product, String uom) {
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Product...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", product);
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search UoM...");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, uom);
    }

    private void verifyCountryCityDependency() {
        browser.openPath(VERIFICATION_URI);
        browser.clickOnElementWithText(HtmlElementType.SPAN, "Customer");

        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.GLYPH, "sap-icon--add", HtmlAttribute.CLASS, "fd-button fd-button--compact fd-button--transparent"));
        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.CLASS, "fd-button", HtmlAttribute.NGCLICK, "refreshCountry()"));

        asserCountryCity("Bulgaria", "Sofia");
        asserCountryCity("Italy", "Rome");
    }

    private void asserCountryCity(String country, String city) {
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Country...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", country);
        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.CLASS, "fd-button", HtmlAttribute.NGCLICK, "refreshCity()"));
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search City...");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, city);
    }
}
