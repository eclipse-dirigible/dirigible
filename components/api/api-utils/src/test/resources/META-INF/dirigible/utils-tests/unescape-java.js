import { Escape } from '@aerokit/sdk/utils/escape';
import { Assert } from 'test/assert';

const input = 'java \\t characters \\n';
const result = Escape.unescapeJava(input);

Assert.assertEquals(result, 'java \t characters \n');
