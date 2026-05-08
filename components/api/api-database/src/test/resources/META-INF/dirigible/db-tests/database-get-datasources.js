import { Database } from '@aerokit/sdk/db/database';
import { Assert } from 'test/assert';

const datasources = Database.getDataSources();

console.log(JSON.stringify(datasources));

Assert.assertTrue(((datasources !== null) && (datasources !== undefined)));
