package com.minewaku.chatter.profile.profile.internal.application.fakes;

import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** A transaction manager that only remembers what happened, so tests can see commits and rollbacks. */
public class RecordingTransactionManager implements PlatformTransactionManager {

    public final List<String> events = new ArrayList<>();

    public TransactionTemplate template() {
        return new TransactionTemplate(this);
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        events.add("begin");
        return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        events.add("commit");
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        events.add("rollback");
    }
}
