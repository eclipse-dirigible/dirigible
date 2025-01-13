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

const RedisFacade = Java.type("org.eclipse.dirigible.components.api.redis.RedisFacade");

export class Client {
    private readonly native: any;

    constructor() {
        this.native = RedisFacade.getClient();
    }

    public append(key: string, value: string): number {
        return this.native.append(key, value);
    }

    public bitcount(key: string): number {
        return this.native.bitcount(key);
    }

    public decr(key: string): number {
        return this.native.decr(key);
    }

    public del(key: string): number {
        return this.native.del(key);
    }

    public exists(key: string): boolean {
        return this.native.exists(key);
    }

    public get(key: string): string {
        return this.native.get(key);
    }

    public incr(key: string): number {
        return this.native.incr(key);
    }

    public keys(pattern: string): string[] {
        return this.native.keys(pattern);
    }

    public set(key: string, value: string): string {
        return this.native.set(key, value);
    }

    // Lists

    public lindex(key: string, index: number): string {
        return this.native.lindex(key, index);
    }

    public llen(key: string): number {
        return this.native.llen(key);
    }

    public lpop(key: string): string {
        return this.native.lpop(key);
    }

    public lpush(key: string, ...value: string[]) {
        return this.native.lpush(key, value);
    }

    public lrange(key: string, start: number, stop: number): string[] {
        return this.native.lrange(key, start, stop);
    }

    public rpop(key: string): string {
        return this.native.rpop(key);
    }

    public rpush(key: string, ...value: string[]): number {
        return this.native.rpush(key, value);
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
    // @ts-ignore
    module.exports = Client;
}
