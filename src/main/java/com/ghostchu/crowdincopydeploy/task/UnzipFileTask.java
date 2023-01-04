package com.ghostchu.crowdincopydeploy.task;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jetbrains.annotations.NotNull;

import java.io.*;
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
            ZipArchiveEntry entry;
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
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
                InputStream inputStream = zipFile.getInputStream(entry);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(inputStream.readAllBytes());
                    fos.flush();
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
