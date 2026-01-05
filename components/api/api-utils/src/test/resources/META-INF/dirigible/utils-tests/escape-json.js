
import { Escape } from '@aerokit/sdk/utils/escape';
import { Assert } from 'test/assert';

const input = 'json \t characters \n';
const result = Escape.escapeJavascript(input);

Assert.assertEquals(result, 'json \\t characters \\n');
