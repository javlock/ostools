package com.github.javlock.ostools.executor;

public interface ExecutorMasterOutputListener {

	void appendInput(String line);

	void appendOutput(String line) throws Exception;

	void startedProcess(Long pid);

}
