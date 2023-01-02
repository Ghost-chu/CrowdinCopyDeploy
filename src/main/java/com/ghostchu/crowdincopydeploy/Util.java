package com.ghostchu.crowdincopydeploy;

import com.ghostchu.crowdincopydeploy.exception.UnirestRequestException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Util {
    public static void downloadFile(@NotNull String url, @NotNull File saveTo) {
        try (UnirestInstance downloadInstance = Unirest.spawnInstance()) {
            HttpResponse<File> download = downloadInstance.get(url)
                    .asFile(saveTo.getPath());
            downloadInstance.shutDown();
            if (!download.isSuccess())
                throw new UnirestRequestException("Downloading " + url, download);
        }
    }

}
