/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.email.mgt.internal;

import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.List;

public class I18nMgtDataHolder{
    private RealmService realmService;
    private RegistryService registryService;
    private ConfigurationManager configurationManager;
    private RegistryResourceMgtService registryResourceMgtService;
    private List<NotificationTemplate> defaultEmailTemplates = new ArrayList<>();
    private List<NotificationTemplate> defaultSMSTemplates = new ArrayList<>();

    private static I18nMgtDataHolder instance = new I18nMgtDataHolder();

    private I18nMgtDataHolder() {
    }

    public static I18nMgtDataHolder getInstance() {
        return instance;
    }

    public RealmService getRealmService() {
        if (realmService == null) {
            throw new RuntimeException("Realm Service has not been set. Component has not initialized properly.");
        }
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public ConfigurationManager getConfigurationManager() {

        return configurationManager;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {

        this.configurationManager = configurationManager;
    }

    public RegistryService getRegistryService() {
        if (registryService == null) {
            throw new RuntimeException("Registry Service has not been set. Component has not initialized properly.");
        }
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public RegistryResourceMgtService getRegistryResourceMgtService() {
        if (registryResourceMgtService == null) {
            throw new RuntimeException("Registry Resource Mgt Service has not been set." +
                    "Component has not initialized properly.");
        }
        return registryResourceMgtService;
    }

    public void setRegistryResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {
        this.registryResourceMgtService = registryResourceMgtService;
    }

    public void setDefaultEmailTemplates(List<NotificationTemplate> defaultEmailTemplates) {

        this.defaultEmailTemplates = defaultEmailTemplates;
    }

    public List<NotificationTemplate> getDefaultEmailTemplates() {

        return defaultEmailTemplates;
    }

    public void setDefaultSMSTemplates(List<NotificationTemplate> defaultEmailTemplates) {

        this.defaultSMSTemplates = defaultEmailTemplates;
    }

    public List<NotificationTemplate> getDefaultSMSTemplates() {

        return defaultSMSTemplates;
    }
}
