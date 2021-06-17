package com.example.filesystem.model;

import lombok.Data;

import java.util.LinkedList;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 20:24
 */
@Data
public class IndexFile {

    private Integer index;
    private String path;

    private Boolean isCatalog;//true--目录文件 false--文件

    private Boolean isPublic;//true--public false--private

    private FatBlock firstBlock;

    private IndexFCBRow parent;//父目录

    private String modifyTime;//最后修改时间

    private Integer status;//文件状态 0--关闭 1--打开 2--有用户正在写 -1--文目录

    private LinkedList<IndexFCBRow> children;//子目录



    public IndexFile(Integer index, String path, Boolean isCatalog, Boolean isPublic,
                     FatBlock firstBlock, IndexFCBRow parent, String modifyTime,
                     Integer status, LinkedList<IndexFCBRow> children) {
        this.index = index;
        this.path = path;
        this.isCatalog = isCatalog;
        this.isPublic = isPublic;
        this.firstBlock = firstBlock;
        this.parent = parent;
        this.modifyTime = modifyTime;
        this.status = status;
        this.children = children;
    }
}
