import { Cmis as cmis } from 'sdk/cms/cmis';
import { Assert } from 'test/assert';

const session = cmis.getSession();

const rootFolder = session.getRootFolder();

const properties = {};
properties[cmis.OBJECT_TYPE_ID] = cmis.OBJECT_TYPE_FOLDER;
properties[cmis.NAME] = 'test1';
const result = rootFolder.createFolder(properties);

Assert.assertTrue(result !== null && result !== undefined);
