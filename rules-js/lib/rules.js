/*
* Copyright (c) 2011-2014 Ant Kutschera
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
require('es6-collections');
var _ = require('underscore');

var endOfTokenChars = new Set();
{
    endOfTokenChars.add(' ');
    endOfTokenChars.add('&');
    endOfTokenChars.add('|');
    endOfTokenChars.add('.');
    endOfTokenChars.add('(');
    endOfTokenChars.add(')');
    endOfTokenChars.add('[');
    endOfTokenChars.add(']');
    endOfTokenChars.add('{');
    endOfTokenChars.add('}');
    endOfTokenChars.add('+');
    endOfTokenChars.add('-');
    endOfTokenChars.add('/');
    endOfTokenChars.add('*');
    endOfTokenChars.add('=');
    endOfTokenChars.add('!');
}


// ////////////////// RULE ////////////////////////////////

/**
 * An {@link Engine} contains a list of rules.  The engine 
 * can then be asked to provide the outcome of the Action associated with the best 
 * matching rule (see {@link Engine#executeBestAction}), 
 * or to provide a list of matching rules (see {@link Engine#getMatchingRules}).  
 * Each rule is "evaluated"
 * by using the {@link #expression} supplied in the constructor 
 * {@link Rule}.
 * The expression must be valid expression language.  During evaludation, 
 * the input object passed to the engine is mapped to the name "input", which can be
 * used in the rules.  For example, consider the following rule:<br>
 * <br>
 * &nbsp;&nbsp;&nbsp;<code>input.person1.name == "ant" &amp;&amp; input.person1.gender == "male"</code><br>
 * <br>
 * Note how the rule evaluates to "true" or "false".  For any rule to be a candidate
 * to have an associated {@link IAction} executed, it must evaluate to "true".  The rule in the 
 * example above requires an input object which conforms to the bean specification, and 
 * which is composed of an object named "person1", which has attributes"name"
 * and "gender".  If an input object is supplied to the engine such that this rule
 * evaluates to true, then it is a candidate to have its action run, either by 
 * the engine, when {@link Engine#executeBestAction} is called, 
 * or by the application, when {@link Engine#getMatchingRules}
 * returns the rule and the application decides to run the action.<br>
 * <br>
 * Rules belong to namespaces, so that a single engine can be used to evaluate rules 
 * from different components within an application.  within a namespace, all rules must
 * have unique names (this is checked when adding rules to an engine).  The reason is that
 * rules can be composed of {@link SubRule}s and when they are composed in that manner, 
 * rules refer to subrules by their names.<br>
 * <br>
 * A rule has a priority too.  In certain cases, where the application only requires the 
 * best rule to be used, the priority helps the application decide which rule to run 
 * when there is more than one match.<br>
 * <br>
 * The description in a rule is imply to aid in rule management.<br>
 * <br>
 * Typically rules are customisable at runtime using some kind of administration UI.
 * While this framework does not provide an out of the box framework, it has been 
 * designed such that the rules could be created from a persistent store.
 * It is entirely conceivable that an application would load rules from a database
 * and execute existing actions based on the rules.  Only when a new action is 
 * required, would an application need to be upgraded and redeployed.<br>
 * 
 * @constructor
 * @author Ant
 * @property name {String} The name of the rule.  Should be unique within the namespace (tested when adding rules to the {@link Engine}).
 * @property expression {String} A Javascript rule. All variables must come from the object called "input".  The rule MUST evaluate to "true" if it is to be a candidate for execution.
 * @property outcome {Object} Normally a string, this is the name of an action to run, if this rule is the winner or this is the result of calling the method {@link Engine#getBestOutcome}.
 * @property priority {int} The priority of a rule is used when sorting the matching rules.  A higher number indicates a higher priority and that causes the rule to be placed nearer the start in the Array of matching rules.  If the best outcome is being determined, or the best action, then it is the outcome/action at index 0 of the matching rules which is used.
 * @property namespace {String} The namespace of this rule, for example a version number or component name. 
 *  When the engine is used, the caller supplies an optional namespace pattern (regular expression) which is used 
 *  to filter the rules which are used in determining outcomes.
 * @property description {String} An optional description of the rule, helpful for managing rules.
 */
function Rule(name, expression, outcome, priority, namespace, description){

    if(name === null) throw new Error('name may not be null');
    if(expression === null) throw new Error('expression may not be null');
    if(namespace === null) throw new Error('namespace may not be null');

    this.name = name;
    this.expression = expression;
    this.outcome = outcome;
    this.priority = priority;
    this.namespace = namespace;
    this.description = description;

    /**
   	 * @return the {@link namespace} concatenated with the {@link name}, separated by a '.'
     */
    this.getFullyQualifiedName = function(){
        return namespace + '.' + name;
    };
}
exports.Rule = Rule;

// ////////////////// SUB RULE ////////////////////////////////

/**
 * A sub rule can be used to make rules which are built up from partial expressions and expressions 
 * contained in (sub)rules.  A sub rule will
 * <b>never</b> have an associated action executed, rather it will only ever be evaluated, as part of the evaluation
 * of a rule which references it.  Rules may reference sub rules within the same namespace, using the '#'
 * character.  E.g.:<br>
 * <br>
 * SubRule 1: input.age &gt; 30<br>
 * SubRule 2: input.height &lt; 100<br>
 * Rule 3: #1 &amp;&amp; !#2 <br>
 * @constructor
 * @author Ant
 * @property name {String} The name of the rule.  Should be unique within the namespace (tested when adding rules to the {@link Engine}).
 * @property expression {String} A Javascript rule. All variables must come from the object called "input".  The rule MUST evaluate to "true" if it is to be a candidate for execution.
 * @property namespace {String} The namespace of this rule, for example a version number or component name. 
 *  When the engine is used, the caller supplies an optional namespace pattern (regular expression) which is used 
 *  to filter the rules which are used in determining outcomes.
 * @property description {String} An optional description of the rule, helpful for managing rules.
 */
function SubRule(name, expression, namespace, description){
    this.superclass(name, expression, null, -1, namespace, description);
}
SubRule.prototype = new Rule('', '', '', -1, '');
SubRule.prototype.constructor = SubRule;
SubRule.prototype.superclass = Rule;
exports.SubRule = SubRule;

// ////////////////// EXCEPTIONS ////////////////////////////////

/**
 * thrown when rules are added to the engine, if another rule
 * has the same fully qualified name.
 * @constructor
 * @author Ant
 * @see Rule#getFullyQualifiedName()
 */
function DuplicateNameException(msg){
    this.msg = msg;
}
exports.DuplicateNameException = DuplicateNameException;

/**
 * Every rule has an outcome.  If the engine is given actions
 * then every possible outcome must have an action.  If the {@link Engine}
 * isn't given a suitable action to an outcome, this exception is thrown.
 * @constructor
 * @author Ant
 */
function NoActionFoundException(msg){
    this.msg = msg;
}
exports.NoActionFoundException = NoActionFoundException;

/**
 * When replacing {@link SubRule} placeholders (the '#' character) in rules, this exception may
 * be thrown if no suitable subrule can be found.  
 * 
 * @see Engine#Engine
 * @constructor
 * @author Ant
 */
function ParseException(msg){
    this.msg = msg;
}
exports.ParseException = ParseException;

/**
 * thrown when no matching rule is found.
 * 
 * @see Engine#executeBestAction
 * @constructor
 * @author Ant
 */
function NoMatchingRuleFoundException(){}
exports.NoMatchingRuleFoundException = NoMatchingRuleFoundException;

// ////////////////// ENGINE ////////////////////////////////

/**
 * A Rule Engine.  Can evaluate rules and execute actions or simply provide an 
 * ordered list of the best matching {@link Rule}s.<br>
 * <br>
 * A fuller explanation of how to use rules can be found in the documentation for {@link Rule}s.<br>
 * <br>
 * Imagine a tarif system which your business needed to be highly configurable.
 * The business wants to be able to specify new tarifs or update existing ones
 * at any time.  The criteria used to determine which tarifs are relevant
 * to a customer wanting to make a purchase are based on the attributes belonging 
 * firstly to the person, and secondly to their account.  For example, a customer
 * who is under 26 is eligible for a youth tarif.  Seniors are eligible for their own
 * tarif too.  Additionally, customers who do not get a youth or senior tarif
 * are entitled to a loyalty tarif if they have had an account for more than 24 months.<br>
 * <br>
 * These business rules can be modelled in a database as a set of strings.  The data 
 * can then be loaded and modelled in the code using the {@link Rule} API. 
 * Consider the following four rules:<br>
 * <br>
 * <code>
 * var r1 = new Rule("YouthTarif",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"input.age &lt; 26", "YT2011", 3,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"ch.maxant.someapp.tarifs");<br>
 * var r2 = new Rule("SeniorTarif",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"input.age &gt; 59", "ST2011", 3,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"ch.maxant.someapp.tarifs");<br>
 * var r3 = new Rule("DefaultTarif",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"!#YouthTarif &amp;&amp; !#SeniorTarif", "DT2011", 3,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"ch.maxant.someapp.tarifs");<br>
 * var r4 = new Rule("LoyaltyTarif",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"#DefaultTarif &amp;&amp; input.account.ageInMonths &gt; 24",<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"LT2011", 4,<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"ch.maxant.someapp.tarifs");<br>
 * </code>
 * <br>
 * The following object model can then be created and used to evaluate the rules.<br>
 * <br>
 * <code>
 * var request = {person: {name: "p", age: 35}, account: {ageInMonths: 25}};
 * </code>
 * <br>
 * <br>
 * The following code can then be used to determine the best tarif, or list of all matching tarifs:<br>
 * <br>
 * <code>
 * //will return both DT2011 and LT2011, since both are relevant.<br>
 * //Since LT2011 has a higher priority, it is first in the list.<br>
 * var matches = engine.getMatchingRules(request);<br>
 * <br>
 * // returns "LT2011", since the loyalty tarif has a higher priority<br>
 * var tarif = engine.getBestOutcome(request); <br>
 * </code>
 * <br>
 * See {@link Rule} for more details.  See the tests for more examples!
 *
 * @constructor
 * @author Ant
 * @property rules {Array} The rules which define the system.
 * @throws DuplicateNameException thrown if any rules have the same name within a namespace
 * @throws ParseException Thrown if a subrule which is referenced in a rule cannot be resolved.
 */
function Engine(rules){
    this.uniqueOutcomes = new Set();
    this.rules = [];
    
    { //construct
        console.log('\r\n\r\n*****Initialising rule engine...*****');
        var self = this;
        var start = new Date();
        
        var names = collectNamesAndUniqueOutcomes(rules, this);

        var parsedRules = replaceAllReferences(rules, names);

        self.rules = _.filter(parsedRules, function(rule){ return !(rule instanceof SubRule);});
        
        console.log('*****Engine initialisation completed in ' + (new Date()-start) + ' ms*****\r\n');
    }

    /* returns a <code>Set</code> of fully qualified names, and adds each outcome to <code>this.uniqueOutcomes</code>. */
    function collectNamesAndUniqueOutcomes(rules, self){
        var names = new Map();
        _.each(rules, function(rule){
            var fullyQualifiedName = rule.getFullyQualifiedName();
            if(names.has(fullyQualifiedName)){
                throw new DuplicateNameException('The name ' + fullyQualifiedName + ' was found in a different rule.');
            }
            names.set(fullyQualifiedName, rule);
            self.uniqueOutcomes.add(rule.outcome);
        });
        return names;
    }
    
    function replaceAllReferences(rules, names){
        var parsedRules = [];

        //now replace all rule references with the actual rule, contained within brackets
        while(true){
            var foundRuleReference = false;
            _.each(rules, function(rule){
                var idx1 = rule.expression.indexOf('#');
                if(idx1 > -1){
                    foundRuleReference = true;
                    //search to end of expression for next symbol
                    var idx2 = idx1 + 1; //to skip #                        
                    while(true){
                        idx2++;
                        if(idx2 >= rule.expression.length){
                            break;
                        }
                        var c = rule.expression.charAt(idx2);
                        if(endOfTokenChars.has(c)){
                            break;
                        }
                    }
                    
                    var token = rule.expression.substring(idx1+1, idx2);
                    var fullyQualifiedRuleRef = rule.namespace + "." + token;
                    var ruleToAdd = names.get(fullyQualifiedRuleRef);
                    if(!ruleToAdd){
                        throw new ParseException('Error while attempting to add subrule to rule ' + rule.getFullyQualifiedName() + '.  Unable to replace #' + token + ' with subrule ' + fullyQualifiedRuleRef + ' because no subrule with that fully qualified name was found');
                    }
                    var newExpression = '';
                    if(idx1 > 0){
                        newExpression += rule.expression.substring(0, idx1);
                    }
                    newExpression += '(' + ruleToAdd.expression + ')';
                    if(idx2 < rule.expression.length){
                        newExpression += rule.expression.substring(idx2, rule.expression.length);
                    }
                    if(rule instanceof SubRule){
                        parsedRules[parsedRules.length] = new SubRule(rule.name, newExpression, rule.namespace, rule.description);
                    }else{
                        parsedRules[parsedRules.length] = new Rule(rule.name, newExpression, rule.outcome, rule.priority, rule.namespace, rule.description);
                    }
                }else{
                    parsedRules[parsedRules.length] = rule;
                }
            }); // jshint ignore:line
            if(!foundRuleReference){
                //yay, all done!
                break;
            }else{
                //go thru again, because there are still rules which need substituting
                rules = parsedRules;
                parsedRules = [];
            }
        }

        return parsedRules;
    }        
}
exports.Engine = Engine;

/**
 * @param nameSpacePattern {String} optional.  if not null, then only rules with matching namespaces are evaluated.
 * @param input {Object} the Object containing all inputs to the expression language rule.
 * @return {Array} an Array of Rules which evaluated to "true", sorted by <code>Rule.priority</code>, with the highest priority rules first in the list.
 */
Engine.prototype.getMatchingRules = function(input, nameSpacePattern) {
    
    var pattern = null;
    if(nameSpacePattern !== null){
        pattern = new RegExp(nameSpacePattern);
    }
    
    var matchingRules = [];
    _.each(this.rules, function(rule){
        
        var idx = pattern === null ? 1 : rule.namespace.search(nameSpacePattern);
        if(idx >= 0){

            //yup its bad, but its the basis of the engine!
            var o = eval(rule.expression); // jshint ignore:line 

            var msg = rule.getFullyQualifiedName() + "-{" + rule.expression + "}";
            if(true === o){
                matchingRules[matchingRules.length] = rule;
                console.log("matched: " + msg);
            }else{
                console.log("unmatched: " + msg);
            }
        }
    });
    
    //order by priority - reversed, since we want highest priority first in the list!
    matchingRules = _.sortBy(matchingRules, function(rule){ return -rule.priority; });
    
    return matchingRules;
};

/**
 * Evaluates all rules against the input and returns the result of the outcome associated with the rule having the highest priority.
 * @param nameSpacePattern {String} optional.  if not null, then only rules with matching namespaces are evaluated.
 * @param input {Object} the Object containing all inputs to the expression language rule.
 * @return {Object} The outcome belonging to the best rule which is found.
 * @throws NoMatchingRuleFoundException If no matching rule was found.  Rules must evaluate to true in order to be candidates.
 */
Engine.prototype.getBestOutcome = function(input, nameSpacePattern){
    var matches = this.getMatchingRules(input, nameSpacePattern);
    if(matches === null || matches.length === 0){
        throw new NoMatchingRuleFoundException();
    }else{
        return matches[0].outcome;
    }
};

/**
 * Evaluates all rules against the input and returns the result of the action associated with the rule having the 
 * highest priority.
 * @param nameSpacePattern {String} optional.  if not null, then only rules with matching namespaces are evaluated.
 * @param input {Object} the Object containing all inputs to the Javascript rule.
 * @param actions {Array} an Array of Objects containing one per possible outcome.  These objects must have a property 
 *         called `name`. The object whose name is equal to the winning outcome will be executed by having the 
 *         <code>execute</code> function of that object called. When calling the function, it is passed the 
 *         input object.
 * @return {Object} The result of the action (object) with the same name as the winning rules outcome.
 * @throws NoMatchingRuleFoundException If no matching rule was found.  Rules must evaluate to true in order to be candidates.
 * @throws NoActionFoundException If no action with a name matching the winning rules outcome was found.
 * @throws DuplicateNameException if any actions have the same name.
 */
Engine.prototype.executeBestAction = function(input, actions, nameSpacePattern) {
    
    var actionsMap = validateActions(actions, this.uniqueOutcomes);
    var bestOutcome = this.getBestOutcome(input, nameSpacePattern);
    return actionsMap.get(bestOutcome).execute(input);
};

/**
 * Evaluates all rules against the input and then executes all action associated with the positive rules outcomes, in order of highest priority first.<br>
 * <br>
 * Any outcome is only ever executed once!<br>
 * <br>
 * <b>NOTE THAT THIS METHOD DISREGARDS ANY RETURN VALUES OF ACTIONS!!</b>
 * @param {String} nameSpacePattern optional.  if not null, then only rules with matching namespaces are evaluated.
 * @param {Object} input the Object containing all inputs to the expression language rule.
 * @param {Array} actions an Array of Objects containing one per possible outcome.  These objects must have a property called `name`.
 *          The objects whose names are equal to the positive outcomes will be executed.
 *          by having the `execute` function of that object called.
 * @throws NoMatchingRuleFoundException If no matching rule was found.  Rules must evaluate to true in order to be candidates.
 * @throws NoActionFoundException If no action with a name matching the winning rules outcome was found.
 * @throws DuplicateNameException if any actions have the same name.
 */
Engine.prototype.executeAllActions = function(input, actions, nameSpacePattern) {
    var actionsMap = validateActions(actions, this.uniqueOutcomes);
    var matchingRules = this.getMatchingRules(input, nameSpacePattern);
    var executedOutcomes = new Set();
    _.each(matchingRules, function(r){
        //only run, if not already run!
        if(!executedOutcomes.has(r.outcome)){
            actionsMap.get(r.outcome).execute(input);
            executedOutcomes.add(r.outcome);
        }
    });
};

//private, inner
function validateActions(actions, uniqueOutcomes) {
    //do any actions have duplicate names?
    var actionsMap = new Map();
    _.each(actions, function(a){
        if(actionsMap.has(a.name)){
            throw new DuplicateNameException('The name ' + a.name + ' was found in a different action.  Action names must be unique.');
        }else{
            actionsMap.set(a.name, a);
        }
    });
    
    //do we have at least one action for every possible outcome?  
    //better to test now, rather than waiting for such a branch to 
    //randomly be executed one day in production
    //n.b. subrules have outcome == null, so skip them
    _.each(uniqueOutcomes.values(), function(outcome){
        if(outcome !== null && !actionsMap.has(outcome)){
            throw new NoActionFoundException('No action has been associated with the outcome "' + outcome + '"');
        }
    });
    
    return actionsMap;
}
