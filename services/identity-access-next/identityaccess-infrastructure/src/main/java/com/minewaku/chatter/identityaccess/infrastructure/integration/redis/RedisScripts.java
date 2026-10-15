package com.minewaku.chatter.identityaccess.infrastructure.integration.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** Loads the Lua scripts under {@code classpath:redis/}. */
public final class RedisScripts {

    private RedisScripts() {}

    public static <T> DefaultRedisScript<T> load(String fileName, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/" + fileName));
        script.setResultType(resultType);
        return script;
    }
}
