package com.course.newsplatform.service;

public interface LogService {

    void operation(String module, String action, String detail);

    void audit(String bizType, Long bizId, String action, String remark);
}
