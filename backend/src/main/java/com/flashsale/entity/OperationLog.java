package com.flashsale.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
