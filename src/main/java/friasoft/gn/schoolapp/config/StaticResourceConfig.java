package friasoft.gn.schoolapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final AppUploadRoot appUploadRoot;

    public StaticResourceConfig(AppUploadRoot appUploadRoot) {
        this.appUploadRoot = appUploadRoot;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = appUploadRoot.getRoot();
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadRoot + "/");
    }
}
