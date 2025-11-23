package com.minewaku.chatter.services;

import java.util.function.Supplier;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;


public interface IRateLimitService {
	Supplier<BucketConfiguration> bucketConfiguration(Bandwidth bandwidth);
	Bucket redisBucket(String key, Bandwidth bandwidth);
}
