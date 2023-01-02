package com.ghostchu.crowdincopydeploy.task;

import com.crowdin.client.Client;
import com.crowdin.client.translations.model.ProjectBuild;
import lukfor.progress.tasks.ITaskRunnable;
import lukfor.progress.tasks.monitors.ITaskMonitor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class TranslationBuildMonitorTask implements ITaskRunnable {
    private final static long POLL_SECONDS = 3;
    private final Instant START_TIME = Instant.now();
    private final long buildId;
    private final long projectId;
    private final Client client;
    private long previousProgress = 0L;
    private String lastStatus = "unknown";

    public TranslationBuildMonitorTask(@NotNull Client client, long projectId, long buildId) {
        this.client = client;
        this.buildId = buildId;
        this.projectId = projectId;
    }

    @Override
    public void run(@NotNull ITaskMonitor monitor) throws Exception {
        monitor.begin("Translation Build", 100);
        long MAX_WAIT_SECONDS = 3600L;
        while (!Instant.now().isAfter(START_TIME.plusSeconds(MAX_WAIT_SECONDS))) {
            //noinspection BusyWait
            Thread.sleep(POLL_SECONDS * 1000L);
            if (poll(monitor)) {
                break;
            }
        }
    }

    public boolean poll(@NotNull ITaskMonitor monitor) {
        try {
            ProjectBuild projectBuild = client
                    .getTranslationsApi()
                    .checkBuildStatus(projectId, buildId)
                    .getData();
            long newProgress = projectBuild.getProgress();
            monitor.worked(newProgress - previousProgress);
            previousProgress = newProgress;
            lastStatus = projectBuild.getStatus();
            return projectBuild.getStatus().equalsIgnoreCase("finished") || projectBuild.getStatus().equalsIgnoreCase("failed") || projectBuild.getStatus().equalsIgnoreCase("canceled");
        } catch (Exception e) {
            return false;
        }
    }

    @NotNull
    public String getLastStatus() {
        return lastStatus;
    }
}
