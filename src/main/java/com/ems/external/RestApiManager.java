package com.ems.external;

import com.ems.config.EmployeeManagementConfig;
import com.ems.exception.ApiError;
import com.ems.exception.EmployeeManagementException;
import com.ems.exception.PayrollServiceException;
import com.ems.util.TransformUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static com.ems.constants.StringConstants.LOG_ID;

@Component
public class RestApiManager {

    @Autowired
    private EmployeeManagementConfig appConfiguration;

    private static final Logger log = LoggerFactory.getLogger(RestApiManager.class);

    public <T> T get(String baseUrl, String url, String query, HttpHeaders requestHeaders,
                     Class<T> responseClassType, int readTimeout) {
        ResponseEntity<T> responseEntity = null;
        try {
            String fullUrl = getFullUrl(baseUrl, url, query);
            HttpEntity<Object> requestEntity = new HttpEntity<Object>(requestHeaders);
            log.info("The URL called : {} and readTimeout sent : {} with logId : {}", fullUrl, readTimeout);
            RestTemplate restTemplate = appConfiguration.restTemplate();
            HttpComponentsClientHttpRequestFactory rf = (HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory();
            rf.setReadTimeout(readTimeout);
            responseEntity = restTemplate.exchange(fullUrl, HttpMethod.GET, requestEntity, responseClassType);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return responseEntity.getBody();
            }
        } catch (Exception e) {
            handleException(responseEntity, e);
        }
        return null;
    }

    public <T> T post(String baseUrl, String url, String query, Object body,
                      HttpHeaders requestHeaders, Class<T> responseClassType, int readTimeout) {
        ResponseEntity<T> responseEntity = null;
        try {
            String fullUrl = getFullUrl(baseUrl, url, query);
            String bodyJson = null;
            if (body != null) {
                bodyJson = TransformUtil.toJson(body);
            }
            HttpEntity<Object> requestEntity = new HttpEntity<>(bodyJson, requestHeaders);
            log.info("The URL called : {} and readTimeout sent : {}", fullUrl, readTimeout);
            RestTemplate restTemplate = appConfiguration.restTemplate();
            HttpComponentsClientHttpRequestFactory rf = (HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory();
            rf.setReadTimeout(readTimeout);
            responseEntity =
                    restTemplate.exchange(fullUrl, HttpMethod.POST, requestEntity, responseClassType);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return responseEntity.getBody();
            }
        } catch (Exception e) {
            handleException(responseEntity, e);
        }
        return null;
    }

    private String getFullUrl(String baseUrl, String url, String query) {
        StringBuilder fullUrl = new StringBuilder();
        fullUrl.append(baseUrl);
        if (url != null) {
            fullUrl.append(url);
        }
        if (query != null && query.startsWith("?")) {
            query = query.substring(1);
        }
        query = StringUtils.trimToNull(query);
        if (query != null) {
            fullUrl.append("?");
            fullUrl.append(query);
        }
        return fullUrl.toString();
    }

    private void handleException(ResponseEntity responseEntity, Exception e) {
        log.error("Error in RestApiManager : {} ; Exception : {}", responseEntity, e);
        if (e instanceof HttpClientErrorException) {
            ApiError apiError = new ApiError(((HttpClientErrorException) e).getStatusCode(), e.getMessage(), ((HttpClientErrorException) e).getResponseBodyAsString());
            throw new EmployeeManagementException(apiError);
        } else if (responseEntity != null) {
            ApiError apiError = new ApiError(responseEntity.getStatusCode(), e.getMessage(), responseEntity.toString());
            throw new EmployeeManagementException(apiError);
        } else {
            throw new PayrollServiceException("Payroll service failing ", MDC.get(LOG_ID));
        }
    }


}
