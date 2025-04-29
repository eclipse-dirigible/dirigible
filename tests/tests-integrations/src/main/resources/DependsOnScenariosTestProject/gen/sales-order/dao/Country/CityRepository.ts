import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";

export interface CityEntity {
    readonly Id: number;
    Name?: string;
    Country?: number;
}

export interface CityCreateEntity {
    readonly Name?: string;
    readonly Country?: number;
}

export interface CityUpdateEntity extends CityCreateEntity {
    readonly Id: number;
}

export interface CityEntityOptions {
    $filter?: {
        equals?: {
            Id?: number | number[];
            Name?: string | string[];
            Country?: number | number[];
        };
        notEquals?: {
            Id?: number | number[];
            Name?: string | string[];
            Country?: number | number[];
        };
        contains?: {
            Id?: number;
            Name?: string;
            Country?: number;
        };
        greaterThan?: {
            Id?: number;
            Name?: string;
            Country?: number;
        };
        greaterThanOrEqual?: {
            Id?: number;
            Name?: string;
            Country?: number;
        };
        lessThan?: {
            Id?: number;
            Name?: string;
            Country?: number;
        };
        lessThanOrEqual?: {
            Id?: number;
            Name?: string;
            Country?: number;
        };
    },
    $select?: (keyof CityEntity)[],
    $sort?: string | (keyof CityEntity)[],
    $order?: 'ASC' | 'DESC',
    $offset?: number,
    $limit?: number,
}

interface CityEntityEvent {
    readonly operation: 'create' | 'update' | 'delete';
    readonly table: string;
    readonly entity: Partial<CityEntity>;
    readonly key: {
        name: string;
        column: string;
        value: number;
    }
}

interface CityUpdateEntityEvent extends CityEntityEvent {
    readonly previousEntity: CityEntity;
}

export class CityRepository {

    private static readonly DEFINITION = {
        table: "CITY",
        properties: [
            {
                name: "Id",
                column: "CITY_ID",
                type: "INTEGER",
                id: true,
                autoIncrement: true,
            },
            {
                name: "Name",
                column: "CITY_NAME",
                type: "VARCHAR",
            },
            {
                name: "Country",
                column: "CITY_COUNTRY",
                type: "INTEGER",
            }
        ]
    };

    private readonly dao;

    constructor(dataSource = "DefaultDB") {
        this.dao = daoApi.create(CityRepository.DEFINITION, undefined, dataSource);
    }

    public findAll(options: CityEntityOptions = {}): CityEntity[] {
        return this.dao.list(options);
    }

    public findById(id: number): CityEntity | undefined {
        const entity = this.dao.find(id);
        return entity ?? undefined;
    }

    public create(entity: CityCreateEntity): number {
        const id = this.dao.insert(entity);
        this.triggerEvent({
            operation: "create",
            table: "CITY",
            entity: entity,
            key: {
                name: "Id",
                column: "CITY_ID",
                value: id
            }
        });
        return id;
    }

    public update(entity: CityUpdateEntity): void {
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "CITY",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "Id",
                column: "CITY_ID",
                value: entity.Id
            }
        });
    }

    public upsert(entity: CityCreateEntity | CityUpdateEntity): number {
        const id = (entity as CityUpdateEntity).Id;
        if (!id) {
            return this.create(entity);
        }

        const existingEntity = this.findById(id);
        if (existingEntity) {
            this.update(entity as CityUpdateEntity);
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
            table: "CITY",
            entity: entity,
            key: {
                name: "Id",
                column: "CITY_ID",
                value: id
            }
        });
    }

    public count(options?: CityEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "CITY"');
        if (resultSet !== null && resultSet[0] !== null) {
            if (resultSet[0].COUNT !== undefined && resultSet[0].COUNT !== null) {
                return resultSet[0].COUNT;
            } else if (resultSet[0].count !== undefined && resultSet[0].count !== null) {
                return resultSet[0].count;
            }
        }
        return 0;
    }

    private async triggerEvent(data: CityEntityEvent | CityUpdateEntityEvent) {
        const triggerExtensions = await extensions.loadExtensionModules("DependsOnScenariosTestProject-Country-City", ["trigger"]);
        triggerExtensions.forEach(triggerExtension => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }            
        });
        producer.topic("DependsOnScenariosTestProject-Country-City").send(JSON.stringify(data));
    }
}
