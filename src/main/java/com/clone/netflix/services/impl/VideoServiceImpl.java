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
import java.util.List;

@Service
public class VideoServiceImpl implements VideoService {

    @Value("${files.video}")
    String DIR;

    @Value("${file.video.hsl}")
    String HSL_DIR;


    private VideoRepository videoRepository;


    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PostConstruct
    public void init() {

        File file = new File(DIR);


        try {
            Files.createDirectories(Paths.get(HSL_DIR));
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
            processVideo(savedVideo.getVideoId());

            // Delete temp uploaded file (optional)
            Files.deleteIfExists(tempPath);

            return savedVideo;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error in processing video ");
        }
    }

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


    @Override
    public Video get(String videoId) {
        Video video = videoRepository.findById(videoId).orElseThrow(() -> new RuntimeException("video not found"));
        return video;
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    @Override
    public String processVideo(String videoId) {

        Video video = this.get(videoId);
        String filePath = video.getFilePath();

        //path where to store data:
        Path videoPath = Paths.get(filePath);


//        String output360p = HSL_DIR + videoId + "/360p/";
//        String output720p = HSL_DIR + videoId + "/720p/";
//        String output1080p = HSL_DIR + videoId + "/1080p/";

        try {
//            Files.createDirectories(Paths.get(output360p));
//            Files.createDirectories(Paths.get(output720p));
//            Files.createDirectories(Paths.get(output1080p));

            // ffmpeg command
            Path outputPath = Paths.get(HSL_DIR, videoId);

            Files.createDirectories(outputPath);


            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    videoPath, outputPath, outputPath
            );

//            StringBuilder ffmpegCmd = new StringBuilder();
//            ffmpegCmd.append("ffmpeg  -i ")
//                    .append(videoPath.toString())
//                    .append(" -c:v libx264 -c:a aac")
//                    .append(" ")
//                    .append("-map 0:v -map 0:a -s:v:0 640x360 -b:v:0 800k ")
//                    .append("-map 0:v -map 0:a -s:v:1 1280x720 -b:v:1 2800k ")
//                    .append("-map 0:v -map 0:a -s:v:2 1920x1080 -b:v:2 5000k ")
//                    .append("-var_stream_map \"v:0,a:0 v:1,a:0 v:2,a:0\" ")
//                    .append("-master_pl_name ").append(HSL_DIR).append(videoId).append("/master.m3u8 ")
//                    .append("-f hls -hls_time 10 -hls_list_size 0 ")
//                    .append("-hls_segment_filename \"").append(HSL_DIR).append(videoId).append("/v%v/fileSequence%d.ts\" ")
//                    .append("\"").append(HSL_DIR).append(videoId).append("/v%v/prog_index.m3u8\"");

            System.out.println(ffmpegCmd);
            //file this command
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("video processing failed!!");
            }

            return videoId;


        } catch (IOException ex) {
            throw new RuntimeException("Video processing fail!!");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
}
