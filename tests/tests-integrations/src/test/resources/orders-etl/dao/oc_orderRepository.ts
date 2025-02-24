import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";

export interface oc_orderEntity {
    readonly order_id: number;
    invoice_no: number;
    invoice_prefix: string;
    store_id: number;
    store_name: string;
    store_url: string;
    customer_id: number;
    customer_group_id: number;
    firstname: string;
    lastname: string;
    email: string;
    telephone: string;
    fax: string;
    custom_field: string;
    payment_firstname: string;
    payment_lastname: string;
    payment_company: string;
    payment_address_1: string;
    payment_address_2: string;
    payment_city: string;
    payment_postcode: string;
    payment_country: string;
    payment_country_id: number;
    payment_zone: string;
    payment_zone_id: number;
    payment_address_format: string;
    payment_custom_field: string;
    payment_method: string;
    payment_code: string;
    shipping_firstname: string;
    shipping_lastname: string;
    shipping_company: string;
    shipping_address_1: string;
    shipping_address_2: string;
    shipping_city: string;
    shipping_postcode: string;
    shipping_country: string;
    shipping_country_id: number;
    shipping_zone: string;
    shipping_zone_id: number;
    shipping_address_format: string;
    shipping_custom_field: string;
    shipping_method: string;
    shipping_code: string;
    comment: string;
    total: number;
    order_status_id: number;
    affiliate_id: number;
    commission: number;
    marketing_id: number;
    tracking: string;
    language_id: number;
    currency_id: number;
    currency_code: string;
    currency_value: number;
    ip: string;
    forwarded_ip: string;
    user_agent: string;
    accept_language: string;
    date_added: Date;
    date_modified: Date;
}

export interface oc_orderCreateEntity {
    readonly invoice_no: number;
    readonly invoice_prefix: string;
    readonly store_id: number;
    readonly store_name: string;
    readonly store_url: string;
    readonly customer_id: number;
    readonly customer_group_id: number;
    readonly firstname: string;
    readonly lastname: string;
    readonly email: string;
    readonly telephone: string;
    readonly fax: string;
    readonly custom_field: string;
    readonly payment_firstname: string;
    readonly payment_lastname: string;
    readonly payment_company: string;
    readonly payment_address_1: string;
    readonly payment_address_2: string;
    readonly payment_city: string;
    readonly payment_postcode: string;
    readonly payment_country: string;
    readonly payment_country_id: number;
    readonly payment_zone: string;
    readonly payment_zone_id: number;
    readonly payment_address_format: string;
    readonly payment_custom_field: string;
    readonly payment_method: string;
    readonly payment_code: string;
    readonly shipping_firstname: string;
    readonly shipping_lastname: string;
    readonly shipping_company: string;
    readonly shipping_address_1: string;
    readonly shipping_address_2: string;
    readonly shipping_city: string;
    readonly shipping_postcode: string;
    readonly shipping_country: string;
    readonly shipping_country_id: number;
    readonly shipping_zone: string;
    readonly shipping_zone_id: number;
    readonly shipping_address_format: string;
    readonly shipping_custom_field: string;
    readonly shipping_method: string;
    readonly shipping_code: string;
    readonly comment: string;
    readonly total: number;
    readonly order_status_id: number;
    readonly affiliate_id: number;
    readonly commission: number;
    readonly marketing_id: number;
    readonly tracking: string;
    readonly language_id: number;
    readonly currency_id: number;
    readonly currency_code: string;
    readonly currency_value: number;
    readonly ip: string;
    readonly forwarded_ip: string;
    readonly user_agent: string;
    readonly accept_language: string;
    readonly date_added: Date;
    readonly date_modified: Date;
}

export interface oc_orderUpdateEntity extends oc_orderCreateEntity {
    readonly order_id: number;
}

export interface oc_orderEntityOptions {
    $filter?: {
        equals?: {
            order_id?: number | number[];
            invoice_no?: number | number[];
            invoice_prefix?: string | string[];
            store_id?: number | number[];
            store_name?: string | string[];
            store_url?: string | string[];
            customer_id?: number | number[];
            customer_group_id?: number | number[];
            firstname?: string | string[];
            lastname?: string | string[];
            email?: string | string[];
            telephone?: string | string[];
            fax?: string | string[];
            custom_field?: string | string[];
            payment_firstname?: string | string[];
            payment_lastname?: string | string[];
            payment_company?: string | string[];
            payment_address_1?: string | string[];
            payment_address_2?: string | string[];
            payment_city?: string | string[];
            payment_postcode?: string | string[];
            payment_country?: string | string[];
            payment_country_id?: number | number[];
            payment_zone?: string | string[];
            payment_zone_id?: number | number[];
            payment_address_format?: string | string[];
            payment_custom_field?: string | string[];
            payment_method?: string | string[];
            payment_code?: string | string[];
            shipping_firstname?: string | string[];
            shipping_lastname?: string | string[];
            shipping_company?: string | string[];
            shipping_address_1?: string | string[];
            shipping_address_2?: string | string[];
            shipping_city?: string | string[];
            shipping_postcode?: string | string[];
            shipping_country?: string | string[];
            shipping_country_id?: number | number[];
            shipping_zone?: string | string[];
            shipping_zone_id?: number | number[];
            shipping_address_format?: string | string[];
            shipping_custom_field?: string | string[];
            shipping_method?: string | string[];
            shipping_code?: string | string[];
            comment?: string | string[];
            total?: number | number[];
            order_status_id?: number | number[];
            affiliate_id?: number | number[];
            commission?: number | number[];
            marketing_id?: number | number[];
            tracking?: string | string[];
            language_id?: number | number[];
            currency_id?: number | number[];
            currency_code?: string | string[];
            currency_value?: number | number[];
            ip?: string | string[];
            forwarded_ip?: string | string[];
            user_agent?: string | string[];
            accept_language?: string | string[];
            date_added?: Date | Date[];
            date_modified?: Date | Date[];
        };
        notEquals?: {
            order_id?: number | number[];
            invoice_no?: number | number[];
            invoice_prefix?: string | string[];
            store_id?: number | number[];
            store_name?: string | string[];
            store_url?: string | string[];
            customer_id?: number | number[];
            customer_group_id?: number | number[];
            firstname?: string | string[];
            lastname?: string | string[];
            email?: string | string[];
            telephone?: string | string[];
            fax?: string | string[];
            custom_field?: string | string[];
            payment_firstname?: string | string[];
            payment_lastname?: string | string[];
            payment_company?: string | string[];
            payment_address_1?: string | string[];
            payment_address_2?: string | string[];
            payment_city?: string | string[];
            payment_postcode?: string | string[];
            payment_country?: string | string[];
            payment_country_id?: number | number[];
            payment_zone?: string | string[];
            payment_zone_id?: number | number[];
            payment_address_format?: string | string[];
            payment_custom_field?: string | string[];
            payment_method?: string | string[];
            payment_code?: string | string[];
            shipping_firstname?: string | string[];
            shipping_lastname?: string | string[];
            shipping_company?: string | string[];
            shipping_address_1?: string | string[];
            shipping_address_2?: string | string[];
            shipping_city?: string | string[];
            shipping_postcode?: string | string[];
            shipping_country?: string | string[];
            shipping_country_id?: number | number[];
            shipping_zone?: string | string[];
            shipping_zone_id?: number | number[];
            shipping_address_format?: string | string[];
            shipping_custom_field?: string | string[];
            shipping_method?: string | string[];
            shipping_code?: string | string[];
            comment?: string | string[];
            total?: number | number[];
            order_status_id?: number | number[];
            affiliate_id?: number | number[];
            commission?: number | number[];
            marketing_id?: number | number[];
            tracking?: string | string[];
            language_id?: number | number[];
            currency_id?: number | number[];
            currency_code?: string | string[];
            currency_value?: number | number[];
            ip?: string | string[];
            forwarded_ip?: string | string[];
            user_agent?: string | string[];
            accept_language?: string | string[];
            date_added?: Date | Date[];
            date_modified?: Date | Date[];
        };
        contains?: {
            order_id?: number;
            invoice_no?: number;
            invoice_prefix?: string;
            store_id?: number;
            store_name?: string;
            store_url?: string;
            customer_id?: number;
            customer_group_id?: number;
            firstname?: string;
            lastname?: string;
            email?: string;
            telephone?: string;
            fax?: string;
            custom_field?: string;
            payment_firstname?: string;
            payment_lastname?: string;
            payment_company?: string;
            payment_address_1?: string;
            payment_address_2?: string;
            payment_city?: string;
            payment_postcode?: string;
            payment_country?: string;
            payment_country_id?: number;
            payment_zone?: string;
            payment_zone_id?: number;
            payment_address_format?: string;
            payment_custom_field?: string;
            payment_method?: string;
            payment_code?: string;
            shipping_firstname?: string;
            shipping_lastname?: string;
            shipping_company?: string;
            shipping_address_1?: string;
            shipping_address_2?: string;
            shipping_city?: string;
            shipping_postcode?: string;
            shipping_country?: string;
            shipping_country_id?: number;
            shipping_zone?: string;
            shipping_zone_id?: number;
            shipping_address_format?: string;
            shipping_custom_field?: string;
            shipping_method?: string;
            shipping_code?: string;
            comment?: string;
            total?: number;
            order_status_id?: number;
            affiliate_id?: number;
            commission?: number;
            marketing_id?: number;
            tracking?: string;
            language_id?: number;
            currency_id?: number;
            currency_code?: string;
            currency_value?: number;
            ip?: string;
            forwarded_ip?: string;
            user_agent?: string;
            accept_language?: string;
            date_added?: Date;
            date_modified?: Date;
        };
        greaterThan?: {
            order_id?: number;
            invoice_no?: number;
            invoice_prefix?: string;
            store_id?: number;
            store_name?: string;
            store_url?: string;
            customer_id?: number;
            customer_group_id?: number;
            firstname?: string;
            lastname?: string;
            email?: string;
            telephone?: string;
            fax?: string;
            custom_field?: string;
            payment_firstname?: string;
            payment_lastname?: string;
            payment_company?: string;
            payment_address_1?: string;
            payment_address_2?: string;
            payment_city?: string;
            payment_postcode?: string;
            payment_country?: string;
            payment_country_id?: number;
            payment_zone?: string;
            payment_zone_id?: number;
            payment_address_format?: string;
            payment_custom_field?: string;
            payment_method?: string;
            payment_code?: string;
            shipping_firstname?: string;
            shipping_lastname?: string;
            shipping_company?: string;
            shipping_address_1?: string;
            shipping_address_2?: string;
            shipping_city?: string;
            shipping_postcode?: string;
            shipping_country?: string;
            shipping_country_id?: number;
            shipping_zone?: string;
            shipping_zone_id?: number;
            shipping_address_format?: string;
            shipping_custom_field?: string;
            shipping_method?: string;
            shipping_code?: string;
            comment?: string;
            total?: number;
            order_status_id?: number;
            affiliate_id?: number;
            commission?: number;
            marketing_id?: number;
            tracking?: string;
            language_id?: number;
            currency_id?: number;
            currency_code?: string;
            currency_value?: number;
            ip?: string;
            forwarded_ip?: string;
            user_agent?: string;
            accept_language?: string;
            date_added?: Date;
            date_modified?: Date;
        };
        greaterThanOrEqual?: {
            order_id?: number;
            invoice_no?: number;
            invoice_prefix?: string;
            store_id?: number;
            store_name?: string;
            store_url?: string;
            customer_id?: number;
            customer_group_id?: number;
            firstname?: string;
            lastname?: string;
            email?: string;
            telephone?: string;
            fax?: string;
            custom_field?: string;
            payment_firstname?: string;
            payment_lastname?: string;
            payment_company?: string;
            payment_address_1?: string;
            payment_address_2?: string;
            payment_city?: string;
            payment_postcode?: string;
            payment_country?: string;
            payment_country_id?: number;
            payment_zone?: string;
            payment_zone_id?: number;
            payment_address_format?: string;
            payment_custom_field?: string;
            payment_method?: string;
            payment_code?: string;
            shipping_firstname?: string;
            shipping_lastname?: string;
            shipping_company?: string;
            shipping_address_1?: string;
            shipping_address_2?: string;
            shipping_city?: string;
            shipping_postcode?: string;
            shipping_country?: string;
            shipping_country_id?: number;
            shipping_zone?: string;
            shipping_zone_id?: number;
            shipping_address_format?: string;
            shipping_custom_field?: string;
            shipping_method?: string;
            shipping_code?: string;
            comment?: string;
            total?: number;
            order_status_id?: number;
            affiliate_id?: number;
            commission?: number;
            marketing_id?: number;
            tracking?: string;
            language_id?: number;
            currency_id?: number;
            currency_code?: string;
            currency_value?: number;
            ip?: string;
            forwarded_ip?: string;
            user_agent?: string;
            accept_language?: string;
            date_added?: Date;
            date_modified?: Date;
        };
        lessThan?: {
            order_id?: number;
            invoice_no?: number;
            invoice_prefix?: string;
            store_id?: number;
            store_name?: string;
            store_url?: string;
            customer_id?: number;
            customer_group_id?: number;
            firstname?: string;
            lastname?: string;
            email?: string;
            telephone?: string;
            fax?: string;
            custom_field?: string;
            payment_firstname?: string;
            payment_lastname?: string;
            payment_company?: string;
            payment_address_1?: string;
            payment_address_2?: string;
            payment_city?: string;
            payment_postcode?: string;
            payment_country?: string;
            payment_country_id?: number;
            payment_zone?: string;
            payment_zone_id?: number;
            payment_address_format?: string;
            payment_custom_field?: string;
            payment_method?: string;
            payment_code?: string;
            shipping_firstname?: string;
            shipping_lastname?: string;
            shipping_company?: string;
            shipping_address_1?: string;
            shipping_address_2?: string;
            shipping_city?: string;
            shipping_postcode?: string;
            shipping_country?: string;
            shipping_country_id?: number;
            shipping_zone?: string;
            shipping_zone_id?: number;
            shipping_address_format?: string;
            shipping_custom_field?: string;
            shipping_method?: string;
            shipping_code?: string;
            comment?: string;
            total?: number;
            order_status_id?: number;
            affiliate_id?: number;
            commission?: number;
            marketing_id?: number;
            tracking?: string;
            language_id?: number;
            currency_id?: number;
            currency_code?: string;
            currency_value?: number;
            ip?: string;
            forwarded_ip?: string;
            user_agent?: string;
            accept_language?: string;
            date_added?: Date;
            date_modified?: Date;
        };
        lessThanOrEqual?: {
            order_id?: number;
            invoice_no?: number;
            invoice_prefix?: string;
            store_id?: number;
            store_name?: string;
            store_url?: string;
            customer_id?: number;
            customer_group_id?: number;
            firstname?: string;
            lastname?: string;
            email?: string;
            telephone?: string;
            fax?: string;
            custom_field?: string;
            payment_firstname?: string;
            payment_lastname?: string;
            payment_company?: string;
            payment_address_1?: string;
            payment_address_2?: string;
            payment_city?: string;
            payment_postcode?: string;
            payment_country?: string;
            payment_country_id?: number;
            payment_zone?: string;
            payment_zone_id?: number;
            payment_address_format?: string;
            payment_custom_field?: string;
            payment_method?: string;
            payment_code?: string;
            shipping_firstname?: string;
            shipping_lastname?: string;
            shipping_company?: string;
            shipping_address_1?: string;
            shipping_address_2?: string;
            shipping_city?: string;
            shipping_postcode?: string;
            shipping_country?: string;
            shipping_country_id?: number;
            shipping_zone?: string;
            shipping_zone_id?: number;
            shipping_address_format?: string;
            shipping_custom_field?: string;
            shipping_method?: string;
            shipping_code?: string;
            comment?: string;
            total?: number;
            order_status_id?: number;
            affiliate_id?: number;
            commission?: number;
            marketing_id?: number;
            tracking?: string;
            language_id?: number;
            currency_id?: number;
            currency_code?: string;
            currency_value?: number;
            ip?: string;
            forwarded_ip?: string;
            user_agent?: string;
            accept_language?: string;
            date_added?: Date;
            date_modified?: Date;
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
        table: "oc_order",
        properties: [
            {
                name: "order_id",
                column: "order_id",
                type: "INT",
                id: true,
                autoIncrement: true,
                required: true
            },
            {
                name: "invoice_no",
                column: "invoice_no",
                type: "INT",
                required: true
            },
            {
                name: "invoice_prefix",
                column: "invoice_prefix",
                type: "VARCHAR",
                required: true
            },
            {
                name: "store_id",
                column: "store_id",
                type: "INT",
                required: true
            },
            {
                name: "store_name",
                column: "store_name",
                type: "VARCHAR",
                required: true
            },
            {
                name: "store_url",
                column: "store_url",
                type: "VARCHAR",
                required: true
            },
            {
                name: "customer_id",
                column: "customer_id",
                type: "INT",
                required: true
            },
            {
                name: "customer_group_id",
                column: "customer_group_id",
                type: "INT",
                required: true
            },
            {
                name: "firstname",
                column: "firstname",
                type: "VARCHAR",
                required: true
            },
            {
                name: "lastname",
                column: "lastname",
                type: "VARCHAR",
                required: true
            },
            {
                name: "email",
                column: "email",
                type: "VARCHAR",
                required: true
            },
            {
                name: "telephone",
                column: "telephone",
                type: "VARCHAR",
                required: true
            },
            {
                name: "fax",
                column: "fax",
                type: "VARCHAR",
                required: true
            },
            {
                name: "custom_field",
                column: "custom_field",
                type: "TEXT",
                required: true
            },
            {
                name: "payment_firstname",
                column: "payment_firstname",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_lastname",
                column: "payment_lastname",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_company",
                column: "payment_company",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_address_1",
                column: "payment_address_1",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_address_2",
                column: "payment_address_2",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_city",
                column: "payment_city",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_postcode",
                column: "payment_postcode",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_country",
                column: "payment_country",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_country_id",
                column: "payment_country_id",
                type: "INT",
                required: true
            },
            {
                name: "payment_zone",
                column: "payment_zone",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_zone_id",
                column: "payment_zone_id",
                type: "INT",
                required: true
            },
            {
                name: "payment_address_format",
                column: "payment_address_format",
                type: "TEXT",
                required: true
            },
            {
                name: "payment_custom_field",
                column: "payment_custom_field",
                type: "TEXT",
                required: true
            },
            {
                name: "payment_method",
                column: "payment_method",
                type: "VARCHAR",
                required: true
            },
            {
                name: "payment_code",
                column: "payment_code",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_firstname",
                column: "shipping_firstname",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_lastname",
                column: "shipping_lastname",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_company",
                column: "shipping_company",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_address_1",
                column: "shipping_address_1",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_address_2",
                column: "shipping_address_2",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_city",
                column: "shipping_city",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_postcode",
                column: "shipping_postcode",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_country",
                column: "shipping_country",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_country_id",
                column: "shipping_country_id",
                type: "INT",
                required: true
            },
            {
                name: "shipping_zone",
                column: "shipping_zone",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_zone_id",
                column: "shipping_zone_id",
                type: "INT",
                required: true
            },
            {
                name: "shipping_address_format",
                column: "shipping_address_format",
                type: "TEXT",
                required: true
            },
            {
                name: "shipping_custom_field",
                column: "shipping_custom_field",
                type: "TEXT",
                required: true
            },
            {
                name: "shipping_method",
                column: "shipping_method",
                type: "VARCHAR",
                required: true
            },
            {
                name: "shipping_code",
                column: "shipping_code",
                type: "VARCHAR",
                required: true
            },
            {
                name: "comment",
                column: "comment",
                type: "TEXT",
                required: true
            },
            {
                name: "total",
                column: "total",
                type: "DECIMAL",
                required: true
            },
            {
                name: "order_status_id",
                column: "order_status_id",
                type: "INT",
                required: true
            },
            {
                name: "affiliate_id",
                column: "affiliate_id",
                type: "INT",
                required: true
            },
            {
                name: "commission",
                column: "commission",
                type: "DECIMAL",
                required: true
            },
            {
                name: "marketing_id",
                column: "marketing_id",
                type: "INT",
                required: true
            },
            {
                name: "tracking",
                column: "tracking",
                type: "VARCHAR",
                required: true
            },
            {
                name: "language_id",
                column: "language_id",
                type: "INT",
                required: true
            },
            {
                name: "currency_id",
                column: "currency_id",
                type: "INT",
                required: true
            },
            {
                name: "currency_code",
                column: "currency_code",
                type: "VARCHAR",
                required: true
            },
            {
                name: "currency_value",
                column: "currency_value",
                type: "DECIMAL",
                required: true
            },
            {
                name: "ip",
                column: "ip",
                type: "VARCHAR",
                required: true
            },
            {
                name: "forwarded_ip",
                column: "forwarded_ip",
                type: "VARCHAR",
                required: true
            },
            {
                name: "user_agent",
                column: "user_agent",
                type: "VARCHAR",
                required: true
            },
            {
                name: "accept_language",
                column: "accept_language",
                type: "VARCHAR",
                required: true
            },
            {
                name: "date_added",
                column: "date_added",
                type: "DATETIME",
                required: true
            },
            {
                name: "date_modified",
                column: "date_modified",
                type: "DATETIME",
                required: true
            }
        ]
    };

    private readonly dao;

    constructor(dataSource = "OpenCartDB") {
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
            table: "oc_order",
            entity: entity,
            key: {
                name: "order_id",
                column: "order_id",
                value: id
            }
        });
        return id;
    }

    public update(entity: oc_orderUpdateEntity): void {
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "oc_order",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "order_id",
                column: "order_id",
                value: entity.order_id
            }
        });
    }

    public upsert(entity: oc_orderCreateEntity | oc_orderUpdateEntity): number {
        const id = (entity as oc_orderUpdateEntity).order_id;
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
            table: "oc_order",
            entity: entity,
            key: {
                name: "order_id",
                column: "order_id",
                value: id
            }
        });
    }

    public count(options?: oc_orderEntityOptions): number {
        return this.dao.count(options);
    }

    public customDataCount(): number {
        const resultSet = query.execute('SELECT COUNT(*) AS COUNT FROM "oc_order"');
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
        triggerExtensions.forEach(triggerExtension => {
            try {
                triggerExtension.trigger(data);
            } catch (error) {
                console.error(error);
            }            
        });
        producer.topic("OpenCartDB-oc_order-oc_order").send(JSON.stringify(data));
    }
}
