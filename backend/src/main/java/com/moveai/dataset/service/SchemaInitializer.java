package com.moveai.dataset.service;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;

/**
 * 05A §2 의 DDL 을 적용한다.
 *
 * <p>Hibernate {@code ddl-auto} 는 none 이다. 스키마가 두 곳에서 만들어지면 05A 와 어긋난
 * 테이블이 조용히 생긴다. DDL 은 이 스크립트 하나로만 만든다.
 */
@Service
public class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);
    private static final String SCHEMA_PATH = "db/schema.sql";

    private final DataSource dataSource;

    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void apply() {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(SCHEMA_PATH));
            log.info("schema applied from {}", SCHEMA_PATH);
        } catch (Exception exception) {
            throw new IllegalStateException("DDL 적용에 실패했다: " + SCHEMA_PATH, exception);
        }
    }
}
