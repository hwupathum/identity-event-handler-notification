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

package org.wso2.carbon.email.mgt.dao;

import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.util.List;

/**
 * This interface is used to perform CRUD operations on email and SMS templates.
 */
public interface NotificationTemplateDAO {

    /**
     * Add a notification template type to given tenant.
     * @param displayName           Display name of the template type.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerException
     */
    void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException;

    /**
     * Delete a notification template from given tenant.
     * @param displayName           Display name of the template type.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerException
     */
    void deleteNotificationTemplateTypeByName(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException;

    /**
     * Get all notification template types for given tenant.
     *
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return                      List of available template types.
     * @throws NotificationTemplateManagerException
     */
    List<String> getNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException;

    /**
     * Check whether the given notification template type exists for given tenant.
     * @param displayName           Display name of the template type.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return                      True if the template type exists, false otherwise.
     * @throws NotificationTemplateManagerException
     */
    boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException;

    /**
     * Update a notification template is exists or add a new template if not exists for given tenant.
     * @param notificationTemplate  Notification template.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerException
     */
    void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException;

    /**
     * Add a default notification templates to given tenant.
     * @param notificationTemplates List of notification templates.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return                      Number of templates added.
     * @throws NotificationTemplateManagerException
     */
    int addDefaultNotificationTemplates(List<NotificationTemplate> notificationTemplates, String notificationChannel,
                                         String tenantDomain)
            throws NotificationTemplateManagerException;

    /**
     * Delete a notification template from given tenant.
     * @param displayName           Display Name.
     * @param locale                Locale of the template.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @throws NotificationTemplateManagerException
     */
    void deleteNotificationTemplate(String displayName, String locale, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException;

    /**
     * Get a notification template from given tenant.
     * @param displayName           Display Name.
     * @param locale                Locale of the template.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return                      Notification template.
     * @throws NotificationTemplateManagerException
     */
    NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                 String tenantDomain)
            throws NotificationTemplateManagerException;

    /**
     * Check whether the given notification template exists for given tenant.
     * @param displayName           Display Name.
     * @param locale                Locale of the template.
     * @param notificationChannel   Notification channel.
     * @param tenantDomain          Tenant domain.
     * @return                      True if the template exists, false otherwise.
     * @throws NotificationTemplateManagerException
     */
    boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException;

    // Email template related methods

    /**
     * Get all email templates for given tenant.
     * @param tenantDomain  Tenant domain.
     * @return              List of available email templates.
     * @throws NotificationTemplateManagerException
     */
    List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws NotificationTemplateManagerException;

    /**
     * Get email templates for given template type and tenant.
     * @param templateType  Template type.
     * @param tenantDomain  Tenant domain.
     * @return              List of available email templates.
     * @throws NotificationTemplateManagerException
     */
    List<EmailTemplate> getEmailTemplates(String templateType, String tenantDomain)
            throws NotificationTemplateManagerException;

}
