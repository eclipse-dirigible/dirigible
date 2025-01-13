import { Request } from 'sdk/http/request';
import { Assert } from 'test/assert';

Assert.assertEquals(JSON.stringify(Request.getHeaderNames()), '["Authorization","header1","header2"]');
