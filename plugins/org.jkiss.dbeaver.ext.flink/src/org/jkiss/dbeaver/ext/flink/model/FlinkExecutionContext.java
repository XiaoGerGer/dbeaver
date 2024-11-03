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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;

public class FlinkExecutionContext extends GenericExecutionContext {
    private static final Log log = Log.getLog(FlinkExecutionContext.class);

    public FlinkExecutionContext(JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @Override
    public void setDefaultCatalog(DBRProgressMonitor monitor, GenericCatalog catalog, GenericSchema schema) throws DBCException {
        if (catalog == null) {
            log.debug("Null current catalog");
            return;
        }
        GenericCatalog oldSelectedCatalog = getDefaultCatalog();
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) {
            String changeQuery = String.format("USE CATALOG `%s`", catalog.getName());
            try (JDBCPreparedStatement dbStat = session.prepareStatement(changeQuery)) {
                dbStat.execute();
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        if (oldSelectedCatalog != null) {
            DBUtils.fireObjectSelect(oldSelectedCatalog, false, this);
        }
        DBUtils.fireObjectSelect(catalog, true, this);
        if (schema != null) {
            setDefaultSchema(monitor, schema);
        }
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, GenericSchema schema) throws DBCException {
        if (schema == null) {
            log.debug("Null current schema");
            return;
        }
        GenericSchema oldSelectedSchema = getDefaultSchema();
        GenericCatalog catalog = getDefaultCatalog();
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE_SET_SCHEMA)) {
            String changeQuery = String.format("USE `%s`.`%s`", catalog.getName(), schema.getName());
            try (JDBCPreparedStatement dbStat = session.prepareStatement(changeQuery)) {
                dbStat.execute();
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }

        if (oldSelectedSchema != null) {
            DBUtils.fireObjectSelect(oldSelectedSchema, false, this);
        }
        DBUtils.fireObjectSelect(schema, true, this);
    }
}
