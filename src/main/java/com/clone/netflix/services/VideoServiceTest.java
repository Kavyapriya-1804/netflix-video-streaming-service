package com.clone.netflix.services;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoServiceTest {

    public void generateAdaptiveHls(String videoId, String videoPath) throws IOException, InterruptedException {
        String HLS_DIR = "/Users/kavinkumarbaskar/project/netflix/data/videos_hls";
        String parentDir = HLS_DIR + "/" + videoId;

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
}
