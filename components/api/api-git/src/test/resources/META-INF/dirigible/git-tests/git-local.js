import { Client } from '@aerokit/sdk/git/client';
import { Assert } from 'test/assert';
import { Workspace } from '@aerokit/sdk/platform/workspace';

const user = 'dirigible';
const email = 'dirigible@eclipse.com';
const workspaceName = 'test-workspace';
const projectName = 'test-project';
const repositoryName = projectName;

const workspace = Workspace.createWorkspace(workspaceName);
const project = workspace.createProject(projectName);
let firstFile = project.createFile("firstFile.js");
firstFile.setText('first file content');

Client.initRepository(user, email, workspaceName, projectName, repositoryName, 'initial commit');

let repos = Client.getGitRepositories(workspaceName);
console.log("Repository name " + repos[0].getName())
Assert.assertTrue(repos[0].getName() === repositoryName);

let secondFile = project.createFile("secondFile.js");
secondFile.setText('second file content');
console.log("Getting staged changes...");
let staged = Client.getStagedChanges(workspaceName, repositoryName);
console.log("Staged changes count: " + staged.size())
Assert.assertTrue(staged.size() === 0);

console.log("Getting unstaged changes...");
let unstaged = Client.getUnstagedChanges(workspaceName, repositoryName);
console.log("Unstaged changes count: " + unstaged.size())
Assert.assertTrue(unstaged.size() === 1);

Client.commit(user, email, workspaceName, repositoryName, "second file added", true);
const his = Client.getHistory(repositoryName, workspaceName, projectName);

console.log("History size: " + his.size())
Assert.assertTrue(his.size() === 2);

let branches = Client.getLocalBranches(workspaceName, repositoryName);
console.log("Local branches size: " + branches.size());
Assert.assertTrue(branches.size() === 1);

Client.createBranch(workspaceName, repositoryName, 'new-branch', 'master');
branches = Client.getLocalBranches(workspaceName, repositoryName);
console.log("New local branches size: " + branches.size());
Assert.assertTrue(branches.size() === 2);

const status = Client.status(workspaceName, repositoryName);
console.log("Status is clean: " + status.isClean());
Assert.assertTrue(status.isClean() === true);

console.log("Deleting test repo...");
Client.deleteRepository(workspaceName, repositoryName);
console.log("Repositories size at the end: " + Client.getGitRepositories(workspaceName).size());
Assert.assertTrue(Client.getGitRepositories(workspaceName).size() === 0);
