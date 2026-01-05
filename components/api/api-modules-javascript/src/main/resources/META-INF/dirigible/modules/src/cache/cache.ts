/**
 * Cache
 * * Provides a static utility for interacting with a server-side cache facade, enabling
 * simple key-value storage, retrieval, and invalidation operations.
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