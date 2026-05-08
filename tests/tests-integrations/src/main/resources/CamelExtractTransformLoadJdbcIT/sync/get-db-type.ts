import { database, DatabaseSystem } from "@aerokit/sdk/db";

export function onMessage(message: any) {
    const connection = database.getConnection();
    const databaseType = connection.getDatabaseSystem();

    console.log("JDBC Database type: " + databaseType);

    let databaseTypeString;

    switch (databaseType) {
        case DatabaseSystem.H2:
            databaseTypeString = 'H2';
            break;
        case DatabaseSystem.POSTGRESQL:
            databaseTypeString = 'Postgres';
            break;
        case DatabaseSystem.MSSQL:
            databaseTypeString = 'MSSQL';
            break;
        default:
            throw new Error(`Unsupported connection of type ${databaseType}`);
    }

    console.log("db_type: " + databaseTypeString);

    message.setExchangeProperty("db_type", databaseTypeString);

    return message;
}
