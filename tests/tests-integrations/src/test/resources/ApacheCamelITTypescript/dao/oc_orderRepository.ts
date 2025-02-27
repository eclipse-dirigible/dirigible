import { query } from "sdk/db";
import { producer } from "sdk/messaging";
import { extensions } from "sdk/extensions";
import { dao as daoApi } from "sdk/db";

export interface oc_orderEntity {
    readonly ORDER_ID: number;
    INVOICE_NO: number;
    INVOICE_PREFIX: string;
    STORE_ID: number;
    STORE_NAME: string;
    STORE_URL: string;
    CUSTOMER_ID: number;
    CUSTOMER_GROUP_ID: number;
    FIRSTNAME: string;
    LASTNAME: string;
    EMAIL: string;
    TELEPHONE: string;
    FAX: string;
    CUSTOM_FIELD: string;
    PAYMENT_FIRSTNAME: string;
    PAYMENT_LASTNAME: string;
    PAYMENT_COMPANY: string;
    PAYMENT_ADDRESS_1: string;
    PAYMENT_ADDRESS_2: string;
    PAYMENT_CITY: string;
    PAYMENT_POSTCODE: string;
    PAYMENT_COUNTRY: string;
    PAYMENT_COUNTRY_ID: number;
    PAYMENT_ZONE: string;
    PAYMENT_ZONE_ID: number;
    PAYMENT_ADDRESS_FORMAT: string;
    PAYMENT_CUSTOM_FIELD: string;
    PAYMENT_METHOD: string;
    PAYMENT_CODE: string;
    SHIPPING_FIRSTNAME: string;
    SHIPPING_LASTNAME: string;
    SHIPPING_COMPANY: string;
    SHIPPING_ADDRESS_1: string;
    SHIPPING_ADDRESS_2: string;
    SHIPPING_CITY: string;
    SHIPPING_POSTCODE: string;
    SHIPPING_COUNTRY: string;
    SHIPPING_COUNTRY_ID: number;
    SHIPPING_ZONE: string;
    SHIPPING_ZONE_ID: number;
    SHIPPING_ADDRESS_FORMAT: string;
    SHIPPING_CUSTOM_FIELD: string;
    SHIPPING_METHOD: string;
    SHIPPING_CODE: string;
    COMMENT: string;
    TOTAL: number;
    ORDER_STATUS_ID: number;
    AFFILIATE_ID: number;
    COMMISSION: number;
    MARKETING_ID: number;
    TRACKING: string;
    LANGUAGE_ID: number;
    CURRENCY_ID: number;
    CURRENCY_CODE: string;
    CURRENCY_VALUE: number;
    IP: string;
    FORWARDED_IP: string;
    USER_AGENT: string;
    ACCEPT_LANGUAGE: string;
    DATE_ADDED: Date;
    DATE_MODIFIED: Date;
}

export interface oc_orderCreateEntity {
    readonly INVOICE_NO: number;
    readonly INVOICE_PREFIX: string;
    readonly STORE_ID: number;
    readonly STORE_NAME: string;
    readonly STORE_URL: string;
    readonly CUSTOMER_ID: number;
    readonly CUSTOMER_GROUP_ID: number;
    readonly FIRSTNAME: string;
    readonly LASTNAME: string;
    readonly EMAIL: string;
    readonly TELEPHONE: string;
    readonly FAX: string;
    readonly CUSTOM_FIELD: string;
    readonly PAYMENT_FIRSTNAME: string;
    readonly PAYMENT_LASTNAME: string;
    readonly PAYMENT_COMPANY: string;
    readonly PAYMENT_ADDRESS_1: string;
    readonly PAYMENT_ADDRESS_2: string;
    readonly PAYMENT_CITY: string;
    readonly PAYMENT_POSTCODE: string;
    readonly PAYMENT_COUNTRY: string;
    readonly PAYMENT_COUNTRY_ID: number;
    readonly PAYMENT_ZONE: string;
    readonly PAYMENT_ZONE_ID: number;
    readonly PAYMENT_ADDRESS_FORMAT: string;
    readonly PAYMENT_CUSTOM_FIELD: string;
    readonly PAYMENT_METHOD: string;
    readonly PAYMENT_CODE: string;
    readonly SHIPPING_FIRSTNAME: string;
    readonly SHIPPING_LASTNAME: string;
    readonly SHIPPING_COMPANY: string;
    readonly SHIPPING_ADDRESS_1: string;
    readonly SHIPPING_ADDRESS_2: string;
    readonly SHIPPING_CITY: string;
    readonly SHIPPING_POSTCODE: string;
    readonly SHIPPING_COUNTRY: string;
    readonly SHIPPING_COUNTRY_ID: number;
    readonly SHIPPING_ZONE: string;
    readonly SHIPPING_ZONE_ID: number;
    readonly SHIPPING_ADDRESS_FORMAT: string;
    readonly SHIPPING_CUSTOM_FIELD: string;
    readonly SHIPPING_METHOD: string;
    readonly SHIPPING_CODE: string;
    readonly COMMENT: string;
    readonly TOTAL: number;
    readonly ORDER_STATUS_ID: number;
    readonly AFFILIATE_ID: number;
    readonly COMMISSION: number;
    readonly MARKETING_ID: number;
    readonly TRACKING: string;
    readonly LANGUAGE_ID: number;
    readonly CURRENCY_ID: number;
    readonly CURRENCY_CODE: string;
    readonly CURRENCY_VALUE: number;
    readonly IP: string;
    readonly FORWARDED_IP: string;
    readonly USER_AGENT: string;
    readonly ACCEPT_LANGUAGE: string;
    readonly DATE_ADDED: Date;
    readonly DATE_MODIFIED: Date;
}

export interface oc_orderUpdateEntity extends oc_orderCreateEntity {
    readonly ORDER_ID: number;
}

export interface oc_orderEntityOptions {
    $filter?: {
        equals?: {
            ORDER_ID?: number | number[];
            INVOICE_NO?: number | number[];
            INVOICE_PREFIX?: string | string[];
            STORE_ID?: number | number[];
            STORE_NAME?: string | string[];
            STORE_URL?: string | string[];
            CUSTOMER_ID?: number | number[];
            CUSTOMER_GROUP_ID?: number | number[];
            FIRSTNAME?: string | string[];
            LASTNAME?: string | string[];
            EMAIL?: string | string[];
            TELEPHONE?: string | string[];
            FAX?: string | string[];
            CUSTOM_FIELD?: string | string[];
            PAYMENT_FIRSTNAME?: string | string[];
            PAYMENT_LASTNAME?: string | string[];
            PAYMENT_COMPANY?: string | string[];
            PAYMENT_ADDRESS_1?: string | string[];
            PAYMENT_ADDRESS_2?: string | string[];
            PAYMENT_CITY?: string | string[];
            PAYMENT_POSTCODE?: string | string[];
            PAYMENT_COUNTRY?: string | string[];
            PAYMENT_COUNTRY_ID?: number | number[];
            PAYMENT_ZONE?: string | string[];
            PAYMENT_ZONE_ID?: number | number[];
            PAYMENT_ADDRESS_FORMAT?: string | string[];
            PAYMENT_CUSTOM_FIELD?: string | string[];
            PAYMENT_METHOD?: string | string[];
            PAYMENT_CODE?: string | string[];
            SHIPPING_FIRSTNAME?: string | string[];
            SHIPPING_LASTNAME?: string | string[];
            SHIPPING_COMPANY?: string | string[];
            SHIPPING_ADDRESS_1?: string | string[];
            SHIPPING_ADDRESS_2?: string | string[];
            SHIPPING_CITY?: string | string[];
            SHIPPING_POSTCODE?: string | string[];
            SHIPPING_COUNTRY?: string | string[];
            SHIPPING_COUNTRY_ID?: number | number[];
            SHIPPING_ZONE?: string | string[];
            SHIPPING_ZONE_ID?: number | number[];
            SHIPPING_ADDRESS_FORMAT?: string | string[];
            SHIPPING_CUSTOM_FIELD?: string | string[];
            SHIPPING_METHOD?: string | string[];
            SHIPPING_CODE?: string | string[];
            COMMENT?: string | string[];
            TOTAL?: number | number[];
            ORDER_STATUS_ID?: number | number[];
            AFFILIATE_ID?: number | number[];
            COMMISSION?: number | number[];
            MARKETING_ID?: number | number[];
            TRACKING?: string | string[];
            LANGUAGE_ID?: number | number[];
            CURRENCY_ID?: number | number[];
            CURRENCY_CODE?: string | string[];
            CURRENCY_VALUE?: number | number[];
            IP?: string | string[];
            FORWARDED_IP?: string | string[];
            USER_AGENT?: string | string[];
            ACCEPT_LANGUAGE?: string | string[];
            DATE_ADDED?: Date | Date[];
            DATE_MODIFIED?: Date | Date[];
        };
        notEquals?: {
            ORDER_ID?: number | number[];
            INVOICE_NO?: number | number[];
            INVOICE_PREFIX?: string | string[];
            STORE_ID?: number | number[];
            STORE_NAME?: string | string[];
            STORE_URL?: string | string[];
            CUSTOMER_ID?: number | number[];
            CUSTOMER_GROUP_ID?: number | number[];
            FIRSTNAME?: string | string[];
            LASTNAME?: string | string[];
            EMAIL?: string | string[];
            TELEPHONE?: string | string[];
            FAX?: string | string[];
            CUSTOM_FIELD?: string | string[];
            PAYMENT_FIRSTNAME?: string | string[];
            PAYMENT_LASTNAME?: string | string[];
            PAYMENT_COMPANY?: string | string[];
            PAYMENT_ADDRESS_1?: string | string[];
            PAYMENT_ADDRESS_2?: string | string[];
            PAYMENT_CITY?: string | string[];
            PAYMENT_POSTCODE?: string | string[];
            PAYMENT_COUNTRY?: string | string[];
            PAYMENT_COUNTRY_ID?: number | number[];
            PAYMENT_ZONE?: string | string[];
            PAYMENT_ZONE_ID?: number | number[];
            PAYMENT_ADDRESS_FORMAT?: string | string[];
            PAYMENT_CUSTOM_FIELD?: string | string[];
            PAYMENT_METHOD?: string | string[];
            PAYMENT_CODE?: string | string[];
            SHIPPING_FIRSTNAME?: string | string[];
            SHIPPING_LASTNAME?: string | string[];
            SHIPPING_COMPANY?: string | string[];
            SHIPPING_ADDRESS_1?: string | string[];
            SHIPPING_ADDRESS_2?: string | string[];
            SHIPPING_CITY?: string | string[];
            SHIPPING_POSTCODE?: string | string[];
            SHIPPING_COUNTRY?: string | string[];
            SHIPPING_COUNTRY_ID?: number | number[];
            SHIPPING_ZONE?: string | string[];
            SHIPPING_ZONE_ID?: number | number[];
            SHIPPING_ADDRESS_FORMAT?: string | string[];
            SHIPPING_CUSTOM_FIELD?: string | string[];
            SHIPPING_METHOD?: string | string[];
            SHIPPING_CODE?: string | string[];
            COMMENT?: string | string[];
            TOTAL?: number | number[];
            ORDER_STATUS_ID?: number | number[];
            AFFILIATE_ID?: number | number[];
            COMMISSION?: number | number[];
            MARKETING_ID?: number | number[];
            TRACKING?: string | string[];
            LANGUAGE_ID?: number | number[];
            CURRENCY_ID?: number | number[];
            CURRENCY_CODE?: string | string[];
            CURRENCY_VALUE?: number | number[];
            IP?: string | string[];
            FORWARDED_IP?: string | string[];
            USER_AGENT?: string | string[];
            ACCEPT_LANGUAGE?: string | string[];
            DATE_ADDED?: Date | Date[];
            DATE_MODIFIED?: Date | Date[];
        };
        contains?: {
            ORDER_ID?: number;
            INVOICE_NO?: number;
            INVOICE_PREFIX?: string;
            STORE_ID?: number;
            STORE_NAME?: string;
            STORE_URL?: string;
            CUSTOMER_ID?: number;
            CUSTOMER_GROUP_ID?: number;
            FIRSTNAME?: string;
            LASTNAME?: string;
            EMAIL?: string;
            TELEPHONE?: string;
            FAX?: string;
            CUSTOM_FIELD?: string;
            PAYMENT_FIRSTNAME?: string;
            PAYMENT_LASTNAME?: string;
            PAYMENT_COMPANY?: string;
            PAYMENT_ADDRESS_1?: string;
            PAYMENT_ADDRESS_2?: string;
            PAYMENT_CITY?: string;
            PAYMENT_POSTCODE?: string;
            PAYMENT_COUNTRY?: string;
            PAYMENT_COUNTRY_ID?: number;
            PAYMENT_ZONE?: string;
            PAYMENT_ZONE_ID?: number;
            PAYMENT_ADDRESS_FORMAT?: string;
            PAYMENT_CUSTOM_FIELD?: string;
            PAYMENT_METHOD?: string;
            PAYMENT_CODE?: string;
            SHIPPING_FIRSTNAME?: string;
            SHIPPING_LASTNAME?: string;
            SHIPPING_COMPANY?: string;
            SHIPPING_ADDRESS_1?: string;
            SHIPPING_ADDRESS_2?: string;
            SHIPPING_CITY?: string;
            SHIPPING_POSTCODE?: string;
            SHIPPING_COUNTRY?: string;
            SHIPPING_COUNTRY_ID?: number;
            SHIPPING_ZONE?: string;
            SHIPPING_ZONE_ID?: number;
            SHIPPING_ADDRESS_FORMAT?: string;
            SHIPPING_CUSTOM_FIELD?: string;
            SHIPPING_METHOD?: string;
            SHIPPING_CODE?: string;
            COMMENT?: string;
            TOTAL?: number;
            ORDER_STATUS_ID?: number;
            AFFILIATE_ID?: number;
            COMMISSION?: number;
            MARKETING_ID?: number;
            TRACKING?: string;
            LANGUAGE_ID?: number;
            CURRENCY_ID?: number;
            CURRENCY_CODE?: string;
            CURRENCY_VALUE?: number;
            IP?: string;
            FORWARDED_IP?: string;
            USER_AGENT?: string;
            ACCEPT_LANGUAGE?: string;
            DATE_ADDED?: Date;
            DATE_MODIFIED?: Date;
        };
        greaterThan?: {
            ORDER_ID?: number;
            INVOICE_NO?: number;
            INVOICE_PREFIX?: string;
            STORE_ID?: number;
            STORE_NAME?: string;
            STORE_URL?: string;
            CUSTOMER_ID?: number;
            CUSTOMER_GROUP_ID?: number;
            FIRSTNAME?: string;
            LASTNAME?: string;
            EMAIL?: string;
            TELEPHONE?: string;
            FAX?: string;
            CUSTOM_FIELD?: string;
            PAYMENT_FIRSTNAME?: string;
            PAYMENT_LASTNAME?: string;
            PAYMENT_COMPANY?: string;
            PAYMENT_ADDRESS_1?: string;
            PAYMENT_ADDRESS_2?: string;
            PAYMENT_CITY?: string;
            PAYMENT_POSTCODE?: string;
            PAYMENT_COUNTRY?: string;
            PAYMENT_COUNTRY_ID?: number;
            PAYMENT_ZONE?: string;
            PAYMENT_ZONE_ID?: number;
            PAYMENT_ADDRESS_FORMAT?: string;
            PAYMENT_CUSTOM_FIELD?: string;
            PAYMENT_METHOD?: string;
            PAYMENT_CODE?: string;
            SHIPPING_FIRSTNAME?: string;
            SHIPPING_LASTNAME?: string;
            SHIPPING_COMPANY?: string;
            SHIPPING_ADDRESS_1?: string;
            SHIPPING_ADDRESS_2?: string;
            SHIPPING_CITY?: string;
            SHIPPING_POSTCODE?: string;
            SHIPPING_COUNTRY?: string;
            SHIPPING_COUNTRY_ID?: number;
            SHIPPING_ZONE?: string;
            SHIPPING_ZONE_ID?: number;
            SHIPPING_ADDRESS_FORMAT?: string;
            SHIPPING_CUSTOM_FIELD?: string;
            SHIPPING_METHOD?: string;
            SHIPPING_CODE?: string;
            COMMENT?: string;
            TOTAL?: number;
            ORDER_STATUS_ID?: number;
            AFFILIATE_ID?: number;
            COMMISSION?: number;
            MARKETING_ID?: number;
            TRACKING?: string;
            LANGUAGE_ID?: number;
            CURRENCY_ID?: number;
            CURRENCY_CODE?: string;
            CURRENCY_VALUE?: number;
            IP?: string;
            FORWARDED_IP?: string;
            USER_AGENT?: string;
            ACCEPT_LANGUAGE?: string;
            DATE_ADDED?: Date;
            DATE_MODIFIED?: Date;
        };
        greaterThanOrEqual?: {
            ORDER_ID?: number;
            INVOICE_NO?: number;
            INVOICE_PREFIX?: string;
            STORE_ID?: number;
            STORE_NAME?: string;
            STORE_URL?: string;
            CUSTOMER_ID?: number;
            CUSTOMER_GROUP_ID?: number;
            FIRSTNAME?: string;
            LASTNAME?: string;
            EMAIL?: string;
            TELEPHONE?: string;
            FAX?: string;
            CUSTOM_FIELD?: string;
            PAYMENT_FIRSTNAME?: string;
            PAYMENT_LASTNAME?: string;
            PAYMENT_COMPANY?: string;
            PAYMENT_ADDRESS_1?: string;
            PAYMENT_ADDRESS_2?: string;
            PAYMENT_CITY?: string;
            PAYMENT_POSTCODE?: string;
            PAYMENT_COUNTRY?: string;
            PAYMENT_COUNTRY_ID?: number;
            PAYMENT_ZONE?: string;
            PAYMENT_ZONE_ID?: number;
            PAYMENT_ADDRESS_FORMAT?: string;
            PAYMENT_CUSTOM_FIELD?: string;
            PAYMENT_METHOD?: string;
            PAYMENT_CODE?: string;
            SHIPPING_FIRSTNAME?: string;
            SHIPPING_LASTNAME?: string;
            SHIPPING_COMPANY?: string;
            SHIPPING_ADDRESS_1?: string;
            SHIPPING_ADDRESS_2?: string;
            SHIPPING_CITY?: string;
            SHIPPING_POSTCODE?: string;
            SHIPPING_COUNTRY?: string;
            SHIPPING_COUNTRY_ID?: number;
            SHIPPING_ZONE?: string;
            SHIPPING_ZONE_ID?: number;
            SHIPPING_ADDRESS_FORMAT?: string;
            SHIPPING_CUSTOM_FIELD?: string;
            SHIPPING_METHOD?: string;
            SHIPPING_CODE?: string;
            COMMENT?: string;
            TOTAL?: number;
            ORDER_STATUS_ID?: number;
            AFFILIATE_ID?: number;
            COMMISSION?: number;
            MARKETING_ID?: number;
            TRACKING?: string;
            LANGUAGE_ID?: number;
            CURRENCY_ID?: number;
            CURRENCY_CODE?: string;
            CURRENCY_VALUE?: number;
            IP?: string;
            FORWARDED_IP?: string;
            USER_AGENT?: string;
            ACCEPT_LANGUAGE?: string;
            DATE_ADDED?: Date;
            DATE_MODIFIED?: Date;
        };
        lessThan?: {
            ORDER_ID?: number;
            INVOICE_NO?: number;
            INVOICE_PREFIX?: string;
            STORE_ID?: number;
            STORE_NAME?: string;
            STORE_URL?: string;
            CUSTOMER_ID?: number;
            CUSTOMER_GROUP_ID?: number;
            FIRSTNAME?: string;
            LASTNAME?: string;
            EMAIL?: string;
            TELEPHONE?: string;
            FAX?: string;
            CUSTOM_FIELD?: string;
            PAYMENT_FIRSTNAME?: string;
            PAYMENT_LASTNAME?: string;
            PAYMENT_COMPANY?: string;
            PAYMENT_ADDRESS_1?: string;
            PAYMENT_ADDRESS_2?: string;
            PAYMENT_CITY?: string;
            PAYMENT_POSTCODE?: string;
            PAYMENT_COUNTRY?: string;
            PAYMENT_COUNTRY_ID?: number;
            PAYMENT_ZONE?: string;
            PAYMENT_ZONE_ID?: number;
            PAYMENT_ADDRESS_FORMAT?: string;
            PAYMENT_CUSTOM_FIELD?: string;
            PAYMENT_METHOD?: string;
            PAYMENT_CODE?: string;
            SHIPPING_FIRSTNAME?: string;
            SHIPPING_LASTNAME?: string;
            SHIPPING_COMPANY?: string;
            SHIPPING_ADDRESS_1?: string;
            SHIPPING_ADDRESS_2?: string;
            SHIPPING_CITY?: string;
            SHIPPING_POSTCODE?: string;
            SHIPPING_COUNTRY?: string;
            SHIPPING_COUNTRY_ID?: number;
            SHIPPING_ZONE?: string;
            SHIPPING_ZONE_ID?: number;
            SHIPPING_ADDRESS_FORMAT?: string;
            SHIPPING_CUSTOM_FIELD?: string;
            SHIPPING_METHOD?: string;
            SHIPPING_CODE?: string;
            COMMENT?: string;
            TOTAL?: number;
            ORDER_STATUS_ID?: number;
            AFFILIATE_ID?: number;
            COMMISSION?: number;
            MARKETING_ID?: number;
            TRACKING?: string;
            LANGUAGE_ID?: number;
            CURRENCY_ID?: number;
            CURRENCY_CODE?: string;
            CURRENCY_VALUE?: number;
            IP?: string;
            FORWARDED_IP?: string;
            USER_AGENT?: string;
            ACCEPT_LANGUAGE?: string;
            DATE_ADDED?: Date;
            DATE_MODIFIED?: Date;
        };
        lessThanOrEqual?: {
            ORDER_ID?: number;
            INVOICE_NO?: number;
            INVOICE_PREFIX?: string;
            STORE_ID?: number;
            STORE_NAME?: string;
            STORE_URL?: string;
            CUSTOMER_ID?: number;
            CUSTOMER_GROUP_ID?: number;
            FIRSTNAME?: string;
            LASTNAME?: string;
            EMAIL?: string;
            TELEPHONE?: string;
            FAX?: string;
            CUSTOM_FIELD?: string;
            PAYMENT_FIRSTNAME?: string;
            PAYMENT_LASTNAME?: string;
            PAYMENT_COMPANY?: string;
            PAYMENT_ADDRESS_1?: string;
            PAYMENT_ADDRESS_2?: string;
            PAYMENT_CITY?: string;
            PAYMENT_POSTCODE?: string;
            PAYMENT_COUNTRY?: string;
            PAYMENT_COUNTRY_ID?: number;
            PAYMENT_ZONE?: string;
            PAYMENT_ZONE_ID?: number;
            PAYMENT_ADDRESS_FORMAT?: string;
            PAYMENT_CUSTOM_FIELD?: string;
            PAYMENT_METHOD?: string;
            PAYMENT_CODE?: string;
            SHIPPING_FIRSTNAME?: string;
            SHIPPING_LASTNAME?: string;
            SHIPPING_COMPANY?: string;
            SHIPPING_ADDRESS_1?: string;
            SHIPPING_ADDRESS_2?: string;
            SHIPPING_CITY?: string;
            SHIPPING_POSTCODE?: string;
            SHIPPING_COUNTRY?: string;
            SHIPPING_COUNTRY_ID?: number;
            SHIPPING_ZONE?: string;
            SHIPPING_ZONE_ID?: number;
            SHIPPING_ADDRESS_FORMAT?: string;
            SHIPPING_CUSTOM_FIELD?: string;
            SHIPPING_METHOD?: string;
            SHIPPING_CODE?: string;
            COMMENT?: string;
            TOTAL?: number;
            ORDER_STATUS_ID?: number;
            AFFILIATE_ID?: number;
            COMMISSION?: number;
            MARKETING_ID?: number;
            TRACKING?: string;
            LANGUAGE_ID?: number;
            CURRENCY_ID?: number;
            CURRENCY_CODE?: string;
            CURRENCY_VALUE?: number;
            IP?: string;
            FORWARDED_IP?: string;
            USER_AGENT?: string;
            ACCEPT_LANGUAGE?: string;
            DATE_ADDED?: Date;
            DATE_MODIFIED?: Date;
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
