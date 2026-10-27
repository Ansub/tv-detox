package com.ansub.tvhome;

// "Good evening, Alex" at the top of the apps screen. Without a name (Guest), just "Good evening".
final class Greeting {
    private Greeting() {}

    static String of(int hourOfDay, String nameOrNull) {
        String g = hourOfDay >= 5 && hourOfDay < 12 ? "Good morning"
                : hourOfDay >= 12 && hourOfDay < 17 ? "Good afternoon" : "Good evening";
        return nameOrNull == null ? g : g + ", " + nameOrNull;
    }
}
