package com.project.optrabidz;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestingSetupTest {
    @Test
    void junitIsWiredIntoMaven() {
        assertThat("optrabidz").isEqualTo("optrabidz");
    }
}

