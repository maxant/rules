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
// run like this: mocha rules-test.js

var test = require('unit.js');
var assert = test.assert;
var fail = assert.fail;
var equal = assert.equal;

var rh = require('./require_helper.js'); //necessary for coverage!
var rules = rh('rules.js');
var Rule = rules.Rule;
var Engine = rules.Engine;
var NoActionFoundException = rules.NoActionFoundException;
var DuplicateNameException = rules.DuplicateNameException;
var SubRule = rules.SubRule;
var ParseException = rules.ParseException;
var NoMatchingRuleFoundException = rules.NoMatchingRuleFoundException;

describe('rules', function(){
  describe('rule class', function(){
    it('bad args', function(){
        try{
            var r1 = new Rule(null, null, null);
            fail('expected an error');
        }catch(err){
            assert('name may not be null', err);
        }

        try{
            var r2 = new Rule('name', null);
            fail('expected an error');
        }catch(err){
            assert('expression may not be null', err);
        }

        try{
            var r3 = new Rule('name', 'expression', null, null, null);
            fail('expected an error');
        }catch(err){
            assert('namespace may not be null', err);
        }        
    });

    it('fully qualified name', function(){
        var r = new Rule('name', 'expression', null, null, 'namespace');
        assert('TODO', r.getFullyQualifiedName());
    });
  });

  describe('subrule class', function(){
    it('general', function(){
        var rule = new SubRule('name', 'expression', 'namespace', 'description');
        equal(rule.name, 'name');
        equal(rule.expression, 'expression');
        equal(rule.namespace, 'namespace');
        equal(rule.description, 'description');
        equal(rule.priority, -1);
        assert(!rule.outcome);
        
        assert(rule instanceof SubRule);
        assert(rule instanceof Rule);
        assert(rule instanceof Object);
    });

  });

  describe('engine class', function(){
    it('one rule, no execution', function(){
        var rules = [];
        rules[rules.length] = new Rule('name', 'expression', null, 1, 'namespace');
        var engine = new Engine(rules);
        
        equal(1, engine.rules.length);
        equal('name', engine.rules[0].name);
    });

    it('engine call with namespace', function(){
        var rules = [];
        rules[rules.length] = new Rule('name', 'true', 'outcome', 1, 'namespace');
        var engine = new Engine(rules);
        var bestOutcome = engine.getBestOutcome({}, 'namespace');
        equal('outcome', bestOutcome);
        bestOutcome = engine.getBestOutcome({}, null);
        equal('outcome', bestOutcome);
    });

    it('one rule that contains another, no execution', function(){
        var rules = [];
        rules[rules.length] = new Rule('name', 'input.age > 30 && #2', null, 1, 'namespace');
        rules[rules.length] = new SubRule('2', 'true', 'namespace');
        var engine = new Engine(rules);
        
        equal('name', engine.rules[0].name);
        equal('input.age > 30 && (true)', engine.rules[0].expression);
        equal(1, engine.rules.length);
    });

    it('two rules that contains two others, no execution', function(){
        var rules = [];
        rules[rules.length] = new Rule('a', '#1 && #2', null, 1, 'namespace');
        rules[rules.length] = new SubRule('1', 'input.age > 30', 'namespace');
        rules[rules.length] = new SubRule('2', 'input.age<90', 'namespace');
        rules[rules.length] = new Rule('b', '#2&&#1', null, 1, 'namespace');
        var engine = new Engine(rules);
        
        equal(2, engine.rules.length);
        equal('a', engine.rules[0].name);
        equal('(input.age > 30) && (input.age<90)', engine.rules[0].expression);
        equal('b', engine.rules[1].name);
        equal('(input.age<90)&&(input.age > 30)', engine.rules[1].expression);
    });

    it('getBestOutcome', function(){
        var rules = [];
        rules[rules.length] = new Rule('is adult', 'input.age > 18', 'ADULT', 1, 'namespace');
        rules[rules.length] = new Rule('is tall', 'input.height > 180', 'TALL', 2, 'namespace');
        rules[rules.length] = new Rule('is male', 'input.sex == "M"', 'MALE', 2, 'namespace');
        var engine = new Engine(rules);

        var bestOutcome = engine.getBestOutcome({age: 19, height: 182, sex: 'F'});
        
        //relevant outcomes are ADULT and TALL, but TALL has higher prio, so its expected
        equal('TALL', bestOutcome);
    });

    it('getBestOutcome, no matching rule', function(){
        var rules = [];
        rules[rules.length] = new Rule('is adult', 'input.age > 18', 'ADULT', 1, 'namespace');
        rules[rules.length] = new Rule('is tall', 'input.height > 180', 'TALL', 2, 'namespace');
        rules[rules.length] = new Rule('is male', 'input.sex == "M"', 'MALE', 2, 'namespace');
        var engine = new Engine(rules);

        try{
            engine.getBestOutcome({age: 10, height: 162, sex: 'F'});
            fail('exception expected');
        }catch(err){
            assert(err instanceof NoMatchingRuleFoundException);
        }
    });

    it('getMatchingRules', function(){
        var rules = [];
        rules[rules.length] = new Rule('is adult', 'input.age > 18', 'ADULT', 1, 'namespace');
        rules[rules.length] = new Rule('is tall', 'input.height > 180', 'TALL', 2, 'namespace');
        rules[rules.length] = new Rule('is male', 'input.sex == "M"', 'MALE', 2, 'namespace');
        var engine = new Engine(rules);

        var matchingRules = engine.getMatchingRules({age: 10, height: 162, sex: 'F'});
        equal(0, matchingRules.length);

        matchingRules = engine.getMatchingRules({age: 20, height: 162, sex: 'M'});
        equal(2, matchingRules.length);
        equal('MALE', matchingRules[0].outcome);
        equal('ADULT', matchingRules[1].outcome);
    });

    it('getMatchingRules, with namespace', function(){
        var rules = [];
        rules[rules.length] = new Rule('is adult', 'input.age > 18', 'ADULT', 1, 'v1.namespace');
        rules[rules.length] = new Rule('is tall', 'input.height > 180', 'TALL', 2, 'v1.namespace');
        rules[rules.length] = new Rule('is male', 'input.sex == "M"', 'MALE', 2, 'v2.namespace');
        var engine = new Engine(rules);

        var matchingRules = engine.getMatchingRules({age: 20, height: 182, sex: 'M'}, 'v1\\.namespace');
        equal(2, matchingRules.length);
        equal('TALL', matchingRules[0].outcome);
        equal('ADULT', matchingRules[1].outcome);
    });

    it('executeAllActions', function(){
        var rules = [];
        rules[rules.length] = new Rule('is adult', 'input.age > 18', 'ADULT', 1, 'namespace');
        rules[rules.length] = new Rule('is tall', 'input.height > 180', 'TALL', 2, 'namespace');
        rules[rules.length] = new Rule('is male', 'input.sex == "M"', 'MALE', 2, 'namespace');
        var engine = new Engine(rules);

        var results = [];
        engine.executeAllActions({age: 19, height: 182, sex: 'F'}, 
                [
                 {name:'MALE', execute: function(input){results[results.length] = 1;}}, 
                 {name:'ADULT', execute: function(input){results[results.length] = 2;}}, 
                 {name:'TALL', execute: function(input){results[results.length] = 3;}}
                ]);
        
        //relevant outcomes are ADULT and TALL, but TALL has higher prio, so its expected first
        //the related actions returns 3 and 2
        equal(2, results.length);
        equal(3, results[0]);
        equal(2, results[1]);
    });

    it('executeAllActions', function(){
        var rules = [];
        rules[rules.length] = new Rule('is adult', 'input.age > 18', 'ADULT', 1, 'namespace');
        rules[rules.length] = new Rule('is older adult', 'input.age > 26', 'ADULT', 1, 'namespace');
        var engine = new Engine(rules);

        var results = [];
        engine.executeAllActions({age: 29}, 
                [
                 {name:'ADULT', execute: function(input){results[results.length] = 2;}}
                ]);
        
        //even tho two rules result in the same outcome, calling executeAllActions will only execute each 
        //action once!
        equal(1, results.length);
        equal(2, results[0]);
    });

    it('executeBestAction, duplicate names', function(){
        var rules = [];
        rules[rules.length] = new Rule('is adult', 'input.age > 18', 'ADULT', 1, 'namespace');
        rules[rules.length] = new Rule('is tall', 'input.height > 180', 'TALL', 2, 'namespace');
        rules[rules.length] = new Rule('is male', 'input.sex == "M"', 'MALE', 2, 'namespace');
        var engine = new Engine(rules);

        try{
            engine.executeBestAction({age: 19, height: 182, sex: 'F'}, 
                    [
                     {name:'TALL'}, 
                     {name:'MALE'}, 
                     {name:'ADULT'}, 
                     {name:'TALL'} //this is the baddy!
                    ]);
            fail('exception expected');
        }catch(err){
            assert(err instanceof DuplicateNameException);
            equal('The name TALL was found in a different action.  Action names must be unique.', err.msg);
        }
    });
    
    it('executeBestAction, not enough actions names', function(){
        var rules = [];
        rules[rules.length] = new Rule('is adult', 'input.age > 18', 'ADULT', 1, 'namespace');
        rules[rules.length] = new Rule('is tall', 'input.height > 180', 'TALL', 2, 'namespace');
        rules[rules.length] = new Rule('is male', 'input.sex == "M"', 'MALE', 2, 'namespace');
        var engine = new Engine(rules);

        try{
            engine.executeBestAction({age: 19, height: 182, sex: 'F'}, 
                    [
                     {name:'MALE'}, 
                     {name:'ADULT'} 
                     //TALL is missing!
                    ]);
            fail('exception expected');
        }catch(err){
            assert(err instanceof NoActionFoundException);
            equal('No action has been associated with the outcome "TALL"', err.msg);
        }
    });
    
    it('executeBestAction', function(){
        var rules = [];
        rules[rules.length] = new Rule('is adult', 'input.age > 18', 'ADULT', 1, 'namespace');
        rules[rules.length] = new Rule('is tall', 'input.height > 180', 'TALL', 2, 'namespace');
        rules[rules.length] = new Rule('is male', 'input.sex == "M"', 'MALE', 2, 'namespace');
        var engine = new Engine(rules);

        var result = engine.executeBestAction({age: 19, height: 182, sex: 'F'}, 
                [
                 {name:'MALE', execute: function(input){return 1;}}, 
                 {name:'ADULT', execute: function(input){return 2;}}, 
                 {name:'TALL', execute: function(input){return 3;}}
                ]);
        
        //relevant outcomes are ADULT and TALL, but TALL has higher prio, so its expected
        //the related action returns 3
        equal(3, result);
    });

  });

  describe('engine class', function(){
    it('missing subrule', function(){
		var r1 = new Rule("R1", "#R2", "outcome2", 1, "ch.maxant.produkte", "Default Regel");
		var rules = [r1];
        try{
            var e = new Engine(rules);
        }catch(err){
            console.log(err);
            assert(err instanceof ParseException);
            equal('Error while attempting to add subrule to rule ch.maxant.produkte.R1.  Unable to replace #R2 with subrule ch.maxant.produkte.R2 because no subrule with that fully qualified name was found', err.msg);
        }
    });

    it('duplicate rule name', function(){
		var r1 = new Rule("R1", "true", "outcome2", 1, "ch.maxant.produkte", "Default Regel");
		var rules = [r1, r1];
        try{
            var e = new Engine(rules);
        }catch(err){
            console.log(err);
            assert(err instanceof DuplicateNameException);
            equal('The name ch.maxant.produkte.R1 was found in a different rule.', err.msg);
        }
    });

    it('test2', function(){
        //similar to test1, but reversed rule priority.
		var rule1 = new Rule("R1", "input.p1.name == 'ant' && input.p2.name == 'clare'", "outcome1", 0, "ch.maxant.produkte", "Spezi Regel für Familie Kutschera");
		var rule2 = new Rule("R2", "true", "outcome2", 1, "ch.maxant.produkte", "Default Regel");
		var rules = [rule1, rule2];
        
		var action1 = {name: "outcome1", execute: function(i) { return "100.0"; }};
		var action2 = {name: "outcome2", execute: function(i) { return "101.0"; }};
        var actions = [action1, action2];
		
		var e = new Engine(rules);
		
		var input = {p1: {name: "ant"}, p2: {name: "clare"}};
		
        var price = e.executeBestAction(input, actions);
        equal("101.0", price);
    });

    it('testNamespaceMatching', function(){
		var rule1 = new Rule("R1", "true", "outcome1", 0, "ch.maxant.produkte", "one");
		var rule2 = new Rule("R1", "true", "outcome2", 1, "ch.maxant.fahrplan", "two");
		var rules = [rule1, rule2];
		
		var action1 = {name: "outcome1", execute: function(i) {return "100.0";}};
		var action2 = {name: "outcome2", execute: function(i) {return "101.0";}};
		var actions = [action1, action2];
		
        var e = new Engine(rules);
        
        var price = e.executeBestAction({}, actions, "ch\\.maxant\\.prod.*");
        equal("100.0", price);
    });
    
    it('testNamespaceMatchingFailed', function(){
		var rule1 = new Rule("R1", "true", "outcome1", 0, "ch.maxant.produkte", "one");
		var rule2 = new Rule("R1", "true", "outcome2", 1, "ch.maxant.fahrplan", "two");
		var rules = [rule1, rule2];
		
		var action1 = {name: "outcome1", execute: function(i) {return "100.0";}};
		var action2 = {name: "outcome2", execute: function(i) {return "101.0";}};
		var actions = [action1, action2];
		
        var e = new Engine(rules);
		
		var price = null;
		try {
			price = e.executeBestAction({}, actions, "co\\.uk\\.maxant\\..*");
			fail("no matching rule was found");
		} catch (err){
            if(err instanceof NoMatchingRuleFoundException) {
                assert(null === price);
            }else{
                console.log(err);
                fail("not expected");
            }
		}
    });

    
    it('testGetList', function(){
        //tests getting a list of rules associated with actions.
		var rule1 = new Rule("A", "input.distance < 100", "productA", 1, "ch.maxant.produkte", "Rule for product A");
		var rule2 = new Rule("B", "input.distance > 100", "productB", 2, "ch.maxant.produkte", "Rule for product B");
		var rule3 = new Rule("C", "input.distance > 150", "productC", 3, "ch.maxant.produkte", "Rule for product C");
		var rule4 = new Rule("D", "input.distance > 100 && (input.travelClass == 1)", "productC", 4, "ch.maxant.produkte", "Rule for product C");
		var rules = [rule1, rule2, rule3, rule4];

        var e = new Engine(rules);
        
        var request = {travelClass: 2, distance: 50};
        var rs = e.getMatchingRules(request);
        equal(1, rs.length);
        equal(rule1, rs[0]);
        equal("productA", e.getBestOutcome(request));

        request = {travelClass: 2, distance: 102};
        rs = e.getMatchingRules(request);
        equal(1, rs.length);
        equal(rule2, rs[0]);
        equal("productB", e.getBestOutcome(request));
        
        request = {travelClass: 2, distance: 152};
        rs = e.getMatchingRules(request);
        equal(2, rs.length);
        equal(rule3, rs[0]);
        equal(rule2, rs[1]);
        equal("productC", e.getBestOutcome(request));

        request = {travelClass: 1, distance: 50};
        rs = e.getMatchingRules(request);
        equal(1, rs.length);
        equal(rule1, rs[0]);
        equal("productA", e.getBestOutcome(request));
        
        request = {travelClass: 1, distance: 102};
        rs = e.getMatchingRules(request);
        equal(2, rs.length);
        equal(rule4, rs[0]);
        equal(rule2, rs[1]);
        equal("productC", e.getBestOutcome(request));
        
        request = {travelClass: 1, distance: 152};
        rs = e.getMatchingRules(request);
        equal(3, rs.length);
        equal(rule4, rs[0]);
        equal(rule3, rs[1]);
        equal(rule2, rs[2]);
        equal("productC", e.getBestOutcome(request));
    });

    it('testBadExpression', function(){
		var rule = new Rule("1", "input someIllegalOperator 345", "SomeCommand", 0, "ch.maxant.produkte");
		var rules = [rule];

		try{
			new Engine(rules).getBestOutcome({});
			fail("no exception found");
		}catch(err){
            console.log(err);
			assert(err instanceof SyntaxError);
			assert('unexpected_token_identifier', err.type);
		}
    });

    it('testReferencedRuleIsARuleRatherThanASubrule', function(){
		var rule1 = new Rule("1", "true", "SomeCommand", 1, "ch.maxant.produkte");
		var rule2 = new Rule("2", "#1", "SomeCommand", 2, "ch.maxant.produkte");
		var rules = [rule1, rule2];

		try{
			var e = new Engine(rules, true);
			
			var rs = e.getMatchingRules(234);
			
			//both are returned, because a referencedrule is also a rule.  only subrules are not returned!
			
			equal(2, rs.length);
			equal(rule2.name, rs[0].name);
			equal(rule1, rs[1]);
			
		}catch(ex){
            console.log(ex);
			assert(ex instanceof ParseException);
			equal("Error while attempting to add subrule to rule ch.maxant.produkte.2.  Unable to replace #1 with subrule ch.maxant.produkte.1 because it is a rule, rather than a subrule", ex.msg);
		}
    });

    it('testSubrules', function(){
	    var sr1 = new SubRule("1", "true", "ch.maxant.test");
	    var sr2 = new SubRule("2", "false", "ch.maxant.test");
	    var rule = new Rule("3", "#1 && !#2", "Bingo", 1, "ch.maxant.test");
	    var rules = [sr1, sr2, rule];
	    
        var e = new Engine(rules);
        
        var matches = e.getMatchingRules(null);
        equal(1, matches.length);
        equal(rule.name, matches[0].name);
        
        var result = e.getBestOutcome(null);
        equal("Bingo", result);
    });

    it('testSubrules reference subrule', function(){
	    var sr1 = new SubRule("1", "!#2", "ch.maxant.test");
	    var sr2 = new SubRule("2", "false", "ch.maxant.test");
	    var rule = new Rule("3", "#1", "Bingo", 1, "ch.maxant.test");
	    var rules = [sr1, sr2, rule];
	    
        var e = new Engine(rules);
        
        var matches = e.getMatchingRules(null);
        equal(1, matches.length);
        equal(rule.name, matches[0].name);
        
        var result = e.getBestOutcome(null);
        equal("Bingo", result);
    });

    it('testSubrulesWithActions', function(){
	    var sr1 = new SubRule("1", "true", "ch.maxant.test");
	    var sr2 = new SubRule("2", "false", "ch.maxant.test");
	    var rule = new Rule("3", "#1 && !#2", "Bingo", 1, "ch.maxant.test");
	    var rules = [sr1, sr2, rule];

        var action1 = {name: "Bingo", execute: function(i) {return "100.0";}};
        var actions = [action1];

        var e = new Engine(rules);
        
        var price = e.executeBestAction({}, actions);
        equal("100.0", price);
    });

    it('testJavadocExample', function(){
		var r1 = new Rule("YouthTarif", "input.person.age < 26", "YT2011", 3, "ch.maxant.someapp.tarifs");
		var r2 = new Rule("SeniorTarif", "input.person.age > 59", "ST2011", 3, "ch.maxant.someapp.tarifs");
		var r3 = new Rule("DefaultTarif", "!#YouthTarif && !#SeniorTarif", "DT2011", 3, "ch.maxant.someapp.tarifs");
		var r4 = new Rule("LoyaltyTarif", "#DefaultTarif && input.account.ageInMonths > 24", "LT2011", 4, "ch.maxant.someapp.tarifs");
		var rules = [r1, r2, r3, r4];

        var engine = new Engine(rules);

        var request = {person: {name: "p", age: 24}, account: {ageInMonths: 5}};
        var tarif = engine.getBestOutcome(request);
        equal("YT2011", tarif);
        equal(1, engine.getMatchingRules(request).length);
        
        request.person.age = 24;
        request.account.ageInMonths = 35;
        tarif = engine.getBestOutcome(request);
        equal("YT2011", tarif);
        equal(1, engine.getMatchingRules(request).length);
        
        request.person.age = 35;
        request.account.ageInMonths = 5;
        tarif = engine.getBestOutcome(request);
        equal("DT2011", tarif);
        equal(1, engine.getMatchingRules(request).length);
        
        request.person.age = 35;
        request.account.ageInMonths = 35;
        tarif = engine.getBestOutcome(request);
        equal("LT2011", tarif);
        equal(2, engine.getMatchingRules(request).length); //since DT2011 and LT2011 both match
        
        request.person.age = 65;
        request.account.ageInMonths = 5;
        tarif = engine.getBestOutcome(request);
        equal("ST2011", tarif);
        equal(1, engine.getMatchingRules(request).length);
        
        request.person.age = 65;
        request.account.ageInMonths = 35;
        tarif = engine.getBestOutcome(request);
        equal("ST2011", tarif);
        equal(1, engine.getMatchingRules(request).length);
    });
    
    it('testExecuteBestActionDuplicateName', function(){
		var rule1 = new Rule("1", "true", "SomeCommand", 3, "ch.maxant.produkte");
		var rules = [rule1];

		var action1 = {name: "outcome1", execute: function(i) { return "100.0";}};
		var action2 = {name: "outcome1", execute: function(i) { return "101.0";}};
		var actions = [action1, action2];
		
		try{
			var engine = new Engine(rules);
			engine.executeBestAction({}, actions);
			fail("why no expection for duplicate action name?");
		}catch(ex){
            assert(ex instanceof DuplicateNameException);
			equal("The name outcome1 was found in a different action.  Action names must be unique.", ex.msg);
		}
    });

    it('testExecuteBestActionNoActionFound', function(){
		var rule1 = new Rule("1", "true", "SomeCommand", 3, "ch.maxant.produkte");
		var rules = [rule1];
		
		var action1 = {name: "outcome1", execute: function(i) { return "100.0";}};
		var actions = [action1];
		try{
			var engine = new Engine(rules);
			engine.executeBestAction({}, actions);
			fail("why no expection for duplicate action name?");
		}catch(ex){
            assert(ex instanceof NoActionFoundException);
			equal("No action has been associated with the outcome \"SomeCommand\"", ex.msg);
		}
    });

    it('testExecuteBestActionManyActionsFiring', function(){
		var r1 = new Rule("SendEmailToUser", "input.config.sendUserEmail == true", "SendEmailToUser", 1, "ch.maxant.someapp.config");
        var r2 = new Rule("SendEmailToModerator", "input.config.sendAdministratorEmail == true && input.user.numberOfPostings < 5", "SendEmailToModerator", 2, "ch.maxant.someapp.config");
		var rules = [r1, r2];

		var log = [];
        log.add = function(i){this[this.length] = i;};

		var action1 = {name: "SendEmailToUser", execute: function(i) { 
            log.add("Sending email to user!");
        }};
		var action2 = {name: "SendEmailToModerator", execute: function(i) { 
            log.add("Sending email to moderator!");
        }};
		var actions = [action1, action2];

        var engine = new Engine(rules);

        var forumSetup = {config: {sendUserEmail: true, sendAdministratorEmail: true}, user: {numberOfPostings: 2}};
        
        engine.executeAllActions(forumSetup, actions);
        equal(2, log.length);
        equal("Sending email to moderator!", log[0]);
        equal("Sending email to user!", log[1]);
    });

    it('testRuleWithIteration', function(){
		var expression = 
                "var result = _.find(input.students, function(student){" +
				"	 return student.age < 10;" +
				"});" +
				"result != undefined";

		var r1 = new Rule("containsStudentUnder10", expression , "leaveEarly", 1, "ch.maxant.rules", "If a class contains a student under 10 years of age, then the class may go home early");
		var r2 = new Rule("default", "true" , "leaveOnTime", 0, "ch.maxant.rules", "this is the default");
		var classroom = {students: [{age: 12}, {age: 10}, {age: 8}]};
		var e = new Engine([r1, r2]);
		equal("leaveEarly", e.getBestOutcome(classroom));

        //remove last element
		classroom.students.splice(classroom.students.length-1, 1);

		equal("leaveOnTime", e.getBestOutcome(classroom));
    });

  });
});
