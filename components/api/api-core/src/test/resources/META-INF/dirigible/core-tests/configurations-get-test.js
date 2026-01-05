import { Configurations } from '@aerokit/sdk/core/configurations';
import { Assert } from 'test/assert';

Configurations.set('name1', 'value1');
const result = Configurations.get('name1');

Assert.assertEquals(result, 'value1');
