
import { URL } from '@aerokit/sdk/utils/url';
import { Assert } from 'test/assert';

const input = 'http://www.test.com?var1=abc123&var2=123 456&var3=стойност';
const result = URL.escapeForm(input);
console.log(result);
Assert.assertEquals(result, 'http%3A%2F%2Fwww.test.com%3Fvar1%3Dabc123%26var2%3D123+456%26var3%3D%D1%81%D1%82%D0%BE%D0%B9%D0%BD%D0%BE%D1%81%D1%82');
