package com.minewaku.chatter.identityaccess.domain.aggregate.user.model;

import java.time.LocalDate;
import java.time.Period;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Birthday {
	
	@NonNull
	private final LocalDate value;

    public Birthday(@NonNull LocalDate value) {
		
        if(value.isAfter(LocalDate.now())) {
        	throw new DomainValidationException("Birthday cannot be in the future");
        }
        if(Period.between(value, LocalDate.now()).getYears() > 150) {
        	throw new DomainValidationException("Age seems invalid");
        }
        
        this.value = value;
    }
	
    public int getAge() {
        return Period.between(value, LocalDate.now()).getYears();
    }
}
