package com.koo.bonscore.core.config.mybatis;

import com.koo.bonscore.common.paging.interceptor.PagingInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * <pre>
 * MyBatisConfig.java
 * 설명 : Mybatis 설정
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-01
 */
@Configuration
@MapperScan("com.koo.bonscore")
public class MyBatisConfig {

    /**
     * MyBatis의 SqlSessionFactory를 설정하고 빈으로 등록
     * - 데이터소스 설정
     * - 페이징 인터셉터 등 플러그인 적용
     * - Mapper XML 위치 지정
     * - 카멜케이스 자동 매핑 등 MyBatis 환경설정
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // Mapper XML 경로 설정a
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factoryBean.setMapperLocations(resolver.getResources("classpath:mybatis/mapper/**/*.xml"));

        // MyBatis의 기본 설정
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        configuration.setJdbcTypeForNull(JdbcType.NULL);

        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }

    /**
     * PagingInterceptor를 Bean으로 등록
     * → 다른 설정에서 주입받거나 단독 테스트 가능하게 하기 위함
     */
    @Bean
    public PagingInterceptor pagingInterceptor() {
        return new PagingInterceptor();
    }

    /**
     * ConfigurationCustomizer를 통해 MyBatis 설정에 인터셉터 동적으로 추가
     * → pagingInterceptor() Bean을 주입받아 적용
     * → 위 factoryBean.setPlugins(...)와 중복될 수 있으므로 하나만 유지하는 것도 고려
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer(PagingInterceptor interceptor) {
        return configuration -> configuration.addInterceptor(interceptor);
    }
}
