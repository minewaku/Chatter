# Snowflake ID — Technical Guide

## Overview

Snowflake ID is a distributed unique identifier strategy. It generates **64-bit integers** that are unique, compact, and time-sortable without requiring central coordination.

---

## 1. Structure

A Snowflake ID splits the 64 bits into logical parts:

- **Sign bit (1 bit):** unused, kept as `0` so IDs remain positive.  
- **Timestamp delta (41 bits):** milliseconds since a chosen custom epoch.  
- **Node identifier (10 bits):** identifies the machine/worker/datacenter to ensure uniqueness across nodes.  
- **Sequence (12 bits):** a counter within the same millisecond to allow multiple IDs per node per millisecond.  

**Example (Twitter Snowflake):**


---

## 2. Meaning of Each Part

- **Timestamp** → makes IDs naturally sortable by creation time.  
- **Node ID** → ensures no collision between nodes.  
- **Sequence** → allows thousands of IDs per millisecond on a single node.  

With the default allocation (`41 | 10 | 12`):

- ~69 years lifetime,  
- Up to **1024 nodes**,  
- Up to **4096 IDs/ms/node** (~4 million IDs/s per node).  

---

## 3. Components Needed

To implement Snowflake ID you need:

1. **Custom epoch** (start time).  
2. **Node ID** (unique per worker or datacenter).  
3. **Sequence counter** (resets every millisecond, increments within the same millisecond).  
4. **Last timestamp** (to detect same millisecond or clock rollback).  
5. **Thread safety** (sequence and timestamp must be updated atomically).  

---

## 4. ID Generation Steps

1. Get current timestamp (`now`).  
2. Compute `delta = now - epoch`.  
3. If `delta == lastTimestamp`: increment sequence.  
   - If sequence exceeds max, wait for the next millisecond.  
4. If `delta > lastTimestamp`: reset sequence to 0.  
5. If `delta < lastTimestamp`: handle clock rollback (wait or adjust).  
6. Combine `timestamp`, `nodeId`, and `sequence` into a 64-bit integer:  

```text
id = (delta << (nodeBits + sequenceBits))
   | (nodeId << sequenceBits)
   | sequence

   