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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.EmailTemplateManagerImpl;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.dao.NotificationTemplateDAO;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtClientException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerClientException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerInternalException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.EMAIL_TEMPLATE_PATH;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.EMAIL_TEMPLATE_TYPE_DISPLAY_NAME;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NOT_FOUND;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.SMS_TEMPLATE_PATH;
import static org.wso2.carbon.registry.core.RegistryConstants.PATH_SEPARATOR;

public class NotificationTemplateRegistryDAOImpl  implements NotificationTemplateDAO {

    private static final Log log = LogFactory.getLog(NotificationTemplateRegistryDAOImpl.class);

    private I18nMgtDataHolder dataHolder = I18nMgtDataHolder.getInstance();
    private RegistryResourceMgtService resourceMgtService = dataHolder.getRegistryResourceMgtService();

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        String normalizedDisplayName = I18nEmailUtil.getNormalizedName(displayName);

        // Persist the template type to registry ie. create a directory.
        String path = buildTemplateRootDirectoryPath(normalizedDisplayName, notificationChannel);
        try {
            // Check whether a template exists with the same name.
            if (resourceMgtService.isResourceExists(path, tenantDomain)) {
                throw new NotificationTemplateManagerInternalException("Template type already exists.");
            }
            Collection collection = I18nEmailUtil.createTemplateType(normalizedDisplayName, displayName);
            resourceMgtService.putIdentityResource(collection, path, tenantDomain);
        } catch (IdentityRuntimeException e) {
            throw new NotificationTemplateManagerServerException("Error while adding notification template type.", e);
        }
    }

    @Override
    public void deleteNotificationTemplateTypeByName(String displayName, String notificationChannel,
                                                     String tenantDomain) throws NotificationTemplateManagerException {

        String templateType = I18nEmailUtil.getNormalizedName(displayName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateType;
        try {
            resourceMgtService.deleteIdentityResource(path, tenantDomain);
        } catch (IdentityRuntimeException e) {
            throw new NotificationTemplateManagerServerException("Error while deleting notification template type.", e);
        }
    }

    @Override
    public List<String> getNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        try {
            List<String> templateTypeList = new ArrayList<>();
            Collection collection = (Collection) resourceMgtService.getIdentityResource(EMAIL_TEMPLATE_PATH,
                    tenantDomain);

            for (String templatePath : collection.getChildren()) {
                Resource templateTypeResource = resourceMgtService.getIdentityResource(templatePath, tenantDomain);
                if (templateTypeResource != null) {
                    String emailTemplateType = templateTypeResource.getProperty(EMAIL_TEMPLATE_TYPE_DISPLAY_NAME);
                    templateTypeList.add(emailTemplateType);
                }
            }
            return templateTypeList;
        } catch (IdentityRuntimeException | RegistryException ex) {
            throw new NotificationTemplateManagerServerException("Error while retrieving notification template types.",
                        ex);
        }
    }

    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        // get template directory name from display name.
        String normalizedTemplateName = I18nEmailUtil.getNormalizedName(displayName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + normalizedTemplateName;

        try {
            Resource templateType = resourceMgtService.getIdentityResource(path, tenantDomain);
            return templateType != null;
        } catch (IdentityRuntimeException e) {
            throw new NotificationTemplateManagerException("Error while checking notification template type.", e);
        }
    }

    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        EmailTemplateManagerImpl.validateNotificationTemplate(notificationTemplate);
        String notificationChannel = notificationTemplate.getNotificationChannel();

        Resource templateResource = createTemplateRegistryResource(notificationTemplate);
        String displayName = notificationTemplate.getDisplayName();
        String type = I18nEmailUtil.getNormalizedName(displayName);
        String locale = notificationTemplate.getLocale();

        String path = buildTemplateRootDirectoryPath(type, notificationChannel);
        try {
            // Check whether a template type root directory exists.
            if (!resourceMgtService.isResourceExists(path, tenantDomain)) {
                // Add new template type with relevant properties.
                addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
                if (log.isDebugEnabled()) {
                    String msg = "Creating template type : %s in tenant registry : %s";
                    log.debug(String.format(msg, displayName, tenantDomain));
                }
            }
            resourceMgtService.putIdentityResource(templateResource, path, tenantDomain, locale);
        } catch (IdentityRuntimeException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getMessage(),
                            displayName, locale, tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message);
        }
    }

    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        String notificationChannel = notificationTemplate.getNotificationChannel();

        Resource templateResource = createTemplateRegistryResource(notificationTemplate);
        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();
        String type = I18nEmailUtil.getNormalizedName(displayName);

        String path = buildTemplateRootDirectoryPath(type, notificationChannel);

        try {
            // Check whether a template type root directory exists.
            if (!resourceMgtService.isResourceExists(path, tenantDomain)) {
                // Add new template type with relevant properties.
                addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
                if (log.isDebugEnabled()) {
                    String msg = "Creating template type : %s in tenant registry : %s";
                    log.debug(String.format(msg, displayName, tenantDomain));
                }
            }
            resourceMgtService.putIdentityResource(templateResource, path, tenantDomain, locale);
        } catch (IdentityRuntimeException e) {
            throw new NotificationTemplateManagerServerException("Error while adding notification template.", e);
        }

    }

    @Override
    public int addDefaultNotificationTemplates(List<NotificationTemplate> notificationTemplates,
                                               String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        int numberOfAddedTemplates = 0;

        try {
            for (NotificationTemplate template : notificationTemplates) {
                String displayName = template.getDisplayName();
                String type = I18nEmailUtil.getNormalizedName(displayName);
                String locale = template.getLocale();
                String path = buildTemplateRootDirectoryPath(type, notificationChannel);

            /*Check for existence of each category, since some template may have migrated from earlier version
            This will also add new template types provided from file, but won't update any existing template*/
                if (!resourceMgtService.isResourceExists(addLocaleToTemplateTypeResourcePath(path, locale),
                        tenantDomain)) {
                    try {
                        addNotificationTemplate(template, tenantDomain);
                        if (log.isDebugEnabled()) {
                            String msg = "Default template added to %s tenant registry : %n%s";
                            log.debug(String.format(msg, tenantDomain, template.toString()));
                        }
                        numberOfAddedTemplates++;
                    } catch (NotificationTemplateManagerInternalException e) {
                        log.warn("Template : " + displayName + "already exists in the registry. Hence " +
                                "ignoring addition");
                    }
                }
            }
        } catch (IdentityRuntimeException e) {
            throw new NotificationTemplateManagerServerException("Error while adding default notification templates.", e);
        }
        return numberOfAddedTemplates;
    }

    @Override
    public void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                           String tenantDomain) throws NotificationTemplateManagerException {

        String templateType = I18nEmailUtil.getNormalizedName(displayName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateType;

        try {
            resourceMgtService.deleteIdentityResource(path, tenantDomain, locale);
        } catch (IdentityRuntimeException ex) {
            throw new NotificationTemplateManagerServerException("Error while deleting notification template.", ex);
        }
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String tenantDomain)
            throws NotificationTemplateManagerException {

        // Get notification template registry path.
        String path;
        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            path = SMS_TEMPLATE_PATH + PATH_SEPARATOR + I18nEmailUtil.getNormalizedName(displayName);
        } else {
            path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + I18nEmailUtil.getNormalizedName(displayName);
        }

        // Get registry resource.
        try {
            Resource registryResource = resourceMgtService.getIdentityResource(path, tenantDomain, locale);
            if (registryResource != null) {
                return getNotificationTemplate(registryResource, notificationChannel);
            }
        } catch (IdentityRuntimeException exception) {
            throw new NotificationTemplateManagerServerException("Error while retrieving notification template.", exception);
        }

        return null;
    }

    @Override
    public List<EmailTemplate> getEmailTemplates(String displayName, String tenantDomain)
            throws NotificationTemplateManagerException {

        String templateDirectory = I18nEmailUtil.getNormalizedName(displayName);
        String templateTypeRegistryPath = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateDirectory;

        List<EmailTemplate> templateList = new ArrayList<>();
        Collection templateType = (Collection) resourceMgtService.getIdentityResource(templateTypeRegistryPath,
                tenantDomain);

        if (templateType == null) {
            throw new NotificationTemplateManagerInternalException("No template type found for the given display name.");
        }
        try {
            for (String template : templateType.getChildren()) {
                Resource templateResource = resourceMgtService.getIdentityResource(template, tenantDomain);
                if (templateResource != null) {
                    try {
                        EmailTemplate templateDTO = I18nEmailUtil.getEmailTemplate(templateResource);
                        templateList.add(templateDTO);
                    } catch (I18nEmailMgtException ex) {
                        log.error("Failed retrieving a template object from the registry resource", ex);
                    }
                }
            }
        } catch (RegistryException e) {
            throw new NotificationTemplateManagerServerException("Error while retrieving notification templates.", e);
        }

        return templateList;
    }

    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String tenantDomain) throws NotificationTemplateManagerException {

        // get template directory name from display name.
        String normalizedTemplateName = I18nEmailUtil.getNormalizedName(displayName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + normalizedTemplateName +
                PATH_SEPARATOR + locale.toLowerCase();

        try {
            Resource template = resourceMgtService.getIdentityResource(path, tenantDomain);
            return template != null;
        } catch (IdentityRuntimeException e) {
            throw new NotificationTemplateManagerException("Error while checking notification template existence.", e);
        }
    }

    @Override
    public List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws NotificationTemplateManagerException {

        List<EmailTemplate> templateList = new ArrayList<>();
        try {
            Collection baseDirectory = (Collection) resourceMgtService.getIdentityResource(EMAIL_TEMPLATE_PATH,
                    tenantDomain);

            if (baseDirectory != null) {
                for (String templateTypeDirectory : baseDirectory.getChildren()) {
                    templateList.addAll(
                            getAllTemplatesOfTemplateTypeFromRegistry(templateTypeDirectory, tenantDomain));
                }
            }
        } catch (RegistryException | I18nEmailMgtClientException e) {
            throw new NotificationTemplateManagerServerException("Error while retrieving email templates.", e);
        }
        return templateList;
    }

    // Private methods

    /**
     * Create a registry resource instance of the notification template.
     *
     * @param notificationTemplate Notification template
     * @return Resource
     * @throws NotificationTemplateManagerServerException If an error occurred while creating the resource
     */
    private Resource createTemplateRegistryResource(NotificationTemplate notificationTemplate)
            throws NotificationTemplateManagerServerException {

        String displayName = notificationTemplate.getDisplayName();
        String type = I18nEmailUtil.getNormalizedName(displayName);
        String locale = notificationTemplate.getLocale();
        String body = notificationTemplate.getBody();

        // Set template properties.
        Resource templateResource = new ResourceImpl();
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME, displayName);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_TYPE, type);
        templateResource.setProperty(I18nMgtConstants.TEMPLATE_LOCALE, locale);
        String[] templateContent;
        // Handle contents according to different channel types.
        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationTemplate.getNotificationChannel())) {
            templateContent = new String[]{notificationTemplate.getSubject(), body, notificationTemplate.getFooter()};
            templateResource.setProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE, notificationTemplate.getContentType());
        } else {
            templateContent = new String[]{body};
        }
        templateResource.setMediaType(RegistryConstants.TAG_MEDIA_TYPE);
        String content = new Gson().toJson(templateContent);
        try {
            byte[] contentByteArray = content.getBytes(StandardCharsets.UTF_8);
            templateResource.setContent(contentByteArray);
        } catch (RegistryException e) {
            String code =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CREATING_REGISTRY_RESOURCE.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_CREATING_REGISTRY_RESOURCE.getMessage(),
                            displayName, locale);
            throw new NotificationTemplateManagerServerException(code, message, e);
        }
        return templateResource;
    }

    /**
     * Get the notification template from resource.
     *
     * @param templateResource    {@link org.wso2.carbon.registry.core.Resource} object
     * @param notificationChannel Notification channel
     * @return {@link org.wso2.carbon.identity.governance.model.NotificationTemplate} object
     * @throws NotificationTemplateManagerException Error getting the notification template
     */
    private NotificationTemplate getNotificationTemplate(Resource templateResource, String notificationChannel)
            throws NotificationTemplateManagerException {

        NotificationTemplate notificationTemplate = new NotificationTemplate();

        // Get template meta properties.
        String displayName = templateResource.getProperty(I18nMgtConstants.TEMPLATE_TYPE_DISPLAY_NAME);
        String type = templateResource.getProperty(I18nMgtConstants.TEMPLATE_TYPE);
        String locale = templateResource.getProperty(I18nMgtConstants.TEMPLATE_LOCALE);
        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationChannel)) {
            String contentType = templateResource.getProperty(I18nMgtConstants.TEMPLATE_CONTENT_TYPE);

            // Setting UTF-8 for all the email templates as it supports many languages and is widely adopted.
            // There is little to no value addition making the charset configurable.
            if (contentType != null && !contentType.toLowerCase().contains(I18nEmailUtil.CHARSET_CONSTANT)) {
                contentType = contentType + "; " + I18nEmailUtil.CHARSET_UTF_8;
            }
            notificationTemplate.setContentType(contentType);
        }
        notificationTemplate.setDisplayName(displayName);
        notificationTemplate.setType(type);
        notificationTemplate.setLocale(locale);

        // Process template content.
        String[] templateContentElements = getTemplateElements(templateResource, notificationChannel, displayName,
                locale);
        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            notificationTemplate.setBody(templateContentElements[0]);
        } else {
            notificationTemplate.setSubject(templateContentElements[0]);
            notificationTemplate.setBody(templateContentElements[1]);
            notificationTemplate.setFooter(templateContentElements[2]);
        }
        notificationTemplate.setNotificationChannel(notificationChannel);
        return notificationTemplate;
    }

    /**
     * Process template resource content and retrieve template elements.
     *
     * @param templateResource    Resource of the template
     * @param notificationChannel Notification channel
     * @param displayName         Display name of the template
     * @param locale              Locale of the template
     * @return Template content
     * @throws NotificationTemplateManagerException If an error occurred while getting the template content
     */
    private String[] getTemplateElements(Resource templateResource, String notificationChannel, String displayName,
                                         String locale) throws NotificationTemplateManagerException {

        try {
            Object content = templateResource.getContent();
            if (content != null) {
                byte[] templateContentArray = (byte[]) content;
                String templateContent = new String(templateContentArray, Charset.forName("UTF-8"));

                String[] templateContentElements;
                try {
                    templateContentElements = new Gson().fromJson(templateContent, String[].class);
                } catch (JsonSyntaxException exception) {
                    String error = String.format(IdentityMgtConstants.ErrorMessages.
                            ERROR_CODE_DESERIALIZING_TEMPLATE_FROM_TENANT_REGISTRY.getMessage(), displayName, locale);
                    throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.
                            ERROR_CODE_DESERIALIZING_TEMPLATE_FROM_TENANT_REGISTRY.getCode(), error, exception);
                }

                // Validate template content.
                if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
                    if (templateContentElements == null || templateContentElements.length != 1) {
                        String errorMsg = String.format(IdentityMgtConstants.ErrorMessages.
                                ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getMessage(), displayName, locale);
                        throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.
                                ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getCode(), errorMsg);
                    }
                } else {
                    if (templateContentElements == null || templateContentElements.length != 3) {
                        String errorMsg = String.format(IdentityMgtConstants.ErrorMessages.
                                ERROR_CODE_INVALID_EMAIL_TEMPLATE_CONTENT.getMessage(), displayName, locale);
                        throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.
                                ERROR_CODE_INVALID_EMAIL_TEMPLATE_CONTENT.getCode(), errorMsg);
                    }
                }
                return templateContentElements;
            } else {
                String error = String.format(IdentityMgtConstants.ErrorMessages.
                        ERROR_CODE_NO_CONTENT_IN_TEMPLATE.getMessage(), displayName, locale);
                throw new NotificationTemplateManagerClientException(IdentityMgtConstants.ErrorMessages.
                        ERROR_CODE_NO_CONTENT_IN_TEMPLATE.getCode(), error);
            }
        } catch (RegistryException exception) {
            String error = IdentityMgtConstants.ErrorMessages.
                    ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_OBJECT_FROM_REGISTRY.getMessage();
            throw new NotificationTemplateManagerServerException(IdentityMgtConstants.ErrorMessages.
                    ERROR_CODE_ERROR_RETRIEVING_TEMPLATE_OBJECT_FROM_REGISTRY.getCode(), error, exception);
        }
    }

    /**
     * Loop through all template resources of a given template type registry path and return a list of EmailTemplate
     * objects.
     *
     * @param templateTypeRegistryPath Registry path of the template type.
     * @param tenantDomain             Tenant domain.
     * @return List of extracted EmailTemplate objects.
     * @throws RegistryException if any error occurred.
     */
    private List<EmailTemplate> getAllTemplatesOfTemplateTypeFromRegistry(String templateTypeRegistryPath,
                                                                          String tenantDomain)
            throws RegistryException, I18nEmailMgtClientException {

        List<EmailTemplate> templateList = new ArrayList<>();
        Collection templateType = (Collection) resourceMgtService.getIdentityResource(templateTypeRegistryPath,
                tenantDomain);

        if (templateType == null) {
            String type = templateTypeRegistryPath.split(PATH_SEPARATOR)[
                    templateTypeRegistryPath.split(PATH_SEPARATOR).length - 1];
            String message =
                    String.format("Email Template Type: %s not found in %s tenant registry.", type, tenantDomain);
            throw new I18nEmailMgtClientException(EMAIL_TEMPLATE_TYPE_NOT_FOUND, message);
        }
        for (String template : templateType.getChildren()) {
            Resource templateResource = resourceMgtService.getIdentityResource(template, tenantDomain);
            if (templateResource != null) {
                try {
                    EmailTemplate templateDTO = I18nEmailUtil.getEmailTemplate(templateResource);
                    templateList.add(templateDTO);
                } catch (I18nEmailMgtException ex) {
                    log.error("Failed retrieving a template object from the registry resource", ex);
                }
            }
        }
        return templateList;
    }

    /**
     * Build the template type root directory path.
     *
     * @param type                Template type
     * @param notificationChannel Notification channel (SMS or EMAIL)
     * @return Root directory path
     */
    private String buildTemplateRootDirectoryPath(String type, String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return SMS_TEMPLATE_PATH + PATH_SEPARATOR + type;
        }
        return EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + type;
    }

    /**
     * Add the locale to the template type resource path.
     *
     * @param path  Email template path
     * @param locale Locale code of the email template
     * @return Email template resource path
     */
    private String addLocaleToTemplateTypeResourcePath(String path, String locale) {

        if (StringUtils.isNotBlank(locale)) {
            return path + PATH_SEPARATOR + locale.toLowerCase();
        } else {
            return path;
        }
    }
}
