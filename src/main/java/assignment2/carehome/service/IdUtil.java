package assignment2.carehome.service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.Map;

public class IdUtil {

    private static final Map<String, AtomicInteger> counters = new HashMap<>();

    public static String nextId(String prefix) {
        counters.putIfAbsent(prefix, new AtomicInteger(0));
        int next = counters.get(prefix).incrementAndGet();
        return prefix + next;
    }

    // Optional: reset IDs for testing
    public static void reset() {
        counters.clear();
    }
}
