package com.ansub.tvhome;

import static com.ansub.tvhome.T.check;
import static com.ansub.tvhome.T.eq;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PhotoOrderTest {
    private static List<String> names() {
        List<String> n = new ArrayList<>();
        for (int i = 1; i <= 24; i++) n.add(String.format("%02d", i));
        return n;
    }

    private static List<String> take(PhotoOrder o, int count) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(o.next());
        return out;
    }

    public static void testEachOncePerCycle() {
        PhotoOrder o = new PhotoOrder(names(), 7);
        eq(new HashSet<>(names()), new HashSet<>(take(o, 24)));
        List<String> second = take(o, 24);
        eq(24, new HashSet<>(second).size());
        eq(new HashSet<>(names()), new HashSet<>(second));
    }

    public static void testNoRepeatAcrossCycles() {
        for (long seed = 0; seed < 50; seed++) {
            List<String> all = take(new PhotoOrder(names(), seed), 48);
            check(!all.get(23).equals(all.get(24)), "seed " + seed + " repeats " + all.get(23));
        }
    }

    public static void testSameSeedSameOrder() {
        eq(take(new PhotoOrder(names(), 42), 30), take(new PhotoOrder(names(), 42), 30));
    }

    public static void testShuffledNotSorted() {
        Set<List<String>> orders = new HashSet<>();
        for (long seed = 0; seed < 5; seed++) orders.add(take(new PhotoOrder(names(), seed), 24));
        check(orders.size() > 1, "different seeds give different orders");
        check(!orders.contains(names()), "order is shuffled");
    }

    public static void testSinglePhoto() {
        PhotoOrder o = new PhotoOrder(java.util.Collections.singletonList("01"), 1);
        eq("01", o.next());
        eq("01", o.next());
    }
}
