/**
 * @module db/repository
 * @package @aerokit/sdk/db
 * @name Repository
 * @overview
 * 
 * This module provides an abstract `Repository` class that serves as a base for data access and business logic layers in the Dirigible environment. The `Repository` class wraps the underlying `store` API, providing a structured way to perform CRUD operations on entities while also handling entity metadata, translation, and event triggering. It is designed to be extended by specific repositories for different entities, allowing for customization of behavior and event handling.
 * 
 * ### Key Features
 * - Abstract base class for repositories with built-in support for entity metadata management.
 * - CRUD operations (Create, Read, Update, Delete) with automatic translation of results.
 * - Event triggering mechanism for create, update, and delete operations, allowing for custom logic to be executed in response to these events.
 * - Type safety through TypeScript generics, ensuring that repositories are strongly typed according to their entity definitions.
 * 
 * ### Use Cases
 * - Implementing data access layers for various entities in an application, providing a consistent and reusable approach to database interactions.
 * - Adding custom business logic or side effects in response to entity lifecycle events (e.g., logging, notifications, cache invalidation).
 * - Managing entity metadata and translations in a centralized manner within the repository layer.
 * 
 * ### Example Usage
 * ```ts
 * import { Repository } from "@aerokit/sdk/db";
 * 
 * // Define an entity interface
 * interface User {
 *   id: number;
 *   name: string;
 *   email: string;
 * }
 * 
 * // Create a UserRepository by extending the Repository class
 * class UserRepository extends Repository<User> {
 *   constructor() {
 *     super(User);
 *   }
 * 
 *   // Override triggerEvent to add custom logic on create/update/delete
 *   protected async triggerEvent(data: EntityEvent<User>): Promise<void> {
 *     console.log(`User ${data.operation}d:`, data.entity);
 *     // Additional logic such as sending notifications or updating related data can be added here
 *   }
 * }
 * 
 * // Example of using the UserRepository
 * const userRepo = new UserRepository();
 * const newUserId = userRepo.create({ name: "Alice", email: "alice@example.com" });
 * const user = userRepo.findById(newUserId);
 * console.log(user); // Output: { id: 1, name: "Alice", email: "alice@example.com" }
 * userRepo.update({ id: newUserId, name: "Alice Smith", email: "alice.smith@example.com" });
 * userRepo.deleteById(newUserId);
 * ```
 */

import { store, translator, EntityConstructor, Options } from "@aerokit/sdk/db";

/**
 * Represents the data structure passed to the event trigger method before/after an operation.
 */
export interface EntityEvent<T> {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<T>; // Use Partial<T> for create/delete where some fields might be missing
    readonly key: {
        name: string;
        column: string;
        value: string | number;
    }
    readonly previousEntity?: T;
}


// --- Repository Class ---

/**
 * Abstract base class for data access/business logic, wrapping the `store` API.
 * It handles entity metadata lookup, CRUD operations, translation, and event triggering.
 * @template T The entity type (must be an object).
 */
export abstract class Repository<T extends Record<string, any>> {

    private entityConstructor: EntityConstructor;

    constructor(entityConstructor: EntityConstructor) {
        this.entityConstructor = entityConstructor;
        
        // Caches entity metadata (name, table, id) onto the constructor function for static access
		if (!this.entityConstructor.$entity_name) {
            // Assumes store methods return non-null strings
			this.entityConstructor.$entity_name = store.getEntityName(this.entityConstructor.name);
			this.entityConstructor.$table_name = store.getTableName(this.entityConstructor.name);
			this.entityConstructor.$id_name = store.getIdName(this.entityConstructor.name);
			this.entityConstructor.$id_column = store.getIdColumn(this.entityConstructor.name);
		}
    }

    protected getEntityName(): string {
        // Use non-null assertion since the constructor guarantees these properties exist
        return this.entityConstructor.$entity_name!;
    }

    protected getTableName(): string {
        return this.entityConstructor.$table_name!;
    }

    protected getIdName(): string {
        return this.entityConstructor.$id_name!;
    }

    protected getIdColumn(): string {
        return this.entityConstructor.$id_column!;
    }

    /**
     * Finds all entities matching the given options.
     */
    public findAll(options: Options = {}): T[] {
        // Assume store.list returns T[] but we explicitly cast it to T[]
        const list: T[] = store.list(this.getEntityName(), options);
        translator.translateList(list, options?.language, this.getTableName());
        return list;
    }

    /**
     * Finds a single entity by its primary key ID.
     */
    public findById(id: number | string, options: Options = {}): T | undefined {
        // Assume store.get returns T or null/undefined
        const entity: T | null = store.get(this.getEntityName(), id);
        translator.translateEntity(entity, id, options?.language, this.getTableName());
        return entity ?? undefined;
    }

    /**
     * Creates a new entity in the database.
     * @returns The generated ID (string or number).
     */
    public create(entity: T): string | number {
        const id = store.save(this.getEntityName(), entity);
        this.triggerEvent({
            operation: "create",
            table: this.getTableName(),
            entity: entity,
            key: {
                name: this.getIdName(),
                column: this.getIdColumn(),
                value: id
            }
        });
        return id;
    }

    /**
     * Updates an existing entity.
     * The entity must contain the primary key.
     */
    public update(entity: T): void {
        const idName = this.getIdName();
        const id = entity[idName] as (number | string);
        
        // Retrieve the entity state before update for the event payload
        const previousEntity = this.findById(id);

        store.update(this.getEntityName(), entity);
        
        this.triggerEvent({
            operation: "update",
            table: this.getTableName(),
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: idName,
                column: this.getIdColumn(),
                value: id
            }
        });
    }

    /**
     * Creates the entity if the ID is null/undefined, otherwise updates it.
     * If an ID is provided but the entity doesn't exist, it creates it.
     * @returns The entity's ID.
     */
    public upsert(entity: T): string | number {
        const id = entity[this.getIdName()];
        
        // If no ID is present, save (create)
        if (id === null || id === undefined) {
            return store.save(this.getEntityName(), entity);
        }

        // If ID is present, check existence
        const existingEntity = store.get(this.getEntityName(), id);
        
        if (existingEntity) {
            this.update(entity);
            return id;
        } else {
            // ID exists, but entity does not -> save (create with provided ID)
            return store.save(this.getEntityName(), entity);
        }
    }

    /**
     * Deletes an entity by its primary key ID.
     */
    public deleteById(id: number | string): void {
        // Retrieve entity before removal for the event payload
        const entity = store.get(this.getEntityName(), id);
        
        store.remove(this.getEntityName(), id);

        this.triggerEvent({
            operation: "delete",
            table: this.getTableName(),
            entity: entity,
            key: {
                name: this.getIdName(),
                column: this.getIdColumn(),
                value: id
            }
        });
    }

    /**
     * Counts the number of entities matching the given options.
     */
    public count(options?: Options): number {
        return store.count(this.getEntityName(), options);
    }

    /**
     * Protected method intended for subclass overriding or internal event handling.
     */
    protected async triggerEvent(_data: EntityEvent<T>): Promise<void> {
        // Empty body as in the original code
    }
}
