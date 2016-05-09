/*
 * This file is part of RESTjob Controller.
 *
 * RESTjob Controller is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * RESTjob Controller is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * RESTjob Controller. If not, see http://www.gnu.org/licenses/.
 */
package com.restjob.publishers.filesystem;

import com.restjob.controller.logging.Logger;
import com.restjob.controller.model.Job;
import com.restjob.publishers.BasePublisher;
import com.restjob.util.PayloadUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class FileSystemPublisher extends BasePublisher {

    // Setup logging
    private static final Logger logger = Logger.getLogger(FileSystemPublisher.class);

    private String publishPath;

    @Override
    public boolean initialize(Job job) {
        super.initialize(job);
        Map<String, String> params = PayloadUtil.toParameters(job.getPayload());
        if (!PayloadUtil.requiredParams(params, "publishPath")) {
            job.addMessage("Invalid request. Expected parameter: [publishPath]");
            return false;
        }
        publishPath = MapUtils.getString(params, "publishPath");
        return true;
    }

    public boolean publish(Job job) {
        File path = new File(publishPath);
        if (!path.exists()) {
            job.addMessage("Specified publishPath does not exist.");
            return false;
        } else if (!path.isDirectory()) {
            job.addMessage("Specified publishPath is not a valid directory.");
            return false;
        } else if (!path.canWrite()) {
            job.addMessage("Cannot write to the specified publishPath.");
            return false;
        }
        File report = new File(path, job.getUuid());
        try {
            FileUtils.writeByteArrayToFile(report, getResult());
            job.addMessage("Report written to: " + report.getAbsolutePath());
        } catch (IOException e) {
            job.addMessage(e.getMessage());
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    public String getName() {
        return "File System";
    }

    public String getDescription() {
        return "Publishes results to the file system.";
    }
}