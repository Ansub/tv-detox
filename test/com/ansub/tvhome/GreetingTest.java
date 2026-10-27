package com.ansub.tvhome;

import static com.ansub.tvhome.T.eq;

public final class GreetingTest {
    public static void testNightIsEvening() {
        eq("Good evening, Alex", Greeting.of(4, "Alex"));
    }

    public static void testMorning() {
        eq("Good morning, Alex", Greeting.of(5, "Alex"));
        eq("Good morning", Greeting.of(11, null));
    }

    public static void testAfternoon() {
        eq("Good afternoon, Alex", Greeting.of(12, "Alex"));
        eq("Good afternoon", Greeting.of(16, null));
    }

    public static void testEvening() {
        eq("Good evening, Alex", Greeting.of(17, "Alex"));
        eq("Good evening", Greeting.of(23, null));
    }
}
