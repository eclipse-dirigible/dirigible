import { Globals } from '@aerokit/sdk/core/globals';
import { Assert } from 'test/assert';

Globals.set("name1", "value1");
const result = Globals.get('name1');

Assert.assertEquals(result, 'value1');
