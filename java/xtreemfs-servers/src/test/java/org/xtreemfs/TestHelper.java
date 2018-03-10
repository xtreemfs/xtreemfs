package org.xtreemfs;

import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class TestHelper {
    public static final TestRule testLog = new TestWatcher() {
                                       protected void starting(Description description) {
                                           System.out.println("TEST: " + description.getTestClass().getSimpleName()
                                                   + "." + description.getMethodName());
                                       }
                                   };

}
