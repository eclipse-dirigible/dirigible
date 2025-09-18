/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

const LogFacade = Java.type("org.eclipse.dirigible.components.api.log.LogFacade");


export class Logging {

	public static getLogger(loggerName: string): Logger {
		return new Logger(loggerName);
	}
}

class Logger {

	private loggerName: string;

	constructor(loggerName: string) {
		this.loggerName = loggerName;
	}

	public setLevel(level: string) {
		LogFacade.setLevel(this.loggerName, level);
		return this;
	}

    public isDebugEnabled(): boolean {
        return LogFacade.isDebugEnabled(this.loggerName);
    }

    public isErrorEnabled(): boolean {
        return LogFacade.isErrorEnabled(this.loggerName);
    }

    public isWarnEnabled(): boolean {
        return LogFacade.isWarnEnabled(this.loggerName);
    }

    public isInfoEnabled(): boolean {
        return LogFacade.isInfoEnabled(this.loggerName);
    }

    public isTraceEnabled(): boolean {
        return LogFacade.isTraceEnabled(this.loggerName);
    }

	public log(msg: string, level: string): void {
		const args = Array.prototype.slice.call(arguments);
		let msgParameters = [];

		let rawError = null;
		if (args.length > 2) {
			if (args[args.length-1] instanceof Error) {
                rawError = args[args.length-1];

                if (rawError.stack) {
                    console.debug("Handling error with stack:\n" + rawError.stack);
                }
			}
			const endIndex = rawError ? (args.length - 1) : args.length;
			msgParameters = args.slice(2, endIndex).map(function (param) {
				return typeof param === 'object' ? JSON.stringify(param) : param;
			});
		}

		LogFacade.log(this.loggerName, level, msg, JSON.stringify(msgParameters), rawError);
	}

	public debug(msg: string, ..._): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'DEBUG');//insert DEBUG on second position in arguments array
		this.log.apply(this, args);
	}

	public info(msg: string, ..._): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'INFO');//insert INFO on second position in arguments array
		this.log.apply(this, args);
	}

	public trace(msg: string, ..._): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'TRACE');//insert TRACE on second position in arguments array
		this.log.apply(this, args);
	}

	public warn(msg: string, ..._): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'WARN');//insert WARN on second position in arguments array
		this.log.apply(this, args);
	}
	public error(msg: string, ..._): void {
		const args = Array.prototype.slice.call(arguments);
		args.splice(1, 0, 'ERROR');//insert ERROR on second position in arguments array
		this.log.apply(this, args);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Logging;
}
