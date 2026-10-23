package com.ansub.tvhome;

// Plain string helpers for what the home screen shows. No Android in here, so the tests can run
// them on a laptop (see test.sh).
final class Labels {
    private Labels() {}

    // Philips stores an input name as "name&DEVICE_TYPE" (like "Computer&COMPUTER"), so show the
    // name only. Only on Philips TVs: elsewhere a name like "AUDIO&VIDEO" is the name itself.
    static String withoutDeviceType(String manufacturer, String label) {
        if (!"Philips".equalsIgnoreCase(manufacturer)) return label;
        int amp = label.lastIndexOf('&');
        return amp > 0 && label.substring(amp + 1).matches("[A-Z][A-Z0-9_]{2,}") ? label.substring(0, amp).trim() : label;
    }

    // The app list passed with --es apps "a,b c": package names split on commas and spaces, in
    // order, without blanks or repeats.
    static java.util.List<String> packageList(String list) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (list == null) return out;
        for (String p : list.trim().split("[,\\s]+")) if (!p.isEmpty() && !out.contains(p)) out.add(p);
        return out;
    }
}
