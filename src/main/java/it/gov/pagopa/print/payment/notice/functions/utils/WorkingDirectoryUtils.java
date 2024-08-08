package it.gov.pagopa.print.payment.notice.functions.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Utils methods for working directory
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkingDirectoryUtils {

    public static File createWorkingDirectory() throws IOException {
        File workingDirectory = new File("temp");
        if(!workingDirectory.exists()) {
            Files.createDirectory(workingDirectory.toPath());
        }
        return workingDirectory;
    }

    public static void clearTempDirectory(java.nio.file.Path workingDirPath) {
        try {
            FileUtils.deleteDirectory(workingDirPath.toFile());
        } catch (IOException e) {
            log.warn("Unable to clear working directory", e);
        }
    }

}
