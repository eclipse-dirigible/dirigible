import { store, translator, Options, EntityConstructor } from "sdk/db";

export interface EntityEvent<T> {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<T>;
    readonly key: {
        name: string;
        column: string;
        value: string | number;
    }
    readonly previousEntity?: T;
}

export abstract class Repository<T extends Record<string, any>> {

    private entityConstructor: EntityConstructor;

    constructor(entityConstructor: EntityConstructor) {
        this.entityConstructor = entityConstructor;
		if (!this.entityConstructor.$entity_name) {
			this.entityConstructor.$entity_name = store.getEntityName(this.entityConstructor.name);
			this.entityConstructor.$table_name = store.getTableName(this.entityConstructor.name);
			this.entityConstructor.$id_name = store.getIdName(this.entityConstructor.name);
			this.entityConstructor.$id_column = store.getIdColumn(this.entityConstructor.name);
		}
    }

    protected getEntityName(): string {
        return this.entityConstructor.$entity_name;
    }

    protected getTableName(): string {
        return this.entityConstructor.$table_name;
    }

    protected getIdName(): string {
        return this.entityConstructor.$id_name;
    }

    protected getIdColumn(): string {
        return this.entityConstructor.$id_column;
    }

    public findAll(options: Options = {}): T[] {
        let list = store.list(this.getEntityName(), options);
        translator.translateList(list, options.language, this.getTableName());
        return list;
    }

    public findById(id: number, options: Options = {}): T | undefined {
        const entity = store.get(this.getEntityName(), id);
        translator.translateEntity(entity, id, options.language, this.getTableName());
        return entity ?? undefined;
    }

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

    public update(entity: T): void {
        const previousEntity = this.findById(entity[this.getIdName()]);
        store.update(this.getEntityName(), entity);
        this.triggerEvent({
            operation: "update",
            table: this.getTableName(),
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: this.getIdName(),
                column: this.getIdColumn(),
                value: entity[this.getIdName()]
            }
        });
    }

    public upsert(entity: T): string | number {
        const id = entity[this.getIdName()];
        if (!id) {
            return store.save(this.getEntityName(), entity);
        }

        const existingEntity = store.get(this.getEntityName(), id);
        if (existingEntity) {
            this.update(entity);
            return id;
        } else {
            return store.save(this.getEntityName(), entity);
        }
    }

    public deleteById(id: number): void {
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

    public count(options?: Options): number {
        return store.count(this.getEntityName(), options);
    }

    protected async triggerEvent(_data: EntityEvent<T>) {
    }


}