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

	private resolveError(err: Error): { message: string, stack: any[] } {
		let stack = [];
		if (__engine === 'v8') {
			stack = this.parseIntoStackTraceElementsV8(err.stack);
		} else if (__engine === 'rhino' && err.stack) {
			stack = this.parseIntoStackTraceElementsRhino(err.stack);
		}
		return {
			message: err.message,
			stack: stack
		};
	}

	private parseIntoStackTraceElementsV8(stack: string): { fileName: string; lineNumber: string; declaringClass: any; methodName: string; }[] {
		//"at obj.f2 (:29:9)" - length 8
		//"at :36:4"		  - length 8
		//"at Error (native)" - length 6
		const v8_stack_el_regex = /^\s*at ?(.*?) ((.*?)\s*(?:\()?(?:(.*?))(?::(\d+))?(?::(\d+))(?:\))?|\((native)\))$/;
		let lines_string = stack.split("\n");
		let stackLineIdx = 0;
		let lines = lines_string.map(line => {
			if (stackLineIdx > 0 || line.trim().length < 1) {
				const _segmenets = line.trim().match(v8_stack_el_regex);
				if (_segmenets === null)
					return;
				//resolve object and function if specified
				let _obj, _m;
				if (_segmenets[1]) {
					const _idx = _segmenets[1].lastIndexOf('.');
					if (_idx < 0)
						_m = _segmenets[1];
					else {
						_m = _segmenets[1].substring(_idx + 1, _segmenets[1].length);
						_obj = _segmenets[1].substring(0, _segmenets[1].length - _m.length - 1);
					}
				}
				return {
					fileName: _segmenets.length < 8 ? _segmenets[3] : _segmenets[4],
					lineNumber: _segmenets.length < 8 ? _segmenets[4] : _segmenets[5],
					declaringClass: _obj || "?",
					methodName: _m || "?"
				};
			}
			stackLineIdx++;
			return;
		}).filter((el) => {
			return el !== undefined;
		});
		return lines;
	}

	private parseIntoStackTraceElementsRhino(stack) {
		//at prj1/svc/a.js:28 (anonymous)
		//at prj1/svc/a.js:36
		const rhino_stack_el_regex = /^\s*at (.*?) ?(.*?)(?::(\d+))?(?::(\d+))?\s*(\((.*?)\))?\s*$/;
		let lines = stack.split("\n");
		lines = lines.map(function (line) {
			if (line.trim().length < 1)
				return;
			const _lineSegments = line.match(rhino_stack_el_regex);
			return {
				fileName: _lineSegments[2],
				lineNumber: _lineSegments[3],
				declaringClass: "?",
				methodName: _lineSegments[6] || '?'
			};
		}).filter(function (el) {
			return el !== undefined;
		});
		return lines;

	}

	public setLevel(level: string) {
		LogFacade.setLevel(this.loggerName, level);
		return this;
	}

	public log(msg: string, level: string): void {
		const args = Array.prototype.slice.call(arguments);
		let msgParameters = [];
		let errObjectJson = null;
		if (args.length > 2) {
			if (args[2] instanceof Error) {
				const errObject = this.resolveError(args[2]);
				if (errObject) {
					errObjectJson = JSON.stringify(errObject);
				}
			}
			const sliceIndex = errObjectJson ? 3 : 2;
			msgParameters = args.slice(sliceIndex).map(function (param) {
				return typeof param === 'object' ? JSON.stringify(param) : param;
			});
		}
		LogFacade.log(this.loggerName, level, msg, JSON.stringify(msgParameters), errObjectJson);
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
