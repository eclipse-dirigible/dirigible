import { Engine } from '@aerokit/sdk/platform/engines';
import { Assert } from 'test/assert';

const result = Engine.getTypes();

Assert.assertTrue(result !== undefined && result !== null);
