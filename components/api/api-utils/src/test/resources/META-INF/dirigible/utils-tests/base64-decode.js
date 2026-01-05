
import { Base64 } from '@aerokit/sdk/utils/base64';
import { Assert } from 'test/assert';

const input = 'PT4/';
const result = Base64.decode(input);

console.log('decoded: ' + result);

Assert.assertTrue(result[0] === 61 && result[1] === 62 && result[2] === 63);
