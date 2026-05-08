
import { Hex } from '@aerokit/sdk/utils/hex';
import { Assert } from 'test/assert';

const input = [65, 66, 67];
const result = Hex.encode(input);

Assert.assertEquals(result, '414243');
