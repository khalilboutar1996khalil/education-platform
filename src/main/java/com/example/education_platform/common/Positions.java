package com.example.education_platform.common;

import java.util.List;

/**
 * Keeps ordered children numbered 1..n without gaps, so a missing number always means a bug.
 * Used by chapters and lessons inside a module, and by questions and choices inside a quiz.
 */
public final class Positions {

    private Positions() {
    }

    /** Inserts at a 1-based position, or appends when {@code position} is null. */
    public static <T extends Positioned> void insert(List<T> items, T item, Integer position) {
        int index = position == null ? items.size() : Math.clamp(position - 1, 0, items.size());
        items.add(index, item);
        renumber(items);
    }

    public static <T extends Positioned> void renumber(List<T> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i + 1);
        }
    }
}
