package com.aigc.common.model;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果包装类（非 MyBatis-Plus Page 版本，用于手动构造分页响应）
 * MyBatis-Plus 的 Page 对象已实现分页，此类用于需要自定义分页格式的场景
 */
@Data
public class PageResult<T> {
    /** 总记录数 */
    private long total;
    /** 总页数 */
    private long pages;
    /** 当前页码（从 1 开始） */
    private long current;
    /** 每页大小 */
    private long size;
    /** 当前页数据列表 */
    private List<T> records;

    /**
     * 静态工厂方法，便于链式构造
     */
    public static <T> PageResult<T> of(long total, long pages, long current, long size, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.total = total;
        result.pages = pages;
        result.current = current;
        result.size = size;
        result.records = records;
        return result;
    }
}
