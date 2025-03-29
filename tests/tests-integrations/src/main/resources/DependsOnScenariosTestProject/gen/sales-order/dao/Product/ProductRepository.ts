import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";

export interface ProductEntity {
    readonly Id: number;
    Name?: string;
    UoM?: number;
    Price?: number;
}

export interface ProductCreateEntity {
    readonly Name?: string;
    readonly UoM?: number;
    readonly Price?: number;
}

export interface ProductUpdateEntity extends ProductCreateEntity {
    readonly Id: number;
}

export interface ProductEntityOptions {
    $filter?: {
        equals?: {
            Id?: number | number[];
            Name?: string | string[];
            UoM?: number | number[];
            Price?: number | number[];
        };
        notEquals?: {
            Id?: number | number[];
            Name?: string | string[];
            UoM?: number | number[];
            Price?: number | number[];
        };
        contains?: {
            Id?: number;
            Name?: string;
            UoM?: number;
            Price?: number;
        };
        greaterThan?: {
            Id?: number;
            Name?: string;
            UoM?: number;
            Price?: number;
        };
        greaterThanOrEqual?: {
            Id?: number;
            Name?: string;
            UoM?: number;
            Price?: number;
        };
        lessThan?: {
            Id?: number;
            Name?: string;
            UoM?: number;
            Price?: number;
        };
        lessThanOrEqual?: {
            Id?: number;
            Name?: string;
            UoM?: number;
            Price?: number;
        };
    },
    $select?: (keyof ProductEntity)[],
    $sort?: string | (keyof ProductEntity)[],
    $order?: 'asc' | 'desc',
    $offset?: number,
    $limit?: number,
}

interface ProductEntityEvent {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<ProductEntity>;
    readonly key: {
        name: string;
        column: string;
        value: number;
    }
}

interface ProductUpdateEntityEvent extends ProductEntityEvent {
    readonly previousEntity: ProductEntity;
}

export class ProductRepository {

    private static readonly DEFINITION = {
        table: "PRODUCT",
        properties: [
            {
                name: "Id",
                column: "PRODUCT_ID",
                type: "INTEGER",
                id: true,
                autoIncrement: true,
            },
            {
                name: "Name",
                column: "PRODUCT_NAME",
                type: "VARCHAR",
            },
            {
                name: "UoM",
                column: "PRODUCT_UOM",
                type: "INTEGER",
            },
            {
                name: "Price",
                column: "PRODUCT_PRICE",
                type: "DECIMAL",
            }
        ]
    };

    private readonly dao;

    constructor(dataSource = "DefaultDB") {
        this.dao = daoApi.create(ProductRepository.DEFINITION, null, dataSource);
    }

    public findAll(options?: ProductEntityOptions): ProductEntity[] {
        return this.dao.list(options);
    }

    public findById(id: number): ProductEntity | undefined {
        const entity = this.dao.find(id);
        return entity ?? undefined;
    }

    public create(entity: ProductCreateEntity): number {
        const id = this.dao.insert(entity);
        this.triggerEvent({
            operation: "create",
            table: "PRODUCT",
            entity: entity,
            key: {
                name: "Id",
                column: "PRODUCT_ID",
                value: id
            }
        });
        return id;
    }

    public update(entity: ProductUpdateEntity): void {
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "PRODUCT",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "Id",
                column: "PRODUCT_ID",
                value: entity.Id
            }
        });
    }

    public upsert(entity: ProductCreateEntity | ProductUpdateEntity): number {
        const id = (entity as ProductUpdateEntity).Id;
        if (!id) {
            return this.create(entity);
        }

        const existingEntity = this.findById(id);
        if (existingEntity) {
            this.update(entity as ProductUpdateEntity);
            return id;
        } else {
            return this.create(entity);
        }
    }

    public deleteById(id: number): void {
        const entity = this.dao.find(id);
        this.dao.remove(id);
        this.triggerEvent({
            operation: "delete",
            table: "PRODUCT",
            entity: entity,
            key: {
                name: "Id",
                column: "PRODUCT_ID",
                value: id
            }
        });
    }

    public count(options?: ProductEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "PRODUCT"');
        if (resultSet !== null && resultSet[0] !== null) {
            if (resultSet[0].COUNT !== undefined && resultSet[0].COUNT !== null) {
                return resultSet[0].COUNT;
            } else if (resultSet[0].count !== undefined && resultSet[0].count !== null) {
                return resultSet[0].count;
            }
        }
        return 0;
    }

    private async triggerEvent(data: ProductEntityEvent | ProductUpdateEntityEvent) {
        const triggerExtensions = await extensions.loadExtensionModules("sales-order-app-Product-Product", ["trigger"]);
        triggerExtensions.forEach(triggerExtension => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }            
        });
        producer.topic("sales-order-app-Product-Product").send(JSON.stringify(data));
    }
}
