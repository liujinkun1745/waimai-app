package com.waimai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 本地消息表 — 分布式事务最终一致性的备选方案
 * 下单时在同一事务中写入消息，异步补偿保证最终一致性
 */
@Entity
@Table(name = "local_message")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LocalMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务唯一标识（如订单号） */
    @Column(nullable = false, length = 64)
    private String businessKey;

    /** 消息主题 */
    @Column(nullable = false, length = 64)
    private String topic;

    /** 消息标签 */
    @Column(length = 64)
    private String tag;

    /** 消息体（JSON） */
    @Column(columnDefinition = "TEXT")
    private String messageBody;

    /** 状态：待发送 / 已发送 / 已消费 */
    @Column(nullable = false, length = 10)
    private String status = "待发送";

    /** 重试次数 */
    @Column(nullable = false)
    private Integer retryCount = 0;

    /** 最大重试次数 */
    @Column(nullable = false)
    private Integer maxRetryCount = 10;

    /** 下次重试时间 */
    private LocalDateTime nextRetryAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
