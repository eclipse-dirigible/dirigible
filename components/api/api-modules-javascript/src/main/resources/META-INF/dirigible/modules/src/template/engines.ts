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
 * API v4 Template Engine
 *
 * Note: This module is supported only with the Mozilla Rhino engine
 */
import { Repository } from "sdk/platform/repository";

const REGISTRY_PUBLIC = "/registry/public/";
const MUSTACHE_FILE_EXTENSION = ".mustache";

const TemplateEnginesFacade = Java.type("org.eclipse.dirigible.components.api.templates.TemplateEnginesFacade");

export class TemplateEngines {

    public static getDefaultEngine(): TemplateEngine {
        return this.getVelocityEngine();
    }

    public static getMustacheEngine(): TemplateEngine {
        const engine = TemplateEnginesFacade.getMustacheEngine();
        return new TemplateEngine(engine, "mustache");
    }

    public static getVelocityEngine(): TemplateEngine {
        const engine = TemplateEnginesFacade.getVelocityEngine();
        return new TemplateEngine(engine, "velocity");
    }

    public static getJavascriptEngine(): TemplateEngine {
        const engine = TemplateEnginesFacade.getJavascriptEngine();
        return new TemplateEngine(engine, "javascript");
    }

    public static generate(location: string, template: string, parameters: { [key: string]: any }): string {
        return this.getDefaultEngine().generate(location, template, parameters);
    }

    public static generateFromFile(location: string, parameters: { [key: string]: any }): string | undefined {
        const resource = Repository.getResource(REGISTRY_PUBLIC + location);
        if (resource.exists()) {
            const isMustacheTemplate = location.endsWith(MUSTACHE_FILE_EXTENSION);
            const engine = isMustacheTemplate ? this.getMustacheEngine() : this.getDefaultEngine();
            const template = resource.getText();
            return engine.generate(location, template, parameters);
        }
        return undefined;
    }
}

class TemplateEngine {

    private engine: any;
    private sm: string;
    private em: string;

    constructor(engine: any, type: string) {
        this.engine = engine;
        this.sm = type === "mustache" ? "{{" : null;
        this.em = type === "mustache" ? "}}" : null;
    }

    public generate(location: string, template: string, parameters: { [key: string]: any }): string {
        return this.engine.generate(location, template, JSON.stringify(parameters), this.sm, this.em);
    }

    public setSm(sm: any) {
        this.sm = sm;
    }

    public setEm(em: any) {
        this.em = em;
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = TemplateEngines;
}
