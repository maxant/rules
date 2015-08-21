///////////////////////////////////////////
// some crazy stateful script. normally returns 40000
// but when run by multiple threads, this will return random
// numbers
///////////////////////////////////////////

var someState;

function rule420() {

	someState = 0;
	
	for(var i = 0; i < 40000; i++){
		someState++;
	}
	return someState;
}

