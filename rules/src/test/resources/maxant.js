;
(function() {
	var _global = this;

	///////////////////////////////////////////
	// some javascript function that want to be able
	// to call from an engine rule
	///////////////////////////////////////////
	function rule419(input) {
		return _(input)
		.filter(function(e){ 
			return e.name === "John"; 
		})
		.value().length > 0 ? "Scam" : "OK";
	};

	///////////////////////////////////////////
	//create and assemble object for exporting
	///////////////////////////////////////////
	var maxant = {};
	maxant.rule419 = rule419;

	///////////////////////////////////////////
	//export module depending upon environment, pretty much standard...
	///////////////////////////////////////////
	if (typeof (module) != 'undefined' && module.exports) {
		// Publish as node.js module
		module.exports = maxant;
	} else if (typeof define === 'function' && define.amd) {
		// Publish as AMD module
		define(function() {
			return maxant;
		});
	} else {
		// Publish as global (in browsers and rhino/nashorn)
		var _previousRoot = _global.maxant;

		// **`noConflict()` - (browser only) to reset global 'maxant' var**
		maxant.noConflict = function() {
			_global.maxant = _previousRoot;
			return maxant;
		};

		_global.maxant = maxant;
	}
}).call(this);
