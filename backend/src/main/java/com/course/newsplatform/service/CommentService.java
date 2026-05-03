package com.course.newsplatform.service;

import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.CommentCreateRequest;
import com.course.newsplatform.dto.CommentView;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.entity.Comment;

import java.util.List;

public interface CommentService {

    Long create(CommentCreateRequest request);

    List<CommentView> listByBiz(String bizType, Long bizId);

    PageResponse<CommentView> myComments(PageQuery query);

    void delete(Long id);

    void auditDelete(Long id);

    Comment getById(Long id);

    PageResponse<CommentView> adminListAll(String bizType, int page, int pageSize);
}
