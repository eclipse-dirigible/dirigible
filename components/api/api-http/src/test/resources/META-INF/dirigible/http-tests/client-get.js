import { HttpClient } from '@aerokit/sdk/http/client';
import { Assert } from 'test/assert';

const result = HttpClient.get('https://raw.githubusercontent.com/eclipse/dirigible/master/NOTICE.txt');

console.log(JSON.stringify(result));

Assert.assertTrue((result !== null) && (result !== undefined));
