package com.ethan.janus.core.config;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ExpressionRootObject {

    private Object targetBean;

    /**
     * 拼接 key
     * @param values 拼接使用的参数
     * @return key 字符串
     */
    public String buildKey(Object... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            if (value != null) {
                sb.append(value);
            } else {
                sb.append("#");
            }
            sb.append("_");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
