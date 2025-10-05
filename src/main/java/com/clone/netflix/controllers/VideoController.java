package com.clone.netflix.controllers;

import com.clone.netflix.entities.Video;
import com.clone.netflix.playload.CustomMessage;
import com.clone.netflix.services.VideoService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "*"})
public class VideoController {
    @Value("${file.video.hls}")
    private String HLS_DIR;
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /**
     * Upload video
     * @param file
     * @param title
     * @param description
     * @return
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestParam("file") MultipartFile file, @RequestParam("title") String title, @RequestParam("description") String description) {
        System.out.print("file received : " + file.getOriginalFilename());

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoId(UUID.randomUUID().toString());

        Video savedVideo = videoService.save(video, file);

        if (savedVideo != null) {
            return ResponseEntity.status(HttpStatus.OK).body(video);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CustomMessage.builder().message("Video not uploaded ").success(false).build());
        }
    }


    /**
     * Get all videos
     * @return
     */
    @GetMapping
    public List<Video> getAll() {
        return videoService.getAll();
    }


    /**
     * Serve the master playlist file
     * @param videoId
     * @return
     */
    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> serverMasterFile(
            @PathVariable String videoId
    ) {

        String filePath = Paths.get(HLS_DIR).toString();
//        creating path
        Path path = Paths.get(filePath, videoId, "master.m3u8");

        System.out.println(path);

        if (!Files.exists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl"
                )
                .body(resource);
    }


    /**
     * Get HLS playlist for a specific quality level. (Video of a specified quality)
     * @param videoId
     * @param qualityLevel
     * @return
     */
    @GetMapping("{videoId}/{qualityLevel}/playlist.m3u8")
    public ResponseEntity<Resource> getQualityPlaylist(@PathVariable String videoId, @PathVariable int qualityLevel) {
        try {
            String filePath = Paths.get(HLS_DIR).toString();
            String playlistPath = filePath + "/" + videoId + "/" + qualityLevel + "/playlist.m3u8";
            Resource resource = new UrlResource(Paths.get(playlistPath).toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }


    /**
     * Get HLS segment for a specific quality level.
     * @param videoId
     * @param qualityLevel
     * @param segmentName
     * @return
     */
    @GetMapping("{videoId}/{qualityLevel}/{segmentName}")
    public ResponseEntity<Resource> getSegment(@PathVariable String videoId, @PathVariable int qualityLevel, @PathVariable String segmentName) {
        try {
            String filePath = Paths.get(HLS_DIR).toString();
            String segmentPath = filePath + "/" + videoId + "/" + qualityLevel + "/" + segmentName;
            Resource resource = new UrlResource(Paths.get(segmentPath).toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "video/MP2T")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }


    /**
     * - TESTING PURPOSE
     * Generate HLS streams for a given video
     * @param videoId
     * @param videoPath
     * @return
     */
    @PostMapping("/{videoId}/generate-hls")
    public ResponseEntity<String> generateHls(@PathVariable String videoId, @RequestParam String videoPath) {
        try {
            videoService.generateAdaptiveHls(videoId, videoPath);
            return ResponseEntity.ok("HLS streams generated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating HLS streams: " + e.getMessage());
        }
    }


    /**
     * - TESTING PURPOSE
     * Health endpoint
     * @return
     */
    @GetMapping("/test")
    public String test() {
        return "App is running fine !";
    }


//    /**
//     * Stream full video ( without HLS )
//     * @param videoId
//     * @return
//     */
//    @GetMapping("/stream/{videoId}")
//    public ResponseEntity<Resource> stream(@PathVariable String videoId) {
//
//        Video video = videoService.get(videoId);
//        String contentType = video.getContentType();
//        String filePath = video.getFilePath();
//        Resource resource = new FileSystemResource(filePath);
//        if (contentType == null) {
//            contentType = "application/octet-stream";
//        }
//
//        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
//    }


//    /**
//     * Stream video in range (without HLS)
//     * @param videoId
//     * @param range
//     * @return
//     */
//    @GetMapping("/stream/range/{videoId}")
//    public ResponseEntity<Resource> streamVideoRange(@PathVariable String videoId, @RequestHeader(value = "Range", required = false) String range) {
//        System.out.println(range);
//
//        Video video = videoService.get(videoId);
//        Path path = Paths.get(video.getFilePath());
//
//        Resource resource = new FileSystemResource(path);
//
//        String contentType = video.getContentType();
//
//        if (contentType == null) {
//            contentType = "application/octet-stream";
//
//        }
//
//        long fileLength = path.toFile().length();
//        System.out.println("file length : " + fileLength);
//
//
//        if (range == null) {
//            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
//        }
//
//        long rangeStart;
//        long rangeEnd;
//
//        String[] ranges = range.replace("bytes=", "").split("-");
//        rangeStart = Long.parseLong(ranges[0]);
//
//        rangeEnd = rangeStart + AppConstants.CHUNK_SIZE - 1;
//
//        if (rangeEnd >= fileLength) {
//            rangeEnd = fileLength - 1;
//        }
//
////        if (ranges.length > 1) {
////            rangeEnd = Long.parseLong(ranges[1]);
////        } else {
////            rangeEnd = fileLength - 1;
////        }
////
////        if (rangeEnd > fileLength - 1) {
////            rangeEnd = fileLength - 1;
////        }
//
//
//        System.out.println("range start : " + rangeStart);
//        System.out.println("range end : " + rangeEnd);
//        InputStream inputStream;
//
//        try {
//
//            inputStream = Files.newInputStream(path);
//            inputStream.skip(rangeStart);
//            long contentLength = rangeEnd - rangeStart + 1;
//
//
//            byte[] data = new byte[(int) contentLength];
//            int read = inputStream.read(data, 0, data.length);
//            System.out.println("read(number of bytes) : " + read);
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
//            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
//            headers.add("Pragma", "no-cache");
//            headers.add("Expires", "0");
//            headers.add("X-Content-Type-Options", "nosniff");
//            headers.setContentLength(contentLength);
//
//            return ResponseEntity
//                    .status(HttpStatus.PARTIAL_CONTENT)
//                    .headers(headers)
//                    .contentType(MediaType.parseMediaType(contentType))
//                    .body(new ByteArrayResource(data));
//
//
//        } catch (IOException ex) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//
//
//    }


//    /**
//     * Serve the video segments (without quality levels)
//     * @param videoId
//     * @param segment
//     * @return
//     */
//    @GetMapping("/{videoId}/{segment}.ts")
//    public ResponseEntity<Resource> serveSegments(
//            @PathVariable String videoId,
//            @PathVariable String segment
//    ) {
//
//        // create path for segment
//        Path path = Paths.get(HLS_DIR, videoId, segment + ".ts");
//        if (!Files.exists(path)) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//
//        Resource resource = new FileSystemResource(path);
//
//        return ResponseEntity
//                .ok()
//                .header(
//                        HttpHeaders.CONTENT_TYPE, "video/mp2t"
//                )
//                .body(resource);
//
//    }
}
