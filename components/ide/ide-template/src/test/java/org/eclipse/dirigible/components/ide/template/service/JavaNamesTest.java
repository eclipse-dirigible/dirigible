/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JavaNamesTest {

    @ParameterizedTest
    @CsvSource({//
            "myjob, Myjob", //
            "myJob, MyJob", //
            "MyJob, MyJob", //
            "my-job, MyJob", //
            "my_job, My_job", // an underscore is a legal identifier part, so it is kept
            "'my job', MyJob", //
            "my.long-running job, MyLongRunningJob", //
            "order-item-2, OrderItem2"})
    void derivesPascalCaseClassName(String fileNameBase, String expected) {
        assertThat(JavaNames.toClassName(fileNameBase)).isEqualTo(expected);
    }

    @Test
    void prefixesLeadingDigitSoTheClassNameIsALegalIdentifier() {
        assertThat(JavaNames.toClassName("2fa-check")).isEqualTo("_2faCheck");
    }

    @ParameterizedTest
    @CsvSource({"'', Handler", "'---', Handler"})
    void fallsBackWhenTheFileNameHasNoIdentifierCharacters(String fileNameBase, String expected) {
        assertThat(JavaNames.toClassName(fileNameBase)).isEqualTo(expected);
    }

    @Test
    void fallsBackOnNullFileName() {
        assertThat(JavaNames.toClassName(null)).isEqualTo("Handler");
    }

    @ParameterizedTest
    @CsvSource({//
            "myapp, myapp", //
            "MyApp, myapp", //
            "my-app, myapp", //
            "'My App', myapp", //
            "my_app, my_app", //
            "sales.invoices, salesinvoices"})
    void derivesLowerCasePackageName(String projectName, String expected) {
        assertThat(JavaNames.toPackageName(projectName)).isEqualTo(expected);
    }

    @Test
    void prefixesLeadingDigitSoThePackageNameIsALegalIdentifier() {
        assertThat(JavaNames.toPackageName("2nd-app")).isEqualTo("_2ndapp");
    }

    @ParameterizedTest
    @CsvSource({"package, package_", "new, new_", "true, true_", "null, null_"})
    void escapesPackageNamesThatWouldBeKeywordsOrLiterals(String projectName, String expected) {
        assertThat(JavaNames.toPackageName(projectName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"'', app", "'---', app"})
    void fallsBackWhenTheProjectNameHasNoIdentifierCharacters(String projectName, String expected) {
        assertThat(JavaNames.toPackageName(projectName)).isEqualTo(expected);
    }

    @Test
    void fallsBackOnNullProjectName() {
        assertThat(JavaNames.toPackageName(null)).isEqualTo("app");
    }

}
