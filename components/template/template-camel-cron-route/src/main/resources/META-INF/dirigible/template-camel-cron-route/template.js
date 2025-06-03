export function getTemplate() {
	return {
		name: "Camel Cron Route Project Starter",
		description: "Camel Cron Route Project Starter Template",
		sources: [{
			location: "/template-camel-cron-route/cron-route.camel.template",
			action: "generate",
			engine: "velocity",
			rename: "cron-route.camel"
		}, {
			location: "/template-camel-cron-route/handler.ts.template",
			action: "generate",
            engine: "velocity",
			rename: "handler.ts"
		}, {
			location: "/template-camel-cron-route/tsconfig.json",
			action: "generate",
			rename: "tsconfig.json"
		}, {
            location: "/template-camel-cron-route/project.json",
            action: "generate",
            rename: "project.json"
        }],
		parameters: [],
		"order": 37
	};
};
