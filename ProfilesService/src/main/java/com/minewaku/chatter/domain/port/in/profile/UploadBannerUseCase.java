package com.minewaku.chatter.domain.port.in.profile;

import com.minewaku.chatter.domain.command.profile.UploadFileCommand;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;

public interface UploadBannerUseCase extends UseCaseHandler<UploadFileCommand, Void> {
}
