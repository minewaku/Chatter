package com.minewaku.chatter.services.impl;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.minewaku.chatter.services.IRateLimitService;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;

@Service
public class RateLimitService implements IRateLimitService {
	
    @Autowired
    public ProxyManager<String> lettuceBasedProxyManager;

    @Override
    public Supplier<BucketConfiguration> bucketConfiguration(Bandwidth bandwidth) {  
        return () -> BucketConfiguration.builder()
            .addLimit(bandwidth)
            .build();
    }

    @Override
    public Bucket redisBucket(String key, Bandwidth bandwidth) {
        Supplier<BucketConfiguration> configurationLazySupplier = bucketConfiguration(bandwidth);
        ProxyManager<String> proxyManager = lettuceBasedProxyManager;
        return proxyManager.builder().build(key, configurationLazySupplier);
    }
}
