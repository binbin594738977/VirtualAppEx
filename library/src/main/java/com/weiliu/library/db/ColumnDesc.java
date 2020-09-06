package com.weiliu.library.db;

/**
 * sql table 列的描述
 * Created by qumiao on 2016/5/16.
 */
public class ColumnDesc {

    public final String type;
    public final boolean isPrimaryKey;
    public final boolean isIndex;
    public final boolean isForeignKey;
    public final String referenceTable;
    public final String referenceColumn;

    public ColumnDesc(String type) {
        this.type = type;
        this.isPrimaryKey = false;
        this.isIndex = false;
        this.isForeignKey = false;
        this.referenceTable = null;
        this.referenceColumn = null;
    }

    public ColumnDesc(String type, boolean isPrimaryKey, boolean isIndex) {
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isIndex = isIndex;
        this.isForeignKey = false;
        this.referenceTable = null;
        this.referenceColumn = null;
    }

    public ColumnDesc(String type, boolean isForeignKey, String referenceTable, String referenceColumn) {
        this.type = type;
        this.isPrimaryKey = false;
        this.isIndex = false;
        this.isForeignKey = isForeignKey;
        this.referenceTable = referenceTable;
        this.referenceColumn = referenceColumn;
    }

    public ColumnDesc(String type, boolean isPrimaryKey, boolean isIndex,
                      boolean isForeignKey, String referenceTable, String referenceColumn) {
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isIndex = isIndex;
        this.isForeignKey = isForeignKey;
        this.referenceTable = referenceTable;
        this.referenceColumn = referenceColumn;
    }
}
