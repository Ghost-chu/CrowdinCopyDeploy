package com.ghostchu.crowdincopydeploy.task;

import lukfor.progress.tasks.ITaskRunnable;
import lukfor.progress.tasks.monitors.ITaskMonitor;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class MoveDirectoryTask implements ITaskRunnable {
    private final File from;
    private final File to;
    public MoveDirectoryTask(File from, File to){
        this.from = from;
        this.to = to;
    }
    @Override
    public void run(ITaskMonitor monitor) throws Exception {
        monitor.begin("Move directory",-1);
        FileUtils.deleteDirectory(to);
        FileUtils.moveDirectory(from, to);
        monitor.done();
    }
}
