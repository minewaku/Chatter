package com.minewaku.chatter.identityaccess.user;
import java.time.LocalDate;
public interface RegisterUserUseCase { Result handle(Command c); record Command(String email,String username,LocalDate birthday,String password){} sealed interface Result permits Registered,AccountAlreadyExists{} record Registered(long userId) implements Result{} record AccountAlreadyExists() implements Result{} }