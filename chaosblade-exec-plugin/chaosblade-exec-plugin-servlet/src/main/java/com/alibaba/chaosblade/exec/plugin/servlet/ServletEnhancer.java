/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.chaosblade.exec.plugin.servlet;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.alibaba.chaosblade.exec.common.aop.BeforeEnhancer;
import com.alibaba.chaosblade.exec.common.aop.EnhancerModel;
import com.alibaba.chaosblade.exec.common.model.matcher.MatcherModel;
import com.alibaba.chaosblade.exec.common.util.ReflectUtil;
import com.alibaba.chaosblade.exec.common.util.StringUtils;
import com.alibaba.fastjson.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Changjun Xiao
 */
public class ServletEnhancer extends BeforeEnhancer {

    private static final Logger LOOGER = LoggerFactory.getLogger(ServletEnhancer.class);

    @Override
    public EnhancerModel doBeforeAdvice(ClassLoader classLoader, String className, Object object,
                                        Method method, Object[] methodArguments)
        throws Exception {
        Object request = methodArguments[0];
        String requestURI = ReflectUtil.invokeMethod(request, ServletConstant.GET_REQUEST_URI, new Object[] {}, false);
        String requestMethod = ReflectUtil.invokeMethod(request, ServletConstant.GET_METHOD, new Object[] {}, false);

        MatcherModel matcherModel = new MatcherModel();
        matcherModel.add(ServletConstant.METHOD_KEY, requestMethod);
        matcherModel.add(ServletConstant.REQUEST_PATH_KEY, requestURI);

        LOOGER.debug("servlet matchers: {}", JSON.toJSONString(matcherModel));

        EnhancerModel enhancerModel = new EnhancerModel(classLoader, matcherModel);
        String contentType = ReflectUtil.invokeMethod(request, ServletConstant.GET_CONTENT_TYPE, new Object[] {}, false);
        Map<String, Object> queryString = getQueryString(requestMethod, request, contentType);
        LOOGER.debug("origin params: {}", JSON.toJSONString(queryString));

        enhancerModel.addContextValue(ServletConstant.QUERY_STRING_KEY, queryString);
        enhancerModel.addCustomMatcher(ServletConstant.QUERY_STRING_KEY, ServletParamsMatcher.getInstance());
        return enhancerModel;
    }

    private Map<String, Object> getQueryString(String method, Object request, String contentType) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        if ("post".equalsIgnoreCase(method)) {
            InputStream inputStream = ReflectUtil.invokeMethod(request, ServletConstant.GET_INPUT_STREAM, new Object[] {}, false);
            if (ServletConstant.CONTENT_TYPE_JSON.equalsIgnoreCase(contentType)) {
                Map<String, Object> parameters = JSON.parseObject(inputStream, Map.class);
                return parameters;
            } else {
                Map<String, String[]> parameterMap = ReflectUtil.invokeMethod(request, ServletConstant.GET_PARAMETER_MAP, new Object[] {}, false);
                Set<Map.Entry<String, String[]>> entries = parameterMap.entrySet();
                for (Map.Entry<String, String[]> entry : entries) {
                    String value = "";
                    String[] values = entry.getValue();
                    if (values.length > 0) {
                        value = values[0];
                    }
                    params.put(entry.getKey(), value);
                }
            }
        } else {
            String queryString = ReflectUtil.invokeMethod(request, ServletConstant.GET_QUERY_STRING, new Object[] {}, false);
            if(StringUtils.isNotBlank(queryString)) {
                String[] paramsStr = queryString.split(ServletConstant.AND_SYMBOL);
                for (String s : paramsStr) {
                    int i = s.indexOf(ServletConstant.EQUALS_SYMBOL);
                    if (i != -1) {
                       params.put(s.substring(0, i), s.substring(i + 1));
                    }
                }
            }
        }
        return params;
    }
}
