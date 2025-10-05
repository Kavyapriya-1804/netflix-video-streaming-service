package com.clone.netflix.services.impl;

import com.clone.netflix.entities.Video;
import com.clone.netflix.repositories.VideoRepository;
import com.clone.netflix.services.VideoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoServiceImpl implements VideoService {

    @Value("${files.video}")
    String DIR;

    @Value("${file.video.hls}")
    String HLS_DIR;

    private VideoRepository videoRepository;

    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    /**
     * Create video_hls folder if it's not present
     */
    @PostConstruct
    public void init() {
        File file = new File(DIR);
        try {
            Files.createDirectories(Paths.get(HLS_DIR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!file.exists()) {
            file.mkdir();
            System.out.println("Folder Created:");
        } else {
            System.out.println("Folder already created");
        }

    }


    /**
     * Save video (metadata + file)
     * @param video
     * @param file
     * @return
     */
    @Override
    public Video save(Video video, MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            // Save uploaded file temporarily
            String cleanFileName = StringUtils.cleanPath(filename);
            Path tempPath = Paths.get(DIR, "temp_" + cleanFileName);
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);

            // Concatenate with startup animation BEFORE storing
            String finalOutputPath = attachStartupAnimation(tempPath.toString(), cleanFileName);

            // Save metadata
            video.setContentType(contentType);
            video.setFilePath(finalOutputPath);
            Video savedVideo = videoRepository.save(video);

            // Now process the concatenated video
            // processVideo(savedVideo.getVideoId());
            generateAdaptiveHls(savedVideo.getVideoId(), finalOutputPath);

            // Delete temp uploaded file (optional)
            Files.deleteIfExists(tempPath);

            return savedVideo;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error in processing video ");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Attach startup animation to the beginning of the video using FFmpeg
     * @param filePath
     * @param originalFileName
     * @return
     */
    public String attachStartupAnimation(String filePath, String originalFileName) {
        try {
            String animationPath = "videos/netflix-intro.mp4";
            String outputDir = DIR; // store final video in main dir
            Files.createDirectories(Paths.get(outputDir));

            String outputFilePath = Paths.get(outputDir, originalFileName).toString();


            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -i \"%s\" -filter_complex " +
                            "\"[0:v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2[v0]; " +
                            "[1:v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-ih)/2:(oh-ih)/2[v1]; " +
                            "[v0][0:a][v1][1:a]concat=n=2:v=1:a=1[outv][outa]\" " +
                            "-map \"[outv]\" -map \"[outa]\" -c:v libx264 -crf 23 -preset veryfast -c:a aac \"%s\"",
                    animationPath, filePath, outputFilePath
            );

            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Failed to concatenate the startup animation with the video!");
            }

            System.out.println("Startup animation attached successfully: " + outputFilePath);
            return outputFilePath;

        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Error while attaching startup animation!", ex);
        }
    }


    /**
     * Get video by ID
     * @param videoId
     * @return
     */
    @Override
    public Video get(String videoId) {
        Video video = videoRepository.findById(videoId).orElseThrow(() -> new RuntimeException("video not found"));
        return video;
    }


    /**
     * Get video by title
     * @param title
     * @return
     */
    @Override
    public Video getByTitle(String title) {
        return null;
    }


    /**
     * Get all videos
     * @return
     */
    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }


    /**
     * Generate adaptive HLS for a video
     * @param videoId
     * @param videoPath
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public void generateAdaptiveHls(String videoId, String videoPath) throws IOException, InterruptedException {

        String parentDir = Paths.get(HLS_DIR).toString() + "/" + videoId;

        // Create directories for HLS output
        new File(parentDir).mkdirs();
        for (int i = 0; i < 3; i++) {
            new File(parentDir + "/" + i).mkdirs();
        }

        // FFmpeg command construction
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-i");
        cmd.add(videoPath);

        // Define filter_complex for scaling and splitting video streams
        String filterComplex = "[0:v]split=3[v1][v2][v3];" +
                "[v1]scale=640:360[v1out];" +
                "[v2]scale=1280:720[v2out];" +
                "[v3]scale=1920:1080[v3out]";
        cmd.add("-filter_complex");
        cmd.add(filterComplex);

        // Define video and audio encoding settings
        String[] mappings = {
                "-map", "[v1out]", "-map", "0:a", "-c:v:0", "libx264", "-b:v:0", "800k", "-c:a", "aac", "-b:a", "96k",
                "-map", "[v2out]", "-map", "0:a", "-c:v:1", "libx264", "-b:v:1", "2800k", "-c:a", "aac", "-b:a", "128k",
                "-map", "[v3out]", "-map", "0:a", "-c:v:2", "libx264", "-b:v:2", "5000k", "-c:a", "aac", "-b:a", "192k"
        };
        for (String arg : mappings) {
            cmd.add(arg);
        }

        // HLS options
        cmd.add("-f");
        cmd.add("hls");
        cmd.add("-hls_time");
        cmd.add("6");
        cmd.add("-hls_playlist_type");
        cmd.add("vod");
        cmd.add("-var_stream_map");
        cmd.add("v:0,a:0 v:1,a:1 v:2,a:2");
        cmd.add("-master_pl_name");
        cmd.add("master.m3u8");
        cmd.add("-hls_segment_filename");
        cmd.add(parentDir + "/%v/segment_%03d.ts");
        cmd.add(parentDir + "/%v/playlist.m3u8");

        // Execute FFmpeg command
        System.out.println("Executing FFmpeg command: " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(parentDir));
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        // Handle process completion
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
        }

        System.out.println("Adaptive HLS generation completed for video: " + videoId);
    }

//    /**
//     * Process video (eg: transcode, etc.)
//     * @param videoId
//     * @return
//     */
//    @Override
//    public String processVideo(String videoId) {
//
//        Video video = this.get(videoId);
//        String filePath = video.getFilePath();
//
//        //path where to store data:
//        Path videoPath = Paths.get(filePath);
//
//
////        String output360p = HLS_DIR + videoId + "/360p/";
////        String output720p = HLS_DIR + videoId + "/720p/";
////        String output1080p = HLS_DIR + videoId + "/1080p/";
//
//        try {
////            Files.createDirectories(Paths.get(output360p));
////            Files.createDirectories(Paths.get(output720p));
////            Files.createDirectories(Paths.get(output1080p));
//
//            // ffmpeg command
//            Path outputPath = Paths.get(HLS_DIR, videoId);
//
//            Files.createDirectories(outputPath);
//
//
//            String ffmpegCmd = String.format(
//                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
//                    videoPath, outputPath, outputPath
//            );
//
////            StringBuilder ffmpegCmd = new StringBuilder();
////            ffmpegCmd.append("ffmpeg  -i ")
////                    .append(videoPath.toString())
////                    .append(" -c:v libx264 -c:a aac")
////                    .append(" ")
////                    .append("-map 0:v -map 0:a -s:v:0 640x360 -b:v:0 800k ")
////                    .append("-map 0:v -map 0:a -s:v:1 1280x720 -b:v:1 2800k ")
////                    .append("-map 0:v -map 0:a -s:v:2 1920x1080 -b:v:2 5000k ")
////                    .append("-var_stream_map \"v:0,a:0 v:1,a:0 v:2,a:0\" ")
////                    .append("-master_pl_name ").append(HLS_DIR).append(videoId).append("/master.m3u8 ")
////                    .append("-f hls -hls_time 10 -hls_list_size 0 ")
////                    .append("-hls_segment_filename \"").append(HLS_DIR).append(videoId).append("/v%v/fileSequence%d.ts\" ")
////                    .append("\"").append(HLS_DIR).append(videoId).append("/v%v/prog_index.m3u8\"");
//
//            System.out.println(ffmpegCmd);
//            //file this command
//            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
//            processBuilder.inheritIO();
//            Process process = processBuilder.start();
//            int exit = process.waitFor();
//            if (exit != 0) {
//                throw new RuntimeException("video processing failed!!");
//            }
//
//            return videoId;
//
//
//        } catch (IOException ex) {
//            throw new RuntimeException("Video processing fail!!");
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }

}
