package com.family.kids.service;

import com.family.kids.mapper.ChildMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class AvatarService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_SIDE = 512;
    private static final List<String> ALLOWED_EXT = List.of(".jpg", ".jpeg", ".png", ".webp");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired
    private ChildMapper childMapper;

    @Value("${app.avatar-dir}")
    private String avatarDir;

    private Path root;

    @PostConstruct
    public void init() throws IOException {
        root = Path.of(avatarDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Path getRoot() {
        return root;
    }

    public void saveAvatar(Long childId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先选择一张图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("图片太大了，请选择 5MB 以内的照片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extOf(original).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("只支持 JPG / PNG / WEBP 格式的图片");
        }

        BufferedImage image = null;
        try (InputStream in = file.getInputStream()) {
            image = ImageIO.read(in);
        } catch (IOException ignored) {
        }
        if (image == null) {
            throw new IllegalArgumentException("这个文件不是有效的图片，请换一张试试");
        }

        String fileName = "avatar-" + childId + "-" + LocalDateTime.now().format(TS) + ".jpg";
        Path target = root.resolve(fileName);
        try {
            writeCompressed(image, target);
        } catch (IOException e) {
            throw new IllegalStateException("头像保存失败，请稍后重试", e);
        }

        String oldUrl = childMapper.getAvatarUrl(childId);
        childMapper.updateAvatarUrl(childId, "/uploads/avatars/" + fileName);
        deleteOldAvatar(oldUrl, fileName);
    }

    private void writeCompressed(BufferedImage src, Path target) throws IOException {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w > MAX_SIDE || h > MAX_SIDE) {
            double scale = Math.min((double) MAX_SIDE / w, (double) MAX_SIDE / h);
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
            g.dispose();
            src = scaled;
        }
        try (var os = Files.newOutputStream(target)) {
            ImageIO.write(src, "jpg", os);
        }
    }

    private void deleteOldAvatar(String oldUrl, String newFileName) {
        if (oldUrl == null || oldUrl.isBlank()) {
            return;
        }
        String oldName = oldUrl.substring(oldUrl.lastIndexOf('/') + 1);
        if (oldName.isBlank() || oldName.equals(newFileName)) {
            return;
        }
        if (!oldName.startsWith("avatar-")) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(oldName));
        } catch (IOException ignored) {
        }
    }

    private String extOf(String filename) {
        int p = filename.lastIndexOf('.');
        return p < 0 ? "" : filename.substring(p);
    }
}
