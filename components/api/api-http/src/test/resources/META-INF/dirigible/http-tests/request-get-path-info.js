import { Request } from '@aerokit/sdk/http/request';
import { Assert } from 'test/assert';

Assert.assertEquals(Request.getPathInfo(), '/services/js/http-tests/request-get-path-info.js');
