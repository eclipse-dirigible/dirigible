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

const EtcdFacade = Java.type("org.eclipse.dirigible.components.api.etcd.EtcdFacade");

export class Client {

    private readonly native: any;

    constructor() {
        this.native = EtcdFacade.getClient();
    }

    public putStringValue(key: string, value: string): void {
        this.native.put(StringToByteSequence(key), StringToByteSequence(value));
    }

    public putByteArrayValue(key: string, value: Int8Array): void {
        this.native.put(StringToByteSequence(key), ByteArrayToByteSequence(value));
    }

    public getHeader(key: string): Header {
        return this.get(key, this.native).getHeader();
    }

    public getKvsStringValue(key: string): { [key: string]: string } {
        return this.get(key, this.native).getKvsString();
    }

    public getKvsByteArrayValue(key: string): { [key: string]: Int8Array } {
        return this.get(key, this.native).getKvsByteArray();
    }

    public getCount(key: string): number {
        return this.get(key, this.native).getCount();
    }

    public delete(key: string): void {
        this.native.delete(StringToByteSequence(key));
    }

    private get(key: string, native: any) {
        const etcdCompletableFuture = native.get(StringToByteSequence(key));
        var native = etcdCompletableFuture.get();
        return new GetResponse(native);
    }
}

class GetResponse {
    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    public getHeader(): Header {
        var native = this.native.getHeader();
        return new Header(native);
    }

    public getKvsString(): { [key: string]: string } {
        return KeyValueObjectString(this.native.getKvs());
    }

    public getKvsByteArray(): { [key: string]: Int8Array } {
        return KeyValueObjectByteArray(this.native.getKvs());
    }

    public getCount(): number {
        return this.native.getCount();
    }
}

class Header {
    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    public getRevision(): string {
        return this.native.getRevision();
    }

    public getClusterId(): string {
        return this.native.getClusterId();
    }

    public getMemberId(): string {
        return this.native.getMemberId();
    }

    public getRaftTerm(): string {
        return this.native.getRaftTerm();
    }
}

function StringToByteSequence(str) {
    return EtcdFacade.stringToByteSequence(str);
}

function ByteArrayToByteSequence(arr) {
    return EtcdFacade.byteArrayToByteSequence(arr);
}

function ByteSequenceToString(value) {
    return EtcdFacade.byteSequenceToString(value);
}

function KeyValueObjectString(kvsList) {
    return KeyValueObject(kvsList, ByteSequenceToString);
}

function KeyValueObjectByteArray(kvsList) {
    return KeyValueObject(kvsList, BytesToArray);
}

function KeyValueObject(kvsList, func) {
    var kvObject = {};
    kvsList.forEach(kvs => {
        var key = ByteSequenceToString(kvs.getKey());
        var value = func(kvs.getValue());
        Object.assign(kvObject, { [key]: value });
    })

    return kvObject;
}

function BytesToArray(value) {
    var array = value.getBytes();
    var result = new Int8Array(array.length);
    for (var i = 0; i < array.length; i++) {
        result[i] = parseInt(array[i]);
    }
    return result;
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Client;
}
