/**
 * API Cache
 */
const CacheFacade = Java.type("org.eclipse.dirigible.components.api.cache.CacheFacade");
export class Cache {

    public static contains(key: string): boolean {
        return CacheFacade.contains(key);
    }

    public static get(key: any): any | undefined {
        return CacheFacade.get(key);
    }

    public static set(key: string, data: any): void {
        CacheFacade.set(key, data);
    }

    public static delete(key: string): void {
        CacheFacade.delete(key);
    }

    public static clear(): void {
        CacheFacade.clear();
    }
}
