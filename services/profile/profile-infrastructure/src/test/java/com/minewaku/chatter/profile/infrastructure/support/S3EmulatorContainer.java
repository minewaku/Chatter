package com.minewaku.chatter.profile.infrastructure.support;

import java.time.Duration;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * A throwaway S3-compatible server for the one test that proves the SDK calls mean what the adapter thinks. MinIO is not
 * used: its community edition is archived upstream. Swapping the emulator costs this one class.
 */
public final class S3EmulatorContainer extends GenericContainer<S3EmulatorContainer> {

    public static final String ACCESS_KEY = "emulatoradmin";
    public static final String SECRET_KEY = "emulatorsecret123";

    /** RustFS, pinned by digest (chosen in the emulator spike, see specs/015…/research.md D16). */
    private static final String IMAGE =
            "rustfs/rustfs@sha256:b81d16a90ee25bd9511280127ca1471788c6e874cf0f0da759645f46b2dc1d74";

    public S3EmulatorContainer() {
        super(IMAGE);
        withExposedPorts(9000);
        withEnv("RUSTFS_ACCESS_KEY", ACCESS_KEY);
        withEnv("RUSTFS_SECRET_KEY", SECRET_KEY);
        withTmpFs(Map.of("/data", "rw,mode=1777", "/logs", "rw,mode=1777"));
        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));
    }

    public String endpoint() {
        return "http://" + getHost() + ":" + getMappedPort(9000);
    }
}
