package com.neotys.neoload.model.repository;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface VariableExtractor {
	enum ExtractType {
		BOTH, 
        BODY,
        HEADERS, 
        XPATH, 
        JSON
    }
	String getName();
	Optional<String> getStartExpression();
	Optional<String> getEndExpression();
	ExtractType getExtractType();
	Optional<Integer> getNbOccur();
	boolean getExitOnError();
	Optional<String> getRegExp();
	Optional<String> getGroup();
	Optional<String> getXPath();
	Optional<String> getJsonPath();
}
