package com.botofholding.api.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApplicationInfoService {

    private final String applicationVersion;
    private final String applicationName;
    private final String applicationArtifact;
    private final String applicationBuildTimestamp;

    @Autowired
    public ApplicationInfoService(Optional<BuildProperties> buildPropertiesOpt) {
        if (buildPropertiesOpt.isPresent()) {
            BuildProperties buildProperties = buildPropertiesOpt.get();
            this.applicationVersion = buildProperties.getVersion();
            this.applicationBuildTimestamp = String.valueOf(buildProperties.getTime());
            this.applicationName = buildProperties.getName();
            this.applicationArtifact = buildProperties.getArtifact();
        } else {
            // Provide default/fallback values for when running in an IDE without a full build
            this.applicationVersion = "0.0.0-DEV";
            this.applicationName = "api";
            this.applicationArtifact = "api";
            this.applicationBuildTimestamp = "N/A";
        }
    }

    public String getVersion() {
        return this.applicationVersion;
    }

    public String getName() {
        return this.applicationName;
    }

    public String getArtifact() {
        return this.applicationArtifact;
    }

    public String getBuildTimestamp() {
        return this.applicationBuildTimestamp;
    }

}