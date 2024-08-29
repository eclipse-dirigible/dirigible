
import { Escape } from 'sdk/utils/escape';
import { Assert } from 'test/assert';

const input = 'java \t characters \n';
const result = Escape.escapeJava(input);

Assert.assertEquals(result, 'java \\t characters \\n');
