/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.pgsql.meta;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class Table implements Comparable<Table> {

    private String id;
    private String schema;
    private String name;
    private List<Column> columns;
    private PrimaryKey pk;
    private List<ForeignKey> foreignKeys;
    private Catalog catalog;

    /**
     * In all referredKeys the ForeignKey.tableRef points to this table.
     * Origin table can be retrieved from Column instances.
     */
    private List<ForeignKey> referredKeys;

    private Table() {}

    public Table(String id, Table table, List<ForeignKey> referredKeys) {
        this.id = id;
        this.schema = table.schema;
        this.name = table.name;
        this.columns = table.columns;
        this.pk = table.pk;
        this.foreignKeys = table.foreignKeys;
        this.referredKeys = referredKeys != null ? referredKeys : Collections.emptyList();
    }

    public Table(String schema, String name, List<Column> columns, PrimaryKey key, List<ForeignKey> foreignKeys) {
        this.schema = schema;
        this.name = name;
        if (columns != null) {
            for (Column c : columns) {
                c.table = new TableRef(schema, name);
            }
            this.columns = Collections.unmodifiableList(columns);
        } else {
            this.columns = Collections.emptyList();
        }
        this.pk = key;
        if (foreignKeys != null) {
            this.foreignKeys = Collections.unmodifiableList(foreignKeys);
        } else {
            this.foreignKeys = Collections.emptyList();
        }
    }

    public Column column(String colName) {
        for (Column col : columns) {
            if (col.name.equals(colName)) {
                return col;
            }
        }
        return null;
    }

    public String id() {
        return id;
    }

    public String schema() {
        return schema;
    }

    public String name() {
        return name;
    }

    public TableRef tableRef() {
        return new TableRef(schema, name);
    }

    public String schemaName() {
        StringBuilder sb = new StringBuilder();
        if (schema != null) {
            sb.append(schema).append(".");
        }
        sb.append(name);
        return sb.toString();
    }

    public String quotedSchemaName() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        if (schema != null) {
            sb.append(schema).append("\".\"");
        }
        sb.append(name).append("\"");
        return sb.toString();
    }

    public List<Column> columns() {
        return columns;
    }

    public PrimaryKey pk() {
        return pk;
    }

    public List<ForeignKey> foreignKeys() {
        return foreignKeys;
    }

    public List<ForeignKey> referredKeys() {
        return referredKeys;
    }

    public String ddl() {
        return ddlPretty(false);
    }

    public String ddlPretty(boolean multiline) {

        StringBuilder sb = new StringBuilder("CREATE TABLE \"").append(schema()).append("\".\"").append(name()).append("\" (");
        if (multiline) {
            sb.append("\n    ");
        }
        int i = 0;
        for (Column col : columns()) {
            if (i > 0) {
                sb.append(", ");
                if (multiline) {
                    sb.append("\n    ");
                }
            }
            sb.append("\"").append(col.name()).append("\" ").append(col.typeSpec());
            boolean partOfPk = pk().getColumn(col.name()) != null;
            if (col.unique() && !partOfPk) {
                sb.append(" UNIQUE");
            }
            if (col.notNull() && !partOfPk) {
                sb.append(" NOT NULL");
            }
            i++;
        }
        List<Column> pkcols = pk().columns();
        if (pkcols.size() > 0) {
            if (multiline) {
                sb.append(",\n    ");
            } else {
                sb.append(", ");
            }
            sb.append("PRIMARY KEY (");
            i = 0;
            for (Column col : pkcols) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(col.name()).append("\"");
                i++;
            }
            sb.append(")");
        }

        List<ForeignKey> fkeys = foreignKeys();
        for (ForeignKey fk : fkeys) {
            if (multiline) {
                sb.append(",\n   ");
            } else {
                sb.append(", ");
            }
            sb.append("FOREIGN KEY (");
            i = 0;
            for (Column col : fk.columns()) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(col.name()).append("\"");
                i++;
            }
            sb.append(") REFERENCES \"").append(fk.tableRef().schema()).append("\".\"").append(fk.tableRef().name()).append("\" (");

            Table refTable = catalog.table(fk.tableRef());
            if (refTable != null) {
                i = 0;
                for (Column col : refTable.pk().columns()) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append("\"").append(col.name()).append("\"");
                    i++;
                }
            }
            sb.append(")");
        }

        sb.append(multiline ? "\n)\n" : ")");
        return sb.toString();
    }

    public Key keyForColumnName(String name) {
        Key ret = pkForColumnName(name);
        if (ret == null) {
            ret = foreignKeyForColumnName(name);
        }
        return ret;
    }

    public PrimaryKey pkForColumnName(String name) {
        for (Column c: pk.columns()) {
            if (c.name().equals(name)) {
                return pk;
            }
        }
        return null;
    }

    public ForeignKey foreignKeyForColumnName(String name) {
        for (ForeignKey fk: foreignKeys) {
            for (Column c: fk.columns()) {
                if (c.name().equals(name)) {
                    return fk;
                }
            }
        }
        return null;
    }

    public ForeignKey foreignKeyForFieldName(String name) {
        for (ForeignKey fk: foreignKeys) {
            if (fk.fieldName().equals(name)) {
                return fk;
            }
        }
        return null;
    }

    public Key joinKeyForTable(Table table) {
        for (ForeignKey fk: foreignKeys) {
            if (fk.tableRef().equals(table.tableRef())) {
                return fk;
            }
        }
        for (ForeignKey rfk: referredKeys) {
            if (rfk.columns().get(0).tableRef().equals(table.tableRef())) {
                return pk;
            }
        }
        return null;
    }

    /**
     * This method has package private visibility as it's only supposed to be used during Catalog / Table construction
     *
     * @param catalog
     */
    void catalog(Catalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("catalog == null");
        }
        if (this.catalog != null) {
            throw new RuntimeException("Catalog already set");
        }
        this.catalog = catalog;

        for (ForeignKey fk: foreignKeys) {
            fk.catalog(catalog);
        }
    }

    @Override
    public int compareTo(Table o) {
        if (o == this) {
            return 0;
        }

        if (o == null) {
            return -1;
        }

        return id.compareTo(o.id);
    }

    public Table copy() {
        Table t = new Table();
        t.id = id;
        t.schema = schema;
        t.name = name;
        t.columns = columns;
        t.pk = pk;
        t.referredKeys = referredKeys;

        List<ForeignKey> newFks = new LinkedList<>();
        for (ForeignKey key: foreignKeys) {
            newFks.add(key.copy());
        }
        t.foreignKeys = Collections.unmodifiableList(newFks);
        return t;
    }
}