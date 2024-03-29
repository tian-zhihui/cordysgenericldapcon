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
package com.cordys.coe.ac.genericldap.soap.impl;

import com.cordys.coe.ac.genericldap.EDynamicAction;
import com.cordys.coe.ac.genericldap.connection.IConnectionManager;
import com.cordys.coe.ac.genericldap.exception.GenericLDAPConnectorException;
import com.cordys.coe.ac.genericldap.localization.GenLDAPExceptionMessages;
import com.cordys.coe.ac.genericldap.soap.BaseMethod;
import com.cordys.coe.util.xml.nom.XPathHelper;

import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPathMetaInfo;

import com.novell.ldap.LDAPEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class analyzes the implementation XML of the GetLDAPObject action.
 *
 * @author  pgussow
 */
public class GetLDAPObjectImpl extends BaseImplementation
{
    /**
     * Holds the name of the parameter 'dn'.
     */
    protected static final String PARAM_DN = "dn";
    /**
     * This method holds the definition of the returning attributes.
     */
    private ReturnAttributes m_returnAttributes;

    /**
     * Creates a new GetLDAPObjectImpl object.
     *
     * @param   implementation  The implementation XML.
     *
     * @throws  GenericLDAPConnectorException  In case the implementation is incorrect.
     */
    public GetLDAPObjectImpl(int implementation)
                      throws GenericLDAPConnectorException
    {
        this(EDynamicAction.GET_LDAP_OBJECT, implementation);
    }

    /**
     * Creates a new GetLDAPObjectImpl object.
     *
     * @param   action          The actual action for this implementation.
     * @param   implementation  The implementation XML.
     *
     * @throws  GenericLDAPConnectorException  In case the implementation is incorrect.
     */
    public GetLDAPObjectImpl(EDynamicAction action, int implementation)
                      throws GenericLDAPConnectorException
    {
        super(action, implementation);

        int actionXML = Node.getFirstElement(implementation);
        
        // Parse the implementation XML.
        int dn = XPathHelper.selectSingleNode(actionXML, "impl:dn", m_xmi);

        if (dn == 0)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_MISSING_REQUEST_INFORMATION_0,
                                                    "dn");
        }

        RequestParameter param = RequestParameter.getInstance(dn);

        addRequestParameter(param);

        // Now we need to parse the return structure. It offers the possibility to define which
        // fields should be returned and possibly which type they have.
        int returnXML = XPathHelper.selectSingleNode(actionXML, "impl:return", m_xmi);

        m_returnAttributes = new ReturnAttributes(returnXML, m_xmi);
    }

    /**
     * This method gets the definition of the returning attributes..
     *
     * @return  The definition of the returning attributes..
     */
    public ReturnAttributes getReturnAttributes()
    {
        return m_returnAttributes;
    }

    /**
     * This method will execute the implementation against the actual method.
     *
     * <p>Note: Be aware that this method can be called by multiple threads simultaniously, so NO
     * member variables should be used to store data.</p>
     *
     * @param   method  The actual method.
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions.
     *
     * @see     com.cordys.coe.ac.genericldap.soap.impl.BaseImplementation#handleRequest(com.cordys.coe.ac.genericldap.soap.BaseMethod)
     */
    @Override public void handleRequest(BaseMethod method)
                                 throws GenericLDAPConnectorException
    {
        // Now we have the DN to read, so let's try it.
        IConnectionManager connectionManager = method.getConfiguration().getConnectionManager();

        // Now we need to get the value for the parameter 'dn'
        RequestParameter paramDN = getParameter(PARAM_DN);

        if (paramDN == null)
        {
            throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.GLE_CANT_FIND_THE_PARAMETER_DEFINITION_FOR_PARAMETER_WITH_NAME,
                                                    PARAM_DN);
        }

        XPathMetaInfo xmi = new XPathMetaInfo();
        xmi.addNamespaceBinding("ns", Node.getNamespaceURI(method.getRequestXML()));

        String dn = paramDN.getStringValue(method.getRequestXML(), xmi);

        String[] attributeNames = determineAttributesToIncludeInSearch(method, xmi);
        LDAPEntry entry = connectionManager.readLDAPEntry(dn, attributeNames);

        // Build up the actual response.
        buildResponse(method, connectionManager, xmi, new LDAPEntry[] { entry });
    }

    /**
     * This method sets the definition of the returning attributes..
     *
     * @param  returnAttributes  The definition of the returning attributes..
     */
    public void setReturnAttributes(ReturnAttributes returnAttributes)
    {
        m_returnAttributes = returnAttributes;
    }

    /**
     * This method builds the response for the given list of entries.
     *
     * @param   method             The base method.
     * @param   connectionManager  The connection manager.
     * @param   xmi                The XPathMetaInfo object.
     * @param   entries            The entries to display.
     *
     * @throws  GenericLDAPConnectorException
     */
    protected void buildResponse(BaseMethod method, IConnectionManager connectionManager,
                                 XPathMetaInfo xmi, LDAPEntry[] entries)
                          throws GenericLDAPConnectorException
    {
        // Get the include and exclude attributes.
        Map<String, IAttributeDefinition> includeAttributes = m_returnAttributes
                                                              .getIncludeAttributes(method
                                                                                    .getRequestXML(),
                                                                                    xmi);
        Map<String, IAttributeDefinition> excludeAttributes = m_returnAttributes
                                                              .getExcludeAttributes(method
                                                                                    .getRequestXML(),
                                                                                    xmi);

        if ((entries != null) && (entries.length > 0))
        {
            // The entry was read, so we need to turn it into proper XML.
            int responseXML = method.getResponseXML();

            ResponseBuilder builder = new ResponseBuilder(responseXML, entries,
                                                          connectionManager.getSchema(),
                                                          includeAttributes, excludeAttributes);
            builder.buildResponse();
        }
    }

    /**
     * Determine the attributes to include in the search.
     *
     * @param   method  The basemethod.
     * @param   xmi     The namespace definitions.
     *
     * @return  A list of attribute names
     *
     * @throws  GenericLDAPConnectorException  In case of any exceptions
     */
    protected String[] determineAttributesToIncludeInSearch(BaseMethod method, XPathMetaInfo xmi)
                                                       throws GenericLDAPConnectorException
    {
        Map<String, IAttributeDefinition> includeAttr = getReturnAttributes().getIncludeAttributes(method
                                                                                                   .getRequestXML(),
                                                                                                   xmi);
        Map<String, IAttributeDefinition> excludeAttr = getReturnAttributes().getExcludeAttributes(method
                                                                                                   .getRequestXML(),
                                                                                                   xmi);
        List<String> attributeNames = new ArrayList<String>();

        for (String attName : includeAttr.keySet())
        {
        	// Only return attributes that are not present in exclude list.
            if (!excludeAttr.containsKey(attName))
            {
                attributeNames.add(attName);
                IAttributeDefinition attribute = includeAttr.get(attName);
                if( attribute != null ) {
                	String options = attribute.getOptions();
                	if( !(options == null || options.length() == 0) ) {
                		attributeNames.add(attName + ";" + options);
                	}
                }
            }
        }
        
        if ((includeAttr.size() > 0) && (attributeNames.size() == 0))   
        {
        	// All included attributes are also excluded; throw error
        	throw new GenericLDAPConnectorException(GenLDAPExceptionMessages.NO_ATTRIBUTES_TO_INCLUDE_IN_SEARCH);
        }

        return attributeNames.toArray(new String[0]);
    }    
}
