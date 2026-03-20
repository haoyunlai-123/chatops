package com.haoyunlai.chatops.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haoyunlai.chatops.model.plan.ExecutionPlan;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Primary
@Component
public class JdbcSuspensionStore implements SuspensionStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcSuspensionStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String token, SuspensionContext context) {
        String sql = """
                INSERT INTO agent_suspension
                (token, execution_id, plan_json, next_step_index, global_context, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  execution_id = VALUES(execution_id),
                  plan_json = VALUES(plan_json),
                  next_step_index = VALUES(next_step_index),
                  global_context = VALUES(global_context),
                  created_at = VALUES(created_at)
                """;

        jdbcTemplate.update(sql,
                token,
                context.executionId(),
                toJson(context.plan()),
                context.nextStepIndex(),
                context.globalContext(),
                Timestamp.from(Instant.now()));
    }

    @Override
    @Transactional
    public SuspensionContext remove(String token) {
        String querySql = """
                SELECT execution_id, plan_json, next_step_index, global_context
                FROM agent_suspension
                WHERE token = ?
                """;

        List<SuspensionContext> contexts = jdbcTemplate.query(querySql, (rs, rowNum) -> new SuspensionContext(
                rs.getString("execution_id"),
                fromJson(rs.getString("plan_json")),
                rs.getInt("next_step_index"),
                rs.getString("global_context")
        ), token);

        if (contexts.isEmpty()) {
            return null;
        }

        jdbcTemplate.update("DELETE FROM agent_suspension WHERE token = ?", token);
        return contexts.get(0);
    }

    private String toJson(ExecutionPlan plan) {
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ExecutionPlan", e);
        }
    }

    private ExecutionPlan fromJson(String json) {
        try {
            return objectMapper.readValue(json, ExecutionPlan.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize ExecutionPlan", e);
        }
    }
}
