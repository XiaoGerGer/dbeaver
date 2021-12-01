/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model;

import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.DBPositiveNumberTransformer;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;

public class HANAProcedureParameter extends GenericProcedureParameter {

    private DBSObject tableType;
    private List<HANAInplaceTableTypeColumn> inplaceTableType;
    
    public HANAProcedureParameter(GenericProcedure procedure, String columnName, String typeName,
            int ordinalPosition, int columnSize, Integer scale, DBSProcedureParameterKind parameterKind,
            DBSObject tableType, List<HANAInplaceTableTypeColumn> inplaceTableType, boolean hasDefaultValue) {
        super(procedure, columnName, typeName, 0 /*valueType*/, ordinalPosition, columnSize, scale,
        		0 /*precision*/, false /*notNull*/, null /*remarks*/, parameterKind);
        this.tableType = tableType;
        this.inplaceTableType = inplaceTableType;
        this.autoGenerated = hasDefaultValue;
    }

    @Override
    public DBPImage getObjectImage() {
        if (tableType != null || inplaceTableType != null || HANAProcedure.DATA_TYPE_NAME_ANY_TABLE_TYPE.equals(typeName))
            return DBIcon.TREE_TABLE;
        return super.getObjectImage();
    }
    
    @Property(viewable = true, order = 25, optional = true)
    public DBSObject getTableType() {
        return tableType;
    }

    @Property(viewable = true, order = 26, optional = true)
    public Boolean getHasInplaceTableType() {
        if(inplaceTableType != null)
            return true;
        return null;
    }

    @Override
    @Property(viewable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 40)
    public long getMaxLength() {
        return super.getMaxLength();
    }
    
    @Override
    @Property(viewable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 41)
    public Integer getScale() {
        return super.getScale();
    }
    
    @Override
    @Property(hidden = true)
    public boolean isRequired() {
        return super.isRequired();
    }

    // optional in HANAInplaceTableTypeColumn detail window
    public boolean hasInplaceTableType() {
        return inplaceTableType != null;
    }

    @Association
    public List<HANAInplaceTableTypeColumn> getInplaceTableType(DBRProgressMonitor monitor) throws DBException {
        return inplaceTableType;
    }
    
}
