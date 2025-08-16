package com.example.otel.http.ins;

public interface ServletAdapter<HttpServletRequest, HttpServletResponse> {

    HttpServletRequest asHttpServletRequest(Object servletRequest);

    HttpServletResponse asHttpServletResponse(Object servletResponse);
    String getServletVersion();

    HttpServletRequest wrapRequest(HttpServletRequest httpServletRequest);

    HttpServletResponse wrapResponse(HttpServletResponse httpServletResponse);

    boolean wrapped(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);


    byte[] getRequestBytes(HttpServletRequest httpServletRequest);

    byte[] getResponseBytes(HttpServletResponse httpServletResponse);
}
