package com.haoyunlai.chatops.runtime;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Primary
@Component
public class JdbcExecutionStateStore implements ExecutionStateStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcExecutionStateStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void init(String executionId, String userMessage) {
        String sql = """
                INSERT INTO agent_execution_state
                (execution_id, user_message, intent, total_steps, current_step, status, approval_token, last_message, updated_at)
                VALUES (?, ?, NULL, 0, 0, ?, NULL, ?, ?)
                ON DUPLICATE KEY UPDATE
                  user_message = VALUES(user_message),
                  status = VALUES(status),
                  last_message = VALUES(last_message),
                  updated_at = VALUES(updated_at)
                """;
        jdbcTemplate.update(sql,
                executionId,
                userMessage,
                ExecutionStatus.PENDING.name(),
                "任务已创建，等待执行",
                Timestamp.from(Instant.now()));
    }

    @Override
    public void markRunning(String executionId, String intent, int totalSteps, int currentStep, String message) {
        ExecutionSnapshot old = get(executionId);
        if (old == null) {
            return;
        }
        upsert(new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                intent != null ? intent : old.intent(),
                totalSteps > 0 ? totalSteps : old.totalSteps(),
                currentStep,
                ExecutionStatus.RUNNING,
                null,
                message,
                Instant.now()
        ));
    }

    @Override
    public void markSuspended(String executionId, int currentStep, String approvalToken, String message) {
        ExecutionSnapshot old = get(executionId);
        if (old == null) {
            return;
        }
        upsert(new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                old.intent(),
                old.totalSteps(),
                currentStep,
                ExecutionStatus.SUSPENDED,
                approvalToken,
                message,
                Instant.now()
        ));
    }

    @Override
    public void markBlocked(String executionId, String message) {
        ExecutionSnapshot old = get(executionId);
        if (old == null) {
            return;
        }
        upsert(new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                old.intent(),
                old.totalSteps(),
                old.currentStep(),
                ExecutionStatus.BLOCKED,
                old.approvalToken(),
                message,
                Instant.now()
        ));
    }

    @Override
    public void markFailed(String executionId, int currentStep, String message) {
        ExecutionSnapshot old = get(executionId);
        if (old == null) {
            return;
        }
        upsert(new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                old.intent(),
                old.totalSteps(),
                currentStep,
                ExecutionStatus.FAILED,
                old.approvalToken(),
                message,
                Instant.now()
        ));
    }

    @Override
    public void markSucceeded(String executionId, int totalSteps, String message) {
        ExecutionSnapshot old = get(executionId);
        if (old == null) {
            return;
        }
        upsert(new ExecutionSnapshot(
                old.executionId(),
                old.userMessage(),
                old.intent(),
                totalSteps,
                totalSteps,
                ExecutionStatus.SUCCEEDED,
                old.approvalToken(),
                message,
                Instant.now()
        ));
    }

    @Override
    public ExecutionSnapshot get(String executionId) {
        String sql = """
                SELECT execution_id, user_message, intent, total_steps, current_step, status,
                       approval_token, last_message, updated_at
                FROM agent_execution_state
                WHERE execution_id = ?
                """;
        List<ExecutionSnapshot> snapshots = jdbcTemplate.query(sql, this::mapSnapshot, executionId);
        return snapshots.isEmpty() ? null : snapshots.get(0);
    }

    private void upsert(ExecutionSnapshot snapshot) {
        String sql = """
                INSERT INTO agent_execution_state
                (execution_id, user_message, intent, total_steps, current_step, status, approval_token, last_message, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  user_message = VALUES(user_message),
                  intent = VALUES(intent),
                  total_steps = VALUES(total_steps),
                  current_step = VALUES(current_step),
                  status = VALUES(status),
                  approval_token = VALUES(approval_token),
                  last_message = VALUES(last_message),
                  updated_at = VALUES(updated_at)
                """;
        jdbcTemplate.update(sql,
                snapshot.executionId(),
                snapshot.userMessage(),
                snapshot.intent(),
                snapshot.totalSteps(),
                snapshot.currentStep(),
                snapshot.status().name(),
                snapshot.approvalToken(),
                snapshot.lastMessage(),
                Timestamp.from(snapshot.updatedAt()));
    }

    private ExecutionSnapshot mapSnapshot(ResultSet rs, int rowNum) throws SQLException {
        Timestamp ts = rs.getTimestamp("updated_at");
        return new ExecutionSnapshot(
                rs.getString("execution_id"),
                rs.getString("user_message"),
                rs.getString("intent"),
                rs.getInt("total_steps"),
                rs.getInt("current_step"),
                ExecutionStatus.valueOf(rs.getString("status")),
                rs.getString("approval_token"),
                rs.getString("last_message"),
                ts == null ? Instant.now() : ts.toInstant()
        );
    }
}
