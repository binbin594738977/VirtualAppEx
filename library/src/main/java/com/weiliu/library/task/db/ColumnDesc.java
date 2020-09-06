package com.weiliu.library.task.db;

/**
 * sql table 列的描述
 * Created by qumiao on 2016/5/16.
 */
class ColumnDesc {

    final String type;
    final boolean isPrimaryKey;
    final boolean isIndex;
    final boolean isForeignKey;
    final String referenceTable;
    final String referenceColumn;

    ColumnDesc(String type) {
        this.type = type;
        this.isPrimaryKey = false;
        this.isIndex = false;
        this.isForeignKey = false;
        this.referenceTable = null;
        this.referenceColumn = null;
    }

    ColumnDesc(String type, boolean isPrimaryKey, boolean isIndex) {
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isIndex = isIndex;
        this.isForeignKey = false;
        this.referenceTable = null;
        this.referenceColumn = null;
    }

    ColumnDesc(String type, boolean isForeignKey, String referenceTable, String referenceColumn) {
        this.type = type;
        this.isPrimaryKey = false;
        this.isIndex = false;
        this.isForeignKey = isForeignKey;
        this.referenceTable = referenceTable;
        this.referenceColumn = referenceColumn;
    }

    ColumnDesc(String type, boolean isPrimaryKey, boolean isIndex,
                      boolean isForeignKey, String referenceTable, String referenceColumn) {
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isIndex = isIndex;
        this.isForeignKey = isForeignKey;
        this.referenceTable = referenceTable;
        this.referenceColumn = referenceColumn;
    }
}
