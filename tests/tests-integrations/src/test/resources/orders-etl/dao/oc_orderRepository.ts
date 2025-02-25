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
                name: "INVOICE_NO",
                column: "INVOICE_NO",
                type: "INT",
                required: true
            },
            {
                name: "INVOICE_PREFIX",
                column: "INVOICE_PREFIX",
                type: "VARCHAR",
                required: true
            },
            {
                name: "STORE_ID",
                column: "STORE_ID",
                type: "INT",
                required: true
            },
            {
                name: "STORE_NAME",
                column: "STORE_NAME",
                type: "VARCHAR",
                required: true
            },
            {
                name: "STORE_URL",
                column: "STORE_URL",
                type: "VARCHAR",
                required: true
            },
            {
                name: "CUSTOMER_ID",
                column: "CUSTOMER_ID",
                type: "INT",
                required: true
            },
            {
                name: "CUSTOMER_GROUP_ID",
                column: "CUSTOMER_GROUP_ID",
                type: "INT",
                required: true
            },
            {
                name: "FIRSTNAME",
                column: "FIRSTNAME",
                type: "VARCHAR",
                required: true
            },
            {
                name: "LASTNAME",
                column: "LASTNAME",
                type: "VARCHAR",
                required: true
            },
            {
                name: "EMAIL",
                column: "EMAIL",
                type: "VARCHAR",
                required: true
            },
            {
                name: "TELEPHONE",
                column: "TELEPHONE",
                type: "VARCHAR",
                required: true
            },
            {
                name: "FAX",
                column: "FAX",
                type: "VARCHAR",
                required: true
            },
            {
                name: "CUSTOM_FIELD",
                column: "CUSTOM_FIELD",
                type: "TEXT",
                required: true
            },
            {
                name: "PAYMENT_FIRSTNAME",
                column: "PAYMENT_FIRSTNAME",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_LASTNAME",
                column: "PAYMENT_LASTNAME",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_COMPANY",
                column: "PAYMENT_COMPANY",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_ADDRESS_1",
                column: "PAYMENT_ADDRESS_1",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_ADDRESS_2",
                column: "PAYMENT_ADDRESS_2",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_CITY",
                column: "PAYMENT_CITY",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_POSTCODE",
                column: "PAYMENT_POSTCODE",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_COUNTRY",
                column: "PAYMENT_COUNTRY",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_COUNTRY_ID",
                column: "PAYMENT_COUNTRY_ID",
                type: "INT",
                required: true
            },
            {
                name: "PAYMENT_ZONE",
                column: "PAYMENT_ZONE",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_ZONE_ID",
                column: "PAYMENT_ZONE_ID",
                type: "INT",
                required: true
            },
            {
                name: "PAYMENT_ADDRESS_FORMAT",
                column: "PAYMENT_ADDRESS_FORMAT",
                type: "TEXT",
                required: true
            },
            {
                name: "PAYMENT_CUSTOM_FIELD",
                column: "PAYMENT_CUSTOM_FIELD",
                type: "TEXT",
                required: true
            },
            {
                name: "PAYMENT_METHOD",
                column: "PAYMENT_METHOD",
                type: "VARCHAR",
                required: true
            },
            {
                name: "PAYMENT_CODE",
                column: "PAYMENT_CODE",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_FIRSTNAME",
                column: "SHIPPING_FIRSTNAME",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_LASTNAME",
                column: "SHIPPING_LASTNAME",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_COMPANY",
                column: "SHIPPING_COMPANY",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_ADDRESS_1",
                column: "SHIPPING_ADDRESS_1",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_ADDRESS_2",
                column: "SHIPPING_ADDRESS_2",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_CITY",
                column: "SHIPPING_CITY",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_POSTCODE",
                column: "SHIPPING_POSTCODE",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_COUNTRY",
                column: "SHIPPING_COUNTRY",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_COUNTRY_ID",
                column: "SHIPPING_COUNTRY_ID",
                type: "INT",
                required: true
            },
            {
                name: "SHIPPING_ZONE",
                column: "SHIPPING_ZONE",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_ZONE_ID",
                column: "SHIPPING_ZONE_ID",
                type: "INT",
                required: true
            },
            {
                name: "SHIPPING_ADDRESS_FORMAT",
                column: "SHIPPING_ADDRESS_FORMAT",
                type: "TEXT",
                required: true
            },
            {
                name: "SHIPPING_CUSTOM_FIELD",
                column: "SHIPPING_CUSTOM_FIELD",
                type: "TEXT",
                required: true
            },
            {
                name: "SHIPPING_METHOD",
                column: "SHIPPING_METHOD",
                type: "VARCHAR",
                required: true
            },
            {
                name: "SHIPPING_CODE",
                column: "SHIPPING_CODE",
                type: "VARCHAR",
                required: true
            },
            {
                name: "COMMENT",
                column: "COMMENT",
                type: "TEXT",
                required: true
            },
            {
                name: "TOTAL",
                column: "TOTAL",
                type: "DECIMAL",
                required: true
            },
            {
                name: "ORDER_STATUS_ID",
                column: "ORDER_STATUS_ID",
                type: "INT",
                required: true
            },
            {
                name: "AFFILIATE_ID",
                column: "AFFILIATE_ID",
                type: "INT",
                required: true
            },
            {
                name: "COMMISSION",
                column: "COMMISSION",
                type: "DECIMAL",
                required: true
            },
            {
                name: "MARKETING_ID",
                column: "MARKETING_ID",
                type: "INT",
                required: true
            },
            {
                name: "TRACKING",
                column: "TRACKING",
                type: "VARCHAR",
                required: true
            },
            {
                name: "LANGUAGE_ID",
                column: "LANGUAGE_ID",
                type: "INT",
                required: true
            },
            {
                name: "CURRENCY_ID",
                column: "CURRENCY_ID",
                type: "INT",
                required: true
            },
            {
                name: "CURRENCY_CODE",
                column: "CURRENCY_CODE",
                type: "VARCHAR",
                required: true
            },
            {
                name: "CURRENCY_VALUE",
                column: "CURRENCY_VALUE",
                type: "DECIMAL",
                required: true
            },
            {
                name: "IP",
                column: "IP",
                type: "VARCHAR",
                required: true
            },
            {
                name: "FORWARDED_IP",
                column: "FORWARDED_IP",
                type: "VARCHAR",
                required: true
            },
            {
                name: "USER_AGENT",
                column: "USER_AGENT",
                type: "VARCHAR",
                required: true
            },
            {
                name: "ACCEPT_LANGUAGE",
                column: "ACCEPT_LANGUAGE",
                type: "VARCHAR",
                required: true
            },
            {
                name: "DATE_ADDED",
                column: "DATE_ADDED",
                type: "DATETIME",
                required: true
            },
            {
                name: "DATE_MODIFIED",
                column: "DATE_MODIFIED",
                type: "DATETIME",
                required: true
            }
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
        const previousEntity = this.findById(entity.Id);
        this.dao.update(entity);
        this.triggerEvent({
            operation: "update",
            table: "OC_ORDER",
            entity: entity,
            previousEntity: previousEntity,
            key: {
                name: "ORDER_ID",
                column: "ORDER_ID",
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
