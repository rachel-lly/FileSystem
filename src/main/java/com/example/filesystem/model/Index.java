package com.example.filesystem.model;

import lombok.Data;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 20:21
 */
@Data
public class Index {

    private IndexFile indexFile;//对应索引文件
    private String fileName;//文件名
    private String path;

    public Index(IndexFile indexFile, String fileName, String path) {
        this.indexFile = indexFile;
        this.fileName = fileName;
        this.path = path;
    }
}
