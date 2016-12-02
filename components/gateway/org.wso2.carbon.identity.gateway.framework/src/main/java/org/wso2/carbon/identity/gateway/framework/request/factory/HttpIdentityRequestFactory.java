/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.gateway.framework.request.factory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.gateway.framework.exception.FrameworkClientException;
import org.wso2.carbon.identity.gateway.framework.exception.FrameworkRuntimeException;
import org.wso2.carbon.identity.gateway.framework.request.IdentityRequest;
import org.wso2.carbon.identity.gateway.framework.response.HttpIdentityResponse;

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class HttpIdentityRequestFactory<T extends IdentityRequest.IdentityRequestBuilder> {

    private static Log log = LogFactory.getLog(HttpIdentityRequestFactory.class);
    protected Properties properties;

    public static final String TENANT_DOMAIN_PATTERN = "/t/([^/]+)";

    protected InitConfig initConfig;

    public void init(InitConfig initConfig) throws FrameworkRuntimeException {


        this.initConfig = initConfig;

        IdentityEventListenerConfig identityEventListenerConfig = IdentityUtil.readEventListenerProperty
                (HttpIdentityRequestFactory.class.getName(), this.getClass().getName());

        if (identityEventListenerConfig == null) {
            return;
        }

        if (identityEventListenerConfig.getProperties() != null) {
            for (Map.Entry<Object, Object> property : identityEventListenerConfig.getProperties().entrySet()) {
                String key = (String) property.getKey();
                String value = (String) property.getValue();
                if (!properties.containsKey(key)) {
                    properties.setProperty(key, value);
                } else {
                    log.warn("Property key " + key + " already exists. Cannot add property!!");
                }
            }
        }


    }

    public String getName() {
        return "HttpIdentityRequestFactory";
    }

    public int getPriority() {
        return 100;
    }

    public boolean canHandle(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }

    public IdentityRequest.IdentityRequestBuilder create(HttpServletRequest request, HttpServletResponse response)
            throws FrameworkClientException {

        IdentityRequest.IdentityRequestBuilder builder = new IdentityRequest.IdentityRequestBuilder(request, response);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            builder.addHeader(headerName, request.getHeader(headerName));
        }
        builder.setParameters(request.getParameterMap());

        // add request attributes into the Identity Request
        Enumeration<String> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = attrNames.nextElement();
            builder.addAttribute(attrName, request.getAttribute(attrName));
        }

        // add cookie into Identity Request
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            builder.addCookie(cookie.getName(), cookie);
        }
        return builder;
    }

    public HttpIdentityResponse.HttpIdentityResponseBuilder handleException(FrameworkClientException exception,
                                                                            HttpServletRequest request,
                                                                            HttpServletResponse response) {
        HttpIdentityResponse.HttpIdentityResponseBuilder builder =
                new HttpIdentityResponse.HttpIdentityResponseBuilder();
        builder.setStatusCode(400);
        builder.setBody(exception.getMessage());
        return builder;
    }


    public void create(T builder, HttpServletRequest request, HttpServletResponse response)
            throws FrameworkClientException {

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            builder.addHeader(headerName, request.getHeader(headerName));
        }
        builder.setParameters(request.getParameterMap());
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                builder.addCookie(cookie.getName(), cookie);
            }
        }
        String requestURI = request.getRequestURI();
        Pattern pattern = Pattern.compile(TENANT_DOMAIN_PATTERN);
        Matcher matcher = pattern.matcher(requestURI);
        if (matcher.find()) {
            builder.setTenantDomain(matcher.group(1));
        } else {
            builder.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        }
        builder.setContentType(request.getContentType());
        builder.setContextPath(request.getContextPath());
        builder.setMethod(request.getMethod());
        builder.setPathInfo(request.getPathInfo());
        builder.setPathTranslated(request.getPathTranslated());
        builder.setQueryString(request.getQueryString());
        builder.setRequestURI(requestURI);
        builder.setRequestURL(request.getRequestURL());
        builder.setServletPath(request.getServletPath());
    }


}
