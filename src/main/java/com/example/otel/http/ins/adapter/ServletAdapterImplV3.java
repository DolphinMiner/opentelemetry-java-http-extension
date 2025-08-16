package com.example.otel.http.ins.adapter;


import com.example.otel.http.ins.ServletAdapter;
import com.example.otel.http.ins.model.SpConstants;
import com.example.otel.http.ins.util.IOUtils;
import com.example.otel.http.ins.wrapper.CachedBodyRequestWrapperV3;
import com.example.otel.http.ins.wrapper.CachedBodyResponseWrapperV3;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

import static com.example.otel.http.ins.adapter.ServletAdapterImplUtil.getUrlDecodeBytes;


/**
 * ServletAdapterImplV3
 */
public class ServletAdapterImplV3 implements ServletAdapter<HttpServletRequest, HttpServletResponse> {
    private static final ServletAdapterImplV3 INSTANCE = new ServletAdapterImplV3();

    public static ServletAdapterImplV3 getInstance() {
        return INSTANCE;
    }


    @Override
    public HttpServletRequest asHttpServletRequest(Object servletRequest) {
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            try {
                if (httpServletRequest.getCharacterEncoding() == null) {
                    httpServletRequest.setCharacterEncoding(StandardCharsets.UTF_8.name());
                }
            } catch (Exception e) {
                // ignore
            }
            return httpServletRequest;
        }
        return null;
    }
    @Override
    public HttpServletResponse asHttpServletResponse(Object servletResponse) {
        if (servletResponse instanceof HttpServletResponse) {
            return (HttpServletResponse) servletResponse;
        }
        return null;
    }

    @Override
    public HttpServletRequest wrapRequest(HttpServletRequest httpServletRequest) {
        if (httpServletRequest instanceof CachedBodyRequestWrapperV3) {
            return httpServletRequest;
        }
        return new CachedBodyRequestWrapperV3(httpServletRequest);
    }

    @Override
    public HttpServletResponse wrapResponse(HttpServletResponse httpServletResponse) {
        if (httpServletResponse instanceof CachedBodyResponseWrapperV3) {
            return httpServletResponse;
        }
        return new CachedBodyResponseWrapperV3(httpServletResponse);
    }

    @Override
    public boolean wrapped(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return httpServletRequest instanceof CachedBodyRequestWrapperV3
                && httpServletResponse instanceof CachedBodyResponseWrapperV3;
    }

    @Override
    public byte[] getRequestBytes(HttpServletRequest httpServletRequest) {
        CachedBodyRequestWrapperV3 requestWrapper = (CachedBodyRequestWrapperV3) httpServletRequest;
        byte[] content =  requestWrapper.getContentAsByteArray();
        String contentType = httpServletRequest.getContentType();
        if (content.length > 0) {
            return getUrlDecodeBytes(contentType, content);
        }
        // read request body to cache
        if (httpServletRequest.getContentLength() > 0) {
            try {
                byte[] bytes = IOUtils.copyToByteArray(requestWrapper.getInputStream());

                return getUrlDecodeBytes(contentType, bytes);
            } catch (Exception ignore) {
                // ignore exception
            }
        }
        return getUrlDecodeBytes(contentType, content);
    }

    @Override
    public byte[] getResponseBytes(HttpServletResponse httpServletResponse) {
        return ((CachedBodyResponseWrapperV3) httpServletResponse).getContentAsByteArray();
    }

    @Override
    public String getServletVersion() {
        return SpConstants.SERVLET_V3;
    }
}
