package com.clone.netflix.services;


import com.clone.netflix.entities.Video;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface VideoService {

    /**
     * Save video
     * @param video
     * @param file
     * @return
     */
    Video save(Video video, MultipartFile file);


    /**
     * Get video by ID
     * @param videoId
     * @return
     */
    Video get(String videoId);


    /**
     * Get video by title
     * @param title
     * @return
     */
    Video getByTitle(String title);


    /**
     * Get all videos
     * @return
     */
    List<Video> getAll();


    /**
     * Generate adaptive HLS for a video
     * @param videoId
     * @param videoPath
     * @throws IOException
     * @throws InterruptedException
     */
    void generateAdaptiveHls(String videoId, String videoPath) throws IOException, InterruptedException;


    /**
     * Process video (eg: transcode, etc.)
     */
    //String processVideo(String videoId);

}
