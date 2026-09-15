package com.minewaku.chatter.identityaccess.domain.sharedkernel.value;

public abstract class AggregateRoot<T extends Id> {

    public abstract T getId();

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AggregateRoot<?> that = (AggregateRoot<?>) o;
        if (this.getId() == null || that.getId() == null) {
            return false;
        }
        return this.getId().equals(that.getId());
    }

    @Override
    public final int hashCode() {
        return getId() == null ? super.hashCode() : getId().hashCode();
    }
}