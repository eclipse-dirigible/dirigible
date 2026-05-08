import { Workspace } from "@aerokit/sdk/platform/workspace";
import { Lifecycle } from "@aerokit/sdk/platform/lifecycle";
import { Bytes } from "@aerokit/sdk/io/bytes";
import { Assert } from 'test/assert';

const user = "dirigible";
const workspaceName = "workspace";
const projectName = "project";

const myWorkspace = Workspace.createWorkspace(workspaceName);
const myProject = myWorkspace.createProject("project");
const myFile = myProject.createFile(projectName);
myFile.setContent(Bytes.textToByteArray("console.log('Hello World!');"));

const publishResult = Lifecycle.publish(user, workspaceName, projectName);
const unpublishResult = Lifecycle.unpublish(user, workspaceName, projectName);

Assert.assertTrue(publishResult);
Assert.assertTrue(unpublishResult);
