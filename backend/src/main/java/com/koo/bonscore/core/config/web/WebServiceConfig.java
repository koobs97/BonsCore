//package com.koo.bonscore.core.config.web;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.ws.config.annotation.EnableWs;
//import org.springframework.ws.config.annotation.WsConfigurerAdapter;
//import org.springframework.ws.soap.addressing.server.AnnotationActionEndpointMapping;
//
///**
// * <pre>
// * WebServiceConfig.java
// * 설명 : Spring Web Services (SOAP) 관련 설정을 구성하는 클래스
// * </pre>
// *
// * @author  : koobonsang
// * @version : 1.0
// * @since   : 2025-08-12
// */
//@EnableWs
//@Configuration
//public class WebServiceConfig extends WsConfigurerAdapter {
//
//    /**
//     * Jasypt와 같은 라이브러리가 Bean 초기화 순서에 영향을 주어 발생하는
//     * DelegatingWsConfiguration 관련 경고를 해결하기 위해 Bean을 직접 static으로 선언한다.
//     * 이렇게 하면 Spring이 이 Bean을 다른 설정 Bean보다 먼저, 독립적으로 생성하여
//     * 초기화 순서 문제를 해결할 수 있다.
//     *
//     * @return AnnotationActionEndpointMapping
//     */
//    @Bean
//    public static AnnotationActionEndpointMapping annotationActionEndpointMapping() {
//        return new AnnotationActionEndpointMapping();
//    }
//
//}
