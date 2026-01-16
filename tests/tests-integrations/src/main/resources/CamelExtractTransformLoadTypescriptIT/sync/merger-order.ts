import { oc_orderEntity } from "../dao/oc_orderRepository";
import { database, DatabaseSystem } from "@aerokit/sdk/db";

export function onMessage(message: any) {
    const openCartOrder: oc_orderEntity = message.getBody();
    const exchangeRate = message.getExchangeProperty("currencyExchangeRate");

    console.log(`About to upsert Open cart order [${openCartOrder.ORDER_ID}] using exchange rate [${exchangeRate}]...`);

    upsertOrder(openCartOrder, exchangeRate);
    console.log(`Upserted Open cart order [${openCartOrder.ORDER_ID}]`);

    return message;
}

const H2_MERGE_SQL = `
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

const MSSQL_MERGE = `
    MERGE INTO ORDERS AS target
    USING (
        VALUES (?, ?, ?)
    ) AS source (ID, TOTAL, DATEADDED)
    ON target.ID = source.ID
    WHEN MATCHED THEN
        UPDATE SET
            target.TOTAL     = source.TOTAL,
            target.DATEADDED = source.DATEADDED
    WHEN NOT MATCHED THEN
        INSERT (ID, TOTAL, DATEADDED)
        VALUES (source.ID, source.TOTAL, source.DATEADDED);
`;
function upsertOrder(openCartOrder: oc_orderEntity, exchangeRate: number) {
    const totalEuro = openCartOrder.TOTAL * exchangeRate;

    const connection = database.getConnection();
    const databaseType = connection.getDatabaseSystem();
    console.log("TypeScript Database type: " + databaseType);

    let mergeSQL: string;

    switch (databaseType) {
        case DatabaseSystem.H2:
            mergeSQL = H2_MERGE_SQL;
            break;
        case DatabaseSystem.POSTGRESQL:
            mergeSQL = POSTGRES_SQL;
            break;
        case DatabaseSystem.MSSQL:
            mergeSQL = MSSQL_MERGE;
            break;
        default:
            throw new Error(`Unsupported connection of type ${databaseType}`);
    }

    console.log("mergeSQL:" + mergeSQL);

    const statement = connection.prepareStatement(mergeSQL);
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
