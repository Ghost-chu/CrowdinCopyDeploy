package com.ghostchu.crowdincopydeploy;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.ghostchu.crowdincopydeploy.task.UploadS3Task;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class S3Uploader {
    private static final Logger LOG = LoggerFactory.getLogger(S3Uploader.class);
    private String s3Endpoint;
    private String s3Bucket;
    private String s3AccessKeyId;
    private String s3AccessToken;
    private String s3Region;
    private File deployPath;
    private AmazonS3 s3;
    private String s3Dest;

    public S3Uploader() {
        acceptEnvironmentVariable();
        auth();
        upload();
    }

    private void acceptEnvironmentVariable() {
        s3Endpoint = System.getenv("AWS_S3_ENDPOINT");
        s3Bucket = System.getenv("AWS_S3_BUCKET");
        s3AccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        s3AccessToken = System.getenv("AWS_SECRET_ACCESS_KEY");
        s3Region = System.getenv("AWS_REGION");
        s3Dest = System.getenv("AWS_DEST");
        if (StringUtils.isEmpty(s3Region)) s3Region = "auto";
        if (StringUtils.isEmpty(s3Dest)) s3Dest = "";
        if (StringUtils.isAnyEmpty(s3Endpoint, s3Bucket, s3AccessKeyId, s3AccessToken)) {
            throw new IllegalStateException("S3 arguments not complete.");
        }
        String deployPath = System.getenv("DEPLOY_PATH");
        if (StringUtils.isEmpty(deployPath)) deployPath = "./deploy-target";
        this.deployPath = new File(deployPath);
        this.deployPath.mkdirs();
        LOG.info("Deploy files from {} to S3...", this.deployPath.getAbsolutePath());
    }

    private void auth() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(s3AccessKeyId, s3AccessToken);
        s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(s3Region)
                .build();
        if (!s3.doesBucketExistV2(s3Bucket)) {
            throw new IllegalStateException("The bucket " + s3Bucket + " doesn't exists.");
        }

    }

    @SneakyThrows
    private void upload() {
        new UploadS3Task(s3, s3Bucket, deployPath, s3Dest).run();
    }

}
