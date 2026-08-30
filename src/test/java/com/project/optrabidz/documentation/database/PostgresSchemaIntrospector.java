package com.project.optrabidz.documentation.database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.project.optrabidz.documentation.database.DatabaseSchemaSnapshot.Column;
import com.project.optrabidz.documentation.database.DatabaseSchemaSnapshot.ForeignKey;
import com.project.optrabidz.documentation.database.DatabaseSchemaSnapshot.NamedObject;

final class PostgresSchemaIntrospector {

    DatabaseSchemaSnapshot read(Connection connection) throws SQLException {
        return new DatabaseSchemaSnapshot(
                readTables(connection),
                readColumns(connection),
                readForeignKeys(connection),
                readConstraints(connection, "u"),
                readConstraints(connection, "c"),
                readPartialIndexes(connection),
                readTriggers(connection));
    }

    private List<Column> readColumns(Connection connection) throws SQLException {
        String sql = """
                SELECT table_class.relname AS table_name,
                       attribute.attname AS column_name,
                       NOT attribute.attnotnull AS nullable
                FROM pg_attribute attribute
                JOIN pg_class table_class ON table_class.oid = attribute.attrelid
                JOIN pg_namespace n ON n.oid = table_class.relnamespace
                WHERE n.nspname = 'public'
                  AND table_class.relkind = 'r'
                  AND table_class.relname <> 'flyway_schema_history'
                  AND attribute.attnum > 0
                  AND NOT attribute.attisdropped
                ORDER BY table_class.relname, attribute.attnum
                """;
        List<Column> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new Column(
                        rows.getString("table_name"),
                        rows.getString("column_name"),
                        rows.getBoolean("nullable")));
            }
        }
        return List.copyOf(result);
    }

    private List<String> readTables(Connection connection) throws SQLException {
        String sql = """
                SELECT c.relname
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relkind = 'r'
                  AND c.relname <> 'flyway_schema_history'
                ORDER BY c.relname
                """;
        List<String> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(rows.getString("relname"));
            }
        }
        return List.copyOf(result);
    }

    private List<ForeignKey> readForeignKeys(Connection connection) throws SQLException {
        String sql = """
                SELECT con.conname,
                       child.relname AS child_table,
                       ARRAY(
                         SELECT att.attname
                         FROM unnest(con.conkey) WITH ORDINALITY AS key(attnum, position)
                         JOIN pg_attribute att
                           ON att.attrelid = con.conrelid AND att.attnum = key.attnum
                         ORDER BY key.position
                       ) AS child_columns,
                       parent.relname AS parent_table,
                       ARRAY(
                         SELECT att.attname
                         FROM unnest(con.confkey) WITH ORDINALITY AS key(attnum, position)
                         JOIN pg_attribute att
                           ON att.attrelid = con.confrelid AND att.attnum = key.attnum
                         ORDER BY key.position
                       ) AS parent_columns,
                       EXISTS (
                         SELECT 1
                         FROM unnest(con.conkey) AS key(attnum)
                         JOIN pg_attribute att
                           ON att.attrelid = con.conrelid AND att.attnum = key.attnum
                         WHERE NOT att.attnotnull
                       ) AS nullable,
                       con.confdeltype
                FROM pg_constraint con
                JOIN pg_class child ON child.oid = con.conrelid
                JOIN pg_class parent ON parent.oid = con.confrelid
                JOIN pg_namespace n ON n.oid = child.relnamespace
                WHERE n.nspname = 'public' AND con.contype = 'f'
                ORDER BY con.conname
                """;
        List<ForeignKey> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new ForeignKey(
                        rows.getString("conname"),
                        rows.getString("child_table"),
                        strings(rows.getArray("child_columns")),
                        rows.getString("parent_table"),
                        strings(rows.getArray("parent_columns")),
                        rows.getBoolean("nullable"),
                        deleteAction(rows.getString("confdeltype"))));
            }
        }
        return result.stream().sorted(Comparator.comparing(ForeignKey::name)).toList();
    }

    private List<NamedObject> readConstraints(Connection connection, String type) throws SQLException {
        String sql = """
                SELECT con.conname,
                       table_class.relname AS table_name,
                       ARRAY(
                         SELECT att.attname
                         FROM unnest(con.conkey) WITH ORDINALITY AS key(attnum, position)
                         JOIN pg_attribute att
                           ON att.attrelid = con.conrelid AND att.attnum = key.attnum
                         ORDER BY key.position
                       ) AS columns,
                       pg_get_constraintdef(con.oid, true) AS definition
                FROM pg_constraint con
                JOIN pg_class table_class ON table_class.oid = con.conrelid
                JOIN pg_namespace n ON n.oid = table_class.relnamespace
                WHERE n.nspname = 'public' AND con.contype = ?
                ORDER BY table_class.relname, con.conname
                """;
        List<NamedObject> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new NamedObject(
                            rows.getString("conname"),
                            rows.getString("table_name"),
                            strings(rows.getArray("columns"))));
                }
            }
        }
        return List.copyOf(result);
    }

    private List<NamedObject> readPartialIndexes(Connection connection) throws SQLException {
        String sql = """
                SELECT index_class.relname AS index_name,
                       table_class.relname AS table_name,
                       ARRAY(
                         SELECT att.attname
                         FROM unnest(index_info.indkey::smallint[]) WITH ORDINALITY AS key(attnum, position)
                         JOIN pg_attribute att
                           ON att.attrelid = index_info.indrelid AND att.attnum = key.attnum
                         WHERE key.position <= index_info.indnkeyatts
                         ORDER BY key.position
                       ) AS columns,
                       pg_get_indexdef(index_info.indexrelid) AS definition
                FROM pg_index index_info
                JOIN pg_class index_class ON index_class.oid = index_info.indexrelid
                JOIN pg_class table_class ON table_class.oid = index_info.indrelid
                JOIN pg_namespace n ON n.oid = table_class.relnamespace
                WHERE n.nspname = 'public'
                  AND index_info.indpred IS NOT NULL
                  AND index_info.indisvalid
                ORDER BY table_class.relname, index_class.relname
                """;
        return readNamedObjects(connection, sql, "index_name");
    }

    private List<NamedObject> readTriggers(Connection connection) throws SQLException {
        String sql = """
                SELECT trigger_info.tgname AS trigger_name,
                       table_class.relname AS table_name,
                       ARRAY[]::text[] AS columns,
                       pg_get_triggerdef(trigger_info.oid, true) AS definition
                FROM pg_trigger trigger_info
                JOIN pg_class table_class ON table_class.oid = trigger_info.tgrelid
                JOIN pg_namespace n ON n.oid = table_class.relnamespace
                WHERE n.nspname = 'public' AND NOT trigger_info.tgisinternal
                ORDER BY table_class.relname, trigger_info.tgname
                """;
        return readNamedObjects(connection, sql, "trigger_name");
    }

    private List<NamedObject> readNamedObjects(
            Connection connection, String sql, String nameColumn) throws SQLException {
        List<NamedObject> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new NamedObject(
                        rows.getString(nameColumn),
                        rows.getString("table_name"),
                        strings(rows.getArray("columns"))));
            }
        }
        return List.copyOf(result);
    }

    private List<String> strings(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        return List.of((String[]) array.getArray());
    }

    private String deleteAction(String code) {
        return switch (code) {
            case "a" -> "NO ACTION";
            case "r" -> "RESTRICT";
            case "c" -> "CASCADE";
            case "n" -> "SET NULL";
            case "d" -> "SET DEFAULT";
            default -> throw new IllegalArgumentException("Unknown PostgreSQL delete action: " + code);
        };
    }
}
