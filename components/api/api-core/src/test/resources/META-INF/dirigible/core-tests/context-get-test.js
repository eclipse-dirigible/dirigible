import { Context } from '@aerokit/sdk/core/context';
import { Assert } from 'test/assert';

Context.set('name1', 'value1');
const result = Context.get('name1');

Assert.assertEquals(result, 'value1');
