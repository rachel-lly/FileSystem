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
    private Boolean fileType;//0--目录文件 1--文件

    private Integer permission;//0--public 1--private

    private FatBlock firstBlock;

    private Index parent;//父目录

    private String modifyTime;//最后修改时间

    private Integer status;//文件状态 0--关闭 1--打开 2--有用户正在写 -1--文目录

    private LinkedList<Index> children;//文件目录才有子目录

    public IndexFile(Integer index, String path, Boolean fileType, Integer permission,
                     FatBlock firstBlock, Index parent, String modifyTime,
                     Integer status, LinkedList<Index> children) {
        this.index = index;
        this.path = path;
        this.fileType = fileType;
        this.permission = permission;
        this.firstBlock = firstBlock;
        this.parent = parent;
        this.modifyTime = modifyTime;
        this.status = status;
        this.children = children;
    }
}
