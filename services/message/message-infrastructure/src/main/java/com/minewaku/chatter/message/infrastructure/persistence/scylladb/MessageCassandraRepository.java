package com.minewaku.chatter.message.infrastructure.persistence.scylladb;

import org.springframework.data.cassandra.repository.CassandraRepository;

import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraEntity;
import com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity.MessageCassandraKeyEntity;

public interface MessageCassandraRepository extends CassandraRepository<MessageCassandraEntity, MessageCassandraKeyEntity> {

}
