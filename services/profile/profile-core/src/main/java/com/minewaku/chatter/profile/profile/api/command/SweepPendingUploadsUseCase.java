package com.minewaku.chatter.profile.profile.api.command;

import com.minewaku.chatter.profile.shared.handler.UseCaseHandler;

/** Deletes the storage keys the upload ledger says are no longer needed. Run on a schedule. */
public interface SweepPendingUploadsUseCase
        extends UseCaseHandler<SweepPendingUploadsUseCase.Command, SweepPendingUploadsUseCase.Result> {

    record Command() {}

    /** {@code deleted}: keys removed in this run. {@code failed}: keys whose delete failed and was postponed. */
    record Result(int deleted, int failed) {}
}
