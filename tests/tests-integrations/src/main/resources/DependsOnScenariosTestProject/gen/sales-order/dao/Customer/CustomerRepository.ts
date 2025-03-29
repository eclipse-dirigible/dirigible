import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";

export interface CustomerEntity {
    readonly Id: number;
    Name?: string;
    Country?: number;
    City?: number;
    Address?: string;
}

export interface CustomerCreateEntity {
    readonly Name?: string;
    readonly Country?: number;
    readonly City?: number;
    readonly Address?: string;
}

export interface CustomerUpdateEntity extends CustomerCreateEntity {
    readonly Id: number;
}

export interface CustomerEntityOptions {
    $filter?: {
        equals?: {
            Id?: number | number[];
            Name?: string | string[];
            Country?: number | number[];
            City?: number | number[];
            Address?: string | string[];
        };
        notEquals?: {
            Id?: number | number[];
            Name?: string | string[];
            Country?: number | number[];
            City?: number | number[];
            Address?: string | string[];
        };
        contains?: {
            Id?: number;
            Name?: string;
            Country?: number;
            City?: number;
            Address?: string;
        };
        greaterThan?: {
            Id?: number;
            Name?: string;
            Country?: number;
            City?: number;
            Address?: string;
        };
        greaterThanOrEqual?: {
            Id?: number;
            Name?: string;
            Country?: number;
            City?: number;
            Address?: string;
        };
        lessThan?: {
            Id?: number;
            Name?: string;
            Country?: number;
            City?: number;
            Address?: string;
        };
        lessThanOrEqual?: {
            Id?: number;
            Name?: string;
            Country?: number;
            City?: number;
            Address?: string;
        };
    },
    $select?: (keyof CustomerEntity)[],
    $sort?: string | (keyof CustomerEntity)[],
    $order?: 'asc' | 'desc',
    $offset?: number,
    $limit?: number,
}

interface CustomerEntityEvent {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<CustomerEntity>;
    readonly key: {
        name: string;
        column: string;
        value: number;
    }
}

interface CustomerUpdateEntityEvent extends CustomerEntityEvent {
    readonly previousEntity: CustomerEntity;
}

export class CustomerRepository {

    private static readonly DEFINITION = {
        table: "CUSTOMER",
        properties: [
            {
                name: "Id",
                column: "CUSTOMER_ID",
                type: "INTEGER",
                id: true,
                autoIncrement: true,
            },
            {
                name: "Name",
                column: "CUSTOMER_NAME",
                type: "VARCHAR",
            },
            {
                name: "Country",
                column: "CUSTOMER_COUNTRY",
                type: "INTEGER",
            },
            {
                name: "City",
                column: "CUSTOMER_CITY",
                type: "INTEGER",
            },
            {
                name: "Address",
                column: "CUSTOMER_ADDRESS",
                type: "VARCHAR",
            }
        ]
    };

    private readonly dao;

    constructor(dataSource = "DefaultDB") {
        this.dao = daoApi.create(CustomerRepository.DEFINITION, null, dataSource);
    }

    public findAll(options?: CustomerEntityOptions): CustomerEntity[] {
        return this.dao.list(options);
    }

    public findById(id: number): CustomerEntity | undefined {
        const entity = this.dao.find(id);
        return entity ?? undefined;
    }

    public create(entity: CustomerCreateEntity): number {
        const id = this.dao.insert(entity);
        this.triggerEvent({
            operation: "create",
            table: "CUSTOMER",
            entity: entity,
            key: {
                name: "Id",
                column: "CUSTOMER_ID",
                value: id
            }
        });
        return id;
    }

    public update(entity: CustomerUpdateEntity): void {
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "CUSTOMER",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "Id",
                column: "CUSTOMER_ID",
                value: entity.Id
            }
        });
    }

    public upsert(entity: CustomerCreateEntity | CustomerUpdateEntity): number {
        const id = (entity as CustomerUpdateEntity).Id;
        if (!id) {
            return this.create(entity);
        }

        const existingEntity = this.findById(id);
        if (existingEntity) {
            this.update(entity as CustomerUpdateEntity);
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
            table: "CUSTOMER",
            entity: entity,
            key: {
                name: "Id",
                column: "CUSTOMER_ID",
                value: id
            }
        });
    }

    public count(options?: CustomerEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "CUSTOMER"');
        if (resultSet !== null && resultSet[0] !== null) {
            if (resultSet[0].COUNT !== undefined && resultSet[0].COUNT !== null) {
                return resultSet[0].COUNT;
            } else if (resultSet[0].count !== undefined && resultSet[0].count !== null) {
                return resultSet[0].count;
            }
        }
        return 0;
    }

    private async triggerEvent(data: CustomerEntityEvent | CustomerUpdateEntityEvent) {
        const triggerExtensions = await extensions.loadExtensionModules("sales-order-app-Customer-Customer", ["trigger"]);
        triggerExtensions.forEach(triggerExtension => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }            
        });
        producer.topic("sales-order-app-Customer-Customer").send(JSON.stringify(data));
    }
}
