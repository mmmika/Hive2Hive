package org.hive2hive.core.extras.buffer;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hive2hive.core.api.interfaces.IFileManager;
import org.hive2hive.core.exceptions.IllegalFileLocation;
import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.core.extras.Extra;
import org.hive2hive.core.processes.files.list.FileTaste;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.hive2hive.processframework.exceptions.ProcessExecutionException;
import org.hive2hive.processframework.interfaces.IProcessComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extra
public class AddFileBuffer extends BaseFileBuffer {

	private static final Logger logger = LoggerFactory.getLogger(AddFileBuffer.class);

	public AddFileBuffer(IFileManager fileManager) {
		super(fileManager);
	}

	@Override
	protected void processBuffer(IFileBufferHolder buffer) {
		Set<File> fileBuffer = filterBuffer(buffer.getFileBuffer(), buffer.getSyncFiles());

		for (File toAdd : fileBuffer) {
			try {
				IProcessComponent<?> process = fileManager.add(toAdd);
				if (!fileManager.isAutostart()) {
					process.execute();
				}
			} catch (NoSessionException | NoPeerConnectionException | IllegalFileLocation | InvalidProcessStateException ex) {
				logger.error("Cannot start a process to add {}", toAdd.getName(), ex);
			} catch (ProcessExecutionException ex) {
				logger.error("Process execution to add {} failed.", toAdd.getName(), ex);
			}
		}
	}

	private Set<File> filterBuffer(List<File> fileBuffer, Set<FileTaste> syncFiles) {
		// remove the files from the buffer which are already in the DHT
		// the event has been triggered by Hive2Hive when downloading it.
		for (FileTaste syncFile : syncFiles) {
			fileBuffer.remove(syncFile.getFile());
		}

		Set<File> filtered = new HashSet<File>(fileBuffer);

		// only keep top-parent(s) to the buffer, filter out the rest
		for (File bufferedFile : fileBuffer) {
			// iterate through every file in buffer and check if there is a parent in the buffer
			for (File possibleParent : fileBuffer) {
				if (!bufferedFile.equals(possibleParent)
						&& bufferedFile.getAbsolutePath().startsWith(possibleParent.getAbsolutePath())) {
					logger.debug("Parent ({}) already in buffer, no need to add child ({}), too.",
							possibleParent.getAbsolutePath(), bufferedFile.getAbsolutePath());
					filtered.remove(bufferedFile);
				}
			}
		}

		return filtered;
	}

}