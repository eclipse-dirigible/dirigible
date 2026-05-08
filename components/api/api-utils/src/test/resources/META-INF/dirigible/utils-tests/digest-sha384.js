
import { Digest } from '@aerokit/sdk/utils/digest';
import { Assert } from 'test/assert';

const input = [41, 42, 43];
const result = Digest.sha384(input);

console.log(JSON.stringify(result));

Assert.assertTrue(result.length === 48 && result[0] === 119);
