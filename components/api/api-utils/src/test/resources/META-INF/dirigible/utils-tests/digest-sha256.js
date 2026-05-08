
import { Digest } from '@aerokit/sdk/utils/digest';
import { Assert } from 'test/assert';

const input = [41, 42, 43];
const result = Digest.sha256(input);

console.log(JSON.stringify(result));

Assert.assertTrue(result.length === 32 && result[0] === 100);
