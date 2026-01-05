
import { Escape } from '@aerokit/sdk/utils/escape';
import { Assert } from 'test/assert';

const input = '"<>';
const result = Escape.escapeXml(input);

Assert.assertEquals(result, '&quot;&lt;&gt;');
