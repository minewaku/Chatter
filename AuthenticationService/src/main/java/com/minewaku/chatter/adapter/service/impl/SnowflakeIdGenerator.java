package com.minewaku.chatter.adapter.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.minewaku.chatter.adapter.db.postgresql.VariableServerRepository;
import com.minewaku.chatter.domain.port.out.service.IdGenerator;


/**
 * Author: Guizhou Not Enough Technology Co., Ltd
 * Github: @see <a href="https://github.com/chinabugotech" /a>
 * Source code: @see <a href="https://www.cnblogs.com/relucent/p/4955340.html" /a>
 * 
 * Note: Note: This snippet has been modified to fit the project configuration, and the comments have been translated into English 
 */
@Service
public class SnowflakeIdGenerator implements IdGenerator {
	
	// ==============================Fields===========================================
    /** Start timestamp (2015-01-01) */
    private final long twepoch = 1420041600000L;

    /** Allowed system clock rollback tolerance (2 seconds) */
    private final long timeOffset = 2000L;

    /** Number of bits allocated for worker ID */
    private final long workerIdBits = 5L;

    /** Number of bits allocated for datacenter ID */
    private final long datacenterIdBits = 5L;

    /** Maximum supported worker ID, result is 31 (bit shift trick to calculate max value from N bits) 
     * 
     *  -1L                = 111111...11111111
	 *	(-1L << 5)         = 111111...11100000
	 *	XOR                = 000000...00011111  (equals to 31)
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /** Maximum supported datacenter ID, result is 31 */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    /** Number of bits allocated for sequence */
    private final long sequenceBits = 12L;

    /** Worker ID is shifted left by 12 bits */
    private final long workerIdShift = sequenceBits;

    /** Datacenter ID is shifted left by 17 bits (12+5) */
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    /** Timestamp is shifted left by 22 bits (5+5+12) */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /** Sequence mask, here equals 4095 (0b111111111111 = 0xfff = 4095) */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);
    
    /** Worker ID (0~31) */
    private long workerId;

    /** Datacenter ID (0~31) */
    private long datacenterId;

    /** Sequence within the same millisecond (0~4095) */
    private long sequence = 0L;

    /** Timestamp of the last generated ID */
    private long lastTimestamp = -1L;

    // ==============================Constructors=====================================
    /**
     * Constructor
     * @param workerId Worker ID (0~31)
     * @param datacenterId Datacenter ID (0~31)
     */
    public SnowflakeIdGenerator(
    		@Value("${worker.id:0}") long workerId,
            VariableServerRepository variableServerRepository) {

        long newDatacenterId = variableServerRepository.getServerId();
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (newDatacenterId > maxDatacenterId || newDatacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = newDatacenterId;
    }

    // ==============================Methods==========================================
    /**
     * Get the next ID (this method is thread-safe)
     * @return SnowflakeId
     */
	@Override
    public synchronized long generate() {
        long timestamp = timeGen();

        // Current timestamp is less than the last generated timestamp
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            // Clock rollback is within the allowed range, e.g., due to leap second
            if (offset < timeOffset) {
                timestamp = lastTimestamp;
            }
            // Clock rollback exceeds tolerance, throw exception
            else {
                throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
            }
        }

        // If generating within the same millisecond, increment sequence
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // Sequence overflow within the millisecond
            if (sequence == 0) {
                // Block until next millisecond, then get new timestamp
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        // Timestamp changed, reset sequence
        else {
            sequence = 0L;
        }

        // Update last timestamp
        lastTimestamp = timestamp;

        // Shift and combine with OR operation to form a 64-bit ID
        return ((timestamp - twepoch) << timestampLeftShift) //
                | (datacenterId << datacenterIdShift) //
                | (workerId << workerIdShift) //
                | sequence;
    }

    /**
     * Block until the next millisecond, until a new timestamp is obtained
     * @param lastTimestamp Last generated timestamp
     * @return Current timestamp
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * Return the current time in milliseconds
     * @return Current timestamp (ms)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
