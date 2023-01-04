package com.ghostchu.crowdincopydeploy.task;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import me.tongfei.progressbar.ProgressBar;

import java.io.File;

public class UploadS3Task implements Runnable{
    private final String s3Bucket;
    private final File deployPath;
    private final AmazonS3 s3;
    private final String s3Dest;
    public UploadS3Task(AmazonS3 s3, String s3Bucket, File deployPath,  String s3Dest) {
        this.s3Bucket = s3Bucket;
        this.deployPath = deployPath;
        this.s3 = s3;
        this.s3Dest = s3Dest;
    }

    @Override
    public void run() {
        try(ProgressBar pb = new ProgressBar("S3 upload", 1)) {
            TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3).build();
            MultipleFileUpload upload = tm.uploadDirectory(s3Bucket, s3Dest, deployPath, true);
            while (!upload.isDone()){
                Thread.sleep(200);
                if(upload.getProgress().getTotalBytesToTransfer() != -1)
                    pb.maxHint(upload.getProgress().getTotalBytesToTransfer());
                pb.stepTo(upload.getProgress().getBytesTransferred());
                updateProgressBarStatus(pb, upload.getState());
            }
            updateProgressBarStatus(pb, upload.getState());
        } catch (InterruptedException e) {
           Thread.currentThread().interrupt();
        }
    }
    private void updateProgressBarStatus( ProgressBar bar, Transfer.TransferState  state){
        switch (state){
            case Failed ->
                    bar.setExtraMessage("Failed to uploading files to S3 bucket");
            case Waiting ->
                    bar.setExtraMessage("Waiting for upload");
            case Canceled ->
                    bar.setExtraMessage("Upload cancelled");
            case InProgress ->
                    bar.setExtraMessage("Uploading");
            case Completed ->
                    bar.setExtraMessage("Completed");
        }
    }

}
