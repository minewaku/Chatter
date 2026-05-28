package com.minewaku.chatter.profile.application.messaging.publisher;

import java.util.List;

public interface EventDispatcher<T> {
    void dispatch(T event);
    void dispatch(List<? extends T> events); 
}

