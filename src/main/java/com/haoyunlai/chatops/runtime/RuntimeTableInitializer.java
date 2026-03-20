package com.haoyunlai.chatops.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RuntimeTableInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public RuntimeTableInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS agent_execution_state (
                  execution_id VARCHAR(64) PRIMARY KEY,
                  user_message TEXT,
                  intent VARCHAR(255),
                  total_steps INT NOT NULL,
                  current_step INT NOT NULL,
                  status VARCHAR(32) NOT NULL,
                  approval_token VARCHAR(64),
                  last_message TEXT,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS agent_suspension (
                  token VARCHAR(64) PRIMARY KEY,
                  execution_id VARCHAR(64) NOT NULL,
                  plan_json LONGTEXT NOT NULL,
                  next_step_index INT NOT NULL,
                  global_context LONGTEXT,
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_agent_suspension_execution_id (execution_id)
                )
                """);

        log.info("✅ 已确保 MySQL 假表存在: agent_execution_state, agent_suspension");
    }
}
