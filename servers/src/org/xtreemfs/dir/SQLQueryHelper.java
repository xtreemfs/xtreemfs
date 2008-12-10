/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.dir;


public class SQLQueryHelper {

    public static String createInsertStatement(String tableName,
            String[] columnNames, Object[] values) {

        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO ");
        sb.append(tableName);

        if (columnNames != null) {
            sb.append(" (");
            sb.append(arrayToString(columnNames));
            sb.append(")");
        }

        sb.append(" VALUES (");
        sb.append(valArrayToString(values));
        sb.append(")");

        return sb.toString();
    }

    public static String createDeleteStatement(String tableName,
            String whereStatement) {
        StringBuffer sb = new StringBuffer();
        sb.append("DELETE FROM ");
        sb.append(tableName);
        sb.append(" WHERE ");
        sb.append(whereStatement);

        return sb.toString();
    }

    public static String createQueryStatement(String tableName,
            String[] columnNames, String whereStatement) {

        StringBuffer sb = new StringBuffer();
        sb.append("SELECT ");

        if (columnNames != null)
            sb.append(arrayToString(columnNames));
        else
            sb.append("*");

        sb.append(" FROM ");
        sb.append(tableName);

        if (whereStatement != null && whereStatement.length() > 0) {
            sb.append(" WHERE ");
            sb.append(whereStatement);
        }

        return sb.toString();
    }

    public static String createQueryStatement(String[] tableNames,
            String[] columnNames, String whereStatement) {

        return createQueryStatement(arrayToString(tableNames), columnNames,
                whereStatement);
    }

    public static String createLeftJoinQueryStatement(String[] tableNames,
            String[] colIds, String[] leftJoinTableNames, String onStatement,
            String whereStatement) {

        StringBuffer sb = new StringBuffer();
        sb.append("SELECT DISTINCT ");
        sb.append(colIds == null ? "*" : arrayToString(colIds));
        sb.append(" FROM ");
        sb.append(arrayToString(tableNames, " JOIN "));
        sb.append(" LEFT JOIN ");
        sb.append(arrayToString(leftJoinTableNames));
        sb.append(" ON ");
        sb.append(onStatement);
        if (whereStatement != null) {
            sb.append(" WHERE ");
            sb.append(whereStatement);
        }

        return sb.toString();
    }

    public static String createUpdateStatement(String tableName,
            String[] columnNames, Object[] values, String whereStatement) {

        StringBuffer sb = new StringBuffer();
        sb.append("UPDATE ");
        sb.append(tableName);
        sb.append(" SET ");

        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(columnNames[i]);
            sb.append(" = ");
            sb.append(values[i] instanceof String ? "\"" + values[i] + "\""
                    : values[i]);
        }

        if (whereStatement != null) {
            sb.append(" WHERE ");
            sb.append(whereStatement);
        }

        return sb.toString();
    }

    private static String arrayToString(String[] array, String separator) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1)
                sb.append(separator);
        }

        return sb.toString();
    }

    private static String arrayToString(String[] array) {
        return arrayToString(array, ", ");
    }

    private static String valArrayToString(Object[] vals) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < vals.length; i++) {
            sb.append(vals[i] instanceof String ? "'" + vals[i] + "'"
                    : vals[i]);
            if (i < vals.length - 1)
                sb.append(", ");
        }

        return sb.toString();
    }

}
