/*
 * The single navigation group of the My Shell: every generated personal perspective declares
 * groupId 'my', defined once here (the same one-definition pattern as the application shell's
 * navigation groups).
 */
exports.getPerspectiveGroup = () => ({
	id: 'my',
	label: 'My pages',
	order: 10,
	// The service classifies a group by the presence of `items` - the aggregated personal
	// perspectives are pushed into it by groupId.
	items: []
});
