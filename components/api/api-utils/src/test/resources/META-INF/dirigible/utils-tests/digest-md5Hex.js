
import { Digest } from '@aerokit/sdk/utils/digest';
import { Assert } from 'test/assert';

const input = 'ABC';
const result = Digest.md5Hex(input);

console.log(result);

Assert.assertEquals(result, '902fbdd2b1df0c4f70b4a5d23525e932');
