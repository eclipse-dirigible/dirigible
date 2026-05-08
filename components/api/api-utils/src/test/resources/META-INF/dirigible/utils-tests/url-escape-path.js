
import { URL } from '@aerokit/sdk/utils/url';
import { Assert } from 'test/assert';

const input = 'http://www.test.com?var1=abc123&var2=123 456&var3=стойност';
const result = URL.escapePath(input);
console.log(result);
Assert.assertEquals(result, 'http:%2F%2Fwww.test.com%3Fvar1=abc123&var2=123%20456&var3=%D1%81%D1%82%D0%BE%D0%B9%D0%BD%D0%BE%D1%81%D1%82');
