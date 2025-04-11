import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";

export interface SalesOrderPaymentEntity {
    readonly Id: number;
    SalesOrder?: number;
    Customer?: number;
    CustomerPayment?: number;
    Amount?: number;
}

export interface SalesOrderPaymentCreateEntity {
    readonly SalesOrder?: number;
    readonly Customer?: number;
    readonly CustomerPayment?: number;
    readonly Amount?: number;
}

export interface SalesOrderPaymentUpdateEntity extends SalesOrderPaymentCreateEntity {
    readonly Id: number;
}

export interface SalesOrderPaymentEntityOptions {
    $filter?: {
        equals?: {
            Id?: number | number[];
            SalesOrder?: number | number[];
            Customer?: number | number[];
            CustomerPayment?: number | number[];
            Amount?: number | number[];
        };
        notEquals?: {
            Id?: number | number[];
            SalesOrder?: number | number[];
            Customer?: number | number[];
            CustomerPayment?: number | number[];
            Amount?: number | number[];
        };
        contains?: {
            Id?: number;
            SalesOrder?: number;
            Customer?: number;
            CustomerPayment?: number;
            Amount?: number;
        };
        greaterThan?: {
            Id?: number;
            SalesOrder?: number;
            Customer?: number;
            CustomerPayment?: number;
            Amount?: number;
        };
        greaterThanOrEqual?: {
            Id?: number;
            SalesOrder?: number;
            Customer?: number;
            CustomerPayment?: number;
            Amount?: number;
        };
        lessThan?: {
            Id?: number;
            SalesOrder?: number;
            Customer?: number;
            CustomerPayment?: number;
            Amount?: number;
        };
        lessThanOrEqual?: {
            Id?: number;
            SalesOrder?: number;
            Customer?: number;
            CustomerPayment?: number;
            Amount?: number;
        };
    },
    $select?: (keyof SalesOrderPaymentEntity)[],
    $sort?: string | (keyof SalesOrderPaymentEntity)[],
    $order?: 'asc' | 'desc',
    $offset?: number,
    $limit?: number,
}

interface SalesOrderPaymentEntityEvent {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<SalesOrderPaymentEntity>;
    readonly key: {
        name: string;
        column: string;
        value: number;
    }
}

interface SalesOrderPaymentUpdateEntityEvent extends SalesOrderPaymentEntityEvent {
    readonly previousEntity: SalesOrderPaymentEntity;
}

export class SalesOrderPaymentRepository {

    private static readonly DEFINITION = {
        table: "SALESORDERPAYMENT",
        properties: [
            {
                name: "Id",
                column: "SALESORDERPAYMENT_ID",
                type: "INTEGER",
                id: true,
                autoIncrement: true,
            },
            {
                name: "SalesOrder",
                column: "SALESORDERPAYMENT_SALESORDER",
                type: "INTEGER",
            },
            {
                name: "Customer",
                column: "SALESORDERPAYMENT_CUSTOMER",
                type: "INTEGER",
            },
            {
                name: "CustomerPayment",
                column: "SALESORDERPAYMENT_CUSTOMERPAYMENT",
                type: "INTEGER",
            },
            {
                name: "Amount",
                column: "SALESORDERPAYMENT_AMOUNT",
                type: "DECIMAL",
            }
        ]
    };

    private readonly dao;

    constructor(dataSource = "DefaultDB") {
        this.dao = daoApi.create(SalesOrderPaymentRepository.DEFINITION, null, dataSource);
    }

    public findAll(options?: SalesOrderPaymentEntityOptions): SalesOrderPaymentEntity[] {
        return this.dao.list(options);
    }

    public findById(id: number): SalesOrderPaymentEntity | undefined {
        const entity = this.dao.find(id);
        return entity ?? undefined;
    }

    public create(entity: SalesOrderPaymentCreateEntity): number {
        const id = this.dao.insert(entity);
        this.triggerEvent({
            operation: "create",
            table: "SALESORDERPAYMENT",
            entity: entity,
            key: {
                name: "Id",
                column: "SALESORDERPAYMENT_ID",
                value: id
            }
        });
        return id;
    }

    public update(entity: SalesOrderPaymentUpdateEntity): void {
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "SALESORDERPAYMENT",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "Id",
                column: "SALESORDERPAYMENT_ID",
                value: entity.Id
            }
        });
    }

    public upsert(entity: SalesOrderPaymentCreateEntity | SalesOrderPaymentUpdateEntity): number {
        const id = (entity as SalesOrderPaymentUpdateEntity).Id;
        if (!id) {
            return this.create(entity);
        }

        const existingEntity = this.findById(id);
        if (existingEntity) {
            this.update(entity as SalesOrderPaymentUpdateEntity);
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
            table: "SALESORDERPAYMENT",
            entity: entity,
            key: {
                name: "Id",
                column: "SALESORDERPAYMENT_ID",
                value: id
            }
        });
    }

    public count(options?: SalesOrderPaymentEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "SALESORDERPAYMENT"');
        if (resultSet !== null && resultSet[0] !== null) {
            if (resultSet[0].COUNT !== undefined && resultSet[0].COUNT !== null) {
                return resultSet[0].COUNT;
            } else if (resultSet[0].count !== undefined && resultSet[0].count !== null) {
                return resultSet[0].count;
            }
        }
        return 0;
    }

    private async triggerEvent(data: SalesOrderPaymentEntityEvent | SalesOrderPaymentUpdateEntityEvent) {
        const triggerExtensions = await extensions.loadExtensionModules("DependsOnScenariosTestProject-SalesOrder-SalesOrderPayment", ["trigger"]);
        triggerExtensions.forEach(triggerExtension => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }            
        });
        producer.topic("DependsOnScenariosTestProject-SalesOrder-SalesOrderPayment").send(JSON.stringify(data));
    }
}
