package com.generic4.itda.config;

import com.generic4.itda.config.file.FileUploadProperties;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FileUploadProperties fileUploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String profileLocation = toResourcePath(fileUploadProperties.getProfileImageDirectory());
        String proposalLocation = toResourcePath(fileUploadProperties.getProposalDirectory());
        String resumeLocation = toResourcePath(fileUploadProperties.getResumeDirectory());

        registry.addResourceHandler("/files/profile/**")
                .addResourceLocations(profileLocation);

        registry.addResourceHandler("/files/proposal/**")
                .addResourceLocations(proposalLocation);

        registry.addResourceHandler("/files/resume/**")
                .addResourceLocations(resumeLocation);
    }

    private String toResourcePath(Path path) {
        String location = path.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
