import { Env } from '@aerokit/sdk/core/env';
import { Assert } from 'test/assert';

const obj = Env.list();
const key = Object.keys(obj)[0];

const result = Env.get(key);

Assert.assertTrue(result !== undefined && result !== null);
