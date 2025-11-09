const MongoDBFacade = Java.type("org.eclipse.dirigible.components.api.mongodb.MongoDBFacade");
const TimeUnit = Java.type("java.util.concurrent.TimeUnit");
import { UUID } from "sdk/utils/uuid";

/**
 * Define a common type for input to functions that accept either a plain JavaScript object
 * (which will be implicitly converted to DBObject) or an existing DBObject wrapper instance.
 */
type DBInput = { [key: string]: any } | DBObject | undefined | null;

/**
 * DBObject object represents a BSON document used for queries, insertions, and updates.
 * It wraps the underlying native Java object.
 */
export class DBObject {

    /**
     * The underlying native Java object representing the BSON document.
     * @private
     */
    public native: any;

    /**
     * Constructs a new DBObject instance.
     * @param native The native MongoDB object (e.g., com.mongodb.DBObject)
     */
    constructor(native: any) {
        this.native = native;
    }

    /**
     * Appends a key-value pair to the DBObject.
     * @param key The field name.
     * @param value The value to append.
     * @returns The current DBObject instance for chaining.
     */
    public append(key: string, value: any): DBObject {
        this.native.append(key, value);
        return this;
    }

    /**
     * Converts the DBObject to a standard JavaScript object representation (JSON).
     * @returns A plain JavaScript object.
     */
    public toJson(): { [key: string]: any } {
        return this.native.toJson();
    }

    /**
     * Marks the object as a partial object (used internally by MongoDB driver).
     */
    public markAsPartialObject(): void {
        this.native.markAsPartialObject();
    }

    /**
     * Checks if the object is a partial object.
     * @returns True if partial, false otherwise.
     */
    public isPartialObject(): boolean {
        return this.native.isPartialObject();
    }

    /**
     * Checks if the DBObject contains a field with the specified key.
     * @param key The field name.
     * @returns True if the field exists, false otherwise.
     */
    public containsField(key: string): boolean {
        return this.native.containsField(key);
    }

    /**
     * Gets the value associated with the given key.
     * @param key The field name.
     * @returns The field value.
     */
    public get(key: string): any {
        return this.native.get(key);
    }

    /**
     * Puts a key-value pair into the DBObject.
     * @param key The field name.
     * @param value The value to put.
     * @returns The previous value associated with the key, or null.
     */
    public put(key: string, value: any): any {
        return this.native.put(key, value);
    }

    /**
     * Removes a field from the DBObject.
     * @param key The field name to remove.
     * @returns The removed field value.
     */
    public removeField(key: string): any {
        return this.native.removeField(key);
    }

}

/**
 * Copies the properties from the native MongoDB object's JSON representation
 * onto the public properties of the DBObject wrapper instance.
 * This allows direct access like `dbObject.fieldName`.
 * @param dbObject The DBObject wrapper instance.
 * @returns The extracted properties object.
 */
function extract(dbObject: DBObject): { [key: string]: any } {
    if (!dbObject.native) {
        return {};
    }
    // Parse the native object's JSON representation
    var extracted = JSON.parse(dbObject.native.toJson());
    // Copy all properties to the wrapper instance
    for (var propertyName in extracted) {
        dbObject[propertyName] = extracted[propertyName];
    }
    return extracted;
}

/**
 * Helper function to implicitly convert a plain JS object to a DBObject,
 * or return the existing DBObject instance.
 * @param object Input object which can be a plain object, DBObject, or undefined/null.
 * @returns DBObject or undefined.
 */
function implicit(object: DBInput): DBObject | undefined {
    if (!object) {
        return undefined; // Explicitly return undefined if input is falsy
    }
    // Check if it already has the native property (assumes it's a DBObject or similar wrapper)
    if ((object as any).native) {
        return object as DBObject;
    }

    // Convert plain object to DBObject
    var dbObject = createBasicDBObject();

    for (var propertyName in object) {
        dbObject.append(propertyName, object[propertyName]);
    }
    return dbObject;
}

/**
 * Creates a new, empty DBObject instance.
 * @returns A new DBObject.
 */
export function createBasicDBObject(): DBObject {
    const dbObject = new DBObject(MongoDBFacade.createBasicDBObject());
    extract(dbObject);
    return dbObject;
}

/**
 * Client object wrapper for connecting to MongoDB.
 */
export class Client {

    /**
     * The underlying native MongoDB client object.
     * @private
     */
    private readonly native: any;

    /**
     * Constructs a new MongoDB Client instance.
     * @param uri The MongoDB connection URI.
     * @param user The username for authentication.
     * @param password The password for authentication.
     */
    constructor(uri: string, user: string, password: string) {
        this.native = MongoDBFacade.getClient(uri, user, password)
    }

    /**
     * Retrieves a database instance.
     * @param name Optional name of the database. If not provided, the default database name is used.
     * @returns A DB instance.
     */
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
 * DB object wrapper for a MongoDB database.
 */
export class DB {

    /**
     * The underlying native MongoDB DB object.
     * @private
     */
    private readonly native: any;

    /**
     * Constructs a new DB instance.
     * @param native The native MongoDB DB object.
     */
    constructor(native: any) {
        this.native = native;
    }

    /**
     * Retrieves a collection instance from the database.
     * @param name The name of the collection.
     * @returns A DBCollection instance.
     */
    public getCollection(name: string): DBCollection {
        const native = this.native.getCollection(name);
        return new DBCollection(native);
    }
}

/**
 * DBCollection object wrapper for a MongoDB collection.
 */
export class DBCollection {

    /**
     * The underlying native MongoDB DBCollection object.
     * @private
     */
    private readonly native: any;

    /**
     * Constructs a new DBCollection instance.
     * @param native The native MongoDB DBCollection object.
     */
    constructor(native: any) {
        this.native = native;
    }

    /**
     * Inserts a document into the collection.
     * @param dbObject The document to insert (can be a plain JS object or DBObject).
     */
    public insert(dbObject: DBInput): void {
        const dbo = implicit(dbObject);
        if (dbo) {
            this.native.insert(dbo.native);
        }
    }

    /**
     * Finds documents matching the query.
     * @param query The query specification (can be a plain JS object or DBObject).
     * @param projection The fields to include or exclude (can be a plain JS object or DBObject).
     * @returns A DBCursor for iterating over results.
     */
    public find(query?: DBInput, projection?: DBInput): DBCursor {
        const q = implicit(query);
        const p = implicit(projection);

        var native = null;
        if (q) {
            if (p) {
                native = this.native.find(q.native, p.native);
            } else {
                native = this.native.find(q.native);
            }
        } else {
            native = this.native.find();
        }

        return new DBCursor(native);
    }

    /**
     * Finds a single document matching the query.
     * @param query The query specification.
     * @param projection The fields to include or exclude.
     * @param sort The sorting specification.
     * @returns The found document as a DBObject.
     */
    public findOne(query: DBInput, projection: DBInput, sort: DBInput): DBObject {
        const q = implicit(query);
        const p = implicit(projection);
        const s = implicit(sort);

        var dbObject = createBasicDBObject();
        var native = null;
        if (q) {
            if (p) {
                if (s) {
                    native = this.native.findOne(q.native, p.native, s.native);
                } else {
                    native = this.native.findOne(q.native, p.native);
                }
            } else {
                native = this.native.findOne(q.native);
            }
        } else {
            native = this.native.findOne();
        }
        dbObject.native = native;
        extract(dbObject);
        return dbObject;
    }

    /**
     * Finds a single document by its string ID.
     * @param id The string ID of the document.
     * @param projection The fields to include or exclude.
     * @returns The found document as a DBObject.
     */
    public findOneById(id: string, projection?: DBInput): DBObject {
        const p = implicit(projection);
        const dbObject = createBasicDBObject();
        let native = null;
        if (p) {
            native = this.native.findOne(id, p.native);
        } else {
            native = this.native.findOne(id);
        }
        dbObject.native = native;
        extract(dbObject);
        return dbObject;
    }

    /**
     * Counts the number of documents in the collection, optionally filtered by a query.
     * @param query Optional query to filter the count.
     * @returns The number of documents.
     */
    public count(query?: DBInput): number {
        const q = implicit(query);
        if (q) {
            return this.native.count(q.native);
        }
        return this.native.count();
    }

    /**
     * Gets the count of documents (alias for count).
     * @param query Optional query to filter the count.
     * @returns The number of documents.
     */
    public getCount(query: DBInput): number {
        const q = implicit(query);
        if (q) {
            return this.native.getCount(q.native);
        }
        return this.native.getCount();
    }

    /**
     * Creates an index on the collection.
     * @param keys The index key specification.
     * @param options Optional index options.
     */
    public createIndex(keys: DBInput, options: DBInput): void {
        const k = implicit(keys);
        const o = implicit(options);
        if (k) {
            if (o) {
                this.native.createIndex(k.native, o.native);
            } else {
                this.native.createIndex(k.native);
            }
        } else {
            throw new Error("At least Keys parameter must be provided");
        }
    }

    /**
     * Creates an index on a single field by name.
     * @param name The name of the field to index.
     */
    public createIndexForField(name: string): void {
        if (name) {
            this.native.createIndex(name);
        } else {
            throw new Error("The filed name must be provided");
        }
    }

    /**
     * Retrieves the distinct values for a specified field across a collection.
     * NOTE: The signature in the original code seems slightly off compared to typical MongoDB drivers.
     * This implementation follows the original structure using `keys.native` if `keys` is provided.
     * @param name The field name.
     * @param query Optional query to filter results.
     * @param keys Optional keys to use for distinct (replaces 'name' if provided and query exists).
     */
    public distinct(name: string, query: DBInput, keys: DBInput): void {
        const q = implicit(query);
        const k = implicit(keys);
        if (name) {
            if (q) {
                // If query is provided, use keys for the field name if k is present, otherwise use 'name'
                this.native.distinct(k ? k.native : name, q.native);
            } else {
                this.native.distinct(name);
            }
        } else {
            throw new Error("At least the filed name parameter must be provided");
        }
    }

    /**
     * Drops a specified index.
     * @param index The name of the index or the DBObject representing the index keys.
     */
    public dropIndex(index: string | DBInput): void {
        if (typeof index === 'string') {
            this.native.dropIndex(index);
        } else {
            const dbo = implicit(index);
            if (dbo) {
                this.native.dropIndex(dbo.native);
            }
        }
    }

    /**
     * Drops a specified index by name.
     * @param name The name of the index.
     */
    public dropIndexByName(name: string): void {
        this.native.dropIndex(name);
    }

    /**
     * Drops all indexes on the collection.
     */
    public dropIndexes(): void {
        this.native.dropIndexes();
    }

    /**
     * Removes documents from the collection matching the query.
     * @param query The deletion query specification.
     */
    public remove(query: DBInput): void {
        const q = implicit(query);
        if (q) {
            this.native.remove(q.native);
        }
    }

    /**
     * Renames the collection.
     * @param newName The new name for the collection.
     */
    public rename(newName: string): void {
        this.native.rename(newName);
    }

    /**
     * Saves a document to the collection. If the document has an `_id`, it performs an update;
     * otherwise, it performs an insert.
     * @param dbObject The document to save.
     */
    public save(dbObject: DBInput): void {
        const dbo = implicit(dbObject);
        if (dbo) {
            this.native.save(dbo.native);
        }
    }

    /**
     * Updates documents in the collection matching the query.
     * @param query The update query specification.
     * @param update The update operation specification (e.g., {$set: {...}}).
     * @param upsert If true, creates a new document if no documents match the query.
     * @param multi If true, updates all documents matching the query; otherwise, only one.
     */
    public update(query: DBInput, update: DBInput, upsert?: boolean, multi?: boolean): void {
        const q = implicit(query);
        const u = implicit(update);
        if (q) {
            if (u) {
                if (upsert) {
                    if (multi) {
                        this.native.update(q.native, u.native, upsert, multi);
                    } else {
                        this.native.update(q.native, u.native, upsert);
                    }
                } else {
                    this.native.update(q.native, u.native);
                }
            } else {
                throw new Error("The update parameter must be provided");
            }
        } else {
            throw new Error("The query parameter must be provided");
        }
    }

    /**
     * Updates multiple documents in the collection matching the query.
     * (Equivalent to calling `update` with `multi=true` and `upsert=true` implicitly).
     * @param query The update query specification.
     * @param update The update operation specification.
     */
    public updateMulti(query: DBInput, update: DBInput): void {
        const q = implicit(query);
        const u = implicit(update);
        if (q) {
            if (u) {
                this.native.update(q.native, u.native, true, true); // Assuming updateMulti means upsert=true and multi=true
            } else {
                throw new Error("The update parameter must be provided");
            }
        } else {
            throw new Error("The query parameter must be provided");
        }
    };

    /**
     * Calculates the next sequential ID based on the largest existing `_id` in the collection.
     * Assumes `_id` is a numeric field.
     * @returns The next available sequential ID (starting at 1 if collection is empty).
     */
    public getNextId(): number {
        var cursor = this.find({}, { "_id": 1 }).sort({ "_id": -1 }).limit(1);
        if (!cursor.hasNext()) {
            return 1;
        } else {
            return cursor.next()["_id"] + 1;
        }
    }

    /**
     * Generates a new random UUID (Universally Unique Identifier).
     * @returns A string representing the UUID.
     */
    public generateUUID(): string {
        return UUID.random();
    }

}

/**
 * DBCursor object wrapper for iterating over results of a MongoDB query.
 */
export class DBCursor {

    /**
     * The underlying native MongoDB DBCursor object.
     * @private
     */
    private readonly native: any;

    /**
     * Constructs a new DBCursor instance.
     * @param native The native MongoDB DBCursor object.
     */
    constructor(native: any) {
        this.native = native;
    }

    /**
     * Returns the single result from the cursor.
     * @returns A DBObject representing the document.
     */
    public one(): DBObject {
        const dbObject = new DBObject(this.native.one());
        extract(dbObject);
        return dbObject;
    }

    /**
     * Sets the batch size for the cursor.
     * @param numberOfElements The batch size.
     * @returns The DBCursor instance for chaining.
     */
    public batchSize(numberOfElements: number): DBCursor {
        this.native.batchSize(numberOfElements);
        return this;
    }

    /**
     * Gets the current batch size.
     * @returns The batch size.
     */
    public getBatchSize(): number {
        return this.native.getBatchSize();
    }

    /**
     * Gets the collection associated with this cursor.
     * @returns The DBCollection instance.
     */
    public getCollection(): DBCollection {
        return new DBCollection(this.native.getCollection());
    }

    /**
     * Gets the cursor ID.
     * @returns The cursor ID string.
     */
    public getCursorId(): string {
        return this.native.getCursorId();
    }

    /**
     * Gets the projection object (fields wanted) used in the query.
     * @returns The projection DBObject.
     */
    public getKeysWanted(): DBObject {
        const dbObject = new DBObject(this.native.getKeysWanted());
        extract(dbObject);
        return dbObject;
    }

    /**
     * Gets the limit set on the cursor.
     * @returns The limit number.
     */
    public getLimit(): number {
        return this.native.getLimit();
    }

    /**
     * Closes the cursor.
     */
    public close(): void {
        this.native.close();
    }

    /**
     * Checks if there is a next document in the cursor.
     * @returns True if there is a next document, false otherwise.
     */
    public hasNext(): boolean {
        return this.native.hasNext();
    }

    /**
     * Retrieves the next document in the cursor.
     * @returns The next document as a DBObject.
     */
    public next(): DBObject {
        const dbObject = new DBObject(this.native.next());
        extract(dbObject);
        return dbObject;
    }

    /**
     * Gets the query object used to create this cursor.
     * @returns The query DBObject.
     */
    public getQuery(): DBObject {
        const dbObject = new DBObject(this.native.getQuery());
        extract(dbObject);
        return dbObject;
    }

    /**
     * Gets the number of documents matched by the query.
     * @returns The total number of documents.
     */
    public length(): number {
        return this.native.length();
    }

    /**
     * Specifies the order in which the query returns the results.
     * @param orderBy The sorting specification (e.g., {field: 1} for ascending).
     * @returns The DBCursor instance for chaining.
     */
    public sort(orderBy: DBInput): DBCursor {
        const dbo = implicit(orderBy);
        if (!dbo) {
            throw new Error("The orderBy parameter must be provided");
        }
        this.native.sort(dbo.native);
        return this;
    }

    /**
     * Limits the number of results to be returned.
     * @param limit The maximum number of documents to return.
     * @returns The DBCursor instance for chaining.
     */
    public limit(limit: number): DBCursor {
        this.native.limit(limit);
        return this;
    }

    /**
     * Specifies the exclusive upper bound for a specific index.
     * @param min The minimum value.
     * @returns The DBCursor instance for chaining.
     */
    public min(min: number): DBCursor {
        this.native.min(min);
        return this;
    }

    /**
     * Specifies the exclusive upper bound for a specific index.
     * @param max The maximum value.
     * @returns The DBCursor instance for chaining.
     */
    public max(max: number): DBCursor {
        this.native.max(max);
        return this;
    }

    /**
     * Sets a timeout for the server to execute the query.
     * @param maxTime The maximum time in milliseconds.
     * @returns The DBCursor instance for chaining.
     */
    public maxTime(maxTime: number): DBCursor {
        this.native.maxTime(maxTime, TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * Gets the size of the result set.
     * @returns The size number.
     */
    public size(): number {
        return this.native.size();
    }

    /**
     * Skips the specified number of documents.
     * @param numberOfElements The number of documents to skip.
     * @returns The DBCursor instance for chaining.
     */
    public skip(numberOfElements: number): DBCursor {
        this.native.skip(numberOfElements);
        return this;
    }

}