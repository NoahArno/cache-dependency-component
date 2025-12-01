package top.noaharno.cachedependency.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author NoahArno
 * @version 1.0.0
 * @since 2025/11/27 15:24
 */
@Slf4j
public class SqlAnalysisUtil {

    private static final Map<String, Set<String>> sqlTableMap = new HashMap<>();

    /**
     * 获取SQL中的表名
     *
     * @param sqlId     Mybatis 中对应 SQL 的 ID，例如 top.noah.demo.mapper.UserMapper.update
     * @param sqlDetail SQL 语句
     * @return 表名集合
     */
    public static Set<String> getTableSet(String sqlId, String sqlDetail) {
        if (StringUtils.isAnyBlank(sqlId, sqlDetail)) {
            return new HashSet<>();
        }
        if (sqlTableMap.containsKey(sqlId)) {
            return sqlTableMap.get(sqlId);
        }
        try {
            // 使用 jsqlparser 解析 SQL，获取涉及的表
            Statement statement = CCJSqlParserUtil.parse(sqlDetail);
            TablesNamesFinder<Object> finder = new TablesNamesFinder<>();
            Set<String> tables = finder.getTables(statement);
            sqlTableMap.put(sqlId, tables);
            return tables;
        } catch (net.sf.jsqlparser.JSQLParserException e) {
            log.warn("解析SQL失败：{}", e.getMessage(), e);
            return new HashSet<>();
        }
    }
}
