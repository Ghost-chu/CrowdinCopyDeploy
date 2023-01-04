package com.ghostchu.crowdincopydeploy.task;

import com.ghostchu.crowdincopydeploy.exception.UnirestRequestException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import me.tongfei.progressbar.ProgressBar;

import java.io.File;

public class DownloadFileTask implements Runnable {
    private final String url;
    private final File to;

    public DownloadFileTask(String url, File to) {
        this.url = url;
        this.to = to;
    }

    @Override
    public void run() {
        try (UnirestInstance downloadInstance = Unirest.spawnInstance();
             ProgressBar pb = new ProgressBar("Downloading", 1)) {
            HttpResponse<File> download = downloadInstance.get(url)
                    .downloadMonitor(((field, fileName, bytesWritten, totalBytes) -> {
                        pb.maxHint(totalBytes);
                        pb.stepTo(bytesWritten);
                    }))
                    .asFile(to.getPath());
            downloadInstance.shutDown();
            if (!download.isSuccess())
                throw new UnirestRequestException("Downloading " + url, download);
        }
    }
}
