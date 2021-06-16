package com.example.filesystem.model;

import lombok.Data;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 20:21
 */
@Data
public class IndexFile {

    private FSFile FSFile;//对应文件
    private String fileName;//文件名
    private String path;

    public IndexFile(FSFile FSFile, String fileName, String path) {
        this.FSFile = FSFile;
        this.fileName = fileName;
        this.path = path;
    }
}
