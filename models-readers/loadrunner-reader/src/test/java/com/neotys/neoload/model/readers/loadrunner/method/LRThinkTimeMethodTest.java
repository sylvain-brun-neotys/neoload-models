package com.neotys.neoload.model.readers.loadrunner.method;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.neotys.neoload.model.listener.TestEventListener;
import com.neotys.neoload.model.parsers.CPP14Parser.MethodcallContext;
import com.neotys.neoload.model.readers.loadrunner.ImmutableMethodCall;
import com.neotys.neoload.model.readers.loadrunner.LoadRunnerReader;
import com.neotys.neoload.model.readers.loadrunner.LoadRunnerVUVisitor;
import com.neotys.neoload.model.readers.loadrunner.MethodCall;
import com.neotys.neoload.model.repository.Delay;
import com.neotys.neoload.model.repository.ImmutableDelay;

public class LRThinkTimeMethodTest {
	
	public static final MethodCall LR_THINK_TIME = ImmutableMethodCall.builder()
			.name("\"lr_think_time\"")
			.addParameters("10")
			.build();

	private static final LoadRunnerReader LOAD_RUNNER_READER = new LoadRunnerReader(new TestEventListener(), "", "");
	private static final LoadRunnerVUVisitor LOAD_RUNNER_VISITOR = new LoadRunnerVUVisitor(LOAD_RUNNER_READER, "{", "}", "");
	private static final MethodcallContext METHOD_CALL_CONTEXT = new MethodcallContext(null, 0);
	
	@Test
	public void testGetElement() {		
		
		final Delay actualDelay = (Delay) (new LRThinkTimeMethod()).getElement(LOAD_RUNNER_VISITOR, LR_THINK_TIME, METHOD_CALL_CONTEXT);

		final Delay expectedDelay = ImmutableDelay.builder()
				.name("delay")
				.delay("10000")
				.build();	

		assertEquals(expectedDelay, actualDelay);
	}

}
