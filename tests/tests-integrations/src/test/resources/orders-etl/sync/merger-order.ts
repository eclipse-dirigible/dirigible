import { oc_orderEntity } from "orders-etl/dao/oc_orderRepository";
import { database } from "sdk/db";

export function onMessage(message: any) {
    const openCartOrder: oc_orderEntity = message.getBody();
    const exchangeRate = message.getExchangeProperty("currencyExchangeRate");

    console.log(`About to upsert Open cart order [${openCartOrder.ORDER_ID}] using exchange rate [${exchangeRate}]...`);

    upsertOrder(openCartOrder, exchangeRate);
    console.log(`Upserted Open cart order [${openCartOrder.ORDER_ID}]`);

    return message;
}

// Define SQL statements for H2 and PostgreSQL
const H2_MERGE_SQL = `
    MERGE INTO ORDERS
        (ID, TOTAL, DATEADDED) 
    KEY(ID)
    VALUES (?, ?, ?)
`;

const POSTGRES_UPSERT_SQL = `
    INSERT INTO ORDERS
        (ID, TOTAL, DATEADDED) 
    VALUES (?, ?, ?)
    ON CONFLICT (ID) DO UPDATE SET
        TOTAL = EXCLUDED.TOTAL,
        DATEADDED = EXCLUDED.DATEADDED
`;

function upsertOrder(openCartOrder: oc_orderEntity, exchangeRate: number) {
    const totalEuro = openCartOrder.TOTAL * exchangeRate;

    const connection = database.getConnection();
    const databaseType = connection.getMetaData().getDatabaseProductName().toLowerCase();

    // Choose the appropriate SQL statement based on the database type
    const sql = databaseType.includes("postgresql") ? POSTGRES_UPSERT_SQL : H2_MERGE_SQL;

    const statement = connection.prepareStatement(sql);
    try {
        statement.setLong(1, openCartOrder.ORDER_ID);
        statement.setDouble(2, totalEuro);
        statement.setTimestamp(3, openCartOrder.DATE_ADDED);
        statement.executeUpdate();
    } finally {
        statement.close();
        connection.close();
    }
}