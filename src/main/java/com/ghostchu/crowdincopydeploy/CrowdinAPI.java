package com.ghostchu.crowdincopydeploy;

import com.crowdin.client.Client;
import com.crowdin.client.core.model.Credentials;
import com.crowdin.client.core.model.DownloadLink;
import com.crowdin.client.translations.model.CrowdinTranslationCreateProjectBuildForm;
import com.crowdin.client.users.model.User;
import com.ghostchu.crowdincopydeploy.exception.UnirestRequestException;
import com.ghostchu.crowdincopydeploy.task.DownloadFileTask;
import com.ghostchu.crowdincopydeploy.task.TranslationBuildMonitorTask;
import com.ghostchu.crowdincopydeploy.task.UnzipFileTask;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class CrowdinAPI {
    private static final Logger LOG = LoggerFactory.getLogger(CrowdinAPI.class);
    private static final String API_ROOT = "https://api.crowdin.com/api/v2";
    private String token;
    private long projectId;
    private long branchId;
    private List<String> targetLanguageIds;
    private Client client;
    private String branchName;

    public CrowdinAPI() {
        acceptVariable();
        auth();
        prepare();
    }

    private void acceptVariable() {
        this.token = System.getenv("CROWDIN_ACCESS_TOKEN");
        Validate.notEmpty(token);
        this.branchId = Long.parseLong(System.getenv("CROWDIN_PROJECT_BRANCH_ID"));
        projectId = Long.parseLong(System.getenv("CROWDIN_PROJECT_ID"));
        LOG.info("Branch Id: {}", branchId);
        LOG.info("Project Id: {}", projectId);
    }

    private void auth() {
        LOG.info("Authenticating to Crowdin via API v2...");
        Credentials credentials = new Credentials(token, null);
        client = new Client(credentials);
        User user = client.getUsersApi().getAuthenticatedUser().getData();
        LOG.info("Logged in with user {} (uid={}).", user.getUsername(), user.getId());
        Unirest.config()
                .addDefaultHeader("Content-Type", "application/json")
                .addDefaultHeader("Accept", "application/json")
                .addDefaultHeader("User-Agent", "CrowdinCopyDeploy/1.0.0")
                .addDefaultHeader("Authorization", "Bearer " + token)
                .followRedirects(true)
                .enableCookieManagement(true)
                .automaticRetries(true);
    }

    private void prepare() {
        LOG.info("Performing the branch id to name lookup...");
        branchName = client.getSourceFilesApi().getBranch(projectId, branchId).getData().getName();
        LOG.info("Branch: {} <-> {}", branchId, branchName);
        LOG.info("Polling the information about target languages ids...");
        targetLanguageIds = client.getProjectsGroupsApi()
                .getProject(projectId)
                .getData()
                .getTargetLanguageIds();
        LOG.info("Target Language Ids: {}", targetLanguageIds.size());

    }

    public void buildTranslations(@NotNull File uncompressTo) {
        LOG.info("Building translation for languages: {}", targetLanguageIds);
        CrowdinTranslationCreateProjectBuildForm form = generateTranslationBuildForm(branchId, targetLanguageIds);
        long buildId = client.getTranslationsApi()
                .buildProjectTranslation(projectId, form)
                .getData()
                .getId();
        LOG.info("Translation build started with build id: {}", buildId);
        TranslationBuildMonitorTask work = new TranslationBuildMonitorTask(client, projectId, buildId);
        work.run();
        if (!work.getLastStatus().equalsIgnoreCase("finished")) {
            throw new IllegalStateException("Incomplete translation build: " + work.getLastStatus());
        }
        LOG.info("Translation build successfully.");
        downloadTranslations(buildId, uncompressTo);
    }

    @NotNull
    private CrowdinTranslationCreateProjectBuildForm generateTranslationBuildForm(long branchId, @NotNull List<String> languageIds) {
        CrowdinTranslationCreateProjectBuildForm form = new CrowdinTranslationCreateProjectBuildForm();
        form.setExportApprovedOnly(false);
        form.setSkipUntranslatedFiles(false);
        form.setSkipUntranslatedFiles(false);
        form.setSkipUntranslatedStrings(false);
        form.setBranchId(branchId);
        form.setTargetLanguageIds(languageIds);
        LOG.info("Generated translation build form: {}", form);
        return form;
    }

    private void downloadTranslations(long buildId, @NotNull File uncompressTo) {
        File saveTo = new File(UUID.randomUUID() + ".zip");
        LOG.info("Getting the project translations download URL...");
        DownloadLink link = client.getTranslationsApi()
                .downloadProjectTranslations(projectId, buildId)
                .getData();
        LOG.info("Downloading translations zip from {} to {}... (expires in {})", link.getUrl(), saveTo.getPath(), link.getExpireIn());
        new DownloadFileTask(link.getUrl(), saveTo).run();
        new UnzipFileTask(saveTo, uncompressTo).run();
       // saveTo.delete();
    }

    @NotNull
    public Map<String, List<String>> manifestGenerateContentSections(@NotNull Map<String, Object> languageMappingRaw) {
        Map<String, Map<String, String>> mapping = bakeLanguageMapping(languageMappingRaw);
        Map<String, List<String>> map = new LinkedHashMap<>();

        for (String locale : ProgressBar.wrap(targetLanguageIds, "Manifest paths")) {
            //LOG.info("Generating for {} locale...", locale);
            List<String> list = new ArrayList<>();
            for (String file : manifestGenerateFiles()) {
                String mappedCode = getMappedLanguageCode(locale, "locale", mapping);
                String path = "/content" + file.replace("%locale%", mappedCode);
                list.add(path);
            }
            map.put(locale, list);
        }
        return map;
    }

    @NotNull
    private Map<String, Map<String, String>> bakeLanguageMapping(@NotNull Map<String, Object> languageMappingRaw) {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : ProgressBar.wrap(languageMappingRaw.entrySet(), "Bake language mapping")) {
            //noinspection unchecked
            map.put(entry.getKey(), (Map<String, String>) entry.getValue());
        }
        return map;
    }

    @NotNull
    public List<String> manifestGenerateFiles() {
        List<String> output = new ArrayList<>();
        HttpResponse<JsonNode> response = Unirest.get(API_ROOT + "/projects/" + projectId + "/files")
                .asJson();
        if (!response.isSuccess())
            throw new UnirestRequestException("Getting files", response);
        JSONArray files = response.getBody().getObject().getJSONArray("data");
        for (int i = 0; i < files.length(); i++) {
            JSONObject singleFile = files.getJSONObject(i).getJSONObject("data");
            String name = singleFile.getString("name");
            if (!singleFile.getString("status").equalsIgnoreCase("active")) {
                LOG.info("Skipping {} because it status is {}", name, singleFile.getString("status"));
                continue;
            }
            if (!singleFile.has("exportOptions")) {
                LOG.info("Skipping {} because it doesn't have exportOptions field.", name);
                continue;
            }
            String pattern = singleFile.getJSONObject("exportOptions").getString("exportPattern");
            String path = "/" + branchName + pattern.replace("%original_file_name%", name);
            output.add(path);
        }
        return output;
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    private String getMappedLanguageCode(@NotNull String locale, @NotNull String localeFormat, @NotNull Map<String, Map<String, String>> languageMapping) {
        Map<String, String> map = languageMapping.get(locale);
        if (map == null) return locale;
        return map.getOrDefault(localeFormat, locale);
    }

    @NotNull
    public List<String> manifestGenerateLanguages() {
        return targetLanguageIds;
    }

    @NotNull
    public Map<String, Object> manifestGenerateLanguageMapping() {
        HttpResponse<JsonNode> response = Unirest.get(API_ROOT + "/projects/" + projectId)
                .asJson();
        if (!response.isSuccess())
            throw new UnirestRequestException("Getting language mapping", response);
        return response.getBody().getObject()
                .getJSONObject("data")
                .getJSONObject("languageMapping").toMap();
    }

    @NotNull
    public String getBranchName() {
        return branchName;
    }
}

