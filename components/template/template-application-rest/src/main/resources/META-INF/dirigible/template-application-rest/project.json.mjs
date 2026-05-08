import { workspace } from '@aerokit/sdk/platform';

export function generate(json) {
	const parameters = JSON.parse(json);

	const newProjectFile = JSON.stringify({
		'guid': parameters.projectName
	});

	const currenctWorkspace = workspace.getWorkspace(parameters.workspaceName);
	const currentProject = currenctWorkspace.getProject(parameters.projectName);
	const maybeProjectFile = currentProject.getFile('project.json');

	if (maybeProjectFile.exists()) {
		const projectFileContent = maybeProjectFile.getText();
		if (projectFileContent.trim() !== '') {
			return projectFileContent;
		}
	}

	return newProjectFile;
}
