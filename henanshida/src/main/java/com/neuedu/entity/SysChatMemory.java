package com.neuedu.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大模型会话记忆表 实体类。
 * 一行 = 一个会话（memoryId）的完整历史消息，messages 为 langchain4j 序列化后的 JSON。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_chat_memory")
public class SysChatMemory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话标识（memoryId），主键，由调用方指定，非自增 */
    @Id(keyType = KeyType.None)
    private String memoryId;

    /** 历史消息 JSON（ChatMessageSerializer.messagesToJson 的结果） */
    private String messages;

    /** 最近一次更新时间 */
    private LocalDateTime updateTime;
}