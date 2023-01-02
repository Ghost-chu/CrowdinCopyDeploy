package com.ghostchu.crowdincopydeploy.task;

import com.ghostchu.crowdincopydeploy.exception.UnirestRequestException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import lukfor.progress.tasks.ITaskRunnable;
import lukfor.progress.tasks.monitors.ITaskMonitor;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadFileTask implements ITaskRunnable {
    private final String url;
    private final File to;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private long lastWritten = 0;

    public DownloadFileTask(String url, File to) {
        this.url = url;
        this.to = to;
    }

    @Override
    public void run(ITaskMonitor monitor) throws Exception {
        try (UnirestInstance downloadInstance = Unirest.spawnInstance()) {
            HttpResponse<File> download = downloadInstance.get(url)
                    .downloadMonitor(((field, fileName, bytesWritten, totalBytes) -> {
                        if (!started.get()) {
                            monitor.begin("Downloading from " + url, totalBytes);
                        } else {
                            monitor.worked(bytesWritten - lastWritten);
                            lastWritten = bytesWritten;
                        }
                    }))
                    .asFile(to.getPath());
            downloadInstance.shutDown();
            if (!download.isSuccess())
                throw new UnirestRequestException("Downloading " + url, download);
        }
    }
}
