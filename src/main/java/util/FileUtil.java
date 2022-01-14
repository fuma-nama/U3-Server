package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static api.APIController.apiURL;

@Configuration
public class FileUtil implements WebMvcConfigurer {
    public static final String SERVER_ICON = "server/", USER_AVATAR = "user/", MESSAGE_FILE = "message/", EMOJI = "emoji/";

    private static final Path uploadDir = Paths.get("images");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        registry.addResourceHandler("/images/**").addResourceLocations("file:/"+ uploadPath + "/");
    }

    /**@return File URL**/
    public static String saveFile(String name, String type, MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            Path uploadDir = FileUtil.uploadDir.resolve(type);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve(name);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return apiURL + "images/" + type + name;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IOException("Failed to save image file: " + name, ioe);
        }
    }

    public static String saveFile(int container, String name, String type, MultipartFile file) throws IOException {
        return saveFile(name, type + container + "/", file);
    }

    /**@return File URL**/
    public static String saveFileByID(int ID, String type, MultipartFile file) throws IOException {
        return saveFileByID(String.valueOf(ID), type, file);
    }

    /**@return File URL**/
    public static String saveFileByID(String ID, String type, MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null) throw new IllegalArgumentException("Invalid Multipart File");

        int dot = name.lastIndexOf('.');
        if (dot != -1)
            name = ID + name.substring(dot);
        else
            name = ID;

        return saveFile(name, type, file);
    }

    public static void deleteFile(String name, String type) throws IOException {
        Path uploadDir = FileUtil.uploadDir.resolve(type);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Files.deleteIfExists(uploadDir.resolve(name));
    }

    public static void deleteFile(String url) throws IOException {
        Path path = Path.of(url.substring(apiURL.length()));
        System.out.println(path);
        Files.deleteIfExists(path);
    }
}
