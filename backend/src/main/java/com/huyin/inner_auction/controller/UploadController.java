package com.huyin.inner_auction.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.external.base-url:}")
    private String externalBaseUrl;

    // POST /api/uploads : upload file, trả JSON { url: "..." }
    @PostMapping
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file_required"));
        }
        try {
            String original = StringUtils.cleanPath(file.getOriginalFilename());
            String ext = "";
            int ix = original.lastIndexOf('.');
            if (ix >= 0) ext = original.substring(ix);
            String filename = UUID.randomUUID().toString() + ext;

            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);

            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = buildAbsoluteUrl(request, filename);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "save_failed"));
        }
    }

    // GET /api/uploads : list all uploaded files as absolute URLs
    @GetMapping
    public ResponseEntity<?> listUploads(HttpServletRequest request) {
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                return ResponseEntity.ok(List.of());
            }
            List<String> urls = Files.list(dir)
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .map(fname -> buildAbsoluteUrl(request, fname))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(urls);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "list_failed"));
        }
    }

    // GET /api/uploads/{filename} : stream the file (safe)
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            // Prevent path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path file = dir.resolve(filename).normalize();
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new UrlResource(file.toUri());
            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.noCache()) // or set caching policy
                    .body(resource);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String buildAbsoluteUrl(HttpServletRequest request, String filename) {
        if (externalBaseUrl != null && !externalBaseUrl.isBlank()) {
            return externalBaseUrl.replaceAll("/$", "") + "/uploads/" + filename;
        } else {
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isBlank()) scheme = request.getScheme();
            String host = request.getHeader("X-Forwarded-Host");
            if (host == null || host.isBlank()) host = request.getServerName();
            String portHeader = request.getHeader("X-Forwarded-Port");
            int port = (portHeader != null && !portHeader.isBlank()) ? Integer.parseInt(portHeader) : request.getServerPort();

            String portPart = "";
            if (!("http".equalsIgnoreCase(scheme) && port == 80) && !("https".equalsIgnoreCase(scheme) && port == 443)) {
                portPart = ":" + port;
            }
            return scheme + "://" + host + portPart + "/uploads/" + filename;
        }
    }
}