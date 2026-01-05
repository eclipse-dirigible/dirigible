import { query } from "@aerokit/sdk/db";
import { producer } from "@aerokit/sdk/messaging";
import { extensions } from "@aerokit/sdk/extensions";
import { dao as daoApi } from "@aerokit/sdk/db";

export interface oc_orderEntity {
    readonly ORDER_ID: number;
    TOTAL: number;
    DATE_ADDED: Date;
}

export interface oc_orderCreateEntity {
    readonly TOTAL: number;
    readonly DATE_ADDED: Date;
}

export interface oc_orderUpdateEntity extends oc_orderCreateEntity {
    readonly ORDER_ID: number;
}

export interface oc_orderEntityOptions {
    $filter?: {
        equals?: {
            ORDER_ID?: number | number[];
            TOTAL?: number | number[];
            DATE_ADDED?: Date | Date[];
        };
        notEquals?: {
            ORDER_ID?: number | number[];
            TOTAL?: number | number[];
            DATE_ADDED?: Date | Date[];
        };
        contains?: {
            ORDER_ID?: number;
            TOTAL?: number;
            DATE_ADDED?: Date;
        };
        greaterThan?: {
            ORDER_ID?: number;
            TOTAL?: number;
            DATE_ADDED?: Date;
        };
        greaterThanOrEqual?: {
            ORDER_ID?: number;
            TOTAL?: number;
            DATE_ADDED?: Date;
        };
        lessThan?: {
            ORDER_ID?: number;
            TOTAL?: number;
            DATE_ADDED?: Date;
        };
        lessThanOrEqual?: {
            ORDER_ID?: number;
            TOTAL?: number;
            DATE_ADDED?: Date;
        };
    },
    $select?: (keyof oc_orderEntity)[],
    $sort?: string | (keyof oc_orderEntity)[],
    $order?: 'asc' | 'desc',
    $offset?: number,
    $limit?: number,
}

interface oc_orderEntityEvent {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<oc_orderEntity>;
    readonly key: {
        name: string;
        column: string;
        value: number;
    }
}

interface oc_orderUpdateEntityEvent extends oc_orderEntityEvent {
    readonly previousEntity: oc_orderEntity;
}

export class oc_orderRepository {

    private static readonly DEFINITION = {
        table: "OC_ORDER",
        properties: [
            {
                name: "ORDER_ID",
                column: "ORDER_ID",
                type: "INT",
                id: true,
                autoIncrement: true,
                required: true
            },
            {
                name: "TOTAL",
                column: "TOTAL",
                type: "DECIMAL",
                required: true
            },
            {
                name: "DATE_ADDED",
                column: "DATE_ADDED",
                type: "DATETIME",
                required: true
            },
        ]
    };

    private readonly dao;

    constructor(dataSource = "DefaultDB") {
        this.dao = daoApi.create(oc_orderRepository.DEFINITION, null, dataSource);
    }

    public findAll(options?: oc_orderEntityOptions): oc_orderEntity[] {
        return this.dao.list(options);
    }

    public findById(id: number): oc_orderEntity | undefined {
        const entity = this.dao.find(id);
        return entity ?? undefined;
    }

    public create(entity: oc_orderCreateEntity): number {
        const id = this.dao.insert(entity);
        this.triggerEvent({
            operation: "create",
            table: "OC_ORDER",
            entity: entity,
            key: {
                name: "ORDER_ID",
                column: "ORDER_ID",
                value: id
            }
        });
        return id;
    }

    public update(entity: oc_orderUpdateEntity): void {
        const previousEntity = this.findById(entity.ORDER_ID);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "OC_ORDER",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "ORDER_ID",
                column: "ORDER_ID",
                value: entity.ORDER_ID
            }
        });
    }

    public upsert(entity: oc_orderCreateEntity | oc_orderUpdateEntity): number {
        const id = (entity as oc_orderUpdateEntity).ORDER_ID;
        if (!id) {
            return this.create(entity);
        }

        const existingEntity = this.findById(id);
        if (existingEntity) {
            this.update(entity as oc_orderUpdateEntity);
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
            table: "OC_ORDER",
            entity: entity,
            key: {
                name: "ORDER_ID",
                column: "ORDER_ID",
                value: id
            }
        });
    }

    public count(options?: oc_orderEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "OC_ORDER"');
        if (resultSet !== null && resultSet[0] !== null) {
            if (resultSet[0].COUNT !== undefined && resultSet[0].COUNT !== null) {
                return resultSet[0].COUNT;
            } else if (resultSet[0].count !== undefined && resultSet[0].count !== null) {
                return resultSet[0].count;
            }
        }
        return 0;
    }

    private async triggerEvent(data: oc_orderEntityEvent | oc_orderUpdateEntityEvent) {
        const triggerExtensions = await extensions.loadExtensionModules("OpenCartDB-oc_order-oc_order", ["trigger"]);
        triggerExtensions.forEach((triggerExtension: { trigger: (arg0: oc_orderEntityEvent | oc_orderUpdateEntityEvent) => void; }) => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }
        });
        producer.topic("OpenCartDB-oc_order-oc_order").send(JSON.stringify(data));
    }
}
