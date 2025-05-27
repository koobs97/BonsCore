//package com.koo.bonscore.common.paging.interceptor;
//
//import com.koo.bonscore.common.paging.dto.req.PageRequest;
//import org.apache.ibatis.executor.parameter.DefaultParameterHandler;
//import org.apache.ibatis.executor.statement.StatementHandler;
//import org.apache.ibatis.mapping.BoundSql;
//import org.apache.ibatis.mapping.ParameterMapping;
//import org.apache.ibatis.plugin.*;
//import org.apache.ibatis.reflection.MetaObject;
//import org.apache.ibatis.reflection.SystemMetaObject;
//
//import java.lang.reflect.Field;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//
//@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
//public class PagingaionInterceptor implements Interceptor {
//
//    @Override
//    public Object intercept(Invocation invocation) throws Throwable {
//        StatementHandler handler = (StatementHandler) invocation.getTarget();
//        MetaObject metaObject = SystemMetaObject.forObject(handler);
//
//        BoundSql boundSql = handler.getBoundSql();
//        Object paramObj = boundSql.getParameterObject();
//
//        if (paramObj instanceof Map) {
//            Map<String, Object> paramMap = (Map<String, Object>) paramObj;
//            if (paramMap.containsKey("pageRequest")) {
//                PageRequest page = (PageRequest) paramMap.get("pageRequest");
//                String originalSql = boundSql.getSql();
//
//                // 1) 총 데이터 수 조회 쿼리
//                String countSql = "SELECT COUNT(*) FROM (" + originalSql + ")";
//
//                // 커넥션 얻기
//                Connection connection = (Connection) invocation.getArgs()[0];
//
//                try (PreparedStatement countStmt = connection.prepareStatement(countSql)) {
//                    DefaultParameterHandler parameterHandler = new DefaultParameterHandler(handler.getBoundSql().getParameterObject());
//                    parameterHandler.setParameters(countStmt);
//
//                    try (ResultSet rs = countStmt.executeQuery()) {
//                        if (rs.next()) {
//                            page.setTotalCount(rs.getInt(1));
//                        }
//                    }
//                }
//
//                // 2) 페이징 쿼리 작성 (오라클 ROWNUM 방식)
//                String pagingSql = "SELECT * FROM (" +
//                        " SELECT inner_query.*, ROWNUM rn FROM (" + originalSql + ") inner_query " +
//                        " WHERE ROWNUM <= " + page.getLimit() +
//                        ") WHERE rn > " + page.getOffset();
//
//                // 원본 SQL 페이징 쿼리로 변경 (reflection 사용)
//                Field field = boundSql.getClass().getDeclaredField("sql");
//                field.setAccessible(true);
//                field.set(boundSql, pagingSql);
//            }
//        }
//
//        return invocation.proceed();
//    }
//
//    @Override
//    public Object plugin(Object target) {
//        if (target instanceof StatementHandler) {
//            return Plugin.wrap(target, this);
//        }
//        return target;
//    }
//
//    @Override
//    public void setProperties(Properties properties) {
//    }
//}