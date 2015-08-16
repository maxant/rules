# maxant-rules

A Rule Engine. Can evaluate rules and execute actions or simply provide an ordered list of the best matching Rules.

See [http://blog.maxant.co.uk/pebble/2014/11/15/1416087180000.html](http://blog.maxant.co.uk/pebble/2014/11/15/1416087180000.html "http://blog.maxant.co.uk/pebble/2014/11/15/1416087180000.html")

## Installation
    npm install maxant-rules

## Usage


I have the requirement to use a rule engine. I want something light weight and fairly simple, yet powerful. While there are products out there which are super good, I don't want something with the big learning overhead. And, I fancied writing my own!

Here are my few basic requirements:

Use some kind of expression language to write the rules,
It should be possible to store rules in a database,
Rules need a priority, so that only the best can be fired,
It should also be possible to fire all matching rules,
Rules should evaluate against one input which can be an object like a tree, containing all the information which rules need to evaluate against
Predefined actions which are programmed in the system should be executed when certains rules fire.
So to help clarify those requirements, imagine the following examples:

1) In some forum system, the administrator needs to be able to configure when emails are sent.

Here, I would write some rules like "when the config flag called sendUserEmail is set to true, send an email to the user" and "when the config flag called sendAdministratorEmail is true and the user has posted less than 5 postings, send an email to the administrator".

2) A tarif system needs to be configurable, so that the best tarif can be offered to customers.

For that, I could write rules like these: "when the person is younger than 26 the Youth tarif is applicable", "when the person is older than 59, the Senior tarif is applicable", and "when the person is neither a youth, nor a senior, then they should be given the Default tarif, unless they have had an account for more than 24 months, in which case they should be offered the Loyalty tarif".

3) A train ticket can be considered to be a product. Depending upon the travel request, different products are suitable.

A rule here, could be something like: "if the travel distance is more than 100km and first class is desired, then product A is to be sold."

Finally, a more complex example, involving some iteration of the input, rather than just property evaluation:

4) Timetable software needs to deterimine when students can leave school.

A rule for that might read: "If a class contains any student under age 10, the entire class gets to leave early. Otherwise, they leave at the normal time."

So, with those requirements in mind, as well as wanting to use Node.js and Javascript (rather than the Java which is what the original engine runs on), I set to work porting the original code. It works like this:

1) An engine is configured with some rules.

2) A rule has these attributes:

- namespace: an engine may contain many rules, but only some may be relevant to a particular call and this namespace can be used for filtering
- name: a unique name within a namespace
- expression: a Javascript expression for the rule
- outcome: a string which the engine might use if this rules expression evaluates to true
- priority: an integer. The bigger the value, the higher the priority.
- description: a useful description to aid the management of rules.

3) The engine is given an input object and evaluates all rules (optionally within a namespace) and either:

- a) returns all rules which evaluate to true,
- b) returns the outcome (string) from the rule with the highest priority, out of all rules evaluating to true,
- c) execute an action (defined within the application) which is associated with the outcome of the rule with the highest priority, out of all rules evaluating to true.

4) "Action"s are objects which the application programmer can supply. An action must have a property called 'name'. When the engine is asked to execute an action based on the rules, the name of the action matching the "winning" rule's outcome property is executed.

5) A rule can be built up of "sub-rules". A subrule is only ever used as a building block on which to base more complex rules. When evaluating rules, the engine will never select a subrule to be the best (highest priority) "winning" rule, i.e. one evaluating to true. Subrules make it easier to build complex rules, as I shall show shortly.

So, time for some code!

First, install the library using `npm install maxant-rules`, passing it the optional `--save` argument to save the dependency to the `package.json` file. 

In order to use the library, you need to "require" the library, for example by adding the following lines to your script. 

    var rules = require('maxant-rules');
    var Rule = rules.Rule;
    var Engine = rules.Engine;
    var NoActionFoundException = rules.NoActionFoundException;
    var DuplicateNameException = rules.DuplicateNameException;
    var SubRule = rules.SubRule;
    var ParseException = rules.ParseException;
    var NoMatchingRuleFoundException = rules.NoMatchingRuleFoundException;

First, let's look at the code for the tarif system:

    var r1 = new Rule("YouthTarif", "input.person.age < 26", "YT2011", 3, "ch.maxant.someapp.tarifs");
    var r2 = new Rule("SeniorTarif", "input.person.age > 59", "ST2011", 3, "ch.maxant.someapp.tarifs");
    var r3 = new Rule("DefaultTarif", "!#YouthTarif && !#SeniorTarif", "DT2011", 3, "ch.maxant.someapp.tarifs");
    var r4 = new Rule("LoyaltyTarif", "#DefaultTarif && input.account.ageInMonths > 24", "LT2011", 4, "ch.maxant.someapp.tarifs");
    var rules = [r1, r2, r3, r4];

    var engine = new Engine(rules);

    var request = {person: {name: "p", age: 24}, account: {ageInMonths: 5}};
    var tarif = engine.getBestOutcome(request);

So, in the above code, I have added 4 rules to the engine. Then, I created a tarif request object, which is the input object. That object is passed into the engine, when I ask the engine to give me the best outcome. In this case, the best outcome is the string "YT2011", the name of the most suitable tarif for the customer I added to the tarif request.

How does it all work? When the engine is given the rules, it does some validation on them. Notice how the first two rules refer to an object called "input"? That is the object passed into the "getBestOutcome" method on the engine. The engine creates a local reference to the input object named `input` and then calls `eval` for each rules expression.  Anytime an expression evaluates to "true", the rule is put to the side as a candidate to be the winner. At the end, the candidates are sorted in order of priority, and the outcome property of the rule with the highest priority is returned by the engine.

Notice how the third and fourth rules contain the '#' character. That is not standard Javascript. The engine examines all rules when they are passed to it, and it replaces any token starting with a hash symbol, with the expression found in the rule named the same as the token. It wraps the expression in brackets. The logger outputs the full rule after reference rules have been resolved and replaced, just in case you want to check the rule.

In the above business case, we were only interested in the best tarif for the customer. Equally, we might have been interested in a list of possible tarifs, so that we could offer the customer a choice. In that case, we could have called the "getMatchingRules" method on the engine, which would have returned all rules, sorted by priority. The tarif names are (in this case) the "outcome" field of the rules.

In the above example, I wanted to receive any of the four outcomes, from the four rules. Sometimes however, you might want to build complex rules based on building blocks, but you might never want those building blocks to be a winning outcome. The train trip example from above can be used to show what I mean here:

    var rule1 = new SubRule("longdistance", "input.distance > 100", "ch.maxant.produkte");
    var rule2 = new SubRule("firstclass", "input.travelClass == "1", "ch.maxant.produkte");
    var rule3 = new Rule("productA", "#longdistance && #firstclass", "productA", 3, "ch.maxant.produkte");
    var rules = [rule1, rule2, rule3];

    var e = new Engine(rules);

    var request = {travelClass: 1, distance: 150};
    var rs = e.getMatchingRules(request); 

In the above code, I build rule3 from two subrules. But I never want the outcomes of those building blocks to be output from the engine. So I create them as SubRules. SubRules don't have an outcome property or priority. They are simply used to build up more complex rules. After the engine has used the sub-rules to replace all tokens beginning in a hash during initialisation, it discards the SubRules - they are not evaluated.

The travel request above contains the distance and travel class. 

Next, consider the business case of wanting to configure a forum system. The code below introduces actions. Actions are created by the application programmer and supplied to the engine. The engine takes the outcomes (as described in the first example), and searches for actions with the same names as those outcomes, and calls the "execute" method on those actions. This functionality is useful when a system must be capable of predefined things, but the choice of what to do needs to be highly configurable and independent of deployment.

    var r1 = new Rule("SendEmailToUser", "input.config.sendUserEmail == true", "SendEmailToUser", 1, "ch.maxant.someapp.config");
    var r2 = new Rule("SendEmailToModerator", "input.config.sendAdministratorEmail == true and input.user.numberOfPostings < 5", "SendEmailToModerator", 2, "ch.maxant.someapp.config");
    var rules = [r1, r2];
		
    var log = [];
    log.add = function(i){this[this.length] = i;};
		
    var action1 = {name: "SendEmailToUser", execute: function(i) {
        log.add("Sending email to user!");
    }};
    var action2 = {name: "SendEmailToModerator", execute: function(i) {
        log.add("Sending email to moderator!");
    }};

    var engine = new Engine(rules);

    var forumSetup = 
    {config: 
        {sendUserEmail: true, 
         sendAdministratorEmail: true}, 
     user: {numberOfPostings: 2}};
			
    engine.executeAllActions(forumSetup, [a1, a2]);

In the code above, the actions are passed to the engine when we call the "executeAllActions" method. In this case, both actions are executed, because the forumSetup object causes both rules to evaluate to true. Note that the actions are executed in the order of highest priority rule first. Each action is only ever executed once - it's name is noted after execution and it will not be executed again, until the engines "execute*Action*" method is called again. Also, if you only want the action associated with the best outcome to be executed, call the "executeBestAction" method instead of "executeAllActions".

Finally, let's consider the classroom example.

    var expression = 
        "var result = _.find(input.students, function(student){" +
        "    return student.age < 10;" +
        "});" +
        "result != undefined";

    var r1 = new Rule("containsStudentUnder10", expression , "leaveEarly", 1, "ch.maxant.rules", "If a class contains a student under 10 years of age, then the class may go home early");
		
    var r2 = new Rule("default", "true" , "leaveOnTime", 0, "ch.maxant.rules", "this is the default");
		
    var classroom = {students: [{age: 12}, {age: 10}, {age: 8}]};

    var e = new Engine([r1, r2]);
		
    var outcome = e.getBestOutcome(classroom);

The outcome above is "leaveEarly", because the classroom contains one student whose age is less than 10. See how I have written an expression which uses the 'find' method from the underscorejs library. The engine simply requires a rule to return true, if the rule is to be considered a candidate for firing.

There are more examples in the tests contained in the source code, which can be found here: [https://github.com/maxant/rules/tree/master/rules-js](https://github.com/maxant/rules/tree/master/rules-js "https://github.com/maxant/rules/tree/master/rules-js").

So, the requirements are fulfiled, except for "It should be possible to store rules in a database". While this library doesn't support reading and writing rules to / from a database, rules are String based. So it wouldn't be hard to create some database code which reads rules out of a database and populates Rule objects and passes them to the Engine. I haven't added this to the library, because normally these things as well as the management of rules is something quite project specific. And because my library will never be as cool or popular as something like Drools, I'm not sure it would be worth my while to add such functionality.