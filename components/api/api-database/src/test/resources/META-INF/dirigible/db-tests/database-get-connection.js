import { Database } from '@aerokit/sdk/db/database';
import { Update } from '@aerokit/sdk/db/update';
import { Assert } from 'test/assert';

Update.execute("CREATE TABLE T (A INT, B VARCHAR(10))");
Update.execute("INSERT INTO T VALUES (1, 'ABC')");
Update.execute("INSERT INTO T VALUES (2, 'DEF')");

const sql = "SELECT * FROM T WHERE A = ?";

let value;
const connection = Database.getConnection();
try {
	const statement = connection.prepareStatement(sql);
	try {
		statement.setInt(1, 2);
		const resultset = statement.executeQuery();
		try {
			while (resultset.next()) {
				value = resultset.getString('B');
				console.log('B: ' + value);
			}
		} finally {
			resultset.close();
		}
	} finally {
		statement.close();
	}
} finally {
	connection.close();
}

Update.execute("DROP TABLE T");

Assert.assertTrue(value == 'DEF');
