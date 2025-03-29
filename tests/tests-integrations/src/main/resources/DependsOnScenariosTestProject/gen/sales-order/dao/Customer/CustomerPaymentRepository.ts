import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";

export interface CustomerPaymentEntity {
    readonly Id: number;
    Name?: string;
    Customer?: number;
    Amount?: number;
}

export interface CustomerPaymentCreateEntity {
    readonly Name?: string;
    readonly Customer?: number;
    readonly Amount?: number;
}

export interface CustomerPaymentUpdateEntity extends CustomerPaymentCreateEntity {
    readonly Id: number;
}

export interface CustomerPaymentEntityOptions {
    $filter?: {
        equals?: {
            Id?: number | number[];
            Name?: string | string[];
            Customer?: number | number[];
            Amount?: number | number[];
        };
        notEquals?: {
            Id?: number | number[];
            Name?: string | string[];
            Customer?: number | number[];
            Amount?: number | number[];
        };
        contains?: {
            Id?: number;
            Name?: string;
            Customer?: number;
            Amount?: number;
        };
        greaterThan?: {
            Id?: number;
            Name?: string;
            Customer?: number;
            Amount?: number;
        };
        greaterThanOrEqual?: {
            Id?: number;
            Name?: string;
            Customer?: number;
            Amount?: number;
        };
        lessThan?: {
            Id?: number;
            Name?: string;
            Customer?: number;
            Amount?: number;
        };
        lessThanOrEqual?: {
            Id?: number;
            Name?: string;
            Customer?: number;
            Amount?: number;
        };
    },
    $select?: (keyof CustomerPaymentEntity)[],
    $sort?: string | (keyof CustomerPaymentEntity)[],
    $order?: 'asc' | 'desc',
    $offset?: number,
    $limit?: number,
}

interface CustomerPaymentEntityEvent {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<CustomerPaymentEntity>;
    readonly key: {
        name: string;
        column: string;
        value: number;
    }
}

interface CustomerPaymentUpdateEntityEvent extends CustomerPaymentEntityEvent {
    readonly previousEntity: CustomerPaymentEntity;
}

export class CustomerPaymentRepository {

    private static readonly DEFINITION = {
        table: "CUSTOMERPAYMENT",
        properties: [
            {
                name: "Id",
                column: "CUSTOMERPAYMENT_ID",
                type: "INTEGER",
                id: true,
                autoIncrement: true,
            },
            {
                name: "Name",
                column: "CUSTOMERPAYMENT_NAME",
                type: "VARCHAR",
            },
            {
                name: "Customer",
                column: "CUSTOMERPAYMENT_CUSTOMER",
                type: "INTEGER",
            },
            {
                name: "Amount",
                column: "CUSTOMERPAYMENT_AMOUNT",
                type: "DECIMAL",
            }
        ]
    };

    private readonly dao;

    constructor(dataSource = "DefaultDB") {
        this.dao = daoApi.create(CustomerPaymentRepository.DEFINITION, null, dataSource);
    }

    public findAll(options?: CustomerPaymentEntityOptions): CustomerPaymentEntity[] {
        return this.dao.list(options);
    }

    public findById(id: number): CustomerPaymentEntity | undefined {
        const entity = this.dao.find(id);
        return entity ?? undefined;
    }

    public create(entity: CustomerPaymentCreateEntity): number {
        const id = this.dao.insert(entity);
        this.triggerEvent({
            operation: "create",
            table: "CUSTOMERPAYMENT",
            entity: entity,
            key: {
                name: "Id",
                column: "CUSTOMERPAYMENT_ID",
                value: id
            }
        });
        return id;
    }

    public update(entity: CustomerPaymentUpdateEntity): void {
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "CUSTOMERPAYMENT",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "Id",
                column: "CUSTOMERPAYMENT_ID",
                value: entity.Id
            }
        });
    }

    public upsert(entity: CustomerPaymentCreateEntity | CustomerPaymentUpdateEntity): number {
        const id = (entity as CustomerPaymentUpdateEntity).Id;
        if (!id) {
            return this.create(entity);
        }

        const existingEntity = this.findById(id);
        if (existingEntity) {
            this.update(entity as CustomerPaymentUpdateEntity);
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
            table: "CUSTOMERPAYMENT",
            entity: entity,
            key: {
                name: "Id",
                column: "CUSTOMERPAYMENT_ID",
                value: id
            }
        });
    }

    public count(options?: CustomerPaymentEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "CUSTOMERPAYMENT"');
        if (resultSet !== null && resultSet[0] !== null) {
            if (resultSet[0].COUNT !== undefined && resultSet[0].COUNT !== null) {
                return resultSet[0].COUNT;
            } else if (resultSet[0].count !== undefined && resultSet[0].count !== null) {
                return resultSet[0].count;
            }
        }
        return 0;
    }

    private async triggerEvent(data: CustomerPaymentEntityEvent | CustomerPaymentUpdateEntityEvent) {
        const triggerExtensions = await extensions.loadExtensionModules("sales-order-app-Customer-CustomerPayment", ["trigger"]);
        triggerExtensions.forEach(triggerExtension => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }            
        });
        producer.topic("sales-order-app-Customer-CustomerPayment").send(JSON.stringify(data));
    }
}
