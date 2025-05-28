package com.koo.bonscore.core.config.mybatis;

import com.koo.bonscore.common.page.PagingInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan("com.koo.bonscore")
public class MyBatisConfig {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPlugins(new Interceptor[]{
                new PagingInterceptor()
        });

        // Mapper XML 경로 설정
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factoryBean.setMapperLocations(resolver.getResources("classpath:mybatis/mapper/**/*.xml"));

        // Camel Case 매핑 설정
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);

        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }

    @Bean
    public PagingInterceptor pagingInterceptor() {
        return new PagingInterceptor();
    }

    @Bean
    public ConfigurationCustomizer configurationCustomizer(PagingInterceptor interceptor) {
        return configuration -> configuration.addInterceptor(interceptor);
    }
}
