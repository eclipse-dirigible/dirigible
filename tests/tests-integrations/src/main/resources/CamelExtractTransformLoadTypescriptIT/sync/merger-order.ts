import { oc_orderEntity } from "../dao/oc_orderRepository";
import { database } from "@aerokit/sdk/db";

export function onMessage(message: any) {
    const openCartOrder: oc_orderEntity = message.getBody();
    const exchangeRate = message.getExchangeProperty("currencyExchangeRate");

    console.log(`About to upsert Open cart order [${openCartOrder.ORDER_ID}] using exchange rate [${exchangeRate}]...`);

    upsertOrder(openCartOrder, exchangeRate);
    console.log(`Upserted Open cart order [${openCartOrder.ORDER_ID}]`);

    return message;
}

const MERGE_SQL = `
    MERGE INTO ORDERS
        (ID, TOTAL, DATEADDED) 
    KEY(ID)
    VALUES (?, ?, ?)
`;

const POSTGRES_SQL = `
    INSERT INTO "ORDERS"
        ("ID", "TOTAL", "DATEADDED") 
    VALUES (?, ?, ?)
    ON CONFLICT ("ID") DO UPDATE SET
        "TOTAL" = EXCLUDED."TOTAL",
        "DATEADDED" = EXCLUDED."DATEADDED"
`;

function upsertOrder(openCartOrder: oc_orderEntity, exchangeRate: number) {
    const totalEuro = openCartOrder.TOTAL * exchangeRate;

    const connection = database.getConnection();
    const databaseType = connection.getDatabaseSystem();
    console.log("TypeScript Database type: " + databaseType);

    // 3 is Enum for H2 databasse
    const statement = databaseType == 3 ? connection.prepareStatement(MERGE_SQL) : connection.prepareStatement(POSTGRES_SQL);
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
