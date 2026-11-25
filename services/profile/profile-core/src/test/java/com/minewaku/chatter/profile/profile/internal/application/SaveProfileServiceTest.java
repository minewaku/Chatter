package com.minewaku.chatter.profile.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profile.profile.api.command.SaveProfileUseCase;
import com.minewaku.chatter.profile.profile.api.exception.InvalidProfileValueException;
import com.minewaku.chatter.profile.profile.internal.application.command.SaveProfileService;
import com.minewaku.chatter.profile.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profile.profile.internal.model.Bio;
import com.minewaku.chatter.profile.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profile.profile.internal.model.DisplayName;
import com.minewaku.chatter.profile.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SaveProfileServiceTest {

    record Call(UserId userId, DisplayName displayName, Bio bio, Instant now) {}

    static class RecordingProfiles extends FakeProfileStore {
        final List<Call> calls = new ArrayList<>();
        boolean answer = true;

        @Override
        public boolean save(UserId userId, DisplayName displayName, Bio bio, Instant now) {
            calls.add(new Call(userId, displayName, bio, now));
            return answer;
        }
    }

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private final RecordingProfiles profiles = new RecordingProfiles();

    private SaveProfileService serviceAt(Instant instant) {
        return new SaveProfileService(profiles, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void savingANameOnAMissingProfileAppliesTheEditWithTheClockTime() {
        var result = serviceAt(NOW).handle(new SaveProfileUseCase.Command(7, "  Mine ", null));

        assertThat(result.changed()).isTrue();
        assertThat(profiles.calls).hasSize(1);
        Call call = profiles.calls.get(0);
        assertThat(call.userId()).isEqualTo(UserId.of(7));
        assertThat(call.displayName().value()).isEqualTo("Mine");
        assertThat(call.bio()).isNull();
        assertThat(call.now()).isEqualTo(NOW);
    }

    @Test
    void aBioOnlySavePassesNoName() {
        serviceAt(NOW).handle(new SaveProfileUseCase.Command(7, null, "Hello"));

        Call call = profiles.calls.get(0);
        assertThat(call.displayName()).isNull();
        assertThat(call.bio().value()).isEqualTo("Hello");
    }

    @Test
    void aBioSentAsEmptyClearsItRatherThanLeavingItUnchanged() {
        serviceAt(NOW).handle(new SaveProfileUseCase.Command(7, null, ""));

        assertThat(profiles.calls.get(0).bio().value()).isEmpty();
    }

    @Test
    void savingNothingChangesNothingAndCreatesNoRow() {
        var result = serviceAt(NOW).handle(new SaveProfileUseCase.Command(7, null, null));

        assertThat(result.changed()).isFalse();
        assertThat(profiles.calls).isEmpty();
    }

    @Test
    void reportsNoChangeWhenTheValuesEqualWhatIsStored() {
        profiles.answer = false;

        var result = serviceAt(NOW).handle(new SaveProfileUseCase.Command(7, "Mine", "Hello"));

        assertThat(result.changed()).isFalse();
    }

    @Test
    void anInvalidNameIsRefusedAndTheRepositoryIsNeverCalled() {
        assertThatThrownBy(() -> serviceAt(NOW).handle(new SaveProfileUseCase.Command(7, "x".repeat(33), "Hello")))
                .isInstanceOf(InvalidProfileValueException.class);

        assertThat(profiles.calls).isEmpty();
    }

    @Test
    void usesTheInjectedClock() {
        Instant later = NOW.plusSeconds(3600);

        serviceAt(NOW).handle(new SaveProfileUseCase.Command(7, "Mine", null));
        serviceAt(later).handle(new SaveProfileUseCase.Command(7, "Mine", null));

        assertThat(profiles.calls).extracting(Call::now).containsExactly(NOW, later);
    }

    @Test
    void savingTextLeavesBothImageSlotsUntouched() {
        FakeProfileStore store = new FakeProfileStore();
        store.save(UserId.of(7), DisplayName.of("Before"), null, NOW);
        store.replaceImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(1), NOW);
        store.replaceImage(UserId.of(7), ImageSlot.BANNER, ImageIds.at(2), NOW);

        var result = new SaveProfileService(store, Clock.fixed(NOW.plusSeconds(60), ZoneOffset.UTC))
                .handle(new SaveProfileUseCase.Command(7, "After", "New bio"));

        assertThat(result.changed()).isTrue();
        assertThat(store.rows.get(7L).displayName().value()).isEqualTo("After");
        assertThat(store.rows.get(7L).avatar()).isEqualTo(ImageIds.at(1));
        assertThat(store.rows.get(7L).banner()).isEqualTo(ImageIds.at(2));
    }

    @Test
    void anInvalidBioIsRefusedAndNothingIsWritten() {
        FakeProfileStore store = new FakeProfileStore();
        SaveProfileService service = new SaveProfileService(store, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.handle(new SaveProfileUseCase.Command(7, null, "x".repeat(5000))))
                .isInstanceOf(InvalidProfileValueException.class);
        assertThat(store.rows).isEmpty();
    }
}
