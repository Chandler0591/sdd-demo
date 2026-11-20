package com.library.id;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 统一雪花 ID 生成器：所有实体字符串主键由该生成器赋值，
 * 机器号来自配置项 library.snowflake.worker-id（多实例部署时每台机器需不同）。
 */
@Component
public class SnowflakeIdGenerator {

    private final Snowflake snowflake;

    public SnowflakeIdGenerator(@Value("${library.snowflake.worker-id:1}") long workerId) {
        this.snowflake = IdUtil.getSnowflake(workerId);
    }

    /**
     * 生成 19 位雪花 ID 字符串。
     */
    public String nextId() {
        return snowflake.nextIdStr();
    }
}
