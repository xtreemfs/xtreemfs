/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Christian Lorenz (ZIB), Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.xtreemfs.common.logging.Logging;


/**
 * A test suite consisting of all test cases from this package.
 *
 * @author stender
 *
 */
public class AllTests extends TestSuite {

    public static Test suite() throws Exception {

        Logging.start(Logging.LEVEL_ERROR);

        TestSuite mySuite = new TestSuite("XtreemFS Test Suite");

        Collection<Class<? extends TestCase>> testCases = new LinkedList();
        findRecursively(testCases, ".");
        for (Class<? extends TestCase> clazz : testCases) {
            System.out.println("adding test '" + clazz.getName() + "'");
            mySuite.addTestSuite(clazz);
        }

        return mySuite;
    }

    protected static void findRecursively(
        Collection<Class<? extends TestCase>> testCases, String root)
        throws Exception {


        System.out.println("ROOT: "+root);

        BufferedReader buf = new BufferedReader(new InputStreamReader(
            (InputStream) AllTests.class.getResource(root)
                    .getContent()));
        for (;;) {
            String line = buf.readLine();
            if (line == null)
                break;
            if (line.endsWith("Test.class")) {

                String packageName = AllTests.class.getPackage().getName()
                    + root.substring(1).replace('/', '.');

                Class<? extends TestCase> clazz = Class.forName(
                    packageName + "."
                        + line.substring(0, line.length() - ".class".length()))
                        .asSubclass(TestCase.class);

                testCases.add(clazz);

            } else if (!line.endsWith(".class") && !line.startsWith(".")
                      && (line.substring(1).indexOf(".") == -1)) {
                findRecursively(testCases, root + "/" + line);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        TestRunner.run(AllTests.suite());
    }

}
