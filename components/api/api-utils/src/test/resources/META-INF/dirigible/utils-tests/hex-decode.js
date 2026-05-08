
import { Hex } from '@aerokit/sdk/utils/hex';
import { Assert } from 'test/assert';

const input = '414243';
const result = Hex.decode(input);

console.log('decoded: ' + result);

Assert.assertTrue(result[0] === 65 && result[1] === 66 && result[2] === 67)
