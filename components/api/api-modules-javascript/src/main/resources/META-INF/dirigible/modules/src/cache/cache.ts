/**
 * @module cache/cache
 * @package @aerokit/sdk/cache
 * @name Cache
 * @overview
 * 
 * This module provides a `Cache` class that serves as a static utility for interacting with a server-side cache facade. The `Cache` class allows developers to perform simple key-value storage, retrieval, and invalidation operations on the cache, enabling efficient data management and performance optimization within the Dirigible environment.
 * 
 * ### Key Features
 * - Simple key-value storage: Store any serializable data in the cache using a unique key.
 * - Efficient retrieval: Quickly retrieve cached values using their associated keys.
 * - Cache invalidation: Remove specific entries or clear the entire cache when needed.
 * - Server-side management: The cache is managed on the server, allowing for centralized control over caching behavior and policies.
 * 
 * ### Use Cases
 * - Caching frequently accessed data to improve performance and reduce latency.
 * - Storing intermediate results of expensive computations or API calls for reuse.
 * - Managing session data or user-specific information in a scalable manner.
 * - Implementing application-level caching strategies to optimize resource usage and response times.
 * 
 * ### Example Usage
 * ```ts
 * import { Cache } from "@aerokit/sdk/cache";
 * 
 * // Store a value in the cache
 * Cache.set("user_123", { name: "Alice", age: 30 });
 * // Retrieve a value from the cache
 * const userData = Cache.get("user_123");
 * console.log(userData); // Output: { name: "Alice", age: 30 }
 * // Check if a key exists in the cache
 * const exists = Cache.contains("user_123");
 * console.log(exists); // Output: true
 * // Delete a specific key from the cache
 * Cache.delete("user_123");
 * // Clear the entire cache
 * Cache.clear();
 * ```
 */

const CacheFacade = Java.type("org.eclipse.dirigible.components.api.cache.CacheFacade");

export class Cache {

    /**
     * Checks if the cache contains a value for the specified key.
     * @param key The key to check.
     * @returns True if the key exists in the cache, false otherwise.
     */
    public static contains(key: string): boolean {
        return CacheFacade.contains(key);
    }

    /**
     * Retrieves the value associated with the specified key from the cache.
     * @param key The key to retrieve.
     * @returns The cached value, or `undefined` if the key is not found.
     */
    public static get(key: any): any | undefined {
        return CacheFacade.get(key);
    }

    /**
     * Stores a value in the cache under the specified key.
     * Note: The duration/time-to-live (TTL) is typically configured server-side.
     * @param key The key to store the data under.
     * @param data The data to store.
     */
    public static set(key: string, data: any): void {
        CacheFacade.set(key, data);
    }

    /**
     * Removes the key and its associated value from the cache.
     * @param key The key to delete.
     */
    public static delete(key: string): void {
        CacheFacade.delete(key);
    }

    /**
     * Clears all entries from the cache.
     */
    public static clear(): void {
        CacheFacade.clear();
    }
}