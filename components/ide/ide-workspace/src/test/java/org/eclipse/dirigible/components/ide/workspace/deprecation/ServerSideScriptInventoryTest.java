/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.deprecation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.eclipse.dirigible.components.ide.workspace.deprecation.ServerSideScriptInventory.Inventory;
import org.eclipse.dirigible.components.ide.workspace.deprecation.ServerSideScriptInventory.Script;
import org.eclipse.dirigible.components.ide.workspace.deprecation.ServerSideScripts.Kind;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The inventory lists the scripts the deprecation rule counts, file by file, with what porting each
 * one needs.
 */
class ServerSideScriptInventoryTest {

    private static final String PROJECT = "/registry/public/salaries";

    private static final String RETIRED_TEMPLATE = "template-application-angular/template/template.js";

    @TempDir
    Path root;

    private IRepository repository;

    @BeforeEach
    void createRepository() {
        repository = new LocalRepository(root.toString(), true);
    }

    @Test
    void aCustomScriptIsListedWithTheModulesItImportsAndTheDecoratorsItUses() {
        write("/widgets/api/SalariesService.ts", """
                import { SalaryRepository as SalaryDao } from "codbex-salaries/gen/codbex-salaries/dao/Salaries/SalaryRepository";
                import { Controller, Get } from "sdk/http";

                @Controller
                class SalariesService {
                    @Get("/SalariesSum")
                    public SalariesSum() { return new SalaryDao().findAll(); }
                }
                """);

        Script script = single(inventory());

        assertThat(script.path()).isEqualTo("widgets/api/SalariesService.ts");
        assertThat(script.kind()).isEqualTo(Kind.TYPESCRIPT_FILE);
        assertThat(script.generated()).isFalse();
        assertThat(script.template()).isNull();
        assertThat(script.imports()).containsExactly("codbex-salaries/gen/codbex-salaries/dao/Salaries/SalaryRepository", "sdk/http");
        assertThat(script.decorators()).containsExactly("Controller", "Get");
    }

    @Test
    void aGeneratedScriptIsMarkedWithTheTemplateItsDescriptorRecords() {
        write("/salaries.gen", "{\"templateId\": \"" + RETIRED_TEMPLATE + "\", \"genFolderName\": \"salaries\"}");
        write("/gen/salaries/dao/Salaries/SalaryRepository.ts", "import { query } from \"sdk/db\";");

        Script script = single(inventory());

        assertThat(script.generated()).isTrue();
        assertThat(script.template()).isEqualTo(RETIRED_TEMPLATE);
        assertThat(script.imports()).containsExactly("sdk/db");
    }

    @Test
    void anExtensionModuleNamesItsExtensionPointAndTheModuleItRuns() {
        write("/events/CalculateSalary/CalculateSalary.extension",
                "{\"extensionPoint\": \"salaries-Salaries-SalaryItem\", \"module\": \"salaries/events/CalculateSalary/CalculateSalaryHandler.ts\"}");
        write("/events/CalculateSalary/CalculateSalaryHandler.ts", "export const trigger = (event) => {};");

        Inventory inventory = inventory();

        assertThat(inventory.scripts()).extracting(Script::path)
                                       .containsExactlyInAnyOrder("events/CalculateSalary/CalculateSalary.extension",
                                               "events/CalculateSalary/CalculateSalaryHandler.ts");
        Script extension = inventory.scripts()
                                    .stream()
                                    .filter(script -> script.kind() == Kind.EXTENSION_MODULE)
                                    .findFirst()
                                    .orElseThrow();
        assertThat(extension.extensionPoint()).isEqualTo("salaries-Salaries-SalaryItem");
        assertThat(extension.runs()).containsExactly("salaries/events/CalculateSalary/CalculateSalaryHandler.ts");
    }

    @Test
    void aJobNamesTheScriptItRuns() {
        write("/jobs/daily.job", "{\"expression\": \"0 0 1 * * ?\", \"handler\": \"salaries/jobs/daily.ts\"}");

        Script script = single(inventory());

        assertThat(script.kind()).isEqualTo(Kind.JOB);
        assertThat(script.runs()).containsExactly("salaries/jobs/daily.ts");
    }

    @Test
    void browserCodeCallingAScriptUrlIsAReferenceNotAScript() {
        write("/widgets/salaries-sum/controller.js",
                "$http.get('/services/ts/salaries/widgets/api/SalariesService.ts/SalariesSum').then((response) => {});");
        write("/widgets/salaries-sum/index.html", "<div>no script url here</div>");

        Inventory inventory = inventory();

        assertThat(inventory.scripts()).isEmpty();
        assertThat(inventory.references()).singleElement()
                                          .satisfies(reference -> {
                                              assertThat(reference.path()).isEqualTo("widgets/salaries-sum/controller.js");
                                              assertThat(reference.generated()).isFalse();
                                              assertThat(reference.urls()).containsExactly(
                                                      "/services/ts/salaries/widgets/api/SalariesService.ts/SalariesSum");
                                          });
    }

    @Test
    void aJavaScriptUrlIsAReferenceOnlyWhenItCallsThisProject() {
        write("/gen/salaries/ui/launchpad/Home/controller.js",
                "$http.get('/services/js/salaries/gen/salaries/ui/launchpad/Home/tiles.js');");
        write("/gen/salaries/ui/launchpad/Home/index.html",
                "<script src=\"/services/js/resources-core/services/loader.js?id=app\"></script>");

        Inventory inventory = inventory();

        assertThat(inventory.references()).singleElement()
                                          .satisfies(reference -> {
                                              assertThat(reference.path()).isEqualTo("gen/salaries/ui/launchpad/Home/controller.js");
                                              assertThat(reference.generated()).isTrue();
                                          });
    }

    @Test
    void aProjectWithoutScriptsHasAnEmptyInventory() {
        write("/data/salaries.csvim", "{\"files\": []}");
        write("/custom/Payroll.java", "package custom; public class Payroll {}");

        Inventory inventory = inventory();

        assertThat(inventory.project()).isEqualTo("salaries");
        assertThat(inventory.scripts()).isEmpty();
        assertThat(inventory.references()).isEmpty();
    }

    private Inventory inventory() {
        return ServerSideScriptInventory.of("salaries", repository.getCollection(PROJECT));
    }

    private static Script single(Inventory inventory) {
        assertThat(inventory.scripts()).hasSize(1);
        return inventory.scripts()
                        .get(0);
    }

    private void write(String path, String content) {
        repository.createResource(PROJECT + path, content.getBytes(StandardCharsets.UTF_8));
    }
}
