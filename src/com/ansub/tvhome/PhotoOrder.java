package com.ansub.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// The order the lock screen photos come up in: every photo once, shuffled again each time round,
// and never the same photo twice in a row where one round ends and the next begins.
final class PhotoOrder {
    private final List<String> names;
    private final Random random;
    private final List<String> round = new ArrayList<>();
    private String last;

    PhotoOrder(List<String> names, long seed) {
        this.names = new ArrayList<>(names);
        this.random = new Random(seed);
    }

    String next() {
        if (round.isEmpty()) {
            round.addAll(names);
            Collections.shuffle(round, random);
            if (round.size() > 1 && round.get(0).equals(last)) Collections.swap(round, 0, 1 + random.nextInt(round.size() - 1));
        }
        last = round.remove(0);
        return last;
    }
}
