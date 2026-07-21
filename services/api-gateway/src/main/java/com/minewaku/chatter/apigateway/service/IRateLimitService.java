package com.minewaku.chatter.apigateway.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import java.util.function.Supplier;

public interface IRateLimitService {
    Supplier<BucketConfiguration> bucketConfiguration(Bandwidth bandwidth);

    Bucket redisBucket(String key, Bandwidth bandwidth);
}
