package cucumber.runtime;

import cucumber.Cucumber;
import cucumber.FeatureSource;
import cucumber.StepDefinition;
import cucumber.runtime.java.JavaStepDefinition;
import cucumber.runtime.java.ObjectFactory;
import cucumber.runtime.java.pico.PicoFactory;
import gherkin.formatter.PrettyFormatter;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class ExecutorTest {
    @Test
    public void printsSimpleResults() throws NoSuchMethodException, IOException {
        String expectedOutput = "" +
                "Feature: Hello\n" +
                "\n" +
                "  Scenario: Hi           # features/hello.feature:3\n" +
                "    Given I have 3 cukes # ExecutorTest$CukeSteps.haveNCukes(String)\n" +
                "";

        assertOutput(have3Cukes, Pattern.compile("I have (\\d+) cukes"), "haveNCukes", expectedOutput);
    }

    @Test
    public void instantiatesNewWorld() throws NoSuchMethodException, IOException {
        String expectedOutput = "" +
                "Feature: Hello\n" +
                "\n" +
                "  Scenario: Hi           # features/hello.feature:3\n" +
                "    Given I have 3 cukes # ExecutorTest$CukeSteps.keepState(String)\n" +
                "\n" +
                "  Scenario: Hi again      # features/hello.feature:6\n" +
                "    Given I have 10 cukes # ExecutorTest$CukeSteps.keepState(String)\n" +
                "";

        assertOutput(haveManyCukes, Pattern.compile("I have (\\d+) cukes"), "keepState", expectedOutput);
    }

    @Test
    public void printsResultsWithErrors() throws NoSuchMethodException, IOException {
        String expectedOutput = "" +
                "Feature: Hello\n" +
                "\n" +
                "  Scenario: Hi           # features/hello.feature:3\n" +
                "    Given I have 3 cukes # ExecutorTest$CukeSteps.haveNCukesAndFail(String)\n" +
                "      java.lang.RuntimeException: Oh noes\n" +
                "      \tat cucumber.runtime.ExecutorTest$CukeSteps.badStuff(ExecutorTest.java:113)\n" +
                "      \tat cucumber.runtime.ExecutorTest$CukeSteps.haveNCukesAndFail(ExecutorTest.java:109)\n" +
                "      \tat Hello.Hi.Given I have 3 cukes(features/hello.feature:4)\n" +
                "\n";

        assertOutput(have3Cukes, Pattern.compile("I have (\\d+) cukes"), "haveNCukesAndFail", expectedOutput);
    }

    private void assertOutput(String source, Pattern pattern, String methodName, String expectedOutput) throws NoSuchMethodException, IOException {
        Method method = CukeSteps.class.getDeclaredMethod(methodName, String.class);
        ObjectFactory objectFactory = new PicoFactory();
        objectFactory.addClass(method.getDeclaringClass());
        objectFactory.createObjects();
        StepDefinition haveCukes = new JavaStepDefinition(pattern, method, objectFactory, Locale.US);

        StringWriter output = new StringWriter();
        PrettyFormatter pretty = new PrettyFormatter(output, true, true);

        Backend backend = new SimpleBackend(Arrays.asList(haveCukes), objectFactory);
        Cucumber runtime = new Cucumber(backend, pretty);

        runtime.executeSources(Arrays.asList(new FeatureSource(source, "features/hello.feature")));

        assertThat(output.toString(), equalTo(expectedOutput));
    }

    private String have3Cukes = "" +
            "Feature: Hello\n" +
            "\n" +
            "  Scenario: Hi\n" +
            "    Given I have 3 cukes\n" +
            "";

    private String haveManyCukes = "" +
            "Feature: Hello\n" +
            "\n" +
            "  Scenario: Hi\n" +
            "    Given I have 3 cukes\n" +
            "\n" +
            "  Scenario: Hi again\n" +
            "    Given I have 10 cukes\n" +
            "";

    public static class CukeSteps {
        int state = 0;

        public void haveNCukes(String n) {

        }

        public void haveNCukesAndFail(String n) {
            badStuff();
        }

        private void badStuff() {
            throw new RuntimeException("Oh noes");
        }

        public void keepState(String n) {
            if(state > 0) {
                throw new RuntimeException("Didn't get a new instance");
            }
            state++;
        }
    }
}