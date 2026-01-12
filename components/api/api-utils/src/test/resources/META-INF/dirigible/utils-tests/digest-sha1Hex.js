
import { Digest } from '@aerokit/sdk/utils/digest';
import { Assert } from 'test/assert';

const input = [61, 62, 63];
const result = Digest.sha1Hex(input);

console.log(result);

Assert.assertEquals(result, '3b543c8b5ddc61fe39de1e5a3aece34082b12777');
