package com.project.optrabidz.documentation.database;

import java.util.List;

public record DatabaseSchemaSnapshot(
        List<String> tables,
        List<Column> columns,
        List<ForeignKey> foreignKeys,
        List<NamedObject> uniqueConstraints,
        List<NamedObject> checkConstraints,
        List<NamedObject> partialIndexes,
        List<NamedObject> triggers) {

    public record Column(String table, String name, boolean nullable) {
    }

    public record ForeignKey(
            String name,
            String childTable,
            List<String> childColumns,
            String parentTable,
            List<String> parentColumns,
            boolean nullable,
            String onDelete) {
    }

    public record NamedObject(String name, String table, List<String> columns) {
    }
}
