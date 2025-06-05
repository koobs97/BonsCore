package com.koo.bonscore.common.paging.interceptor;

import com.koo.bonscore.common.paging.dto.req.Page;
import com.koo.bonscore.common.paging.support.PageContext;
import com.koo.bonscore.common.paging.annotation.Pageable;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <pre>
 * PagingInterceptor.java
 * 설명 : MyBatis sql을 itercept해서 페이징 처리 관련 sql로 변환
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-28
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        })
})
public class PagingInterceptor implements Interceptor {

    /**
     * 실제 쿼리 실행 전에 페이징 처리 및 Count 쿼리를 수행하여 페이징 SQL로 변환
     * @param invocation Invocation
     * @return invocation.proceed() or List<Object> resultList
     * @throws Throwable ex
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        if (!hasPageableAnnotation(ms)) {
            return invocation.proceed(); // 어노테이션 없으면 그대로 실행
        }

        Page page = extractPageParam(parameter);
        if (page == null || !page.isEnablePaging()) {
            return invocation.proceed(); // 페이징 객체 없으면 그대로 실행
        }

        BoundSql boundSql = ms.getBoundSql(parameter);
        String originalSql = boundSql.getSql();

        // 1. Count 쿼리 실행
        String countSql = "SELECT COUNT(*) FROM (" + originalSql + ") TEMP_COUNT";

        Connection connection = null;
        PreparedStatement countStmt = null;
        ResultSet rs = null;
        try {
            connection = ms.getConfiguration().getEnvironment().getDataSource().getConnection();
            countStmt = connection.prepareStatement(countSql);
            DefaultParameterHandler countPh = new DefaultParameterHandler(ms, parameter, boundSql);
            countPh.setParameters(countStmt);
            rs = countStmt.executeQuery();
            long total = 0;
            if (rs.next()) {
                total = rs.getLong(1);
            }
            PageContext.setTotalCount((int) total);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (countStmt != null) try { countStmt.close(); } catch (Exception ignored) {}
            if (connection != null) try { connection.close(); } catch (Exception ignored) {}
        }



        // 2. 페이징 SQL 조작 (Oracle 기준 ROWNUM 사용)
        String pagingSql =
                "SELECT * FROM (" +
                        "SELECT TMP_TABLE.*, ROWNUM RNUM FROM (" + originalSql + ") TMP_TABLE " +
                        "WHERE ROWNUM <= " + (page.getOffset() + page.getLimit()) +
                        ") WHERE RNUM > " + page.getOffset();

        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), pagingSql, boundSql.getParameterMappings(), parameter);

        MappedStatement newMs = copyMappedStatement(ms, newBoundSql);
        args[0] = newMs;

        // 3. 실제 데이터 쿼리 실행
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) invocation.proceed();

        // 4. PageResult로 래핑
        return resultList;
    }

    // 어노테이션 여부 확인
    private boolean hasPageableAnnotation(MappedStatement ms) {
        String id = ms.getId(); // ex) com.example.mapper.SampleMapper.testSelect
        String className = id.substring(0, id.lastIndexOf('.'));
        String methodName = id.substring(id.lastIndexOf('.') + 1);

        try {
            Class<?> mapperClass = Class.forName(className);
            for (Method method : mapperClass.getDeclaredMethods()) {
                // 같은 이름의 메서드 중에 어노테이션 붙은 메서드가 있는지 확인
                if (method.getName().equals(methodName)
                        && method.isAnnotationPresent(Pageable.class)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Mapper class not found: " + className, e);
        }

        return false;
    }

    private Page extractPageParam(Object parameter) {
        if (parameter instanceof Page) return (Page) parameter;

        if (parameter instanceof Map) {
            for (Object value : ((Map<?, ?>) parameter).values()) {
                if (value instanceof Page) return (Page) value;
            }
        }

        // 리플렉션을 통해 Page 필드 추출
        try {
            Field[] fields = parameter.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(Page.class)) {
                    field.setAccessible(true);
                    Object value = field.get(parameter);
                    if (value instanceof Page) {
                        return (Page) value;
                    }
                }
            }
        } catch (Exception e) {
            // 무시
        }
        return null;
    }

    private MappedStatement copyMappedStatement(MappedStatement ms, BoundSql newBoundSql) {
        return new MappedStatement.Builder(ms.getConfiguration(), ms.getId(),
                (SqlSource) new BoundSqlSqlSource(newBoundSql), ms.getSqlCommandType())
                .resource(ms.getResource())
                .fetchSize(ms.getFetchSize())
                .statementType(ms.getStatementType())
                .keyGenerator(ms.getKeyGenerator())
                .keyProperty(String.join(",", ms.getKeyProperties() != null ? ms.getKeyProperties() : new String[0]))
                .timeout(ms.getTimeout())
                .parameterMap(ms.getParameterMap())
                .resultMaps(ms.getResultMaps())
                .resultSetType(ms.getResultSetType())
                .cache(ms.getCache())
                .build();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    static class BoundSqlSqlSource implements SqlSource {
        private final BoundSql boundSql;
        BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }
        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }

    @Override
    public void setProperties(Properties properties) {}
}
