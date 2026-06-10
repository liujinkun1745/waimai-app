package com.waimai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

/**
 * Seata AT 模式 UndoLog 表（每分片数据库各建一份）
 */
@Entity
@Table(name = "undo_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UndoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(nullable = false, length = 128)
    private String xid;

    @Column(nullable = false, length = 128)
    private String context;

    @Column(name = "rollback_info", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] rollbackInfo;

    @Column(name = "log_status", nullable = false)
    private Integer logStatus;

    @Column(name = "log_created", nullable = false)
    private Timestamp logCreated;

    @Column(name = "log_modified", nullable = false)
    private Timestamp logModified;

    @PrePersist
    protected void onCreate() {
        logCreated = new Timestamp(System.currentTimeMillis());
        logModified = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        logModified = new Timestamp(System.currentTimeMillis());
    }
}
