import { sql, query, update, insert } from "@aerokit/sdk/db";
import { assertEquals, test } from "@aerokit/sdk/junit"

const testName = 'create-insert-select-db-test';
test(testName, () => {
    console.log(`Executing ${testName}...`);
    const tableName = 'TEACHERS';
    const DATE_FORMAT = 'yyyyMMdd';

    try {
        const dropSql = sql.getDialect().drop().table(tableName).build();
        update.execute(dropSql);
    } catch (e) {
        console.log(`Table [${tableName}] is missing`);
    }

    const createTableSql = sql.getDialect()
        .create()
        .table(tableName)
        .column('Id', 'INTEGER')
        .columnVarchar("Name", 50)
        .column('Birthday', 'DATE')
        .build();

    update.execute(createTableSql);

    const resultParameters = {
        dateFormat: DATE_FORMAT
    };

    const selectSql = sql.getDialect()
        .select()
        .from(tableName)
        .build();

    const entries = query.execute(selectSql, undefined, undefined, resultParameters);
    assertEquals('Unexpected entries', 0, entries.length);

    const insertSql = sql.getDialect()//
        .insert()//
        .into(tableName)
        .column('Id')
        .column('Name')
        .column('Birthday')
        .build();

    const batchValues = [[1, "John", "2000-12-20"], [2, "Mary", "2001-11-21"]];

    insert.executeMany(insertSql, batchValues);

    const insertedEntries = query.execute(selectSql, undefined, undefined, resultParameters);
    assertEquals('Unexpected entries after insert', 2, insertedEntries.length);

    const expectedEntries = `[{"Id":1,"Name":"John","Birthday":"20001220"},{"Id":2,"Name":"Mary","Birthday":"20011121"}]`;
    assertEquals('Unexpected entries', expectedEntries, JSON.stringify(insertedEntries));
});
