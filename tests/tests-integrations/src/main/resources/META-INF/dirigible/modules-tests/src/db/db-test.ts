import { sql, query, update, insert } from "sdk/db";
import { assertEquals, test } from "sdk/junit"

test('create-insert-select-db-test', () => {
    const tableName = 'TEACHERS';
    const DATE_FORMAT = 'yyyyMMdd';
    const createTableSql = sql.getDialect()
        .create()
        .table(tableName)
        .column('Id', 'INTEGER')
        .column('Name', 'VARCHAR')
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
    assertEquals('Unexpected entries after insert', 2, entries.length);

    const expectedEntries = `[{"Id":1,"Name":"John","Birthday":"20001220"},{"Id":2,"Name":"Mary","Birthday":"20011121"}]`;
    assertEquals('Unexpected entries', expectedEntries, JSON.stringify(insertedEntries));
});
