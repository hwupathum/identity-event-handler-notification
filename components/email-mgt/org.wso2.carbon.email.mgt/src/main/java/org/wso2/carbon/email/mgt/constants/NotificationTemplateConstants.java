/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.email.mgt.constants;

import org.wso2.carbon.email.mgt.dao.impl.NotificationTemplateDAOImpl;

public class NotificationTemplateConstants {

    private NotificationTemplateConstants() {}

    public static class TemplateTypeTableColumns {

        private TemplateTypeTableColumns() {}
        public static final String ID = "ID";
        public static final String DISPLAY_NAME = "DISPLAY_NAME";
        public static final String TEMPLATE_TYPE = "TEMPLATE_TYPE";
        public static final String CHANNEL = "CHANNEL";
        public static final String TENANT_UUID = "TENANT_UUID";
    }

    public static class TemplateTableColumns {

        private TemplateTableColumns() {}
        public static final String TEMPLATE_TYPE_ID = "TEMPLATE_TYPE_ID";
        public static final String LOCALE = "LOCALE";
        public static final String SUBJECT = "SUBJECT";
        public static final String BODY = "BODY";
        public static final String FOOTER = "FOOTER";
        public static final String CONTENT_TYPE = "CONTENT_TYPE";
    }

    /**
     * SQL Queries used in {@link NotificationTemplateDAOImpl}
     */
    public static class SqlQueries {

        private SqlQueries() {}
        public static final String ADD_NOTIFICATION_TEMPLATE_TYPE = "INSERT INTO IDN_NOTIF_TEMPLATE_TYPE " +
                "(ID, DISPLAY_NAME, TEMPLATE_TYPE, CHANNEL, TENANT_UUID) VALUES (:ID;, :DISPLAY_NAME;, :TEMPLATE_TYPE;, " +
                ":CHANNEL;, :TENANT_UUID;)";
        public static final String GET_NOTIFICATION_TEMPLATE_TYPE_ID = "SELECT ID FROM IDN_NOTIF_TEMPLATE_TYPE " +
                "WHERE TEMPLATE_TYPE = :TEMPLATE_TYPE; AND CHANNEL = :CHANNEL; AND TENANT_UUID = :TENANT_UUID;";
        public static final String GET_NOTIFICATION_TEMPLATE_TYPES = "SELECT DISPLAY_NAME FROM " +
                "IDN_NOTIF_TEMPLATE_TYPE WHERE CHANNEL = :CHANNEL; AND TENANT_UUID = :TENANT_UUID;";
        public static final String DELETE_NOTIFICATION_TEMPLATE_TYPE_BY_NAME = "DELETE FROM IDN_NOTIF_TEMPLATE_TYPE " +
                "WHERE TEMPLATE_TYPE = :TEMPLATE_TYPE; AND CHANNEL = :CHANNEL; AND TENANT_UUID = :TENANT_UUID;";

        public static final String ADD_NOTIFICATION_TEMPLATE = "INSERT INTO IDN_NOTIF_TEMPLATE " +
                "(TEMPLATE_TYPE_ID, LOCALE, SUBJECT, BODY, FOOTER, CONTENT_TYPE) VALUES (:TEMPLATE_TYPE_ID;, :LOCALE;, " +
                ":SUBJECT;, :BODY;, :FOOTER;, :CONTENT_TYPE;)";
        public static final String UPDATE_NOTIFICATION_TEMPLATE = "UPDATE IDN_NOTIF_TEMPLATE SET SUBJECT = :SUBJECT;, " +
                "BODY = :BODY;, FOOTER = :FOOTER;, CONTENT_TYPE = :CONTENT_TYPE; WHERE TEMPLATE_TYPE_ID = " +
                ":TEMPLATE_TYPE_ID; AND LOCALE = :LOCALE;";
        public static final String GET_NOTIFICATION_TEMPLATE_BY_ID = "SELECT TEMPLATE_TYPE_ID, SUBJECT, BODY, FOOTER, " +
                "CONTENT_TYPE FROM IDN_NOTIF_TEMPLATE WHERE TEMPLATE_TYPE_ID = :TEMPLATE_TYPE_ID; AND LOCALE = :LOCALE;";
        public static final String GET_NOTIFICATION_TEMPLATE = "SELECT TEMPLATE_TYPE_ID, SUBJECT, BODY, FOOTER, " +
                "CONTENT_TYPE FROM IDN_NOTIF_TEMPLATE WHERE TEMPLATE_TYPE_ID IN (" + GET_NOTIFICATION_TEMPLATE_TYPE_ID +
                ") AND LOCALE = :LOCALE;";
        public static final String GET_NOTIFICATION_TEMPLATES_BY_TYPE = "SELECT TEMPLATE_TYPE_ID, LOCALE, SUBJECT, " +
                "BODY, FOOTER, CONTENT_TYPE  FROM IDN_NOTIF_TEMPLATE WHERE TEMPLATE_TYPE_ID IN (" +
                GET_NOTIFICATION_TEMPLATE_TYPE_ID + ")";
        public static final String GET_ALL_NOTIFICATION_TEMPLATES = "SELECT TEMPLATE_TYPE_ID, DISPLAY_NAME, TEMPLATE_TYPE, " +
                "LOCALE, SUBJECT, BODY, FOOTER, CONTENT_TYPE FROM IDN_NOTIF_TEMPLATE JOIN IDN_NOTIF_TEMPLATE_TYPE " +
                "ON IDN_NOTIF_TEMPLATE_TYPE.ID=IDN_NOTIF_TEMPLATE.TEMPLATE_TYPE_ID WHERE " +
                "CHANNEL = :CHANNEL; AND TENANT_UUID = :TENANT_UUID;";
        public static final String DELETE_NOTIFICATION_TEMPLATE = "DELETE FROM IDN_NOTIF_TEMPLATE WHERE " +
                "TEMPLATE_TYPE_ID IN (" + GET_NOTIFICATION_TEMPLATE_TYPE_ID + ") AND LOCALE = :LOCALE;";
        public static final String DELETE_NOTIFICATION_TEMPLATES_BY_TYPE = "DELETE FROM IDN_NOTIF_TEMPLATE WHERE " +
                "TEMPLATE_TYPE_ID IN (" + GET_NOTIFICATION_TEMPLATE_TYPE_ID + ")";
    }
}
