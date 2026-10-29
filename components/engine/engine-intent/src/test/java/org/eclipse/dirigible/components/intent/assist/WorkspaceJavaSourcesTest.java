/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.dirigible.components.workspace.domain.Project;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A proposal is compiled with the project's own sources, and with those of every other project of
 * the workspace the file names - the runtime compiles all of them in one batch.
 */
class WorkspaceJavaSourcesTest {

    private static final String WORKSPACE = "/users/admin/workspace";

    @TempDir
    Path root;

    private IRepository repository;

    private final WorkspaceJavaSources sources = new WorkspaceJavaSources();

    @BeforeEach
    void createWorkspace() {
        repository = new LocalRepository(root.toString(), true);
        write("codbex-salaries/gen/codbex_salaries/data/salaries/SalaryRepository.java",
                "package gen.codbex_salaries.data.salaries;\npublic class SalaryRepository {}\n");
        write("codbex-salaries/widgets/api/SalariesService.java", "package widgets.api;\npublic class SalariesService {}\n");
        write("codbex-currencies/gen/codbex_currencies/data/currencies/CurrencyRepository.java",
                "package gen.codbex_currencies.data.currencies;\npublic class CurrencyRepository {}\n");
        write("codbex-uoms/gen/codbex_uoms/data/settings/UoMRepository.java",
                "package gen.codbex_uoms.data.settings;\npublic class UoMRepository {}\n");
    }

    @Test
    void aProjectTheFileNamesByItsNameIsTakenIn() {
        List<ProjectSource> siblings = sources.siblings(salaries(), "widgets/api/SalariesService.java",
                "import { CurrencyRepository } from \"codbex-currencies/gen/codbex-currencies/dao/Currencies/CurrencyRepository\";");

        assertThat(siblings).extracting(ProjectSource::fqn)
                            .containsExactly("gen.codbex_salaries.data.salaries.SalaryRepository",
                                    "gen.codbex_currencies.data.currencies.CurrencyRepository");
        assertThat(siblings).extracting(ProjectSource::path)
                            .contains("../codbex-currencies/gen/codbex_currencies/data/currencies/CurrencyRepository.java");
    }

    @Test
    void aProjectTheFileNamesByItsJavaPackageIsTakenIn() {
        List<ProjectSource> siblings = sources.siblings(salaries(), "widgets/api/SalariesService.java",
                "import gen.codbex_currencies.data.currencies.CurrencyRepository;");

        assertThat(siblings).extracting(ProjectSource::fqn)
                            .contains("gen.codbex_currencies.data.currencies.CurrencyRepository");
    }

    @Test
    void aProjectTheFileDoesNotNameIsLeftOut() {
        List<ProjectSource> siblings = sources.siblings(salaries(), "widgets/api/SalariesService.java", "public class SalariesService {}");

        assertThat(siblings).extracting(ProjectSource::fqn)
                            .containsExactly("gen.codbex_salaries.data.salaries.SalaryRepository");
    }

    private Project salaries() {
        return new Project(repository.getCollection(WORKSPACE + "/codbex-salaries"));
    }

    private void write(String path, String content) {
        repository.createResource(WORKSPACE + "/" + path, content.getBytes(StandardCharsets.UTF_8));
    }
}
