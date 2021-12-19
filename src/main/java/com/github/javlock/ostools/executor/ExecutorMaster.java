package com.github.javlock.ostools.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExecutorMaster {
	ProcessBuilder processBuilder = new ProcessBuilder();
	private String parentProg;
	private ExecutorMasterOutputListener outputListener;

	CopyOnWriteArrayList<String> progs = new CopyOnWriteArrayList<>();
	OutputStreamWriter osw;
	private ArrayList<String> args = new ArrayList<>();

	public ExecutorMaster arg(String arg) {
		String[] ar = arg.split(" ");
		for (String string : ar) {
			args.add(string);
		}
		return this;
	}

	public int call() throws Exception {
		processBuilder.redirectErrorStream(true);

		ArrayList<String> realCmd = new ArrayList<>();
		realCmd.add(parentProg);
		realCmd.addAll(args);

		processBuilder.command(realCmd);
		Process process = processBuilder.start();
		outputListener.startedProcess(process.pid());

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			Thread commandAppender = new Thread((Runnable) () -> {
				OutputStream os = process.getOutputStream();
				osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);

				for (String string : progs) {
					try {
						osw.append(string);
						if (outputListener != null) {
							outputListener.appendInput(string);
						}
						osw.append('\n');
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try {
					osw.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}, parentProg);
			commandAppender.start();
			String line;
			while ((line = reader.readLine()) != null) {
				if (outputListener != null) {
					outputListener.appendOutput(line);
				}
			}
			int exitCode = process.waitFor();
			commandAppender.join();
			return exitCode;
		}

	}

	public ExecutorMaster command(String string) {
		progs.add(string);
		return this;
	}

	public ExecutorMaster dir(File repoDir) {
		processBuilder.directory(repoDir);
		return this;
	}

	public ExecutorMaster parrentCommand(String cmd) {
		parentProg = cmd;
		return this;
	}

	public ExecutorMaster setOutputListener(ExecutorMasterOutputListener listener) {
		this.outputListener = listener;
		return this;
	}

}
