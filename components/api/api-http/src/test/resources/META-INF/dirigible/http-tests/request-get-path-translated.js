import { Request } from 'sdk/http/request';
import { Files } from 'sdk/io/files';
import { Assert } from 'test/assert';

const separator = Files.separator

const expectedPath = `${separator}services${separator}js${separator}http-tests${separator}request-get-path-translated.js`;

Assert.assertTrue(Request.getPathTranslated().endsWith(expectedPath));
