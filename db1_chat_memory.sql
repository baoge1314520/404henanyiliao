-- ============================================================
-- 大模型会话记忆表（ChatMemoryStore 数据库实现）2026-11-13
-- 作业：把课上演示的 Redis 记忆存储改为数据库存储
-- 一行 = 一个会话(memory_id) 的完整历史消息(messages 为 JSON)
-- 可重复执行（IF NOT EXISTS）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_chat_memory (
    memory_id   VARCHAR(64)  PRIMARY KEY,
    messages    TEXT         NOT NULL,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  sys_chat_memory             IS '大模型会话记忆表';
COMMENT ON COLUMN sys_chat_memory.memory_id   IS '会话标识(memoryId)';
COMMENT ON COLUMN sys_chat_memory.messages    IS '历史消息JSON(ChatMessageSerializer)';
COMMENT ON COLUMN sys_chat_memory.update_time IS '最近更新时间';