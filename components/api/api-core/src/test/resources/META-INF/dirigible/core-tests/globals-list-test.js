import { Globals } from '@aerokit/sdk/core/globals';
import { Assert } from 'test/assert';

const result = Globals.list();

Assert.assertTrue(result !== undefined && result !== null, "Result of globals.list() is undefined or null");
