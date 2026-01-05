
import { Escape } from '@aerokit/sdk/utils/escape';
import { Assert } from 'test/assert';

const input = '"<>';
const result = Escape.escapeHtml4(input);

Assert.assertEquals(result, '&quot;&lt;&gt;');
