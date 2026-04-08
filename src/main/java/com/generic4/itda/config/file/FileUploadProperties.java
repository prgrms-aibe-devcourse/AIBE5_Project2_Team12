package com.generic4.itda.config.file;

import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    private String basePath;
    private String profileImageDir;
    private String proposalDir;
    private String resumeDir;


    public Path getBaseDirectory() {
        return Path.of(basePath).normalize();
    }

    public Path getProfileImageDirectory() {
        return getBaseDirectory().resolve(profileImageDir).normalize();
    }

    public Path getProposalDirectory() {
        return getBaseDirectory().resolve(proposalDir).normalize();
    }

    public Path getResumeDirectory() {
        return getBaseDirectory().resolve(resumeDir).normalize();
    }
}
