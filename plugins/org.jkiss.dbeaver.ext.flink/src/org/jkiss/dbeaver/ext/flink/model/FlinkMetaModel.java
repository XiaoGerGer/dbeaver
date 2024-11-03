/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.flink.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.sql.SQLException;
import java.util.Map;

/**
 * FlinkMetaModel
 */
public class FlinkMetaModel extends GenericMetaModel {
    public FlinkMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new FlinkDataSource(monitor, container, this);
    }

    @Override
    public GenericTableBase createTableOrViewImpl(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new GenericView(
                    container,
                    tableName,
                    tableType,
                    dbResult);
        }
        return new FlinkTable(container, tableName, tableType, dbResult);
    }

    @Override
    public GenericTableIndex createIndexImpl(GenericTableBase table, boolean nonUnique, String qualifier, long cardinality, String indexName, DBSIndexType indexType, boolean persisted) {
        return new FlinkIndex((FlinkTable) table, indexName, persisted, "", indexType.getName(), null);
    }

    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase sourceObject, @NotNull Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Flink view/table source")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                dbStat.execute("SHOW CREATE TABLE " + sourceObject.getFullyQualifiedName(DBPEvaluationContext.DDL));
                try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.next()) {
                        String line = dbResult.getString(1);
                        if (line == null) {
                            continue;
                        }
                        sql.append(line).append("\n");
                    }
                    String ddl = sql.toString();
                    if (sourceObject.isView()) {
                        ddl = ddl.replace("CREATE VIEW", "CREATE OR REPLACE VIEW");
                        return SQLFormatUtils.formatSQL(sourceObject.getDataSource(), ddl);
                    }
                    return ddl;
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public boolean supportsTableDDLSplit(GenericTableBase sourceObject) {
        return false;
    }

    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject, @NotNull Map<String, Object> options) throws DBException {
        return getTableDDL(monitor, sourceObject, options);
    }

    @Override
    public GenericTableColumn createTableColumnImpl(@NotNull DBRProgressMonitor monitor, @Nullable JDBCResultSet dbResult, @NotNull GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
        return new FlinkTableColumn(table, columnName, typeName, valueType, sourceType, ordinalPos, columnSize, charLength, scale, precision, radix, notNull, remarks, defaultValue, autoIncrement, autoGenerated);
    }

    @Override
    public boolean supportsNotNullColumnModifiers(DBSObject object) {
        DBPDataSource dataSource = object.getDataSource();
        if (dataSource instanceof JDBCDataSource) {
            return ((JDBCDataSource) dataSource).isServerVersionAtLeast(3, 0);
        } else {
            return false;
        }

    }
}
