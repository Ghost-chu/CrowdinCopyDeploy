package com.ghostchu.crowdincopydeploy.task;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public class UnzipFileTask implements Runnable {
    public UnzipFileTask(File file, File target) {
        this.file = file;
        this.target = target;
    }

    private final File file;
    private final File target;
    @Override
    public void run() {
        try (ZipFile zipFile = new ZipFile(file); ProgressBar pb = new ProgressBar("Unzipping", 1)) {
               pb.maxHint(getSize(zipFile));
            byte[] buffer = new byte[4096];
            ZipArchiveEntry entry;
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            InputStream inputStream;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (entry.isDirectory()) {
                    pb.step();
                    continue;
                }
                File outputFile = new File(target, entry.getName());
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                inputStream = zipFile.getInputStream(entry);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    while (inputStream.read(buffer) > 0) {
                        fos.write(buffer);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                pb.step();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot unzip file " + file.getName(), e);
        }
    }

    public long getSize(@NotNull ZipFile zipFile){
        int size = 0;
        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
        while (entries.hasMoreElements()) {
            size++;
            entries.nextElement();
        }
        return size;
    }
}
