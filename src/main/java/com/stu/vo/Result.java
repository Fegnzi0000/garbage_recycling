package com.stu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Result {

    @Schema(description = "状态码（200-成功，其他-失败）", example = "200")
    private Integer code;

    @Schema(description = "提示信息", example = "操作成功")
    private String msg;

    @Schema(description = "响应数据")
    private Map<String, Object> data = new HashMap<>();

    public static Result success() {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("操作成功");
        return result;
    }

    public static Result success(Map<String, Object> data) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    public static Result success(Object data) {
        Result result = new Result();
        result.setCode(200);
        result.setMsg("操作成功");
        result.getData().put("result", data); // 将对象放入 map 中
        return result;
    }

    public static Result error(String message) {
        Result result = new Result();
        result.setCode(500);
        result.setMsg(message);
        return result;
    }

    public Result put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
