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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.flink.model.jdbc.FlinkJdbcFactory;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCFactory;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLState;

import java.sql.SQLException;

public class FlinkDataSource extends GenericDataSource {
    private static final Log log = Log.getLog(FlinkDataSource.class);

    private static final String CONNECTION_CLOSED_MESSAGE = "Connection is closed";

    public FlinkDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
            throws DBException {
        super(monitor, container, metaModel, new FlinkSQLDialect());
    }

    @Override
    protected FlinkDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        return new FlinkDataSourceInfo(monitor, this, metaData);
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new FlinkExecutionContext(instance, type);
    }

    @NotNull
    @Override
    protected JDBCFactory createJdbcFactory() {
        return new FlinkJdbcFactory();
    }

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        if (error instanceof SQLException && CONNECTION_CLOSED_MESSAGE.equals(error.getMessage())) {
            return ErrorType.CONNECTION_LOST;
        }
        String sqlState = SQLState.getStateFromException(error);
        if (SQLState.SQL_08S01.getCode().equals(sqlState)) {
            // By some reason many Flink errors have this SQL state
            return ErrorType.NORMAL;
        }
        return super.discoverErrorType(error);
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(true, false);
        }
        return null;
    }
}
