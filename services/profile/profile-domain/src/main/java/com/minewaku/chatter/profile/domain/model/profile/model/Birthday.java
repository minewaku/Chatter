package com.minewaku.chatter.profile.domain.model.profile.model;

import java.time.LocalDate;
import java.time.Period;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.DomainValidationException;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Embeddable
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Birthday {
	
	@NonNull
	private LocalDate value;

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
