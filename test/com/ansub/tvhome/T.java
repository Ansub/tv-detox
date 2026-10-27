package com.ansub.tvhome;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

// A tiny test runner: calls every public static void test*() in the classes named on the command line.
public final class T {
    public static void main(String[] args) throws Exception {
        int passed = 0;
        for (String name : args) {
            Method[] methods = Class.forName(name).getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (Method m : methods) {
                if (!m.getName().startsWith("test") || !Modifier.isStatic(m.getModifiers())) continue;
                try {
                    m.invoke(null);
                    passed++;
                } catch (InvocationTargetException e) {
                    System.out.println("FAIL " + simple(name) + "." + m.getName() + ": " + e.getCause());
                    System.exit(1);
                }
            }
        }
        System.out.println("PASS " + passed);
    }

    private static String simple(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    static void eq(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
    }

    static void eq(float expected, float actual) {
        if (Math.abs(expected - actual) > 1e-4f) throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    static String read(String path) {
        try { return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8); }
        catch (Exception e) { throw new AssertionError("can't read " + path); }
    }
}
