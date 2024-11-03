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
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FlinkTable extends GenericTable implements DBPImageProvider, DBPNamedObject2 {
    final public IndexCache indexCache = new IndexCache();

    public FlinkTable(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    @Nullable
    @Override
    public synchronized List<FlinkTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return (List<FlinkTableColumn>) super.getAttributes(monitor);
    }

    @Override
    public Collection<FlinkIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return indexCache.getObjects(monitor, getContainer(), this);
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        indexCache.clearCache();
        return super.refreshObject(monitor);
    }

    public boolean isIndexTable(){
        return getTableType().equals("INDEX_TABLE");
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        if (isIndexTable()) {
            return DBIcon.TREE_TABLE_INDEX;
        } else {
            return DBIcon.TREE_TABLE;
        }
    }

    @Override
    public boolean supportUniqueIndexes() {
        return false;
    }

    public Collection<DBSIndexType> getTableIndexTypes() {
        List<DBSIndexType> indexTypes = new ArrayList<>();
        indexTypes.add(new DBSIndexType("COMPACT", "Compact"));
        indexTypes.add(new DBSIndexType("BITMAP", "Bitmap"));
        return indexTypes;
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<GenericStructContainer, FlinkTable, FlinkIndex, GenericTableIndexColumn> {
        IndexCache()
        {
            super(getCache(), FlinkTable.class, "tab_name", "idx_name");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, FlinkTable forParent)
                throws SQLException
        {
            JDBCPreparedStatement dbStat;
            dbStat = session.prepareStatement("SHOW INDEX ON ?");
            if (forParent != null) {
                dbStat.setString(1, forParent.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected FlinkIndex fetchObject(JDBCSession session, GenericStructContainer owner, FlinkTable parent, String indexName, JDBCResultSet dbResult)
        {
            String FlinkIndexName = CommonUtils.notEmpty(JDBCUtils.safeGetString(dbResult, "idx_name")).trim();
            String comment = JDBCUtils.safeGetString(dbResult, "comment");
            String indexType = CommonUtils.notEmpty(JDBCUtils.safeGetString(dbResult, "idx_type")).trim();
            String indexTableName = CommonUtils.notEmpty(JDBCUtils.safeGetString(dbResult, "idx_tab_name")).trim();
            try {
                FlinkTable table = (FlinkTable) owner.getTable(dbResult.getSession().getProgressMonitor(), indexTableName);
                return new FlinkIndex(parent, FlinkIndexName, true, comment, indexType, table);
            } catch (DBException e) {
                log.debug("Can't read table from index" + indexName, e);
            }
            return new FlinkIndex(parent, FlinkIndexName, true, comment, indexType, null);
        }

        @Nullable
        @Override
        protected GenericTableIndexColumn[] fetchObjectRow(
                JDBCSession session,
                FlinkTable parent, FlinkIndex index, JDBCResultSet dbResult)
                throws DBException
        {
            String columnNames = JDBCUtils.safeGetString(dbResult, "col_names");
            ArrayList<GenericTableIndexColumn> indexColumns = new ArrayList<>();
            if (columnNames != null) {
                if (columnNames.contains(",")) {
                    String[] indexColumnNames = columnNames.split(",");
                    for (String column : indexColumnNames) {
                        GenericTableColumn attribute = parent.getAttribute(session.getProgressMonitor(), column.trim());
                        if (attribute != null) {
                            indexColumns.add(new GenericTableIndexColumn(index, attribute, attribute.getOrdinalPosition(), false));
                        }
                    }
                } else {
                    GenericTableColumn attribute = parent.getAttribute(session.getProgressMonitor(), columnNames.trim());
                    if (attribute != null) {
                        indexColumns.add(new GenericTableIndexColumn(index, attribute, attribute.getOrdinalPosition(), false));
                    }
                }
            }
            return ArrayUtils.toArray(GenericTableIndexColumn.class, indexColumns);
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, FlinkIndex index, List<GenericTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }
}