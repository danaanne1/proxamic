package com.theunknowablebits.proxamic;

import java.nio.ByteBuffer;

/** All document classes implement this */
public interface Document {

	
	<T extends DocumentView> T as(final Class<T> documentClass);

	<T extends DocumentView> T newInstance(Class<T> documentClass);

	ByteBuffer asByteBuffer();
	
	byte [] asBytes();
	
}
