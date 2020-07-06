package org.ssssssss.magicapi.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.ssssssss.magicapi.utils.Assert;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DynamicDataSource {

    private static Logger logger = LoggerFactory.getLogger(DynamicDataSource.class);

    private Map<String, DataSourceNode> dataSourceMap = new HashMap<>();

    public void put(DataSource dataSource) {
        put(null, dataSource);
    }

    public void put(String dataSourceName, DataSource dataSource) {
        if (dataSourceName == null) {
            dataSourceName = "";
        }
        logger.info("注册数据源：{}", StringUtils.isNotBlank(dataSourceName) ? dataSourceName : "default");
        this.dataSourceMap.put(dataSourceName, new DataSourceNode(dataSource));
    }

    public DataSourceNode getDataSource() {
        return getDataSource(null);
    }

    public DataSourceNode getDataSource(String dataSourceName) {
        if (dataSourceName == null) {
            dataSourceName = "";
        }
        DataSourceNode dataSourceNode = dataSourceMap.get(dataSourceName);
        Assert.isNotNull(dataSourceNode, String.format("找不到数据源%s", dataSourceName));
        return dataSourceNode;
    }

    public static class DataSourceNode{

        private DataSourceTransactionManager dataSourceTransactionManager;

        private JdbcTemplate jdbcTemplate;

        private DataSource dataSource;

        public DataSourceNode(DataSource dataSource) {
            this.dataSource = dataSource;
            this.dataSourceTransactionManager = new DataSourceTransactionManager(this.dataSource);
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }

        public JdbcTemplate getJdbcTemplate(){
            return this.jdbcTemplate;
        }

        public DataSourceTransactionManager getDataSourceTransactionManager() {
            return dataSourceTransactionManager;
        }
    }
}