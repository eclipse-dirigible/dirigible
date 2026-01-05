import { Query } from '@aerokit/sdk/db/query';
import { Update } from '@aerokit/sdk/db/update';
import { Assert } from 'test/assert';

Update.execute("CREATE TABLE U (A INT, B VARCHAR(10))");
Update.execute("INSERT INTO U VALUES (1, 'ABC')");
Update.execute("INSERT INTO U VALUES (2, 'DEF')");

const sql = "SELECT COUNT(*) AS C FROM U";
const resultset = Query.execute(sql);

console.log(JSON.stringify(resultset));

Update.execute("DROP TABLE U");

Assert.assertTrue(((resultset !== null) && (resultset !== undefined)));
