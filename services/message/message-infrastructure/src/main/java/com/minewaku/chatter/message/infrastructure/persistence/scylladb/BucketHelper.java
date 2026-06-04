package com.minewaku.chatter.message.infrastructure.persistence.scylladb;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.domain.sharedkernel.service.TimeBasedIdGenerator;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class BucketHelper {
    
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    
    public int calculateWeeklyBucket(long messageId) {
        long timestampMillis = timeBasedIdGenerator.toTimeStamp(messageId);
        return calculateBucketFromTimestamp(timestampMillis);
    }

    public int calculateWeeklyBucket(Instant timestamp) {
        return calculateBucketFromTimestamp(timestamp.toEpochMilli());
    }

    private int calculateBucketFromTimestamp(long timestampMillis) {
        ZonedDateTime dateTime = Instant.ofEpochMilli(timestampMillis)
                                      .atZone(ZoneId.systemDefault());
                                      
        int year = dateTime.get(IsoFields.WEEK_BASED_YEAR);
        int week = dateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        
        return year * 100 + week;
    }
}