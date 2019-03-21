package ch.maxant.rules.blackbox;

import ch.maxant.rules.Engine;
import ch.maxant.rules.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class Issue2Test {

    public static class Model {
        private LocalDate registrationDate;
        private LocalDate today = LocalDate.now();
        private int continuousLoginDays;
        private Map<String, Integer> browseHistory = new HashMap<>();

        //used by program to setup model
        public Model(LocalDate registrationDate, LocalDate today, int continuousLoginDays) {
            this.registrationDate = registrationDate;
            this.today = today;
            this.continuousLoginDays = continuousLoginDays;
        }

        //used by rule
        public int getDaysSinceRegistration() {
            return Period.between(registrationDate, today).getDays();
        }

        //used by rule
        public int getContinuousLoginDays() {
            return continuousLoginDays;
        }

        //used by rule
        public int timesBrowsed(String productCategory) {
            return browseHistory.get(productCategory.toLowerCase());
        }

        //used by program to setup model
        public void addBrowseHistory(String productCategory, int numberOfTimes) {
            browseHistory.put(productCategory.toLowerCase(), numberOfTimes);
        }
    }

	@Test
	public void test() throws Exception {

        //You can store these objects in a database - you just need to write some code to get the strings from the database and construct Java objects from them
		Rule a = new Rule("a", "input.daysSinceRegistration == 3 and input.continuousLoginDays >= 10 and input.timesBrowsed('Electronic products') > 10", "outcome1", 0, "issue2");
		Rule b = new Rule("b", "input.timesBrowsed('Electronic products') == 10 and input.timesBrowsed('Womens clothing') > 5", "outcome2", 0, "issue2");

		Engine engine = new Engine(asList(a, b), true);
		
		Model model = new Model(LocalDate.parse("2017-01-20"), LocalDate.parse("2017-01-23"), 12);
		model.addBrowseHistory("Electronic products", 11);
		model.addBrowseHistory("Womens clothing", 3);

		//check model
        assertEquals(3, model.getDaysSinceRegistration());
        assertEquals(12, model.getContinuousLoginDays());
        assertEquals(11, model.timesBrowsed("Electronic products"));

        //use rule engine to work out which rules match
        List<Rule> matchingRules = engine.getMatchingRules(model);

        //verify results
        assertEquals(1, matchingRules.size());
        Rule matchingRule = matchingRules.get(0);
        assertEquals(a, matchingRule);
        assertEquals("outcome1", matchingRule.getOutcome());
        assertEquals("a", matchingRule.getName());
	}

}
