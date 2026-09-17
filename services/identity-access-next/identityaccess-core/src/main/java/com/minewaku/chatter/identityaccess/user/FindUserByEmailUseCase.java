package com.minewaku.chatter.identityaccess.user;
import java.time.*; import java.util.*;
public interface FindUserByEmailUseCase { Optional<Result> handle(Query q); record Query(String email){} record Result(long userId,String email,String username,LocalDate birthday,String status,boolean accessible,Instant deletedAt,Instant createdAt,Instant updatedAt){} }