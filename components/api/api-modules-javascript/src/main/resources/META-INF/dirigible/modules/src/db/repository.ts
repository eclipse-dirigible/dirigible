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
			this.entityConstructor.$entity_name = (store as any).getEntityName(this.entityConstructor.name);
			this.entityConstructor.$table_name = (store as any).getTableName(this.entityConstructor.name);
			this.entityConstructor.$id_name = (store as any).getIdName(this.entityConstructor.name);
			this.entityConstructor.$id_column = (store as any).getIdColumn(this.entityConstructor.name);
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
        const list: T[] = (store as any).list(this.getEntityName(), options);
        (translator as any).translateList(list, options.language, this.getTableName());
        return list;
    }

    /**
     * Finds a single entity by its primary key ID.
     */
    public findById(id: number | string, options: Options = {}): T | undefined {
        // Assume store.get returns T or null/undefined
        const entity: T | null = (store as any).get(this.getEntityName(), id);
        (translator as any).translateEntity(entity, id, options.language, this.getTableName());
        return entity ?? undefined;
    }

    /**
     * Creates a new entity in the database.
     * @returns The generated ID (string or number).
     */
    public create(entity: T): string | number {
        const id = (store as any).save(this.getEntityName(), entity);
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

        (store as any).update(this.getEntityName(), entity);
        
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
            return (store as any).save(this.getEntityName(), entity);
        }

        // If ID is present, check existence
        const existingEntity = (store as any).get(this.getEntityName(), id);
        
        if (existingEntity) {
            this.update(entity);
            return id;
        } else {
            // ID exists, but entity does not -> save (create with provided ID)
            return (store as any).save(this.getEntityName(), entity);
        }
    }

    /**
     * Deletes an entity by its primary key ID.
     */
    public deleteById(id: number | string): void {
        // Retrieve entity before removal for the event payload
        const entity = (store as any).get(this.getEntityName(), id);
        
        (store as any).remove(this.getEntityName(), id);

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
        return (store as any).count(this.getEntityName(), options);
    }

    /**
     * Protected method intended for subclass overriding or internal event handling.
     */
    protected async triggerEvent(_data: EntityEvent<T>): Promise<void> {
        // Empty body as in the original code
    }
}
