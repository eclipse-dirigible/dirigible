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

const MongoDBFacade = Java.type("org.eclipse.dirigible.components.api.mongodb.MongoDBFacade");
const TimeUnit = Java.type("java.util.concurrent.TimeUnit");
import { UUID } from "sdk/utils/uuid";

export function createBasicDBObject(): DBObject {
    const dbObject = new DBObject(MongoDBFacade.createBasicDBObject());
    extract(dbObject);
    return dbObject;
}

/**
 * Client object
 */
export class Client {

    private readonly native: any;

    constructor(uri: string, user: string, password: string) {
        this.native = MongoDBFacade.getClient(uri, user, password)
    }

    public getDB(name?: string): DB {
        let native = null;
        if (name) {
            native = this.native.getDB(name);
        } else {
            const defaultDB = MongoDBFacade.getDefaultDatabaseName();
            native = this.native.getDB(defaultDB);
        }

        return new DB(native);
    }
}

/**
 * DB object
 */
export class DB {

    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    public getCollection(name: string): DBCollection {
        const native = this.native.getCollection(name);
        return new DBCollection(native);
    }
}

/**
 * DBCollection object
 */
export class DBCollection {

    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    public insert(dbObject): void {
        dbObject = implicit(dbObject);
        this.native.insert(dbObject.native);
    }

    public find(query?, projection?): DBCursor {
        query = implicit(query);
        projection = implicit(projection);

        var native = null;
        if (query) {
            if (projection) {
                native = this.native.find(query.native, projection.native);
            } else {
                native = this.native.find(query.native);
            }
        } else {
            native = this.native.find();
        }

        return new DBCursor(native);
    }

    public findOne(query, projection, sort): DBObject {
        query = implicit(query);
        projection = implicit(projection);
        var dbObject = createBasicDBObject();
        var native = null;
        if (query) {
            if (projection) {
                if (sort) {
                    native = this.native.findOne(query.native, projection.native, sort.native);
                } else {
                    native = this.native.findOne(query.native, projection.native);
                }
            } else {
                native = this.native.findOne(query.native);
            }
        } else {
            native = this.native.findOne();
        }
        dbObject.native = native;
        extract(dbObject);
        return dbObject;
    }

    public findOneById(id: string, projection?): DBObject {
        projection = implicit(projection);
        const dbObject = createBasicDBObject();
        let native = null;
        if (projection) {
            native = this.native.findOne(id, projection.native);
        } else {
            native = this.native.findOne(id);
        }
        dbObject.native = native;
        extract(dbObject);
        return dbObject;
    }

    public count(query?): number {
        query = implicit(query);
        if (query) {
            return this.native.count(query.native);
        }
        return this.native.count();
    }

    public getCount(query): number {
        query = implicit(query);
        if (query) {
            return this.native.getCount(query.native);
        }
        return this.native.getCount();
    }

    public createIndex(keys, options): void {
        keys = implicit(keys);
        options = implicit(options);
        if (keys) {
            if (options) {
                this.native.createIndex(keys.native, options.native);
            } else {
                this.native.createIndex(keys.native);
            }
        } else {
            throw new Error("At least Keys parameter must be provided");
        }
    }

    public createIndexForField(name): void {
        if (name) {
            this.native.createIndex(name);
        } else {
            throw new Error("The filed name must be provided");
        }
    }

    public distinct(name, query, keys): void {
        query = implicit(query);
        if (name) {
            if (query) {
                this.native.distinct(keys.native, query.native);
            } else {
                this.native.distinct(name);
            }
        } else {
            throw new Error("At least the filed name parameter must be provided");
        }
    }

    public dropIndex(index): void {
        this.native.dropIndex(index);
    }

    public dropIndexByName(name: string): void {
        this.native.dropIndex(name);
    }

    public dropIndexes(): void {
        this.native.dropIndexes();
    }

    public remove(query): void {
        query = implicit(query);
        this.native.remove(query.native);
    }

    public rename(newName: string): void {
        this.native.rename(newName);
    }

    public save(dbObject): void {
        dbObject = implicit(dbObject);
        this.native.save(dbObject.native);
    }

    public update(query, update, upsert?, multi?): void {
        query = implicit(query);
        update = implicit(update);
        if (query) {
            if (update) {
                if (upsert) {
                    if (multi) {
                        this.native.update(query.native, update.native, upsert, multi);
                    } else {
                        this.native.update(query.native, update.native, upsert);
                    }
                } else {
                    this.native.update(query.native, update.native);
                }
            } else {
                throw new Error("The query parameter must be provided");
            }
        } else {
            throw new Error("The query parameter must be provided");
        }
    }

    public updateMulti(query, update): void {
        query = implicit(query);
        update = implicit(update);
        if (query) {
            if (update) {
                this.native.update(query.native, update.native);
            } else {
                throw new Error("The query parameter must be provided");
            }
        } else {
            throw new Error("The query parameter must be provided");
        }
    };

    public getNextId(): number {
        var cursor = this.find({}, { "_id": 1 }).sort({ "_id": -1 }).limit(1);
        if (!cursor.hasNext()) {
            return 1;
        } else {
            return cursor.next()["_id"] + 1;
        }
    }

    public generateUUID(): string {
        return UUID.random();
    }

}

/**
 * DBCursor object
 */
export class DBCursor {

    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    public one(): DBObject {
        const dbObject = new DBObject(this.native.one());
        extract(dbObject);
        return dbObject;
    }

    public batchSize(numberOfElements: number): DBCursor {
        this.native.batchSize(numberOfElements);
        return this;
    }

    public getBatchSize(): number {
        return this.native.getBatchSize();
    }

    public getCollection(): DBCollection {
        return new DBCollection(this.native.getCollection());
    }

    public getCursorId(): string {
        return this.native.getCursorId();
    }

    public getKeysWanted(): DBObject {
        const dbObject = new DBObject(this.native.getKeysWanted());
        extract(dbObject);
        return dbObject;
    }

    public getLimit(): number {
        return this.native.getLimit();
    }

    public close(): void {
        this.native.close();
    }

    public hasNext(): boolean {
        return this.native.hasNext();
    }

    public next(): DBObject {
        const dbObject = new DBObject(this.native.next());
        extract(dbObject);
        return dbObject;
    }

    public getQuery(): DBObject {
        const dbObject = new DBObject(this.native.getQuery());
        extract(dbObject);
        return dbObject;
    }

    public length(): number {
        return this.native.length();
    }

    public sort(orderBy): DBCursor {
        orderBy = implicit(orderBy);
        if (!orderBy) {
            throw new Error("The orderBy parameter must be provided");
        }
        this.native.sort(orderBy.native);
        return this;
    }

    public limit(limit: number): DBCursor {
        this.native.limit(limit);
        return this;
    }

    public min(min: number): DBCursor {
        this.native.min(min);
        return this;
    }

    public max(max: number): DBCursor {
        this.native.max(max);
        return this;
    }

    public maxTime(maxTime: number): DBCursor {
        this.native.maxTime(maxTime, TimeUnit.MILLISECONDS);
        return this;
    }

    public size(): number {
        return this.native.size();
    }

    public skip(numberOfElements: number): DBCursor {
        this.native.skip(numberOfElements);
        return this;
    }

}

/**
 * DBObject object
 */
export class DBObject {

    public native: any;

    constructor(native: any) {
        this.native = native;
    }

    public append(key: string, value: any): DBObject {
        this.native.append(key, value);
        return this;
    }

    public toJson(): { [key: string]: any } {
        return this.native.toJson();
    }

    public markAsPartialObject(): void {
        this.native.markAsPartialObject();
    }

    public isPartialObject(): boolean {
        return this.native.isPartialObject();
    }

    public containsField(key: string): boolean {
        return this.native.containsField(key);
    }

    public get(key: string): any {
        return this.native.get(key);
    }

    public put(key: string, value: any): any {
        return this.native.put(key, value);
    }

    public removeField(key: string): any {
        return this.native.removeField(key);
    }

}

function extract(dbObject) {
    if (!dbObject.native) {
        return {};
    }
    var extracted = JSON.parse(dbObject.native.toJson());
    for (var propertyName in extracted) {
        dbObject[propertyName] = extracted[propertyName];
    }
}

function implicit(object: { [key: string]: any } | { native: any } | undefined) {
    if (!object) {
        return object;
    }
    if (object.native) {
        return object;
    }
    var dbObject = createBasicDBObject();

    for (var propertyName in object) {
        dbObject.append(propertyName, object[propertyName]);
    }
    return dbObject;
}

