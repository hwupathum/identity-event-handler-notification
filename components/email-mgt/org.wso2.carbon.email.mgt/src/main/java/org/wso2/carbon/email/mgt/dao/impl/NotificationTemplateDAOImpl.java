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

package org.wso2.carbon.email.mgt.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedPreparedStatement;
import org.wso2.carbon.email.mgt.constants.NotificationTemplateConstants;
import org.wso2.carbon.email.mgt.dao.NotificationTemplateDAO;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerInternalException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class is used to access notification templates from the database.
 */
public class NotificationTemplateDAOImpl implements NotificationTemplateDAO {

    private static final Log log = LogFactory.getLog(NotificationTemplateDAOImpl.class);

    /**
     * @inheritDoc
     */
    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(true)) {
            try {
                // Check if the notification template type already exists.
                if (processGetTemplateTypeID(connection, displayName, notificationChannel, tenantUUID) != null) {
                    throw new NotificationTemplateManagerInternalException("Notification template type already exists.");
                }
                // Add the notification template type.
                processAddTemplateType(connection, displayName, notificationChannel, tenantUUID);

                IdentityDatabaseUtil.commitGovernanceDBTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackGovernanceDBTransaction(connection);
                throw new NotificationTemplateManagerServerException("Error while adding notification template type.", e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerServerException("Error while adding notification template type.", e);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void deleteNotificationTemplateTypeByName(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);
        String templateType = I18nEmailUtil.getNormalizedName(displayName);

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(true)) {
            try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                    NotificationTemplateConstants.SqlQueries.DELETE_NOTIFICATION_TEMPLATE_TYPE_BY_NAME)) {
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TEMPLATE_TYPE, templateType);
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.CHANNEL,
                        notificationChannel);
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TENANT_UUID,
                        tenantUUID);
                statement.executeUpdate();

                processDeleteTemplatesOfType(connection, displayName, notificationChannel, tenantUUID);

                IdentityDatabaseUtil.commitGovernanceDBTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackGovernanceDBTransaction(connection);
                throw new NotificationTemplateManagerException("Error while deleting notification template type.", e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while deleting notification template type.", e);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<String> getNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);
        List<String> templateTypes = new ArrayList<>();

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(false)) {
            try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                    NotificationTemplateConstants.SqlQueries.GET_NOTIFICATION_TEMPLATE_TYPES)) {
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.CHANNEL,
                        notificationChannel);
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TENANT_UUID,
                        tenantUUID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        templateTypes.add(resultSet.getString(1));
                    }
                }
            } catch (SQLException e) {
                throw new NotificationTemplateManagerException("Error while retrieving notification template types.", e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while retrieving notification template types.", e);
        }

        return templateTypes;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(false)) {
            return processGetTemplateTypeID(connection, displayName, notificationChannel, tenantUUID) != null;
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while retrieving notification template type.", e);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);
        String displayName = notificationTemplate.getDisplayName();
        String notificationChannel = notificationTemplate.getNotificationChannel();
        String locale = notificationTemplate.getLocale();

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(true)) {
            try {
                String templateTypeID = processGetTemplateTypeID(connection, displayName, notificationChannel,
                        tenantUUID);
                // Check if the notification template type already exists.
                if (templateTypeID == null) {
                    // Add the notification template type.
                    templateTypeID = processAddTemplateType(connection, displayName, notificationChannel, tenantUUID);
                    if (log.isDebugEnabled()) {
                        String msg = "Creating template type : %s in tenant registry : %s";
                        log.debug(String.format(msg, displayName, tenantDomain));
                    }
                }
                // Check if the notification template already exists.
                if (!processIsTemplateExists(connection, templateTypeID, locale)) {
                    processAddTemplate(connection, templateTypeID, notificationTemplate);
                    if (log.isDebugEnabled()) {
                        String msg = "Creating template %s of type : %s in tenant registry : %s";
                        log.debug(String.format(msg, locale, displayName, tenantDomain));
                    }
                } else {
                    // Update existing template.
                    processUpdateTemplate(connection, templateTypeID, notificationTemplate);
                    if (log.isDebugEnabled()) {
                        String msg = "Updating existing template %s of type : %s in tenant registry : %s";
                        log.debug(String.format(msg, locale, displayName, tenantDomain));
                    }
                }
                IdentityDatabaseUtil.commitGovernanceDBTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackGovernanceDBTransaction(connection);
                throw new NotificationTemplateManagerException("Error while updating notification template.", e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while updating notification template.", e);
        }
    }

    /**
     * Add a new notification template.
     *
     * @param notificationTemplate  Notification template.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerException If an error occurs while adding the notification template.
     */
    protected void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);
        String displayName = notificationTemplate.getDisplayName();
        String notificationChannel = notificationTemplate.getNotificationChannel();
        String locale = notificationTemplate.getLocale();

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(true)) {
            try {
                String templateTypeID = processGetTemplateTypeID(connection, displayName, notificationChannel,
                        tenantUUID);
                // Check if the notification template type already exists.
                if (templateTypeID == null) {
                    // Add the notification template type.
                    templateTypeID = processAddTemplateType(connection, displayName, notificationChannel, tenantUUID);
                    if (log.isDebugEnabled()) {
                        String msg = "Creating template type : %s in tenant registry : %s";
                        log.debug(String.format(msg, displayName, tenantDomain));
                    }
                }
                // Check if the notification template already exists.
                if (processIsTemplateExists(connection, templateTypeID, locale)) {
                    throw new NotificationTemplateManagerInternalException("Notification template already exists.");
                }
                processAddTemplate(connection, templateTypeID, notificationTemplate);
                IdentityDatabaseUtil.commitGovernanceDBTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackGovernanceDBTransaction(connection);
                throw new NotificationTemplateManagerException("Error while adding notification template.", e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while adding notification template.", e);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public int addDefaultNotificationTemplates(List<NotificationTemplate> notificationTemplates,
                                                String notificationChannel,String tenantDomain)
            throws NotificationTemplateManagerException {

        int numberOfAddedTemplates = 0;

        for (NotificationTemplate notificationTemplate : notificationTemplates) {
            String displayName = notificationTemplate.getDisplayName();

            try {
                addNotificationTemplate(notificationTemplate, tenantDomain);
                if (log.isDebugEnabled()) {
                    String msg = "Default template added to %s tenant registry : %n%s";
                    log.debug(String.format(msg, tenantDomain, notificationTemplate));
                }
                numberOfAddedTemplates++;
            } catch (NotificationTemplateManagerInternalException e) {
                log.warn("Template : " + displayName + "already exists in the registry. Hence " +
                        "ignoring addition");
            }
        }
        return numberOfAddedTemplates;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                           String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);
        String templateType = I18nEmailUtil.getNormalizedName(displayName);

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(true)) {
            try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                    NotificationTemplateConstants.SqlQueries.DELETE_NOTIFICATION_TEMPLATE)) {
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TEMPLATE_TYPE, templateType);
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.CHANNEL, notificationChannel);
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TENANT_UUID, tenantUUID);
                statement.setString(NotificationTemplateConstants.TemplateTableColumns.LOCALE, locale);
                statement.executeUpdate();

                IdentityDatabaseUtil.commitGovernanceDBTransaction(connection);
            } catch (SQLException e) {
                IdentityDatabaseUtil.rollbackGovernanceDBTransaction(connection);
                throw new NotificationTemplateManagerException("Error while deleting notification template.", e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while deleting notification template.", e);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);
        String templateType = I18nEmailUtil.getNormalizedName(displayName);
        NotificationTemplate notificationTemplate = null;

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(false)) {
            try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                    NotificationTemplateConstants.SqlQueries.GET_NOTIFICATION_TEMPLATE)) {
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TEMPLATE_TYPE, templateType);
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.CHANNEL, notificationChannel);
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TENANT_UUID, tenantUUID);
                statement.setString(NotificationTemplateConstants.TemplateTableColumns.LOCALE, locale);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        notificationTemplate = new NotificationTemplate();
                        notificationTemplate.setDisplayName(displayName);
                        notificationTemplate.setType(templateType);
                        notificationTemplate.setNotificationChannel(notificationChannel);
                        notificationTemplate.setLocale(locale);
                        notificationTemplate.setSubject(resultSet.getString(
                                NotificationTemplateConstants.TemplateTableColumns.SUBJECT));
                        notificationTemplate.setBody(resultSet.getString(
                                NotificationTemplateConstants.TemplateTableColumns.BODY));
                        notificationTemplate.setFooter(resultSet.getString(
                                NotificationTemplateConstants.TemplateTableColumns.FOOTER));
                        notificationTemplate.setContentType(resultSet.getString(
                                NotificationTemplateConstants.TemplateTableColumns.CONTENT_TYPE));
                    }
                }
            } catch (SQLException e) {
                throw new NotificationTemplateManagerException("Error while retrieving notification template.", e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while retrieving notification template.", e);
        }

        return notificationTemplate;
    }

    /**
     * Get all the notification templates for a given notification channel.
     *
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return List of notification templates.
     * @throws NotificationTemplateManagerException Error while retrieving notification templates.
     */
    protected List<NotificationTemplate> getAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);
        List<NotificationTemplate> notificationTemplates = new ArrayList<>();

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(false)) {
            try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                    NotificationTemplateConstants.SqlQueries.GET_ALL_NOTIFICATION_TEMPLATES)) {
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.CHANNEL, notificationChannel);
                statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TENANT_UUID, tenantUUID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        NotificationTemplate notificationTemplate = new NotificationTemplate();
                        notificationTemplate.setDisplayName(resultSet.getString(
                                NotificationTemplateConstants.TemplateTypeTableColumns.DISPLAY_NAME));
                        notificationTemplate.setType(resultSet.getString(
                                NotificationTemplateConstants.TemplateTypeTableColumns.TEMPLATE_TYPE));
                        notificationTemplate.setNotificationChannel(notificationChannel);
                        notificationTemplate.setLocale(resultSet.getString(
                                NotificationTemplateConstants.TemplateTableColumns.LOCALE));
                        notificationTemplate.setSubject(resultSet.getString(
                                NotificationTemplateConstants.TemplateTableColumns.SUBJECT));
                        notificationTemplate.setBody(resultSet.getString(
                                NotificationTemplateConstants.TemplateTableColumns.BODY));
                        notificationTemplate.setFooter(resultSet.getString(
                                NotificationTemplateConstants.TemplateTableColumns.FOOTER));
                        notificationTemplate.setContentType(resultSet.getString(
                                NotificationTemplateConstants.TemplateTableColumns.CONTENT_TYPE));
                        notificationTemplates.add(notificationTemplate);
                    }
                }
            } catch (SQLException e) {
                throw new NotificationTemplateManagerException("Error while retrieving all notification templates.", e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while retrieving all notification templates.", e);
        }
        return notificationTemplates;
    }

    /**
     * Get all the notification templates for a given display name and notification channel.
     *
     * @param displayName           Display name of the notification template.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return List of notification templates.
     * @throws NotificationTemplateManagerException Error while retrieving notification templates.
     */
    protected List<NotificationTemplate> getNotificationTemplates(String displayName, String notificationChannel,
                                                               String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);
        List<NotificationTemplate> notificationTemplates;

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(false)) {
            try {
                String templateTypeID = processGetTemplateTypeID(connection, displayName, notificationChannel,
                        tenantUUID);
                if (templateTypeID == null) {
                    throw new NotificationTemplateManagerInternalException(
                            "No template type found for the given display name.");
                }
                notificationTemplates = processGetTemplates(connection, displayName, notificationChannel,
                        tenantUUID);
            } catch (SQLException e) {
                throw new NotificationTemplateManagerException("Error while retrieving notification templates.", e);
            }
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while retrieving notification templates.", e);
        }
        return notificationTemplates;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String tenantDomain)
            throws NotificationTemplateManagerException {

        String tenantUUID = getTenantUUID(tenantDomain);

        if (tenantUUID == null) {
            return false;
        }

        try (Connection connection = IdentityDatabaseUtil.getGovernanceDBConnection(false)) {
            String templateTypeID = processGetTemplateTypeID(connection, displayName, notificationChannel,
                    tenantUUID);
            if (templateTypeID == null) {
                return false;
            }
            return processIsTemplateExists(connection, templateTypeID, locale);
        } catch (SQLException e) {
            throw new NotificationTemplateManagerException("Error while retrieving notification template type.", e);
        }
    }

    // Email template related methods

    /**
     * @inheritDoc
     */
    @Override
    public List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws NotificationTemplateManagerException {

        List<EmailTemplate> templateList = new ArrayList<>();
        String channelType = NotificationChannels.EMAIL_CHANNEL.getChannelType();

        getAllNotificationTemplates(channelType, tenantDomain).forEach(notificationTemplate ->
            templateList.add(I18nEmailUtil.buildEmailTemplate(notificationTemplate)));

        return templateList;
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<EmailTemplate> getEmailTemplates(String templateType, String tenantDomain)
            throws NotificationTemplateManagerException {

        List<EmailTemplate> templateList = new ArrayList<>();
        String channelType = NotificationChannels.EMAIL_CHANNEL.getChannelType();

        getNotificationTemplates(templateType, channelType, tenantDomain).forEach(notificationTemplate ->
                templateList.add(I18nEmailUtil.buildEmailTemplate(notificationTemplate)));

        return templateList;
    }

    // Private methods

    /**
     * Get the template type ID from the database for the given display name and notification channel.
     *
     * @param connection            Database connection.
     * @param displayName           Display name of the notification template.
     * @param notificationChannel   Notification channel.
     * @param tenantUUID            Tenant UUID.
     * @return Template type ID.
     * @throws SQLException Error while retrieving template type ID.
     */
    private String processGetTemplateTypeID(Connection connection, String displayName, String notificationChannel,
                                                String tenantUUID) throws SQLException {

        String templateTypeID;
        String templateType = I18nEmailUtil.getNormalizedName(displayName);

        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                NotificationTemplateConstants.SqlQueries.GET_NOTIFICATION_TEMPLATE_TYPE_ID)) {
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TEMPLATE_TYPE, templateType);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.CHANNEL, notificationChannel);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TENANT_UUID, tenantUUID);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    templateTypeID = resultSet.getString(
                            NotificationTemplateConstants.TemplateTypeTableColumns.ID);
                } else {
                    return null;
                }
            }
        }
        return templateTypeID;
    }

    /**
     * Check whether a notification template exists for the given template type ID and locale.
     *
     * @param connection        Database connection.
     * @param templateTypeID    Template type ID.
     * @param locale            Locale.
     * @return List of notification templates.
     * @throws SQLException Error while retrieving notification templates.
     */
    private boolean processIsTemplateExists(Connection connection, String templateTypeID, String locale)
            throws SQLException {

        boolean isExist = false;

        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                NotificationTemplateConstants.SqlQueries.GET_NOTIFICATION_TEMPLATE_BY_ID)) {
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.TEMPLATE_TYPE_ID, templateTypeID);
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.LOCALE, locale);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    isExist = true;
                }
            }
        }
        return isExist;
    }

    /**
     * Delete notification templates of the given display name and notification channel.
     *
     * @param connection            Database connection.
     * @param displayName           Display name of the notification template.
     * @param notificationChannel   Notification channel.
     * @param tenantUUID            Tenant UUID.
     * @throws SQLException Error while retrieving notification templates.
     */
    private void processDeleteTemplatesOfType(Connection connection, String displayName, String notificationChannel,
                                              String tenantUUID) throws SQLException {

        String templateType = I18nEmailUtil.getNormalizedName(displayName);

        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                NotificationTemplateConstants.SqlQueries.DELETE_NOTIFICATION_TEMPLATES_BY_TYPE)) {
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TEMPLATE_TYPE, templateType);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.CHANNEL, notificationChannel);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TENANT_UUID, tenantUUID);
            statement.executeUpdate();
        }
    }

    /**
     * Add notification template types of the given display name and notification channel.
     *
     * @param connection            Database connection.
     * @param displayName           Display name of the notification template.
     * @param notificationChannel   Notification channel.
     * @param tenantUUID            Tenant UUID.
     * @return Template type ID.
     * @throws SQLException Error while retrieving notification templates.
     */
    private String processAddTemplateType(Connection connection, String displayName, String notificationChannel,
                                        String tenantUUID) throws SQLException {

        String id = UUID.randomUUID().toString();
        String templateType = I18nEmailUtil.getNormalizedName(displayName);

        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                NotificationTemplateConstants.SqlQueries.ADD_NOTIFICATION_TEMPLATE_TYPE)) {
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.ID, id);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.DISPLAY_NAME, displayName);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TEMPLATE_TYPE, templateType);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.CHANNEL,
                    notificationChannel);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TENANT_UUID, tenantUUID);
            statement.executeUpdate();
        }
        return id;
    }

    /**
     * Add notification templates of the given template type ID and notification template.
     *
     * @param connection            Database connection.
     * @param templateTypeID        Template type ID.
     * @param notificationTemplate  Notification template.
     * @throws SQLException Error while retrieving notification templates.
     */
    private void processAddTemplate(Connection connection, String templateTypeID,
                                    NotificationTemplate notificationTemplate)
            throws SQLException {

        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                NotificationTemplateConstants.SqlQueries.ADD_NOTIFICATION_TEMPLATE)) {
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.TEMPLATE_TYPE_ID,
                    templateTypeID);
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.LOCALE,
                    notificationTemplate.getLocale());
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.SUBJECT,
                    notificationTemplate.getSubject());
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.BODY,
                    notificationTemplate.getBody());
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.FOOTER,
                    notificationTemplate.getFooter());
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.CONTENT_TYPE,
                    notificationTemplate.getContentType());
            statement.executeUpdate();
        }
    }

    /**
     * Update notification templates of the given template type ID and notification template.
     *
     * @param connection            Database connection.
     * @param templateTypeID        Template type ID.
     * @param notificationTemplate  Notification template.
     * @throws SQLException Error while retrieving notification templates.
     */
    private void processUpdateTemplate(Connection connection, String templateTypeID,
                                       NotificationTemplate notificationTemplate)
            throws SQLException {

        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                NotificationTemplateConstants.SqlQueries.UPDATE_NOTIFICATION_TEMPLATE)) {
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.TEMPLATE_TYPE_ID,
                    templateTypeID);
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.LOCALE,
                    notificationTemplate.getLocale());
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.SUBJECT,
                    notificationTemplate.getSubject());
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.BODY,
                    notificationTemplate.getBody());
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.FOOTER,
                    notificationTemplate.getFooter());
            statement.setString(NotificationTemplateConstants.TemplateTableColumns.CONTENT_TYPE,
                    notificationTemplate.getContentType());
            statement.executeUpdate();
        }
    }

    /**
     * Get all the notification templates for a given display name and notification channel.
     *
     * @param connection            Database connection.
     * @param displayName           Display name of the notification template.
     * @param notificationChannel   Notification channel.
     * @param tenantUUID            Tenant UUID.
     * @return List of notification templates.
     * @throws SQLException Error while retrieving notification templates.
     */
    private List<NotificationTemplate> processGetTemplates(Connection connection, String displayName,
                                                           String notificationChannel, String tenantUUID)
            throws SQLException {

        String templateType = I18nEmailUtil.getNormalizedName(displayName);
        List<NotificationTemplate> notificationTemplates = new ArrayList<>();

        try (NamedPreparedStatement statement = new NamedPreparedStatement(connection,
                NotificationTemplateConstants.SqlQueries.GET_NOTIFICATION_TEMPLATES_BY_TYPE)) {
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TEMPLATE_TYPE, templateType);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.CHANNEL, notificationChannel);
            statement.setString(NotificationTemplateConstants.TemplateTypeTableColumns.TENANT_UUID, tenantUUID);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    NotificationTemplate notificationTemplate = new NotificationTemplate();
                    notificationTemplate.setDisplayName(displayName);
                    notificationTemplate.setNotificationChannel(notificationChannel);
                    notificationTemplate.setType(templateType);
                    notificationTemplate.setLocale(resultSet.getString(
                            NotificationTemplateConstants.TemplateTableColumns.LOCALE));
                    notificationTemplate.setSubject(resultSet.getString(
                            NotificationTemplateConstants.TemplateTableColumns.SUBJECT));
                    notificationTemplate.setBody(resultSet.getString(
                            NotificationTemplateConstants.TemplateTableColumns.BODY));
                    notificationTemplate.setFooter(resultSet.getString(
                            NotificationTemplateConstants.TemplateTableColumns.FOOTER));
                    notificationTemplate.setContentType(resultSet.getString(
                            NotificationTemplateConstants.TemplateTableColumns.CONTENT_TYPE));
                    notificationTemplates.add(notificationTemplate);
                }
            }
        }
        return notificationTemplates;
    }

    /**
     * Get tenant UUID for the given tenant domain.
     *
     * @param tenantDomain Tenant domain.
     * @return Tenant UUID.
     * @throws NotificationTemplateManagerException Error while retrieving tenant UUID.
     */
    private String getTenantUUID(String tenantDomain) throws NotificationTemplateManagerException {
        if (tenantDomain != null) {
            int tenantID = IdentityTenantUtil.getTenantId(tenantDomain);
            // Super tenant does not have a tenant UUID. Therefore, set a hard coded value.
            if (tenantID == MultitenantConstants.SUPER_TENANT_ID) {
                // Set a hard length of 32 characters for super tenant ID.
                // This is to avoid the database column length constraint violation.
                return String.format("%1$-32d", tenantID);
            }
            if (tenantID != MultitenantConstants.INVALID_TENANT_ID) {
                Tenant tenant = IdentityTenantUtil.getTenant(tenantID);
                if (tenant != null) {
                    return tenant.getTenantUniqueID();
                }
            }
        }
        throw new NotificationTemplateManagerException("Invalid tenant domain: " + tenantDomain);
    }
}
