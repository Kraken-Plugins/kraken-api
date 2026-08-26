package unit.plugins.api.ui;

import com.kraken.api.Context;
import org.junit.jupiter.api.Test;
import plugins.api.TestResultManager;
import plugins.api.suite.RegisteredTest;
import plugins.api.suite.SuiteOptions;
import plugins.api.suite.TestGroup;
import plugins.api.suite.TestRegistry;
import plugins.api.suite.TestRunner;
import plugins.api.tests.BaseApiTest;
import plugins.api.ui.ApiTestPanel;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiTestPanelTest {

    @Test
    void theGroupFilterShowsOnlyTheSelectedCategory() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ApiTestPanel panel = new ApiTestPanel(registry(), new TestRunner(),
                    new TestResultManager(), SuiteOptions::defaults);
            JComboBox<?> filter = component(panel, JComboBox.class);

            filter.setSelectedItem("Self check");
            assertNotNull(label(panel, "SELF CHECK"));
            assertNull(label(panel, "QUERY"));

            filter.setSelectedItem("Query");
            assertNull(label(panel, "SELF CHECK"));
            assertNotNull(label(panel, "QUERY"));

            filter.setSelectedItem("All tests");
            assertNotNull(label(panel, "SELF CHECK"));
            assertNotNull(label(panel, "QUERY"));
        });
    }

    private static TestRegistry registry() {
        List<RegisteredTest> tests = Arrays.asList(
                new RegisteredTest(TestGroup.SELF_CHECK, new SelfCheck(), "selfCheck"),
                new RegisteredTest(TestGroup.QUERY, new Query(), "query"));

        return new TestRegistry() {
            @Override
            public synchronized List<RegisteredTest> all() {
                return tests;
            }

            @Override
            public synchronized int size() {
                return tests.size();
            }
        };
    }

    private static JLabel label(Container root, String text) {
        for (JLabel label : components(root, JLabel.class)) {
            if (text.equals(label.getText())) {
                return label;
            }
        }
        return null;
    }

    private static <T extends Component> T component(Container root, Class<T> type) {
        return components(root, type).get(0);
    }

    private static <T extends Component> List<T> components(Container root, Class<T> type) {
        List<T> matches = new java.util.ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                matches.add(type.cast(child));
            }
            if (child instanceof Container) {
                matches.addAll(components((Container) child, type));
            }
        }
        return matches;
    }

    private static class SelfCheck extends BaseApiTest {
        @Override
        protected boolean runTest(Context ctx) {
            return true;
        }

        @Override
        public String getTestName() {
            return "Self Check";
        }
    }

    private static class Query extends BaseApiTest {
        @Override
        protected boolean runTest(Context ctx) {
            return true;
        }

        @Override
        public String getTestName() {
            return "Query";
        }
    }
}
