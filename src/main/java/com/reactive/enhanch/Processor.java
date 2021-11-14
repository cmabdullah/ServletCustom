package com.reactive.enhanch;

import java.nio.channels.SelectionKey;

public interface Processor {
	void read();
	void write();
	default void connect(SelectionKey selectionKey){
		System.out.println("Calling connector");
	}

	String getRequestId();
}
