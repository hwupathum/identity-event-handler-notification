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

import org.wso2.carbon.email.mgt.dao.NotificationTemplateDAO;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is used while migrating the notification templates from registry to database.
 */
public class NotificationTemplateMigrationDAOImpl implements NotificationTemplateDAO {

    private final NotificationTemplateDAO notificationTemplateDAOImpl = new NotificationTemplateDAOImpl();
    private final NotificationTemplateDAO notificationTemplateRegistryDAOImpl = new NotificationTemplateRegistryDAOImpl();

    /**
     * @inheritDoc
     */
    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        // Add the template type to database and then to registry.
        notificationTemplateDAOImpl.addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
        notificationTemplateRegistryDAOImpl.addNotificationTemplateType(displayName, notificationChannel, tenantDomain);

    }

    /**
     * @inheritDoc
     */
    @Override
    public void deleteNotificationTemplateTypeByName(String displayName, String notificationChannel,
                                                     String tenantDomain) throws NotificationTemplateManagerException {

        // Delete the template type from database and then from registry.
        notificationTemplateDAOImpl.deleteNotificationTemplateTypeByName(displayName, notificationChannel, tenantDomain);
        notificationTemplateRegistryDAOImpl.deleteNotificationTemplateTypeByName(displayName, notificationChannel, tenantDomain);
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<String> getNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        // Get the template types from database and then from registry.
        List<String> templateTypes = notificationTemplateDAOImpl.getNotificationTemplateTypes(notificationChannel, tenantDomain);
        List<String> registryTemplateTypes = notificationTemplateRegistryDAOImpl.getNotificationTemplateTypes(notificationChannel, tenantDomain);

        // Add the template types from registry if they are not already in the list.
        for (String registryTemplateType : registryTemplateTypes) {
            if (!templateTypes.contains(registryTemplateType)) {
                templateTypes.add(registryTemplateType);
            }
        }
        return templateTypes;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        // Check if the template type exist in database and then in registry.
        return notificationTemplateDAOImpl.isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain) ||
                notificationTemplateRegistryDAOImpl.isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        // Add or update the template in database and then in registry.
        notificationTemplateDAOImpl.addOrUpdateNotificationTemplate(notificationTemplate, tenantDomain);
        notificationTemplateRegistryDAOImpl.addOrUpdateNotificationTemplate(notificationTemplate, tenantDomain);
    }

    /**
     * @inheritDoc
     */
    @Override
    public int addDefaultNotificationTemplates(List<NotificationTemplate> notificationTemplates,
                                               String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        // Add default templates to database and then to registry.
        notificationTemplateDAOImpl.addDefaultNotificationTemplates(notificationTemplates, notificationChannel, tenantDomain);
        return notificationTemplateRegistryDAOImpl.addDefaultNotificationTemplates(notificationTemplates, notificationChannel, tenantDomain);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                           String tenantDomain) throws NotificationTemplateManagerException {

        // Delete the template from database and then from registry.
        notificationTemplateDAOImpl.deleteNotificationTemplate(displayName, locale, notificationChannel, tenantDomain);
        notificationTemplateRegistryDAOImpl.deleteNotificationTemplate(displayName, locale, notificationChannel, tenantDomain);
    }

    /**
     * @inheritDoc
     */
    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String tenantDomain)
            throws NotificationTemplateManagerException {

        if(notificationTemplateDAOImpl.isNotificationTemplateExists(displayName, locale, notificationChannel, tenantDomain)) {
            // Get the template from database.
            return notificationTemplateDAOImpl.getNotificationTemplate(displayName, locale, notificationChannel, tenantDomain);
        } else if(notificationTemplateRegistryDAOImpl.isNotificationTemplateExists(displayName, locale, notificationChannel, tenantDomain)) {
            // Get the template from registry.
            return notificationTemplateRegistryDAOImpl.getNotificationTemplate(displayName, locale, notificationChannel, tenantDomain);
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String tenantDomain) throws NotificationTemplateManagerException {

        // Check if the template exist in database and then in registry.
        return notificationTemplateDAOImpl.isNotificationTemplateExists(displayName, locale, notificationChannel, tenantDomain) ||
                notificationTemplateRegistryDAOImpl.isNotificationTemplateExists(displayName, locale, notificationChannel, tenantDomain);
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws NotificationTemplateManagerException {

        List<EmailTemplate> emailTemplates = new ArrayList<>();
        // Get the list of all template types.
        List<String> templateTypes = getNotificationTemplateTypes(NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);

        // Get all the templates for each template type.
        for (String templateType : templateTypes) {
            emailTemplates.addAll(getEmailTemplates(templateType, tenantDomain));
        }
        return emailTemplates;
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<EmailTemplate> getEmailTemplates(String templateType, String tenantDomain)
            throws NotificationTemplateManagerException {

        // Get all the templates from database and then from registry.
        List<EmailTemplate> emailTemplates = notificationTemplateDAOImpl.getEmailTemplates(templateType, tenantDomain);
        List<EmailTemplate> registryEmailTemplates = notificationTemplateRegistryDAOImpl.getEmailTemplates(templateType, tenantDomain);

        List<String> locales = emailTemplates.stream().map(EmailTemplate::getLocale).collect(Collectors.toList());

        // Add the templates from registry if they are not already in the list.
        for (EmailTemplate registryEmailTemplate : registryEmailTemplates) {
            if (!locales.contains(registryEmailTemplate.getLocale())) {
                emailTemplates.add(registryEmailTemplate);
            }
        }
        return emailTemplates;
    }
}
