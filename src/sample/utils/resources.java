package sample.utils;

import java.util.Arrays;
import java.util.List;

public class resources {

    // Windows only, should consider other OSs and how to do it, via separate vars or JSON?
    public static final String YOUTUBE_DL_SOURCE = "https://youtube-dl.org/downloads/latest/youtube-dl.exe";

    // Again windows64 only, should be able to look for different OS variants in future
    public static final String FFMPEG_SOURCE = "https://ffmpeg.zeranoe.com/builds/win64/static/";

    public static final List<String> songReferences = Arrays.asList("mp3", "wav", "ogg", "aac");

}
