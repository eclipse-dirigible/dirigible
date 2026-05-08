
import { Escape } from '@aerokit/sdk/utils/escape';
import { Assert } from 'test/assert';

const input = 'javascript \\t characters \\n';
const result = Escape.unescapeJavascript(input);

Assert.assertEquals(result, 'javascript \t characters \n');
