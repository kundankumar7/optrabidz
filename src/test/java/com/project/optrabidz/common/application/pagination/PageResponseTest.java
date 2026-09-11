package com.project.optrabidz.common.application.pagination;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResponseTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalizesNullItemsToAnImmutableEmptyList() {
        PageResponse<String> response = new PageResponse<>(null, 1, 20, 0, 0);

        assertThat(response.items()).isEmpty();
        assertThatThrownBy(() -> response.items().add("unexpected"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defensivelyCopiesItems() {
        List<String> source = new ArrayList<>(List.of("first"));

        PageResponse<String> response = new PageResponse<>(source, 2, 10, 1, 1);
        source.add("second");

        assertThat(response.items()).containsExactly("first");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void serializesTheStableFiveFieldContract() {
        JsonNode json = objectMapper.valueToTree(
                new PageResponse<>(List.of("first"), 2, 10, 1, 1));

        assertThat(json.fieldNames()).toIterable()
                .containsExactlyInAnyOrder(
                        "items", "page", "size", "totalItems", "totalPages");
        assertThat(json.path("items").get(0).asText()).isEqualTo("first");
        assertThat(json.path("page").asInt()).isEqualTo(2);
        assertThat(json.path("size").asInt()).isEqualTo(10);
        assertThat(json.path("totalItems").asLong()).isEqualTo(1);
        assertThat(json.path("totalPages").asInt()).isEqualTo(1);
    }
}
