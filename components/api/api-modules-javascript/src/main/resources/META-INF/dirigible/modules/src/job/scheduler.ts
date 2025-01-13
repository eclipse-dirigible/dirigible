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
 * API Job
 */

import { configurations } from "sdk/core";
const JobFacade = Java.type("org.eclipse.dirigible.components.api.job.JobFacade");

export class Scheduler {

    public static getJobs(): Job[] {
        const jobs = new Array();
        const jobDefinitions = JSON.parse(JobFacade.getJobs());
        for (const definition of jobDefinitions) {
            jobs.push(new Job(definition));
        }
        return jobs;
    }

    public static getJob(name: string): Job {
        const jobDefinition = JSON.parse(JobFacade.getJob(name));
        return new Job(jobDefinition);
    }

    public static enable(name: string): void {
        JobFacade.enable(name);
    }

    public static disable(name: string): void {
        JobFacade.disable(name);
    }

    public static trigger(name: string, parameters: { [key: string]: string } = {}): void {
        JobFacade.trigger(name, JSON.stringify(parameters));
    }

    public static log(name: string, message: string): void {
        JobFacade.log(name, message);
    }

    public static error(name: string, message: string): void {
        JobFacade.error(name, message);
    }

    public static warn(name: string, message: string): void {
        JobFacade.warn(name, message);
    }

    public static info(name: string, message: string): void {
        JobFacade.info(name, message);
    }

}

/**
 * Job object
 */
class Job {

    private data: any;

    constructor(data: any) {
        this.data = data;
    }

    public getName(): string {
        return this.data.name;
    }

    public getGroup(): string {
        return this.data.group;
    }

    public getClazz(): string {
        return this.data.clazz;
    }

    public getDescription(): string {
        return this.data.description;
    }

    public getExpression(): string {
        return this.data.expression;
    }

    public getHandler(): string {
        return this.data.handler;
    }

    public getEngine(): string {
        return this.data.engine;
    }

    public getSingleton(): boolean {
        return this.data.singleton;
    }

    public getEnabled(): boolean {
        return this.data.enabled;
    }

    public getCreatedBy(): string {
        return this.data.createdBy;
    }

    public getCreatedAt(): number {
        return this.data.createdAt;
    }

    public getParameters(): JobParameters {
        return new JobParameters(this.data.parameters);
    }

    public getParameter(name: string): string {
        if (this.data) {
            for (let i in this.data.parameters) {
                if (this.data.parameters[i].name === name) {
                    let value = configurations.get(name);
                    return value && value !== null ? value : this.data.parameters[i].defaultValue;
                }
            }
        } else {
            console.error("Job is not valid");
        }
        return null;
    }

    public enable(): void {
        JobFacade.enable(this.getName());
    }

    public disable(): void {
        JobFacade.disable(this.getName());
    }

    public trigger(parameters: { [key: string]: string } = {}): void {
        JobFacade.trigger(this.getName(), JSON.stringify(parameters));
    }

    public log(message: string): void {
        JobFacade.log(this.getName(), message);
    }

    public error(message: string): void {
        JobFacade.error(this.getName(), message);
    }

    public warn(message: string): void {
        JobFacade.warn(this.getName(), message);
    }

    public info(message: string): void {
        JobFacade.info(this.getName(), message);
    }
}

/**
 * Job Parameters object
 */
class JobParameters {

    private data: any[]

    constructor(data: any[]) {
        this.data = data;
    }

    public get(i: number): JobParameter {
        return new JobParameter(this.data[i]);
    }

    public count(): number {
        return this.data.length;
    }
}

/**
 * Job Parameter object
 */
class JobParameter {

    private data: any;

    constructor(data: any) {
        this.data = data;
    }

    public getName(): string {
        return this.data.name;
    }

    public getDescription(): string {
        return this.data.description;
    }

    public getType(): string {
        return this.data.type;
    }

    public getDefaultValue(): string {
        return this.data.defaultValue;
    }

    public getChoices(): string[] {
        return this.data.choices;
    }

}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Scheduler;
}
