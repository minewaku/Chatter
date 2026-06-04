package com.minewaku.chatter.profile.domain.sharedkernel.value;

public abstract class BaseEntity<ID extends Id> {

    public abstract ID getId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseEntity<?> that = (BaseEntity<?>) o;
        if (this.getId() == null || that.getId() == null) {
            return false;
        }
        return this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId() == null ? super.hashCode() : getId().hashCode();
    }
}