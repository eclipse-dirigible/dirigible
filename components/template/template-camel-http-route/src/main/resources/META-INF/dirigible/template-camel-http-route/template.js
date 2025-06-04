export function getTemplate() {
	return {
		name: "Http Route Project Starter",
		description: "Apache Camel Http Route Project Starter Template",
		sources: [{
			location: "/template-camel-http-route/http-route.camel.template",
			action: "generate",
			engine: "velocity",
			rename: "http-route.camel"
		}, {
			location: "/template-camel-http-route/handler.ts.template",
			action: "generate",
            engine: "velocity",
			rename: "handler.ts"
		}, {
			location: "/template-camel-http-route/tsconfig.json",
			action: "generate",
			rename: "tsconfig.json"
		}, {
            location: "/template-camel-http-route/project.json",
            action: "generate",
            rename: "project.json"
        }],
		parameters: [],
		"order": 38
	};
};
