package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.ConfirmImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException;
import com.minewaku.chatter.profilenext.profile.api.exception.UploadConfirmationRejectedException.Reason;
import com.minewaku.chatter.profilenext.profile.internal.model.ConfirmedUpload;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository.Replacement;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a provider confirmation once. A repeat finds its upload id already stored and changes nothing; a stale one
 * (older than what is stored) changes nothing and its file is queued for removal. A file that breaks the format or
 * size rule is <em>returned</em> as {@code FILE_NOT_ALLOWED}, not thrown, so the queued removal commits.
 */
@Service
@Transactional
public class ConfirmImageUploadService implements ConfirmImageUploadUseCase {

    private final ImageStorage storage;
    private final ProfileRepository profiles;
    private final PendingImageRemovalRepository removals;
    private final ImagePolicy policy;
    private final Clock clock;

    public ConfirmImageUploadService(
            ImageStorage storage,
            ProfileRepository profiles,
            PendingImageRemovalRepository removals,
            ImagePolicy policy,
            Clock clock) {
        this.storage = storage;
        this.profiles = profiles;
        this.removals = removals;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        Instant now = clock.instant();
        ConfirmedUpload upload = storage.verify(command.rawBody(), command.signature(), command.timestamp(), now)
                .orElseThrow(() -> new UploadConfirmationRejectedException(Reason.INVALID_SIGNATURE));
        if (!upload.publicIdMatchesOwner()) {
            throw new UploadConfirmationRejectedException(Reason.USER_MISMATCH);
        }
        if (!policy.allows(upload.format()) || upload.bytes() > policy.maxBytes(upload.slot())) {
            removals.enqueue(upload.publicId(), upload.slot(), now);
            return new Result(Outcome.FILE_NOT_ALLOWED);
        }

        Replacement replacement = profiles.replaceImage(upload.userId(), upload.slot(), upload.image(), now);
        if (replacement.applied()) {
            if (replacement.previous() != null) {
                removals.enqueue(replacement.previous().publicId(), upload.slot(), now);
            }
            return new Result(Outcome.APPLIED);
        }
        boolean repeatOfTheStoredOne = replacement.previous() != null
                && replacement.previous().publicId().equals(upload.publicId());
        if (!repeatOfTheStoredOne) {
            removals.enqueue(upload.publicId(), upload.slot(), now);
        }
        return new Result(Outcome.IGNORED);
    }
}
