/*
 * Generated by Eclipse Dirigible based on model and template.
 *
 * Do not modify the content as it may be re-generated again.
 */
exports.getTemplate = function() {
	return {
		"name": "Database View (Model)",
		"description": "Database View Template",
		"sources": [{
			"location": "/template-database-view/database.view.template", 
			"action": "generate",
			"rename": "{{fileName}}.view"
		}],
		"parameters": [{
			"name": "viewName",
			"label": "View Name"
		}],
		"order": 41
	};
};