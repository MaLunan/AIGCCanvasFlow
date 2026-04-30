package com.aigc.common.model;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long pages;
    private long current;
    private long size;
    private List<T> records;

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
