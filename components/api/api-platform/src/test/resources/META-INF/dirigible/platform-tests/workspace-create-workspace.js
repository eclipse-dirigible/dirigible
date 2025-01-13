import { Workspace } from 'sdk/platform/workspace';
import { Assert } from 'test/assert';

Workspace.createWorkspace('testworkspace');
const workspace = Workspace.getWorkspace('testworkspace');

Assert.assertTrue(workspace.exists());
