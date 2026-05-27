package com.project.optrabidz.marketplace.domain.model;

public enum ListingSortMode {
    NEWEST,
    CLOSING_SOON;

    public static ListingSortMode from(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        for (ListingSortMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return NEWEST;
    }
}
