package com.alibaba.chaosblade.exec.plugin.servlet;

import com.alibaba.chaosblade.exec.common.aop.EnhancerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author yefei
 * @create 2020-06-22 14:33
 */
public class ServletParamsMatcher implements EnhancerModel.CustomMatcher {

    private static final Logger logger = LoggerFactory.getLogger(ServletParamsMatcher.class);

    private static final ServletParamsMatcher CALL_BACK = new ServletParamsMatcher();

    private ServletParamsMatcher() {
    }

    public static ServletParamsMatcher getInstance() {
        return CALL_BACK;
    }

    @Override
    public boolean match(String value, EnhancerModel model) {
        final String queryString = value;
        Map<String, Object> originQueryString = model.getContextValue(ServletConstant.QUERY_STRING_KEY);
        if (queryString == null) {
            return true;
        } else {
            if (originQueryString == null) {
                logger.debug("query string mather fail, originQueryString is null, expectValue:{}", queryString);
                return false;
            } else {
                String[] paramsStr = queryString.split(ServletConstant.AND_SYMBOL);
                for (String s : paramsStr) {
                    int i = s.indexOf(ServletConstant.EQUALS_SYMBOL);
                    if (i != -1) {
                        Object actualValue = originQueryString.get(s.substring(0, i));
                        String expectValue = s.substring(i + 1);
                        if (actualValue == null) {
                            logger.debug("query string mather fail, actualValue is null, expectValue:{}", expectValue);
                            return false;
                        }
                        if (expectValue.equals(actualValue.toString())) {
                            logger.debug("query string mather success, actualValue: {}, expectValue:{}", actualValue, expectValue);
                            continue;
                        } else {
                            logger.debug("query string mather fail, actualValue: {}, expectValue:{}", actualValue, expectValue);
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                return true;
            }
        }
    }
}
