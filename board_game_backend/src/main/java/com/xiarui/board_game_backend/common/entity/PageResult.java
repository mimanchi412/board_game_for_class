package com.xiarui.board_game_backend.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分页响应结果类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends Result<List<T>> {
    
    /** 当前页码 */
    private Long current;
    
    /** 每页显示条数 */
    private Long size;
    
    /** 总记录数 */
    private Long total;
    
    /** 总页数 */
    private Long pages;
    
    public PageResult() {
        super();
    }
    
    public PageResult(Long current, Long size, Long total, List<T> data) {
        super(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
        this.current = current;
        this.size = size;
        this.total = total;
        this.pages = (total + size - 1) / size; // 计算总页数
    }
    
    public PageResult(Long current, Long size, Long total, List<T> data, String message) {
        super(ResultCode.SUCCESS.getCode(), message, data);
        this.current = current;
        this.size = size;
        this.total = total;
        this.pages = (total + size - 1) / size; // 计算总页数
    }
    
    /**
     * 创建分页成功响应
     */
    public static <T> PageResult<T> success(Long current, Long size, Long total, List<T> data) {
        return new PageResult<>(current, size, total, data);
    }
    
    /**
     * 创建分页成功响应（带自定义消息）
     */
    public static <T> PageResult<T> success(Long current, Long size, Long total, List<T> data, String message) {
        return new PageResult<>(current, size, total, data, message);
    }
    
    /**
     * 创建空分页响应
     */
    public static <T> PageResult<T> empty(Long current, Long size) {
        return new PageResult<>(current, size, 0L, null);
    }
}