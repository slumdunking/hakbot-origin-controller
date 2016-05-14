/*
 * This file is part of Hakbot Origin Controller.
 *
 * Hakbot Origin Controller is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Hakbot Origin Controller is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Hakbot Origin Controller. If not, see http://www.gnu.org/licenses/.
 */
package io.hakbot.providers.shell;

import io.hakbot.controller.logging.Logger;
import io.hakbot.controller.model.Job;
import io.hakbot.controller.workers.JobException;
import io.hakbot.providers.BaseProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class ShellProvider extends BaseProvider {

    // Setup logging
    private static final Logger logger = Logger.getLogger(ShellProvider.class);
    private Process process;

    public boolean process(Job job) {
        InputStream inputStream = null;
        job.setSuccess(false);
        try {
            ProcessBuilder pb = new ProcessBuilder(job.getProviderPayload().split(" "));
            process = pb.start();
            int exitCode = process.waitFor();
            byte[] stdout = IOUtils.toByteArray(process.getInputStream());
            byte[] stderr = IOUtils.toByteArray(process.getErrorStream());
            if (logger.isDebugEnabled()) {
                logger.debug("STDOUT:");
                logger.debug(new String(stdout));
                logger.debug("STDERR:");
                logger.debug(new String(stderr));
            }
            super.setResult(stdout);
            if (exitCode != 0) {
                if (StringUtils.isEmpty(super.getResult())) {
                    super.setResult(stderr);
                }
                throw new JobException(exitCode);
            }
        } catch (IOException | InterruptedException e) {
            String message = "Could not execute job.";
            logger.error(message);
            logger.error(e.getMessage());
            job.addMessage(message);
        } catch (JobException e) {
            String message = "Job terminated abnormally. Exit code: " + e.getExitCode();
            logger.error(message);
            logger.error(e.getMessage());
            job.addMessage(message);
        } finally {
            IOUtils.closeQuietly(inputStream);
            job.setCompleted(new Date());
        }
        if (job.getMessage() == null) {
            job.addMessage("Job execution successful");
            job.setSuccess(true);
        }
        return job.getSuccess();
    }

    public boolean cancel() {
        process.destroy();
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        return !process.isAlive();
    }

    public String getName() {
        return "Shell";
    }

    public String getDescription() {
        return "Executes a shell command or script and captures the output from STDOUT/STDERR.";
    }

    public String getResultMimeType() {
        return "text/plain";
    }

    public String getResultExtension() {
        return "txt";
    }

}
