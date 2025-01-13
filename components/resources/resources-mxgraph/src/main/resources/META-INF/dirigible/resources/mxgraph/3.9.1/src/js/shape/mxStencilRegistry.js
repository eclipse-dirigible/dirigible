
var mxStencilRegistry =
{
	/**
	 * Class: mxStencilRegistry
	 * 
	 * A singleton class that provides a registry for stencils and the methods
	 * for painting those stencils onto a canvas or into a DOM.
	 */
	stencils: {},
	
	/**
	 * Function: addStencil
	 * 
	 * Adds the given <mxStencil>.
	 */
	addStencil: function(name, stencil)
	{
		mxStencilRegistry.stencils[name] = stencil;
	},
	
	/**
	 * Function: getStencil
	 * 
	 * Returns the <mxStencil> for the given name.
	 */
	getStencil: function(name)
	{
		return mxStencilRegistry.stencils[name];
	}

};
