import { Query } from '@aerokit/sdk/db/query';
import { Update } from '@aerokit/sdk/db/update';
import { Assert } from 'test/assert';

Update.execute("CREATE TABLE Q (A INT, B VARCHAR(10))");
Update.execute("INSERT INTO Q VALUES (1, 'ABC')");
Update.execute("INSERT INTO Q VALUES (2, 'DEF')");

const sql = "SELECT * FROM Q WHERE A = ?";
const resultset = Query.execute(sql, [1]);

console.log(JSON.stringify(resultset));

Update.execute("DROP TABLE Q");

Assert.assertTrue((resultset !== null) && (resultset !== undefined));
