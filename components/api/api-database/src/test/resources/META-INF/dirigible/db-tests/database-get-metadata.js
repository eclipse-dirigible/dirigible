import { Database } from '@aerokit/sdk/db/database';
import { Assert } from 'test/assert';

const metadata = Database.getMetadata();

console.log(JSON.stringify(metadata));

Assert.assertTrue((metadata !== null) && (metadata !== undefined));
