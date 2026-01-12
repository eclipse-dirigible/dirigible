const EtcdFacade = Java.type("org.eclipse.dirigible.components.api.etcd.EtcdFacade");

// =============================================================================
// UTILITY FUNCTIONS
// These functions handle the conversion between JavaScript/TypeScript types (string, Int8Array)
// and the native Java/Etcd types (ByteSequence, native byte arrays).
// =============================================================================

/**
 * Converts a string to a native Etcd ByteSequence object.
 * @param str The string to convert.
 * @returns The native ByteSequence object (Java type).
 */
function StringToByteSequence(str: string): any {
    return EtcdFacade.stringToByteSequence(str);
}

/**
 * Converts a JavaScript Int8Array (byte array) to a native Etcd ByteSequence object.
 * @param arr The Int8Array to convert.
 * @returns The native ByteSequence object (Java type).
 */
function ByteArrayToByteSequence(arr: Int8Array): any {
    return EtcdFacade.byteArrayToByteSequence(arr);
}

/**
 * Converts a native Etcd ByteSequence object back to a string.
 * @param value The native ByteSequence object.
 * @returns The decoded string.
 */
function ByteSequenceToString(value: any): string {
    return EtcdFacade.byteSequenceToString(value);
}

/**
 * Converts a native Java array of bytes into a JavaScript Int8Array.
 * @param value The native ByteSequence object containing the bytes.
 * @returns A JavaScript Int8Array.
 */
function BytesToArray(value: any): Int8Array {
    const array = value.getBytes(); // Get the native byte array
    const result = new Int8Array(array.length);
    for (let i = 0; i < array.length; i++) {
        // Conversion from Java byte to JavaScript Int8 value
        result[i] = parseInt(array[i], 10); 
    }
    return result;
}

/**
 * Helper to process a list of Key-Value (KV) objects into a standard JavaScript object.
 * @param kvsList The native list of KV objects.
 * @param func The conversion function for the value (e.g., ByteSequenceToString or BytesToArray).
 * @returns A standard JavaScript object mapping string keys to specified values.
 */
function KeyValueObject<T>(kvsList: any[], func: (value: any) => T): { [key: string]: T } {
    const kvObject: { [key: string]: T } = {};
    kvsList.forEach((kvs: any) => {
        const key: string = ByteSequenceToString(kvs.getKey());
        const value: T = func(kvs.getValue());
        kvObject[key] = value;
    });

    return kvObject;
}

/**
 * Processes native KVS list to a JavaScript object with string values.
 */
function KeyValueObjectString(kvsList: any[]): { [key: string]: string } {
    return KeyValueObject(kvsList, ByteSequenceToString);
}

/**
 * Processes native KVS list to a JavaScript object with Int8Array values.
 */
function KeyValueObjectByteArray(kvsList: any[]): { [key: string]: Int8Array } {
    return KeyValueObject(kvsList, BytesToArray);
}

// =============================================================================
// ETCD RESPONSE CLASSES
// =============================================================================

/**
 * Represents the header metadata of an Etcd response.
 */
export class Header {
    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    /** The revision of the key-value store when the request was processed. */
    public getRevision(): string {
        return this.native.getRevision();
    }

    /** The ID of the cluster which the request was sent to. */
    public getClusterId(): string {
        return this.native.getClusterId();
    }

    /** The ID of the member which the request was handled by. */
    public getMemberId(): string {
        return this.native.getMemberId();
    }

    /** The Raft term. */
    public getRaftTerm(): string {
        return this.native.getRaftTerm();
    }
}

/**
 * Represents the response object for a Get operation from Etcd.
 */
export class GetResponse {
    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    /** Retrieves the response header containing cluster metadata. */
    public getHeader(): Header {
        const nativeHeader = this.native.getHeader();
        return new Header(nativeHeader);
    }

    /** Retrieves the Key-Value pairs with values converted to strings. */
    public getKvsString(): { [key: string]: string } {
        return KeyValueObjectString(this.native.getKvs());
    }

    /** Retrieves the Key-Value pairs with values converted to Int8Array (byte arrays). */
    public getKvsByteArray(): { [key: string]: Int8Array } {
        return KeyValueObjectByteArray(this.native.getKvs());
    }

    /** Retrieves the number of Key-Value pairs returned. */
    public getCount(): number {
        return this.native.getCount();
    }
}


// =============================================================================
// ETCD CLIENT FACADE
// =============================================================================

/**
 * Client facade for interacting with the Etcd key-value store.
 */
export class Client {

    private readonly native: any;

    constructor() {
        this.native = EtcdFacade.getClient();
    }

    /**
     * Executes a blocking GET request on the specified key.
     * @param key The key to retrieve.
     * @returns The processed GetResponse object.
     */
    private get(key: string): GetResponse {
        // Etcd client returns a CompletableFuture which is blocked here using .get()
        const etcdCompletableFuture = this.native.get(StringToByteSequence(key));
        const nativeResponse = etcdCompletableFuture.get();
        return new GetResponse(nativeResponse);
    }

    /**
     * Puts (writes) a string value to the specified key.
     * @param key The key to write to.
     * @param value The string value.
     */
    public putStringValue(key: string, value: string): void {
        this.native.put(StringToByteSequence(key), StringToByteSequence(value));
    }

    /**
     * Puts (writes) a byte array value to the specified key.
     * @param key The key to write to.
     * @param value The Int8Array (byte array) value.
     */
    public putByteArrayValue(key: string, value: Int8Array): void {
        this.native.put(StringToByteSequence(key), ByteArrayToByteSequence(value));
    }

    /**
     * Retrieves the response header metadata for a key.
     * @param key The key to query.
     * @returns The {@link Header} object.
     */
    public getHeader(key: string): Header {
        return this.get(key).getHeader();
    }

    /**
     * Retrieves the Key-Value pairs as a JavaScript object with string values.
     * @param key The key (or key prefix) to query.
     * @returns An object mapping keys to string values.
     */
    public getKvsStringValue(key: string): { [key: string]: string } {
        return this.get(key).getKvsString();
    }

    /**
     * Retrieves the Key-Value pairs as a JavaScript object with Int8Array values.
     * @param key The key (or key prefix) to query.
     * @returns An object mapping keys to Int8Array values.
     */
    public getKvsByteArrayValue(key: string): { [key: string]: Int8Array } {
        return this.get(key).getKvsByteArray();
    }

    /**
     * Retrieves the count of Key-Value pairs matching the key (or key prefix).
     * @param key The key (or key prefix) to query.
     * @returns The count of matching entries.
     */
    public getCount(key: string): number {
        return this.get(key).getCount();
    }

    /**
     * Deletes the specified key.
     * @param key The key to delete.
     */
    public delete(key: string): void {
        this.native.delete(StringToByteSequence(key));
    }
}


// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Client;
}