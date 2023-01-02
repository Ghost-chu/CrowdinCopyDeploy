package com.ghostchu.crowdincopydeploy;

import com.ghostchu.crowdincopydeploy.task.MoveDirectoryTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lukfor.progress.TaskService;
import lukfor.progress.tasks.TaskStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

public class CrowdinDeployer {
    private static final Logger LOG = LoggerFactory.getLogger(CrowdinDeployer.class);
    private final File translationTmpUnzipDirectory = new File(UUID.randomUUID() + "-uncompress");
    private final Gson gson = new GsonBuilder().create();
    private final CrowdinAPI crowdinAPI;
    private File deployPath;

    public CrowdinDeployer(@NotNull CrowdinAPI crowdinAPI) {
        this.crowdinAPI = crowdinAPI;
        translationTmpUnzipDirectory.mkdirs();
        translationTmpUnzipDirectory.deleteOnExit();
        acceptEnvironmentVariable();
        crowdinAPI.buildTranslations(translationTmpUnzipDirectory);
        deploy();
    }

    public void acceptEnvironmentVariable() {
        String deployPath = System.getenv("DEPLOY_PATH");
        if (StringUtils.isEmpty(deployPath)) deployPath = "./deploy-target";
        this.deployPath = new File(deployPath);
        this.deployPath.mkdirs();
        LOG.info("Deploy to: {}", this.deployPath.getAbsolutePath());
    }

    private void deploy() {
        LOG.info("Generating manifest.json...");
        generateManifest();
        LOG.info("Moving files into target deploy directory...");
        generateContent();
    }

    private void generateManifest() {
        Instant CURRENT_TIME = Instant.now();
        Map<String, Object> root = new LinkedHashMap<>();
        List<String> files = crowdinAPI.manifestGenerateFiles();
        root.put("files", files);
        root.put("languages", crowdinAPI.manifestGenerateLanguages());
        Map<String, Object> languageMapping = crowdinAPI.manifestGenerateLanguageMapping();
        root.put("language_mapping", languageMapping);
        root.put("custom_languages", Collections.emptyMap());
        root.put("timestamp", CURRENT_TIME.getEpochSecond());
        root.put("content", crowdinAPI.manifestGenerateContentSections(languageMapping));
        try {
            Files.writeString(getManifestFile().toPath(), gson.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void generateContent() {
        File content = new File(deployPath, "content");
        File branch = new File(content, crowdinAPI.getBranchName());
        MoveDirectoryTask task = new MoveDirectoryTask(translationTmpUnzipDirectory, branch);
        TaskStatus status = TaskService.run(task).get(0).getStatus();
        if(status.getThrowable() != null)
            throw new IllegalStateException(status.getThrowable());
        try {
            FileUtils.deleteDirectory(translationTmpUnzipDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    private File getManifestFile() throws IOException {
        File file = new File(deployPath, "manifest.json");
        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        }
        return file;
    }
}
