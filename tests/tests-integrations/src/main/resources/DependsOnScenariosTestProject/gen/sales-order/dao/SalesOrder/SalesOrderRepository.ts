import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";
import { EntityUtils } from "../utils/EntityUtils";

export interface SalesOrderEntity {
    readonly Id: number;
    Customer?: number;
    Date?: Date;
}

export interface SalesOrderCreateEntity {
    readonly Customer?: number;
    readonly Date?: Date;
}

export interface SalesOrderUpdateEntity extends SalesOrderCreateEntity {
    readonly Id: number;
}

export interface SalesOrderEntityOptions {
    $filter?: {
        equals?: {
            Id?: number | number[];
            Customer?: number | number[];
            Date?: Date | Date[];
        };
        notEquals?: {
            Id?: number | number[];
            Customer?: number | number[];
            Date?: Date | Date[];
        };
        contains?: {
            Id?: number;
            Customer?: number;
            Date?: Date;
        };
        greaterThan?: {
            Id?: number;
            Customer?: number;
            Date?: Date;
        };
        greaterThanOrEqual?: {
            Id?: number;
            Customer?: number;
            Date?: Date;
        };
        lessThan?: {
            Id?: number;
            Customer?: number;
            Date?: Date;
        };
        lessThanOrEqual?: {
            Id?: number;
            Customer?: number;
            Date?: Date;
        };
    },
    $select?: (keyof SalesOrderEntity)[],
    $sort?: string | (keyof SalesOrderEntity)[],
    $order?: 'ASC' | 'DESC',
    $offset?: number,
    $limit?: number,
}

interface SalesOrderEntityEvent {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<SalesOrderEntity>;
    readonly key: {
        name: string;
        column: string;
        value: number;
    }
}

interface SalesOrderUpdateEntityEvent extends SalesOrderEntityEvent {
    readonly previousEntity: SalesOrderEntity;
}

export class SalesOrderRepository {

    private static readonly DEFINITION = {
        table: "SALESORDER",
        properties: [
            {
                name: "Id",
                column: "SALESORDER_ID",
                type: "INTEGER",
                id: true,
                autoIncrement: true,
            },
            {
                name: "Customer",
                column: "SALESORDER_CUSTOMER",
                type: "INTEGER",
            },
            {
                name: "Date",
                column: "SALESORDER_DATE",
                type: "DATE",
            }
        ]
    };

    private readonly dao;

    constructor(dataSource = "DefaultDB") {
        this.dao = daoApi.create(SalesOrderRepository.DEFINITION, undefined, dataSource);
    }

    public findAll(options: SalesOrderEntityOptions = {}): SalesOrderEntity[] {
        return this.dao.list(options).map((e: SalesOrderEntity) => {
            EntityUtils.setDate(e, "Date");
            return e;
        });
    }

    public findById(id: number): SalesOrderEntity | undefined {
        const entity = this.dao.find(id);
        EntityUtils.setDate(entity, "Date");
        return entity ?? undefined;
    }

    public create(entity: SalesOrderCreateEntity): number {
        EntityUtils.setLocalDate(entity, "Date");
        const id = this.dao.insert(entity);
        this.triggerEvent({
            operation: "create",
            table: "SALESORDER",
            entity: entity,
            key: {
                name: "Id",
                column: "SALESORDER_ID",
                value: id
            }
        });
        return id;
    }

    public update(entity: SalesOrderUpdateEntity): void {
        // EntityUtils.setLocalDate(entity, "Date");
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "SALESORDER",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "Id",
                column: "SALESORDER_ID",
                value: entity.Id
            }
        });
    }

    public upsert(entity: SalesOrderCreateEntity | SalesOrderUpdateEntity): number {
        const id = (entity as SalesOrderUpdateEntity).Id;
        if (!id) {
            return this.create(entity);
        }

        const existingEntity = this.findById(id);
        if (existingEntity) {
            this.update(entity as SalesOrderUpdateEntity);
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
            table: "SALESORDER",
            entity: entity,
            key: {
                name: "Id",
                column: "SALESORDER_ID",
                value: id
            }
        });
    }

    public count(options?: SalesOrderEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "SALESORDER"');
        if (resultSet !== null && resultSet[0] !== null) {
            if (resultSet[0].COUNT !== undefined && resultSet[0].COUNT !== null) {
                return resultSet[0].COUNT;
            } else if (resultSet[0].count !== undefined && resultSet[0].count !== null) {
                return resultSet[0].count;
            }
        }
        return 0;
    }

    private async triggerEvent(data: SalesOrderEntityEvent | SalesOrderUpdateEntityEvent) {
        const triggerExtensions = await extensions.loadExtensionModules("DependsOnScenariosTestProject-SalesOrder-SalesOrder", ["trigger"]);
        triggerExtensions.forEach(triggerExtension => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }            
        });
        producer.topic("DependsOnScenariosTestProject-SalesOrder-SalesOrder").send(JSON.stringify(data));
    }
}
