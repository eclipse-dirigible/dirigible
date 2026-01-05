
import { Base64 } from '@aerokit/sdk/utils/base64';
import { Assert } from 'test/assert';

const input = [61, 62, 63];
const result = Base64.encode(input);

Assert.assertEquals(result, 'PT4/');
