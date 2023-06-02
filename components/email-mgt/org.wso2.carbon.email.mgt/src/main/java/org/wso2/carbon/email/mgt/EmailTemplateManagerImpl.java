/*
 * Copyright (c) 2016, WSO2 LLC. (http://www.wso2.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.email.mgt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.dao.NotificationTemplateDAO;
import org.wso2.carbon.email.mgt.dao.impl.NotificationTemplateDAOImpl;
import org.wso2.carbon.email.mgt.dao.impl.NotificationTemplateMigrationDAOImpl;
import org.wso2.carbon.email.mgt.dao.impl.NotificationTemplateRegistryDAOImpl;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtClientException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtInternalException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.base.IdentityValidationUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.governance.IdentityGovernanceUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerClientException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerInternalException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.DEFAULT_EMAIL_LOCALE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.DEFAULT_SMS_NOTIFICATION_LOCALE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.EMAIL_TEMPLATE_NAME;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.EMAIL_TEMPLATE_TYPE_REGEX;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NOT_FOUND;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.buildEmailTemplate;
import static org.wso2.carbon.identity.base.IdentityValidationUtil.ValidatorPattern.REGISTRY_INVALID_CHARS_EXISTS;

/**
 * Provides functionality to manage email templates used in notification emails.
 */
public class EmailTemplateManagerImpl implements EmailTemplateManager, NotificationTemplateManager {

    private final NotificationTemplateDAO notificationTemplateDAO;
    private static final Log log = LogFactory.getLog(EmailTemplateManagerImpl.class);

    private static final String TEMPLATE_REGEX_KEY = I18nMgtConstants.class.getName() + "_" + EMAIL_TEMPLATE_NAME;
    private static final String REGISTRY_INVALID_CHARS = I18nMgtConstants.class.getName() + "_" + "registryInvalidChar";
    private static final String EMAIL_TEMPLATE_LOCATION_CONFIG = "RegistryDataStoreLocation.EmailTemplates";

    static {
        IdentityValidationUtil.addPattern(TEMPLATE_REGEX_KEY, EMAIL_TEMPLATE_TYPE_REGEX);
        IdentityValidationUtil.addPattern(REGISTRY_INVALID_CHARS, REGISTRY_INVALID_CHARS_EXISTS.getRegex());
    }

    public EmailTemplateManagerImpl() {

        String emailTemplatesDatabase = IdentityUtil.getProperty(EMAIL_TEMPLATE_LOCATION_CONFIG);
        if (StringUtils.isBlank(emailTemplatesDatabase)) {
            emailTemplatesDatabase = "registry";
        }
        this.notificationTemplateDAO = createNotificationTemplateDAO(emailTemplatesDatabase);
    }

    private NotificationTemplateDAO createNotificationTemplateDAO(String emailTemplatesDatabase) {
        switch (emailTemplatesDatabase) {
            case "database":
                return new NotificationTemplateDAOImpl();
            case "on_migration":
                return new NotificationTemplateMigrationDAOImpl();
            default:
                return new NotificationTemplateRegistryDAOImpl();
        }
    }

    @Override
    public void addEmailTemplateType(String emailTemplateDisplayName, String tenantDomain) throws
            I18nEmailMgtException {

        try {
            addNotificationTemplateType(emailTemplateDisplayName, NotificationChannels.EMAIL_CHANNEL.getChannelType(),
                    tenantDomain);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode())) {
                    throw new I18nEmailMgtInternalException(
                            I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtInternalException(e.getMessage(), e);
        } catch (NotificationTemplateManagerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        validateDisplayNameOfTemplateType(displayName);

        try {
            notificationTemplateDAO.addNotificationTemplateType(displayName, notificationChannel, tenantDomain);

        } catch (NotificationTemplateManagerServerException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getMessage(),
                            displayName, tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message);
        } catch (NotificationTemplateManagerInternalException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getMessage(),
                            displayName, tenantDomain);
            throw new NotificationTemplateManagerInternalException(code, message);
        }
    }

    @Override
    public void deleteEmailTemplateType(String emailTemplateDisplayName, String tenantDomain) throws
            I18nEmailMgtException {

        validateTemplateType(emailTemplateDisplayName, tenantDomain);

        try {
            notificationTemplateDAO.deleteNotificationTemplateTypeByName(emailTemplateDisplayName,
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerException ex) {
            String errorMsg = String.format
                    ("Error deleting email template type %s from %s tenant.", emailTemplateDisplayName, tenantDomain);
            handleServerException(errorMsg, ex);
        }
    }

    /**
     * @param tenantDomain
     * @return
     * @throws I18nEmailMgtServerException
     */
    @Override
    public List<String> getAvailableTemplateTypes(String tenantDomain) throws I18nEmailMgtServerException {

        try {
            return notificationTemplateDAO.getNotificationTemplateTypes(
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerException ex) {
            String errorMsg = String.format("Error when retrieving email template types of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(errorMsg, ex);
        }
    }

    @Override
    public List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws I18nEmailMgtException {


        try {
            return notificationTemplateDAO.getAllEmailTemplates(tenantDomain);
        } catch (NotificationTemplateManagerException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(error, e);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        try {
            NotificationTemplate notificationTemplate = getNotificationTemplate(
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), templateDisplayName, locale, tenantDomain);
            return buildEmailTemplate(notificationTemplate);
        } catch (NotificationTemplateManagerException exception) {
            String errorCode = exception.getErrorCode();
            String errorMsg = exception.getMessage();
            Throwable throwable = exception.getCause();

            // Match NotificationTemplateManagerExceptions with the existing I18nEmailMgtException error types.
            if (StringUtils.isNotEmpty(exception.getErrorCode())) {
                if (IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_NOTIFICATION_TEMPLATE.getCode()
                        .equals(errorCode) || IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_CONTENT_IN_TEMPLATE
                        .getCode().equals(errorCode) ||
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode()
                                .equals(errorCode) ||
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE
                                .getCode().equals(errorCode)) {
                    throw new I18nEmailMgtClientException(errorMsg, throwable);

                } else if (IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE_CONTENT.getCode()
                        .equals(errorCode)) {
                    throw new I18nMgtEmailConfigException(errorMsg, throwable);
                } else if (IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getCode()
                        .equals(errorCode)) {
                    throw new I18nEmailMgtInternalException(I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NODE_FOUND,
                            errorMsg, throwable);
                }
            }
            throw new I18nEmailMgtServerException(exception.getMessage(), exception.getCause());
        }
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        validateTemplateType(templateDisplayName, tenantDomain);

        try {
            return notificationTemplateDAO.getEmailTemplates(templateDisplayName, tenantDomain);
        } catch (NotificationTemplateManagerInternalException ex) {
            String message =
                    String.format("Email Template Type: %s not found in %s tenant registry.", templateDisplayName, tenantDomain);
            throw new I18nEmailMgtException(EMAIL_TEMPLATE_TYPE_NOT_FOUND, message);
        } catch (NotificationTemplateManagerException ex) {
            String error = "Error when retrieving '%s' template type from %s tenant registry.";
            throw new I18nEmailMgtServerException(String.format(error, templateDisplayName, tenantDomain), ex);
        }
    }

    /**
     * Get default notification template locale for a given notification channel.
     *
     * @param notificationChannel Notification channel
     * @return Default locale
     */
    private String getDefaultNotificationLocale(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return DEFAULT_SMS_NOTIFICATION_LOCALE;
        } else {
            return DEFAULT_EMAIL_LOCALE;
        }
    }

    /**
     * Return the notification template from the tenant registry which matches the given channel and template name.
     *
     * @param notificationChannel Notification Channel Name (Eg: SMS or EMAIL)
     * @param templateType        Type of the template
     * @param locale              Locale
     * @param tenantDomain        Tenant Domain
     * @return Return {@link org.wso2.carbon.identity.governance.model.NotificationTemplate} object
     * @throws NotificationTemplateManagerException Error getting the notification template
     */
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale,
            String tenantDomain) throws NotificationTemplateManagerException {

        // Resolve channel to either SMS or EMAIL.
        notificationChannel = resolveNotificationChannel(notificationChannel);
        validateTemplateLocale(locale);
        validateDisplayNameOfTemplateType(templateType);

        // Get notification template.
        NotificationTemplate notificationTemplate = notificationTemplateDAO.getNotificationTemplate(templateType, locale,
                notificationChannel, tenantDomain);

        // Handle not having the requested SMS template type in required locale for this tenantDomain.
        if (notificationTemplate == null) {
            String defaultLocale = getDefaultNotificationLocale(notificationChannel);
            if (StringUtils.equalsIgnoreCase(defaultLocale, locale)) {

                // Template is not available in the default locale. Therefore, breaking the flow at the consuming side
                // to avoid NPE.
                String error = String
                        .format(IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getMessage(),
                                templateType, locale, tenantDomain);
                throw new NotificationTemplateManagerServerException(
                        IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getCode(), error);
            } else {
                if (log.isDebugEnabled()) {
                    String message = String
                            .format("'%s' template in '%s' locale was not found in '%s' tenant. Trying to return the "
                                            + "template in default locale : '%s'", templateType, locale, tenantDomain,
                                    DEFAULT_SMS_NOTIFICATION_LOCALE);
                    log.debug(message);
                }
                // Try to get the template type in default locale.
                return getNotificationTemplate(notificationChannel, templateType, defaultLocale, tenantDomain);
            }
        }
        return notificationTemplate;
    }

    /**
     * Resolve notification channel to a server supported notification channel.
     *
     * @param notificationChannel Notification channel
     * @return Notification channel (EMAIL or SMS)
     */
    private String resolveNotificationChannel(String notificationChannel) {

        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equals(notificationChannel)) {
            return notificationChannel;
        } else if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return notificationChannel;
        } else {
            if (log.isDebugEnabled()) {
                String message = String.format("Notification channel : %s is not supported by the server. "
                                + "Notification channel changed to : %s", notificationChannel,
                        IdentityGovernanceUtil.getDefaultNotificationChannel());
                log.debug(message);
            }
            return IdentityGovernanceUtil.getDefaultNotificationChannel();
        }
    }

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        validateNotificationTemplate(notificationTemplate);

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();

        try {
            notificationTemplateDAO.addOrUpdateNotificationTemplate(notificationTemplate, tenantDomain);
        } catch (NotificationTemplateManagerException e) {
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
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain) throws I18nEmailMgtException {

        NotificationTemplate notificationTemplate = buildNotificationTemplateFromEmailTemplate(emailTemplate);
        try {
            addNotificationTemplate(notificationTemplate, tenantDomain);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode())) {
                    throw new I18nEmailMgtInternalException(
                            I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtInternalException(e.getMessage(), e);
        } catch (NotificationTemplateManagerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }


    @Override
    public void deleteEmailTemplate(String templateTypeName, String localeCode, String tenantDomain) throws
            I18nEmailMgtException {
        // validate the name and locale code.
        if (StringUtils.isBlank(templateTypeName)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email displayName cannot be null.");
        }

        if (StringUtils.isBlank(localeCode)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email locale cannot be null.");
        }

        String channelType = NotificationChannels.EMAIL_CHANNEL.getChannelType();

        try {
            notificationTemplateDAO.deleteNotificationTemplate(templateTypeName, localeCode, channelType, tenantDomain);
        } catch (NotificationTemplateManagerException ex) {
            String msg = String.format("Error deleting %s:%s template from %s tenant registry.", templateTypeName,
                    localeCode, tenantDomain);
            handleServerException(msg, ex);
        }
    }

    @Override
    public void addDefaultEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        try {
            addDefaultNotificationTemplates(NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode())) {
                    throw new I18nEmailMgtInternalException(
                            I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtInternalException(e.getMessage(), e);
        } catch (NotificationTemplateManagerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    /**
     * Add the default notification templates which matches the given notification channel to the respective tenants
     * registry.
     *
     * @param notificationChannel Notification channel (Eg: SMS, EMAIL)
     * @param tenantDomain Tenant domain
     * @throws NotificationTemplateManagerException Error adding the default notification templates
     */
    @Override
    public void addDefaultNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        // Get the list of Default notification templates.
        List<NotificationTemplate> notificationTemplates =
                getDefaultNotificationTemplates(notificationChannel);
        try {
            int numberOfAddedTemplates = notificationTemplateDAO.addDefaultNotificationTemplates(notificationTemplates,
                    notificationChannel, tenantDomain);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Added %d default %s templates to the tenant registry : %s",
                        numberOfAddedTemplates, notificationChannel, tenantDomain));
            }
        } catch (NotificationTemplateManagerException ex) {
            String error = "Error when tried to check for default email templates in tenant registry : %s";
            log.error(String.format(error, tenantDomain), ex);
        }
    }

    /**
     * Get the notification templates which matches the given notification template type.
     *
     * @param notificationChannel Notification channel type. (Eg: EMAIL, SMS)
     * @return List of default notification templates
     */
    @Override
    public List<NotificationTemplate> getDefaultNotificationTemplates(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return I18nMgtDataHolder.getInstance().getDefaultSMSTemplates();
        }
        return I18nMgtDataHolder.getInstance().getDefaultEmailTemplates();
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        try {
            return notificationTemplateDAO.isNotificationTemplateExists(templateTypeDisplayName, locale,
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(error, e);
        }
    }

    @Override
    public boolean isEmailTemplateTypeExists(String templateTypeDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        try {
            return notificationTemplateDAO.isNotificationTemplateTypeExists(templateTypeDisplayName,
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(error, e);
        }
    }

    /**
     * Validate the attributes of a notification template.
     *
     * @param notificationTemplate Notification template
     * @throws NotificationTemplateManagerClientException Invalid notification template.
     */
     public static void validateNotificationTemplate(NotificationTemplate notificationTemplate)
            throws NotificationTemplateManagerClientException {

        if (notificationTemplate == null) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_NULL_TEMPLATE_OBJECT.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_NULL_TEMPLATE_OBJECT.getMessage());
        }
        String displayName = notificationTemplate.getDisplayName();
        validateDisplayNameOfTemplateType(displayName);
        String normalizedDisplayName = I18nEmailUtil.getNormalizedName(displayName);
        if (!StringUtils.equalsIgnoreCase(normalizedDisplayName, notificationTemplate.getType())) {
            if (log.isDebugEnabled()) {
                String message = String.format("In the template normalizedDisplayName : %s is not equal to the " +
                                "template type : %s. Therefore template type is sent to : %s", normalizedDisplayName,
                        notificationTemplate.getType(), normalizedDisplayName);
                log.debug(message);
            }
            notificationTemplate.setType(normalizedDisplayName);
        }
        validateTemplateLocale(notificationTemplate.getLocale());
        String body = notificationTemplate.getBody();
        String subject = notificationTemplate.getSubject();
        String footer = notificationTemplate.getFooter();
        if (StringUtils.isBlank(notificationTemplate.getNotificationChannel())) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_CHANNEL.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_CHANNEL.getMessage());
        }
        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationTemplate.getNotificationChannel())) {
            if (StringUtils.isBlank(body)) {
                String errorCode =
                        I18nEmailUtil.prependOperationScenarioToErrorCode(
                                I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE.getCode(),
                                I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
                throw new NotificationTemplateManagerClientException(errorCode,
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE.getMessage());
            }
            if (StringUtils.isNotBlank(subject) || StringUtils.isNotBlank(footer)) {
                String errorCode =
                        I18nEmailUtil.prependOperationScenarioToErrorCode(
                                I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getCode(),
                                I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
                throw new NotificationTemplateManagerClientException(errorCode,
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SMS_TEMPLATE_CONTENT.getMessage());
            }
        } else {
            if (StringUtils.isBlank(subject) || StringUtils.isBlank(body) || StringUtils.isBlank(footer)) {
                String errorCode =
                        I18nEmailUtil.prependOperationScenarioToErrorCode(
                                I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE.getCode(),
                                I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
                throw new NotificationTemplateManagerClientException(errorCode,
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE.getMessage());
            }
        }
    }

    /**
     * Validate the displayName of a template type.
     *
     * @param templateDisplayName Display name of the notification template
     * @throws I18nEmailMgtClientException Invalid notification template
     */
    private static void validateTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtClientException {

        try {
            validateDisplayNameOfTemplateType(templateDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode())) {
                    throw new I18nEmailMgtClientException("Template Type display name cannot be null", e);
                }
                if (errorCode.contains(
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode())) {
                    throw new I18nEmailMgtClientException(e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtClientException("Invalid notification template", e);
        }
    }

    private void handleServerException(String errorMsg, Throwable ex) throws I18nEmailMgtServerException {

        log.error(errorMsg);
        throw new I18nEmailMgtServerException(errorMsg, ex);
    }

    /**
     * Validate the display name of the notification template.
     *
     * @param displayName Display name
     * @throws NotificationTemplateManagerClientException Invalid notification template name
     */
    private static void validateDisplayNameOfTemplateType(String displayName)
            throws NotificationTemplateManagerClientException {

        if (StringUtils.isBlank(displayName)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getMessage());
        }
        /*Template name can contain only alphanumeric characters and spaces, it can't contain registry invalid
        characters*/
        String[] whiteListPatterns = {TEMPLATE_REGEX_KEY};
        String[] blackListPatterns = {REGISTRY_INVALID_CHARS};
        if (!IdentityValidationUtil.isValid(displayName, whiteListPatterns, blackListPatterns)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getMessage(),
                            displayName);
            throw new NotificationTemplateManagerClientException(errorCode, message);
        }
    }

    /**
     * Validates the locale code of a notification template.
     *
     * @param locale Locale code
     * @throws NotificationTemplateManagerClientException Invalid notification template
     */
    private static void validateTemplateLocale(String locale) throws NotificationTemplateManagerClientException {

        if (StringUtils.isBlank(locale)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_LOCALE.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            throw new NotificationTemplateManagerClientException(errorCode,
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_LOCALE.getMessage());
        }
        // Regex check for registry invalid chars.
        if (!IdentityValidationUtil.isValidOverBlackListPatterns(locale, REGISTRY_INVALID_CHARS)) {
            String errorCode =
                    I18nEmailUtil.prependOperationScenarioToErrorCode(
                            I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE.getCode(),
                            I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE.getMessage(),
                            locale);
            throw new NotificationTemplateManagerClientException(errorCode, message);
        }
    }

    /**
     * Build notification template model from the email template attributes.
     *
     * @param emailTemplate EmailTemplate
     * @return NotificationTemplate
     */
    private NotificationTemplate buildNotificationTemplateFromEmailTemplate(EmailTemplate emailTemplate) {

        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setNotificationChannel(NotificationChannels.EMAIL_CHANNEL.getChannelType());
        notificationTemplate.setSubject(emailTemplate.getSubject());
        notificationTemplate.setBody(emailTemplate.getBody());
        notificationTemplate.setFooter(emailTemplate.getFooter());
        notificationTemplate.setType(emailTemplate.getTemplateType());
        notificationTemplate.setDisplayName(emailTemplate.getTemplateDisplayName());
        notificationTemplate.setLocale(emailTemplate.getLocale());
        notificationTemplate.setContentType(emailTemplate.getEmailContentType());
        return notificationTemplate;
    }
}
