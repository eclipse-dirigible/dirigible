import { Cmis as cmis } from 'sdk/cms/cmis';
import { Assert } from 'test/assert';

const session = cmis.getSession();

const rootFolder = session.getRootFolder();

const result = rootFolder.getChildren();

Assert.assertTrue(result !== null && result !== undefined);
