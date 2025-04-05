import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";

export interface CountryEntity {
    readonly Id: number;
    Name?: string;
}

export interface CountryCreateEntity {
    readonly Name?: string;
}

export interface CountryUpdateEntity extends CountryCreateEntity {
    readonly Id: number;
}

export interface CountryEntityOptions {
    $filter?: {
        equals?: {
            Id?: number | number[];
            Name?: string | string[];
        };
        notEquals?: {
            Id?: number | number[];
            Name?: string | string[];
        };
        contains?: {
            Id?: number;
            Name?: string;
        };
        greaterThan?: {
            Id?: number;
            Name?: string;
        };
        greaterThanOrEqual?: {
            Id?: number;
            Name?: string;
        };
        lessThan?: {
            Id?: number;
            Name?: string;
        };
        lessThanOrEqual?: {
            Id?: number;
            Name?: string;
        };
    },
    $select?: (keyof CountryEntity)[],
    $sort?: string | (keyof CountryEntity)[],
    $order?: 'asc' | 'desc',
    $offset?: number,
    $limit?: number,
}

interface CountryEntityEvent {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<CountryEntity>;
    readonly key: {
        name: string;
        column: string;
        value: number;
    }
}

interface CountryUpdateEntityEvent extends CountryEntityEvent {
    readonly previousEntity: CountryEntity;
}

export class CountryRepository {

    private static readonly DEFINITION = {
        table: "COUNTRY",
        properties: [
            {
                name: "Id",
                column: "COUNTRY_ID",
                type: "INTEGER",
                id: true,
                autoIncrement: true,
            },
            {
                name: "Name",
                column: "COUNTRY_NAME",
                type: "VARCHAR",
            }
        ]
    };

    private readonly dao;

    constructor(dataSource = "DefaultDB") {
        this.dao = daoApi.create(CountryRepository.DEFINITION, null, dataSource);
    }

    public findAll(options?: CountryEntityOptions): CountryEntity[] {
        return this.dao.list(options);
    }

    public findById(id: number): CountryEntity | undefined {
        const entity = this.dao.find(id);
        return entity ?? undefined;
    }

    public create(entity: CountryCreateEntity): number {
        const id = this.dao.insert(entity);
        this.triggerEvent({
            operation: "create",
            table: "COUNTRY",
            entity: entity,
            key: {
                name: "Id",
                column: "COUNTRY_ID",
                value: id
            }
        });
        return id;
    }

    public update(entity: CountryUpdateEntity): void {
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "COUNTRY",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "Id",
                column: "COUNTRY_ID",
                value: entity.Id
            }
        });
    }

    public upsert(entity: CountryCreateEntity | CountryUpdateEntity): number {
        const id = (entity as CountryUpdateEntity).Id;
        if (!id) {
            return this.create(entity);
        }

        const existingEntity = this.findById(id);
        if (existingEntity) {
            this.update(entity as CountryUpdateEntity);
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
            table: "COUNTRY",
            entity: entity,
            key: {
                name: "Id",
                column: "COUNTRY_ID",
                value: id
            }
        });
    }

    public count(options?: CountryEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "COUNTRY"');
        if (resultSet !== null && resultSet[0] !== null) {
            if (resultSet[0].COUNT !== undefined && resultSet[0].COUNT !== null) {
                return resultSet[0].COUNT;
            } else if (resultSet[0].count !== undefined && resultSet[0].count !== null) {
                return resultSet[0].count;
            }
        }
        return 0;
    }

    private async triggerEvent(data: CountryEntityEvent | CountryUpdateEntityEvent) {
        const triggerExtensions = await extensions.loadExtensionModules("DependsOnScenariosTestProject-Country-Country", ["trigger"]);
        triggerExtensions.forEach(triggerExtension => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }            
        });
        producer.topic("DependsOnScenariosTestProject-Country-Country").send(JSON.stringify(data));
    }
}
