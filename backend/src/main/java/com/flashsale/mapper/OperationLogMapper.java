package com.flashsale.mapper;

import com.flashsale.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 新增操作日志
     */
    int insert(OperationLog log);
}
