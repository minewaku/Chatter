package com.minewaku.chatter.domain.value;

import java.time.LocalDate;
import java.time.Period;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

	@JsonCreator
    public Birthday(
    		@JsonProperty("value") @NonNull LocalDate value) {
		
        if(value.isAfter(LocalDate.now())) {
        	throw new IllegalArgumentException("Birthday cannot be in the future");
        }
        if(Period.between(value, LocalDate.now()).getYears() > 150) {
        	throw new IllegalArgumentException("Age seems invalid");
        }
        
        this.value = value;
    }
	
    public int getAge() {
        return Period.between(value, LocalDate.now()).getYears();
    }
}
