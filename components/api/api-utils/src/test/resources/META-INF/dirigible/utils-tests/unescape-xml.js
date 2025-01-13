
import { Escape } from 'sdk/utils/escape';
import { Assert } from 'test/assert';

const input = '&quot;&lt;&gt;';
const result = Escape.unescapeXml(input);

Assert.assertEquals(result, '"<>');
