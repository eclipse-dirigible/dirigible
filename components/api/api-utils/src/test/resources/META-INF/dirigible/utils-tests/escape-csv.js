
import { Escape } from '@aerokit/sdk/utils/escape';
import { Assert } from 'test/assert';

const input = '1,2,3,4,5,6';
const result = Escape.escapeCsv(input);

Assert.assertEquals(result, '"1,2,3,4,5,6"');
