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
        
        ZonedDateTime dateTime = Instant.ofEpochMilli(timestampMillis)
                                      .atZone(ZoneId.systemDefault());
                                      
        // Sử dụng WEEK_BASED_YEAR thay vì getYear() thông thường để tránh lỗi 
        // vào những ngày cuối năm/đầu năm bị lệch tuần chuẩn ISO.
        int year = dateTime.get(IsoFields.WEEK_BASED_YEAR);
        int week = dateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        
        return year * 100 + week; // Trả về định dạng yyyyWW (VD: 202605)
    }
}
