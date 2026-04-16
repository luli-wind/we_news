package com.course.newsplatform.service.impl;

import com.course.newsplatform.common.SecurityUtils;
import com.course.newsplatform.entity.AuditLog;
import com.course.newsplatform.entity.OperationLog;
import com.course.newsplatform.mapper.AuditLogMapper;
import com.course.newsplatform.mapper.OperationLogMapper;
import com.course.newsplatform.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final AuditLogMapper auditLogMapper;
    private final OperationLogMapper operationLogMapper;

    @Override
    public void operation(String module, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setModuleName(module);
        log.setActionName(action);
        try {
            log.setOperatorId(SecurityUtils.currentUserId());
        } catch (Exception ignored) {
            log.setOperatorId(null);
        }
        log.setDetail(detail);
        log.setIp("N/A");
        operationLogMapper.insert(log);
    }

    @Override
    public void audit(String bizType, Long bizId, String action, String remark) {
        AuditLog log = new AuditLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setAction(action);
        log.setOperatorId(SecurityUtils.currentUserId());
        log.setRemark(remark);
        auditLogMapper.insert(log);
    }
}
