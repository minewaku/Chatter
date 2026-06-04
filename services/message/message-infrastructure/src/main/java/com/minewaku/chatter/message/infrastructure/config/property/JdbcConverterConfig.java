package com.minewaku.chatter.message.infrastructure.config.property;

import java.util.Arrays;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import com.minewaku.chatter.message.domain.model.asset.model.AssetId;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.invite.model.InviteId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

@Configuration
public class JdbcConverterConfig extends AbstractJdbcConfiguration {

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(Arrays.asList(
                new AssetIdWritingConverter(),
                new AssetIdReadingConverter(),

                new GuildIdWritingConverter(),
                new GuildIdReadingConverter(),

                new ChannelIdWritingConverter(),
                new ChannelIdReadingConverter(),

                new UserIdWritingConverter(),
                new UserIdReadingConverter(),

                new InviteIdWritingConverter(),
                new InviteIdReadingConverter()
        ));
    }



    @WritingConverter
    public static class AssetIdWritingConverter implements Converter<AssetId, Long> {
        @Override
        public Long convert(AssetId source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    public static class AssetIdReadingConverter implements Converter<Long, AssetId> {
        @Override
        public AssetId convert(Long source) {
            return new AssetId(source);
        }
    }


    
    @WritingConverter
    public static class GuildIdWritingConverter implements Converter<GuildId, Long> {
        @Override
        public Long convert(GuildId source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    public static class GuildIdReadingConverter implements Converter<Long, GuildId> {
        @Override
        public GuildId convert(Long source) {
            return new GuildId(source);
        }
    }



    @WritingConverter
    public static class ChannelIdWritingConverter implements Converter<ChannelId, Long> {
        @Override
        public Long convert(ChannelId source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    public static class ChannelIdReadingConverter implements Converter<Long, ChannelId> {
        @Override
        public ChannelId convert(Long source) {
            return new ChannelId(source);
        }
    }



    @WritingConverter
    public static class UserIdWritingConverter implements Converter<UserId, Long> {
        @Override
        public Long convert(UserId source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    public static class UserIdReadingConverter implements Converter<Long, UserId> {
        @Override
        public UserId convert(Long source) {
            return new UserId(source);
        }
    }



    @WritingConverter
    public static class InviteIdWritingConverter implements Converter<InviteId, Long> {
        @Override
        public Long convert(InviteId source) {
            return source.getValue();
        }
    }

    @ReadingConverter
    public static class InviteIdReadingConverter implements Converter<Long, InviteId> {
        @Override
        public InviteId convert(Long source) {
            return new InviteId(source);
        }
    }
}
