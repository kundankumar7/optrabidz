package com.project.optrabidz.marketplace.application.recommendation;

import com.project.optrabidz.classification.application.dto.response.ClassificationEntryResponse;

public final class ClassificationKey {
    private ClassificationKey() {
    }

    public static String from(ClassificationEntryResponse entry) {
        return entry.type().trim().toUpperCase() + ":" + entry.value().trim().toUpperCase();
    }
}
