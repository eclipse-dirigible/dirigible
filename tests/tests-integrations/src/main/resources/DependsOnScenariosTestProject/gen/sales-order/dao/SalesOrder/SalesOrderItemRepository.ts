import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";

export interface SalesOrderItemEntity {
    readonly Id: number;
    SalesOrder?: number;
    Product?: number;
    UoM?: number;
    Price?: number;
}

export interface SalesOrderItemCreateEntity {
    readonly SalesOrder?: number;
    readonly Product?: number;
    readonly UoM?: number;
    readonly Price?: number;
}

export interface SalesOrderItemUpdateEntity extends SalesOrderItemCreateEntity {
    readonly Id: number;
}

export interface SalesOrderItemEntityOptions {
    $filter?: {
        equals?: {
            Id?: number | number[];
            SalesOrder?: number | number[];
            Product?: number | number[];
            UoM?: number | number[];
            Price?: number | number[];
        };
        notEquals?: {
            Id?: number | number[];
            SalesOrder?: number | number[];
            Product?: number | number[];
            UoM?: number | number[];
            Price?: number | number[];
        };
        contains?: {
            Id?: number;
            SalesOrder?: number;
            Product?: number;
            UoM?: number;
            Price?: number;
        };
        greaterThan?: {
            Id?: number;
            SalesOrder?: number;
            Product?: number;
            UoM?: number;
            Price?: number;
        };
        greaterThanOrEqual?: {
            Id?: number;
            SalesOrder?: number;
            Product?: number;
            UoM?: number;
            Price?: number;
        };
        lessThan?: {
            Id?: number;
            SalesOrder?: number;
            Product?: number;
            UoM?: number;
            Price?: number;
        };
        lessThanOrEqual?: {
            Id?: number;
            SalesOrder?: number;
            Product?: number;
            UoM?: number;
            Price?: number;
        };
    },
    $select?: (keyof SalesOrderItemEntity)[],
    $sort?: string | (keyof SalesOrderItemEntity)[],
    $order?: 'ASC' | 'DESC',
    $offset?: number,
    $limit?: number,
}

interface SalesOrderItemEntityEvent {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<SalesOrderItemEntity>;
    readonly key: {
        name: string;
        column: string;
        value: number;
    }
}

interface SalesOrderItemUpdateEntityEvent extends SalesOrderItemEntityEvent {
    readonly previousEntity: SalesOrderItemEntity;
}

export class SalesOrderItemRepository {

    private static readonly DEFINITION = {
        table: "SALESORDERITEM",
        properties: [
            {
                name: "Id",
                column: "SALESORDERITEM_ID",
                type: "INTEGER",
                id: true,
                autoIncrement: true,
            },
            {
                name: "SalesOrder",
                column: "SALESORDERITEM_SALESORDER",
                type: "INTEGER",
            },
            {
                name: "Product",
                column: "SALESORDERITEM_PRODUCT",
                type: "INTEGER",
            },
            {
                name: "UoM",
                column: "SALESORDERITEM_UOM",
                type: "INTEGER",
            },
            {
                name: "Price",
                column: "SALESORDERITEM_PRICE",
                type: "DECIMAL",
            }
        ]
    };

    private readonly dao;

    constructor(dataSource = "DefaultDB") {
        this.dao = daoApi.create(SalesOrderItemRepository.DEFINITION, undefined, dataSource);
    }

    public findAll(options: SalesOrderItemEntityOptions = {}): SalesOrderItemEntity[] {
        return this.dao.list(options);
    }

    public findById(id: number): SalesOrderItemEntity | undefined {
        const entity = this.dao.find(id);
        return entity ?? undefined;
    }

    public create(entity: SalesOrderItemCreateEntity): number {
        const id = this.dao.insert(entity);
        this.triggerEvent({
            operation: "create",
            table: "SALESORDERITEM",
            entity: entity,
            key: {
                name: "Id",
                column: "SALESORDERITEM_ID",
                value: id
            }
        });
        return id;
    }

    public update(entity: SalesOrderItemUpdateEntity): void {
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "SALESORDERITEM",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "Id",
                column: "SALESORDERITEM_ID",
                value: entity.Id
            }
        });
    }

    public upsert(entity: SalesOrderItemCreateEntity | SalesOrderItemUpdateEntity): number {
        const id = (entity as SalesOrderItemUpdateEntity).Id;
        if (!id) {
            return this.create(entity);
        }

        const existingEntity = this.findById(id);
        if (existingEntity) {
            this.update(entity as SalesOrderItemUpdateEntity);
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
            table: "SALESORDERITEM",
            entity: entity,
            key: {
                name: "Id",
                column: "SALESORDERITEM_ID",
                value: id
            }
        });
    }

    public count(options?: SalesOrderItemEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "SALESORDERITEM"');
        if (resultSet !== null && resultSet[0] !== null) {
            if (resultSet[0].COUNT !== undefined && resultSet[0].COUNT !== null) {
                return resultSet[0].COUNT;
            } else if (resultSet[0].count !== undefined && resultSet[0].count !== null) {
                return resultSet[0].count;
            }
        }
        return 0;
    }

    private async triggerEvent(data: SalesOrderItemEntityEvent | SalesOrderItemUpdateEntityEvent) {
        const triggerExtensions = await extensions.loadExtensionModules("DependsOnScenariosTestProject-SalesOrder-SalesOrderItem", ["trigger"]);
        triggerExtensions.forEach(triggerExtension => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }            
        });
        producer.topic("DependsOnScenariosTestProject-SalesOrder-SalesOrderItem").send(JSON.stringify(data));
    }
}
