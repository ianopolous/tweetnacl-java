package org.peergos.crypto;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class TestRunner
{

    public static void main(String[] args)
    {
        Result result = JUnitCore.runClasses(NativeTest.class, JSTest.class);

        result.getFailures().stream().forEach(System.out::println);

        int status = result.getFailures().size() == 0 ? 0 : 1;
        System.exit(status);
    }
}

