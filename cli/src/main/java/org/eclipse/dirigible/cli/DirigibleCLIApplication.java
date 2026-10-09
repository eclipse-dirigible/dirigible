/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli;

import org.eclipse.dirigible.cli.generate.GenerateCommands;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.shell.core.ConsoleInputProvider;
import org.springframework.shell.core.NonInteractiveShellRunner;
import org.springframework.shell.core.ShellRunner;
import org.springframework.shell.core.SystemShellRunner;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.annotation.EnableCommand;

// The auto-configurations are named, not discovered: the CLI is a launcher and needs none of the
// others (#7642, #7793).
@SpringBootConfiguration
@ComponentScan
@ImportAutoConfiguration(ProjectInfoAutoConfiguration.class)
@EnableCommand({ProjectCommands.class, GenerateCommands.class})
public class DirigibleCLIApplication {

    public static void main(String[] args) {
        // the exit code a command asked for - `generate --check` exits 1 on drift (#7642)
        System.exit(SpringApplication.exit(SpringApplication.run(DirigibleCLIApplication.class, args)));
    }

    /**
     * A command on the command line runs and exits; none opens the shell. Defined here because
     * {@code @EnableCommand} otherwise registers an interactive runner whatever the arguments, which
     * ignores a command given on the command line.
     *
     * @param arguments the command line
     * @param commandParser the command parser
     * @param commandRegistry the command registry
     * @return the shell runner
     */
    @Bean
    ShellRunner shellRunner(ApplicationArguments arguments, CommandParser commandParser, CommandRegistry commandRegistry) {
        return arguments.getSourceArgs().length > 0 ? new NonInteractiveShellRunner(commandParser, commandRegistry)
                : new SystemShellRunner(new ConsoleInputProvider(), commandParser, commandRegistry);
    }

    @Bean
    ApplicationRunner shellApplicationRunner(ShellRunner shellRunner) {
        return arguments -> shellRunner.run(arguments.getSourceArgs());
    }

}
