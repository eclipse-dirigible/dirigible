import { database } from "sdk/db";

export function onMessage(message: any) {
    const connection = database.getConnection();
    const databaseType = connection.getDatabaseSystem();

    console.log("JDBC Database type: " + databaseType);

    message.setExchangeProperty("db_type", databaseType == 3 ? 'H2' : 'Postgres');

    return message;
}