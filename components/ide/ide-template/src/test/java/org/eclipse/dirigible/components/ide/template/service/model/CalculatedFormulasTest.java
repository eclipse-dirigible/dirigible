/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * A calculated-field formula reaches the Java templates as arithmetic over the entity's properties:
 * the property reads a TypeScript-era model writes are rewritten, and a formula that is no
 * arithmetic refuses the generation, every one of them named (#8051).
 */
class CalculatedFormulasTest {

    private static final Set<String> PROPERTIES = Set.of("Quantity", "Price", "Direction", "Net", "VAT", "StartDate", "EndDate");

    @Test
    void thePropertyReadsOfATypeScriptFormulaAreRewrittenToBareNames() {
        assertThat(CalculatedFormulas.normalize("entity[\"Quantity\"]*entity[\"Direction\"]")).isEqualTo("Quantity*Direction");
        assertThat(CalculatedFormulas.normalize("entity['Net'] + entity['VAT']")).isEqualTo("Net + VAT");
        assertThat(CalculatedFormulas.normalize("entity.Quantity * entity.Price")).isEqualTo("Quantity * Price");
    }

    @Test
    void aMethodCallIsNotRewritten() {
        assertThat(CalculatedFormulas.normalize("entity.Net.add(entity.VAT)")).isEqualTo("Net.add(VAT)");
        assertThat(CalculatedFormulas.normalize("entity.getNumber()")).isEqualTo("entity.getNumber()");
    }

    @Test
    void arithmeticOverThePropertiesIsAFormula() {
        assertThat(CalculatedFormulas.isFormula("Quantity*Direction", PROPERTIES)).isTrue();
        assertThat(CalculatedFormulas.isFormula("round(Quantity * Price * (1 + VAT / 100), 2)", PROPERTIES)).isTrue();
        assertThat(CalculatedFormulas.isFormula("businessDaysBetween(StartDate, EndDate) + 1", PROPERTIES)).isTrue();
    }

    @Test
    void anythingElseIsNot() {
        assertThat(CalculatedFormulas.isFormula("Net.add(VAT)", PROPERTIES)).isFalse();
        assertThat(CalculatedFormulas.isFormula("new NumberGeneratorService().generateByType('Contract')", PROPERTIES)).isFalse();
        assertThat(CalculatedFormulas.isFormula("(FirstName && LastName) ? FirstName : Name", PROPERTIES)).isFalse();
        assertThat(CalculatedFormulas.isFormula("Quantity * Discount", PROPERTIES)).as("a property the entity does not have")
                                                                                   .isFalse();
        assertThat(CalculatedFormulas.isFormula("sqrt(Quantity)", PROPERTIES)).as("a function Calc does not know")
                                                                              .isFalse();
    }

    @Test
    void theFormulaOfAModelIsRewrittenInPlace() {
        Map<String, Object> amount = calculated("Amount", "entity[\"Quantity\"]*entity[\"Direction\"]");

        CalculatedFormulas.resolve(List.of(entity("SalaryItem", amount, plain("Quantity"), plain("Direction"))));

        assertThat(amount.get("calculatedPropertyExpressionCreate")).isEqualTo("Quantity*Direction");
        assertThat(amount.get("calculatedPropertyExpressionUpdate")).isEqualTo("Quantity*Direction");
    }

    @Test
    void aModelWithFormulasThatAreNoArithmeticIsRefusedWithEveryOneNamed() {
        Map<String, Object> gross = calculated("Gross", "entity.Net.add(entity.VAT)");
        Map<String, Object> number = calculated("Number", "new NumberGeneratorService().generateByType('Invoice')");

        assertThatThrownBy(() -> CalculatedFormulas.resolve(List.of(entity("Invoice", gross, number, plain("Net"),
                plain("VAT"))))).isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("- Invoice.Gross: entity.Net.add(entity.VAT)")
                                .hasMessageContaining("- Invoice.Number: new NumberGeneratorService().generateByType('Invoice')");
    }

    @Test
    void aFormulaAJavaActionReplacesIsNotChecked() {
        Map<String, Object> number = calculated("Number", "new NumberGeneratorService().generateByType('Invoice')");
        number.put("calculatedActionOnCreate", "custom.InvoiceNumber");
        number.put("calculatedActionOnUpdate", "custom.InvoiceNumber");

        CalculatedFormulas.resolve(List.of(entity("Invoice", number)));

        assertThat(number.get("calculatedPropertyExpressionCreate")).isEqualTo("new NumberGeneratorService().generateByType('Invoice')");
    }

    @Test
    void aProjectionIsNotCheckedItsOwnProjectGeneratesIt() {
        Map<String, Object> name = calculated("Name", "entity[\"FirstName\"] + \" \" + entity[\"LastName\"]");
        Map<String, Object> employee = entity("Employee", name);
        employee.put("type", "PROJECTION");

        CalculatedFormulas.resolve(List.of(employee));

        assertThat(name.get("calculatedPropertyExpressionCreate")).isEqualTo("entity[\"FirstName\"] + \" \" + entity[\"LastName\"]");
    }

    @Test
    void anExtensionsFormulaReadsThePropertiesOfTheEntityItExtends() {
        Map<String, Object> total = calculated("Total", "entity.Quantity * entity.Price");
        Map<String, Object> extension = entity("OrderPricing", total);
        extension.put("type", "EXTENSION");
        extension.put("extensionReferencedEntity", "Order");

        CalculatedFormulas.resolve(List.of(entity("Order", plain("Quantity"), plain("Price")), extension));

        assertThat(total.get("calculatedPropertyExpressionCreate")).isEqualTo("Quantity * Price");
    }

    @Test
    void aPropertyThatIsNotCalculatedIsNotChecked() {
        Map<String, Object> note = calculated("Note", "entity.getNote()");
        note.put("isCalculatedProperty", Boolean.FALSE);

        CalculatedFormulas.resolve(List.of(entity("Invoice", note)));

        assertThat(note.get("calculatedPropertyExpressionCreate")).isEqualTo("entity.getNote()");
    }

    @SafeVarargs
    private static Map<String, Object> entity(String name, Map<String, Object>... properties) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("name", name);
        entity.put("properties", List.of(properties));
        return entity;
    }

    private static Map<String, Object> calculated(String name, String formula) {
        Map<String, Object> property = plain(name);
        property.put("isCalculatedProperty", Boolean.TRUE);
        property.put("calculatedPropertyExpressionCreate", formula);
        property.put("calculatedPropertyExpressionUpdate", formula);
        return property;
    }

    private static Map<String, Object> plain(String name) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        return property;
    }
}
