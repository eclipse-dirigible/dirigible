/**
 * @module redis/client
 * @package @aerokit/sdk/redis
 * @name Client
 * @overview
 * 
 * The Redis Client module provides a JavaScript interface for interacting with Redis, a popular in-memory data structure store. This module serves as a facade for the underlying Java Redis client implementation, allowing developers to perform common Redis operations such as key-value manipulation and list management using a simple and intuitive API. The Client class abstracts the complexities of the Java implementation, providing a seamless experience for developers working with Redis in their applications built on the platform.
 * 
 * ### Key Features:
 * - **Key-Value Operations**: Methods for setting, getting, deleting, and checking the existence of keys, as well as incrementing and decrementing numeric values.
 * - **List Operations**: Methods for managing lists, including pushing and popping elements from both ends of the list and retrieving list elements by index or range.
 * 
 * ### Use Cases:
 * - **Caching**: The Redis Client can be used to implement caching mechanisms in applications, improving performance by storing frequently accessed data in memory.
 * - **Session Management**: Developers can use this module to manage user sessions in web applications by storing session data in Redis.
 * - **Real-Time Data Processing**: The Redis Client is ideal for applications that require real-time data processing and quick access to data structures like lists and sets.
 * 
 * ### Example Usage:
 * ```ts
 * import { Client } from "@aerokit/sdk/redis";
 * 
 * const redisClient = new Client();
 * 
 * // Set a key-value pair
 * redisClient.set("myKey", "Hello, Redis!");
 * 
 * // Get the value of a key
 * const value = redisClient.get("myKey");
 * console.log(value); // Output: Hello, Redis!
 * 
 * // Push values to a list
 * redisClient.rpush("myList", "item1", "item2", "item3");
 * ```
 */

const RedisFacade = Java.type("org.eclipse.dirigible.components.api.redis.RedisFacade");

export class Client {
    private readonly native: any;

    /**
     * Initializes the Redis Client and retrieves the native client instance
     * from the Redis Facade.
     */
    constructor() {
        this.native = RedisFacade.getClient();
    }

    // --- Key-Value and General Operations ---

    /**
     * Appends a value to the value of a key. If the key does not exist,
     * it is created and set to the initial value.
     *
     * @param key The key to append to.
     * @param value The value string to append.
     * @returns The length of the string after the append operation.
     */
    public append(key: string, value: string): number {
        return this.native.append(key, value);
    }

    /**
     * Counts the number of set bits (1s) in the string value of a key.
     *
     * @param key The key to perform the bitcount on.
     * @returns The number of set bits.
     */
    public bitcount(key: string): number {
        return this.native.bitcount(key);
    }

    /**
     * Decrements the number stored at key by one.
     *
     * @param key The key holding the numeric value.
     * @returns The value of key after the decrement.
     */
    public decr(key: string): number {
        return this.native.decr(key);
    }

    /**
     * Deletes the specified key.
     *
     * @param key The key to delete.
     * @returns The number of keys that were removed (1 if successful, 0 otherwise).
     */
    public del(key: string): number {
        return this.native.del(key);
    }

    /**
     * Checks if the specified key exists.
     *
     * @param key The key to check.
     * @returns True if the key exists, false otherwise.
     */
    public exists(key: string): boolean {
        return this.native.exists(key);
    }

    /**
     * Gets the value of the specified key.
     *
     * @param key The key to retrieve the value for.
     * @returns The value of the key, or null if the key does not exist.
     */
    public get(key: string): string {
        return this.native.get(key);
    }

    /**
     * Increments the number stored at key by one.
     *
     * @param key The key holding the numeric value.
     * @returns The value of key after the increment.
     */
    public incr(key: string): number {
        return this.native.incr(key);
    }

    /**
     * Finds all keys matching the given pattern.
     *
     * @param pattern The pattern to match keys against (e.g., "user:*").
     * @returns An array of matching keys.
     */
    public keys(pattern: string): string[] {
        return this.native.keys(pattern);
    }

    /**
     * Sets the string value of a key.
     *
     * @param key The key to set.
     * @param value The string value to assign to the key.
     * @returns 'OK' on success.
     */
    public set(key: string, value: string): string {
        return this.native.set(key, value);
    }

    // --- List Operations ---

    /**
     * Gets an element from a list by its zero-based index.
     *
     * @param key The key of the list.
     * @param index The zero-based index (0 is the first element, -1 is the last).
     * @returns The element at the specified index, or null if the index is out of range.
     */
    public lindex(key: string, index: number): string {
        return this.native.lindex(key, index);
    }

    /**
     * Gets the length of the list stored at the key.
     *
     * @param key The key of the list.
     * @returns The length of the list.
     */
    public llen(key: string): number {
        return this.native.llen(key);
    }

    /**
     * Removes and returns the first element of the list stored at the key (Left POP).
     *
     * @param key The key of the list.
     * @returns The first element of the list, or null when the list is empty.
     */
    public lpop(key: string): string {
        return this.native.lpop(key);
    }

    /**
     * Inserts all specified values at the head of the list stored at the key (Left PUSH).
     *
     * @param key The key of the list.
     * @param value One or more values to prepend to the list.
     * @returns The new length of the list.
     */
    public lpush(key: string, ...value: string[]) {
        return this.native.lpush(key, value);
    }

    /**
     * Returns the specified elements of the list stored at the key.
     *
     * @param key The key of the list.
     * @param start The starting zero-based offset.
     * @param stop The stopping zero-based offset.
     * @returns An array of elements in the specified range.
     */
    public lrange(key: string, start: number, stop: number): string[] {
        return this.native.lrange(key, start, stop);
    }

    /**
     * Removes and returns the last element of the list stored at the key (Right POP).
     *
     * @param key The key of the list.
     * @returns The last element of the list, or null when the list is empty.
     */
    public rpop(key: string): string {
        return this.native.rpop(key);
    }

    /**
     * Inserts all specified values at the tail of the list stored at the key (Right PUSH).
     *
     * @param key The key of the list.
     * @param value One or more values to append to the list.
     * @returns The new length of the list.
     */
    public rpush(key: string, ...value: string[]): number {
        return this.native.rpush(key, value);
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
    // @ts-ignore
    module.exports = Client;
}