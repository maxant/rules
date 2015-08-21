/* 
 * Copyright (c) 2011-2015 Ant Kutschera
 * 
 * This file is part of Ant Kutschera's blog.
 * 
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 * You should have received a copy of the Lesser GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
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
