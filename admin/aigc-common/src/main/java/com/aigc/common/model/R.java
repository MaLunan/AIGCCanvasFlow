package com.aigc.common.model;

import com.aigc.common.enums.ResultCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应包装类
 * 所有接口返回格式：{ code, message, data }
 * 前端 axios 拦截器根据 code === 200 判断是否成功
 */
@Data
public class R<T> implements Serializable {

    /** 业务状态码（200 = 成功，其他 = 各种错误） */
    private int code;
    /** 响应消息（成功或错误描述） */
    private String message;
    /** 响应数据，成功时携带，失败时为 null */
    private T data;

    // 私有构造，强制使用静态工厂方法创建，避免直接 new
    private R() {}

    /** 成功（无数据）*/
    public static <T> R<T> ok() {
        return ok(null);
    }

    /** 成功（携带数据）*/
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code    = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data    = data;
        return r;
    }

    /** 失败（使用预定义错误码枚举）*/
    public static <T> R<T> fail(ResultCode resultCode) {
        R<T> r = new R<>();
        r.code    = resultCode.getCode();
        r.message = resultCode.getMessage();
        return r;
    }

    /** 失败（自定义错误码和消息，用于业务异常场景）*/
    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code    = code;
        r.message = message;
        return r;
    }

    /** 判断当前响应是否为成功状态 */
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}
