import { Env } from '@aerokit/sdk/core/env';
import { Assert } from 'test/assert';

const result = Env.list();

Assert.assertTrue(result !== undefined && result !== null);
