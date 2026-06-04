package com.minewaku.chatter.profile.infrastructure.config.property;

import java.util.Arrays;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import com.minewaku.chatter.profile.domain.model.asset.model.AssetId;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

@Configuration
public class JdbcConverterConfig extends AbstractJdbcConfiguration {

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(Arrays.asList(
                new ProfileIdWritingConverter(),
                new ProfileIdReadingConverter(),

                new AssetIdWritingConverter(),
                new AssetIdReadingConverter()
        ));
    }

    @WritingConverter
    public static class ProfileIdWritingConverter implements Converter<ProfileId, Long> {
        @Override
        public Long convert(ProfileId source) {
            return source.getValue();
        }
    }

    @WritingConverter
    public static class AssetIdWritingConverter implements Converter<AssetId, Long> {
        @Override
        public Long convert(AssetId source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    public static class ProfileIdReadingConverter implements Converter<Long, ProfileId> {
        @Override
        public ProfileId convert(Long source) {
            return new ProfileId(source);
        }
    }

    @ReadingConverter
    public static class AssetIdReadingConverter implements Converter<Long, AssetId> {
        @Override
        public AssetId convert(Long source) {
            return new AssetId(source);
        }
    }
}
