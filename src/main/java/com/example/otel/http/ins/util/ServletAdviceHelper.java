package com.example.otel.http.ins.util;

public class ServletAdviceHelper {


    public static void onServiceEnter(Object servletRequest,
                                      Object servletResponse){
        System.out.println("============================onServiceEnter start============================");
        System.out.println(servletRequest);
        System.out.println(servletResponse);
        System.out.println("============================onServiceEnter end============================");
    }



    public static void onServiceExit(Object servletRequest,
                                     Object servletResponse){
        System.out.println("============================onServiceExit start============================");
        System.out.println(servletRequest);
        System.out.println(servletResponse);
        System.out.println("============================onServiceExit end============================");

    }
}
