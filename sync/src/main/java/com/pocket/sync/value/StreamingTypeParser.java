package com.pocket.sync.value;

import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

/**
 * Methods for creating types from some kind of information such as json.
 */
public interface StreamingTypeParser<T> {
	/**
	 * Create a new instance of this type from this json parser.
	 */
	T create(JsonParser parser) throws IOException;
}
