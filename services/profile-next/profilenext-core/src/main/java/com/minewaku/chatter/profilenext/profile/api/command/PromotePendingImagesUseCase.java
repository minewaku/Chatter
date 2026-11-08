package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;

/** Moves images that were applied to a profile from temporary to permanent storage. Run on a schedule. */
public interface PromotePendingImagesUseCase
        extends UseCaseHandler<PromotePendingImagesUseCase.Command, PromotePendingImagesUseCase.Result> {

    record Command() {}

    /**
     * {@code promoted}: images moved in this run. {@code failed}: attempts that failed in this run, including those
     * given up. {@code reported}: of those, images given up in this run (their slot is cleared if it still held them).
     */
    record Result(int promoted, int failed, int reported) {}
}
