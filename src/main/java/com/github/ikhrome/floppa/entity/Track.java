package com.github.ikhrome.floppa.entity;

import lombok.Data;

@Data
public class Track {

    private static final String BASE_URL = "https://retrowave.ru";

    private String id;
    private String title;
    private Long duration;
    private String streamUrl;
    private String artworkUrl;
}
