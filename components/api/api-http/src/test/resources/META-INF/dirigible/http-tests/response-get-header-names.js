import { Response } from 'sdk/http/response';
import { Assert } from 'test/assert';

Assert.assertTrue(Response.getHeaderNames().includes("header1","header2"));
