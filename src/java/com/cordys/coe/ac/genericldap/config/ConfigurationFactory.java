/*
 * Copyright 2007 Cordys R&D B.V. 
 *
 *   This file is part of the Cordys Generic LDAP Connector. 
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.cordys.coe.ac.genericldap.config;

import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;

/**
 * This factory creates the configuration object that is to be used.
 *
 * @author  pgussow
 */
public class ConfigurationFactory
{
    /**
     * This method creates the configuration object based on the source XML.
     *
     * @param   configuration  The configuration XML.
     *
     * @return  The created configuration object.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     */
    public static IGenLDAPConfiguration createConfiguration(int configuration)
                                                     throws GenericLDAPConnectorException
    {
        return new GenericLDAPConfiguration(configuration);
    }
}
