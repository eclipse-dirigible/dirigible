/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/**
 * Get engine by type
 */

const CommandFacade = Java.type("org.eclipse.dirigible.components.api.platform.CommandFacade");
const ProcessExecutionOptions = Java.type("org.eclipse.dirigible.commons.process.execution.ProcessExecutionOptions");

interface ProcessExecutionOptions {
	workingDirectory: string
}

interface EnvironmentVariables {
	[key: string]: string
}

interface CommandOutput {
	exitCode: number;
	standardOutput: string;
	errorOutput: string;
}

export class Command {

	public static execute(command: string, options?: ProcessExecutionOptions, add?: EnvironmentVariables, remove?: string[]): CommandOutput {
		const processExecutionOptions = new ProcessExecutionOptions();
		if (options) {
			processExecutionOptions.setWorkingDirectory(options.workingDirectory);
		}
		return JSON.parse(CommandFacade.execute(command, add, remove, processExecutionOptions))
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Command;
}