import { workspace } from "@aerokit/sdk/platform";

export function generate(json) {
	const parameters = JSON.parse(json);
	
	let projectFile = {
		"guid": parameters.projectName,
		"actions": []
	};
	const newProjectFile = JSON.stringify(projectFile);

	let currenctWorkspace = workspace.getWorkspace(parameters.workspaceName);
	let currentProject = currenctWorkspace.getProject(parameters.projectName);
	let maybeProjectFile = currentProject.getFile("project.json");
	if (maybeProjectFile.exists()) {
		const projectFileContent = maybeProjectFile.getText();
		if (projectFileContent.trim() === "") {
			return newProjectFile;
		}
		let projectFile = JSON.parse(projectFileContent);
		if (!projectFile.actions) {
			projectFile.actions = [];
			projectFile.actions.push({
				"name": "Build TypeScript",
				"commands": [
					{
						"os": "unix",
						"command": "tsc"
					},
					{
						"os": "windows",
						"command": "cmd /c tsc"
					}
				],
				"registry": "true"
			});
			return JSON.stringify(projectFile);
		} else {
			return projectFileContent;
		}
	} else {
		return newProjectFile;
	}
}
