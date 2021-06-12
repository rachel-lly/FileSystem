package com.example.filesystem.service;

import com.example.filesystem.FileSystemApplication;
import com.example.filesystem.mapper.UserMapper;
import com.example.filesystem.model.*;
import com.example.filesystem.util.Util;
import javax.annotation.Resource;
import java.util.*;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 19:47
 */
@org.springframework.stereotype.Service
public class ServiceImpl implements Service {

    private static final Integer LINE = 10;
    private static final Integer COLUMN = 10;
    private static final Integer BLOCKSIZE = 10;

    @Resource
    private UserMapper mapper;

    private static Integer index = 0;

    public static LinkedList<Index> root = new LinkedList<>();

    public static BitMap bitMap = new BitMap(LINE, COLUMN);
    public static FAT fat = new FAT(LINE, COLUMN);
    public static Disk disk = new Disk(LINE, COLUMN);

    private static LinkedList<Index> openFile = new LinkedList<>();

    static {
        for (int i = 0; i < LINE; i++) {
            for (int j = 0; j < COLUMN; j++) {
                bitMap.getFBlocks()[i][j] = false;
                disk.getContent()[i][j] = null;
            }
        }

        for (int i = 0; i < LINE * COLUMN; i++) {
            fat.getFatBlocks()[i] =  new FatBlock(i,-1);
        }


        Integer freePos = 0;

        bitMap.getFBlocks()[freePos / COLUMN][freePos % COLUMN] = true;

        IndexFile indexFile = new IndexFile(index++, "/", true, true, fat.getFatBlocks()[freePos], null,
                Util.getCurrentTime(), -1, new LinkedList<>());
        root.add(new Index(indexFile, "Share", "/"));
    }

    @Override
    public void initDirectory(String directoryName) {
        //判断根目录下是否已经存在该用户的文件
        for (Index index : root) {
            if (index.getFileName().equals(directoryName)) {
                return;
            }
        }
        //先顺序查找位示图中空闲的位置
        List<Integer> freePos = findDatFreePos(1);
        if (freePos.size() == 1) {
            //修改位示图状态
            Integer place = freePos.get(0);
            bitMap.getFBlocks()[place / COLUMN][place % COLUMN] = true;
            //在根目录下生成一个新的目录文件，以用户名起名, 同时生成索引文件
            IndexFile indexFile = new IndexFile(index++, "/", true, true, fat.getFatBlocks()[freePos.get(0)], null,
                    Util.getCurrentTime(), -1, new LinkedList<>());
            root.add(new Index(indexFile, directoryName, "/"));
        }
    }

    @Override
    public void getDirectory(User user) {
        Index index = FileSystemApplication.userPath.get(user);
        if ("/".equals(index.getPath()) && Util.isNull(index.getFileName())) {
            //说明是在根目录下，输出当前根目录下的所有文件目录项
            for (Index nowIndex : root) {
                System.out.print(nowIndex.getFileName() + " ");
            }
            System.out.print("\r\n");
            return;
        }
        if (!Util.isNull(index.getIndexFile())) {
            //说明是非根目录
            if (index.getIndexFile().getChildren().size() == 0) {
                System.out.println("The current directory is empty！");
            }
            else {
                Iterator<Index> iterator = index.getIndexFile().getChildren().iterator();
                Index help = null;
                for (Index child : root) {
                    if ("Share".equals(child.getFileName())) {
                        help = child;
                        break;
                    }
                }
                while (iterator.hasNext()){
                    Index childrenIndex = iterator.next();
                    //需要看下该文件是否被删除
                    int isFind = 0;
                    if (!Util.isNull(childrenIndex.getIndexFile())) {
                        if (childrenIndex.getIndexFile().getIsCatalog() ||
                                (!childrenIndex.getIndexFile().getIsCatalog() && !childrenIndex.getIndexFile().getIsPublic())) {
                            isFind = 1;
                        }
                    }
                    if (isFind == 0){
                        for (Index remove : help.getIndexFile().getChildren()) {
                            if (remove.getFileName().equals(childrenIndex.getFileName()) && remove.getPath().equals(childrenIndex.getPath())) {
                                isFind = 1;
                                break;
                            }
                        }
                    }
                    if (isFind == 0) {
                        iterator.remove();
                    }
                }
                if (index.getIndexFile().getChildren().size() == 0) {
                    System.out.println("当前文件夹为空！");
                    return;
                }
                for (Index childrenIndex : index.getIndexFile().getChildren()) {
                    System.out.print(childrenIndex.getFileName() + " ");
                }
                System.out.print("\r\n");
            }
        }
    }

    @Override
    public void changeDirectory(String message, User user) {
        //执行查找该目录方法
        Index index = findDirectory(message, user, 0);
        if (Util.isNull(index)) {
            return;
        }
        if (!Util.isNull(index.getIndexFile()) && !index.getIndexFile().getIsCatalog()) {
            System.out.println(index.getFileName() + " 是文件！");
            return;
        }
        //更新用户当前目录
        FileSystemApplication.userPath.remove(user);
        FileSystemApplication.userPath.put(user, index);
    }

    @Override
    public void createDirectory(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        //type为1代表生成一个新的文件夹
        createNewDirectoryOrFile(message, user, 1);
    }

    @Override
    public void createFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        //type为0代表生成一个新的文件
        createNewDirectoryOrFile(message, user, 0);
    }

    @Override
    public void openFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        //查找到某个文件
        Index index = findDirectory(message, user, 0);
        if (!Util.isNull(index)) {
            if (index.getIndexFile().getIsCatalog()) {
                //说明是一个文件夹
                System.out.println(index.getFileName() + " is a directory！");
            }
            else {
                if (index.getIndexFile().getStatus() != 2) {
                    index.getIndexFile().setStatus(1);
                    //判断该文件是否为共享文件,及根目录名和用户名是否匹配
                    if (!isSharedFile(user, index)) {
                        //更新用户当前目录,去到这个文件的父目录下
                        FileSystemApplication.userPath.remove(user);
                        FileSystemApplication.userPath.put(user, index.getIndexFile().getParent());
                    }
                    openFile.add(index);
                    System.out.println("Open " + index.getFileName() + " Successfully！");
                }
                else {
                    System.out.println("Failed to open " + index.getFileName() + ", there are users writing now！");
                }
            }
        }
    }

    @Override
    public void closeFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        //查找到某个文件
        Index index = findOpenFile(message, user);
        if (!Util.isNull(index)) {
            if (index.getIndexFile().getIsCatalog()) {
                //说明是一个文件夹
                System.out.println(index.getFileName() + " is a directory！");
            }
            else {
                if (index.getIndexFile().getStatus() != 2) {
                    index.getIndexFile().setStatus(0);
                    openFile.remove(index);
                    System.out.println("Close " + index.getFileName() + " Successfully！");
                }
                else {
                    System.out.println("Failed to close " + index.getFileName() + ", there are users writing now！");
                }
            }
        }
        else {
            System.out.println("Please open the file firstly！");
        }
    }

    @Override
    public void readFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        //查找到某个文件
        Index index = findOpenFile(message, user);
        if (!Util.isNull(index)) {
            if (index.getIndexFile().getIsCatalog()) {
                //说明是一个文件夹
                System.out.println(index.getFileName() + " is a directory！");
            }
            else {
                switch (index.getIndexFile().getStatus()) {
                    case 0:
                        System.out.println("Please open the file firstly！");
                        break;
                    case 1:
                        //输出文件内容
                        outPutFileContent(index);
                        break;
                    case 2:
                        System.out.println("Failed to read " + index.getFileName() + ", there are users writing now！");
                }
            }
        }
        else {
            System.out.println("Please open the file firstly！");
        }
    }

    @Override
    public void writeFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        //查找到某个文件
        Index index = findOpenFile(message, user);
        if (!Util.isNull(index)) {
            if (index.getIndexFile().getIsCatalog()) {
                //说明是一个文件夹
                System.out.println(index.getFileName() + "is a directory！");
            }
            else {
                switch (index.getIndexFile().getStatus()) {
                    case 0:
                        System.out.println("Please open the file firstly！");
                        break;
                    case 1:
                        //执行文件读写方法
                        index.getIndexFile().setStatus(2);
                        writeFile(index);
                        outPutFileContent(index);
                        break;
                    case 2:
                        System.out.println("Fail to read " + index.getFileName() + ", there are users writing now！");
                }
            }
        }
        else {
            System.out.println("Please open the file firstly！");
        }
    }

    @Override
    public void deleteFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        //执行查找该目录方法
        Index index = findDirectory(message, user, 0);
        if (Util.isNull(index)) {
            return;
        }
        //判断是否为用户个人的目录
        if (Util.isNull(index.getIndexFile().getParent())) {
            System.out.println("The root directory cannot be deleted！");
            return;
        }
        //查看用户是否有权限删除它
        if (!user.getName().equals(index.getPath().substring(1).split("\\/")[0])) {
            System.out.println("You do not have permission to delete this file！");
            return;
        }
        //查看是否需要更换目录，如果删除的正好是自己所在的目录，则需要改变
        if (FileSystemApplication.userPath.get(user).getPath().equals(index.getPath()) &&
                FileSystemApplication.userPath.get(user).getFileName().equals(index.getFileName())) {
            //更新用户当前目录
            FileSystemApplication.userPath.remove(user);
            FileSystemApplication.userPath.put(user, index.getIndexFile().getParent());
        }
        index.getIndexFile().getParent().getIndexFile().getChildren().remove(index);
        //若为共享文件，则需要将其从共享文件夹中删除
        if (index.getIndexFile().getIsPublic()) {
            Index help = null;
            for (Index child : root) {
                if ("Share".equals(child.getFileName())) {
                    help = child;
                    break;
                }
            }
            for (Index remove : help.getIndexFile().getChildren()) {
                if (remove.getFileName().equals(index.getFileName())) {
                    help.getIndexFile().getChildren().remove(remove);
                    break;
                }
            }
        }
        //释放对应的空间
        freeRoom(index);
    }

    @Override
    public void linkFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        //修改message中的内容,查找Share目录中的文件名进行匹配
        String fileName = message.split(" ")[1];
        Index help = null;
        for (Index child : root) {
            if ("Share".equals(child.getFileName())) {
                help = child;
                break;
            }
        }
        Index index = null;
        if (!Util.isNull(help) && !Util.isNull(help.getIndexFile())) {
            if (help.getIndexFile().getChildren().size() == 0) {
                System.out.println("There are no files to share at the moment！");
                return;
            }
            int isFind = 0;
            for (Index file : help.getIndexFile().getChildren()) {
                if (file.getFileName().equals(fileName)) {
                    index = file;
                    isFind = 1;
                    break;
                }
            }
            if (isFind == 0) {
                System.out.println("There is no file which is named " + fileName);
                return;
            }
        }
        if (!Util.isNull(index)) {
            //找到该文件的父目录
            index = findDirectory("cd " + index.getPath().substring(1), user, 1);
            Index userPath = FileSystemApplication.userPath.get(user);
            if (!Util.isNull(index) && !Util.isNull(index.getIndexFile())) {
                if (index.getIndexFile().getChildren().size() == 0) {
                    System.out.println("The file had been deleted！");
                    return;
                }
                int isFind = 0;
                for (Index file : index.getIndexFile().getChildren()) {
                    if (file.getFileName().equals(fileName)) {
                        index = file;
                        isFind = 1;
                        break;
                    }
                }
                if (isFind == 0) {
                    System.out.println("The file had been deleted！");
                    return;
                }
            }
            userPath.getIndexFile().getChildren().add(index);
        }
    }

    @Override
    public void showBitMap() {
        System.out.println();
        for(int i=0;i<COLUMN;i++){
            for(int j=0;j<LINE;j++){
                if(bitMap.getFBlocks()[i][j]){
                    System.out.print("1\t");
                }else{
                    System.out.print("0\t");
                }

            }
            System.out.println();
        }
        System.out.println();
    }

    @Override
    public User login(String name, String password) {

        if(Util.isStringEmpty(name)||Util.isStringEmpty(password)){
            System.out.println("enter empty");
            return null;
        }

        User user = mapper.getUser(name,password);

        if(user!=null){
            System.out.println("Login successfully!");
            return user;
        }else{
            System.out.println("userName or password is wrong!Please enter again!");
            return null;
        }


    }

    private void writeFile(Index index) {
        //先输出文件中已有的内容
        outPutFileContent(index);
        Scanner scanner = new Scanner(System.in);
        StringBuffer result = new StringBuffer();
        while (true) {
            String content = scanner.nextLine() + "\r\n";
            if ("#\r\n".equals(content)) {
                break;
            }
            result.append(content);
        }
        //若文件大小超出范围，则需要存放到其他的盘块中，先找到最后一个盘块，看看剩余容量是否足够
        FatBlock fatBlock = index.getIndexFile().getFirstBlock();
        Integer number;
        int over = 0;
        do {
            if (fatBlock.getNextBlockId() == -1) {
                over = 1;
            }
            else {
                //获取下一个文件分配表项
                Integer next = fatBlock.getNextBlockId();
                fatBlock = fat.getFatBlocks()[next];
            }
        } while (over == 0);
        number = fatBlock.getBlockId();
        if (disk.getContent()[number / COLUMN][number % COLUMN] == null) {
            disk.getContent()[number / COLUMN][number % COLUMN] = "";
        }
        int left = BLOCKSIZE - disk.getContent()[number / COLUMN][number % COLUMN].length();
        if (left > 0) {
            if (result.length() > left) {
                disk.getContent()[number / COLUMN][number % COLUMN] += "\r\n" + result.substring(0, left);
            }
            else {
                disk.getContent()[number / COLUMN][number % COLUMN] += "\r\n" + result;
            }
        }
        if (result.length() > left) {
            //说明还需要再分配新的磁盘空间进行存放
            int num = (result.length() - left) / BLOCKSIZE + 1;
            //剩下的写入的文件长度
            int length = result.length() - left;
            for (int i = 0; i < num; i++) {
                //将部分内容存储到新的盘块中
                //先顺序查找位示图中空闲的位置
                List<Integer> freeLoc = findDatFreePos(1);
                if (freeLoc.size() != 0) {
                    //修改位示图状态
                    Integer place = freeLoc.get(0);
                    bitMap.getFBlocks()[place / COLUMN][place % COLUMN] = true;
                    fatBlock.setNextBlockId(place);
                    if (disk.getContent()[place / COLUMN][place % COLUMN] == null) {
                        disk.getContent()[place / COLUMN][place % COLUMN] = "";
                    }
                    if (length > BLOCKSIZE) {
                        disk.getContent()[place / COLUMN][place % COLUMN] += result.substring(left, left + BLOCKSIZE);
                        left += BLOCKSIZE;
                        length -= BLOCKSIZE;
                    }
                    else {
                        disk.getContent()[place / COLUMN][place % COLUMN] += result.substring(left, left + length);
                    }
                    fatBlock = fat.getFatBlocks()[place];
                }
                else {
                    System.out.println("Disk space is not enough");
                    break;
                }
            }
        }
        //文件时间改为最新的修改时间
        index.getIndexFile().setModifyTime(Util.getCurrentTime());
        index.getIndexFile().setStatus(1);
    }

    private void outPutFileContent(Index index) {
        System.out.println(index.getFileName() + "'s content：");
        //输出所有盘块的内容
        FatBlock fatBlock = index.getIndexFile().getFirstBlock();
        StringBuffer content = new StringBuffer();
        int over = 0;
        do {
            Integer number = fatBlock.getBlockId();
            if (fatBlock.getNextBlockId() == -1) {
                over = 1;
            }
            else {
                //获取下一个文件分配表项
                Integer next = fatBlock.getNextBlockId();
                fatBlock = fat.getFatBlocks()[next];
            }
            if (disk.getContent()[number / COLUMN][number % COLUMN] != null) {
                content.append(disk.getContent()[number / COLUMN][number % COLUMN]);
            }
        } while (over == 0);
        if (content.length() != 0) {
            System.out.println(content.toString());
        }
    }

    private void createNewDirectoryOrFile(String message, User user, Integer type) {
        String[] path = message.split(" ")[1].split("\\/");
        Index userPath = FileSystemApplication.userPath.get(user);
        Index index = new Index(userPath.getIndexFile(), userPath.getFileName(), userPath.getPath());
        List<Index> childDirectory = null;
        //校验欲更换的目录是否存在，存在则一级一级跳转
        for (int i = 0; i < path.length; i++) {
            if ("..".equals(path[i])) {
                //获取上一级目录
                if (!Util.isNull(index.getIndexFile())) {
                    if (Util.isNull(index.getIndexFile().getParent())) {
                        index.setIndexFile(null);
                        index.setFileName(null);
                        index.setPath("/");
                    }
                    else {
                        index = index.getIndexFile().getParent();
                    }
                }
                continue;
            }
            //获取当前目录下的子目录
            if ("/".equals(index.getPath()) && Util.isNull(index.getFileName())) {
                childDirectory = root;
            }
            else if (!Util.isNull(index.getIndexFile())) {
                //说明是非根目录
                if (index.getIndexFile().getChildren().size() == 0) {
                    Index newDirectory = null;
                    //创建一个目录或文件
                    if (i == path.length - 1) {
                        //查看当前目录是否在根目录下创建，是则不允许
                        if ("/".equals(index.getPath()) && Util.isNull(index.getFileName())) {
                            System.out.println("Cannot create files in the root directory！");
                            return;
                        }
                        //在Share文件夹下不可进行创建文件（夹）操作
                        if ("/".equals(index.getPath()) && "Share".equals(index.getFileName())) {
                            System.out.println("Cannot create files in the shared directory！");
                            return;
                        }
                        //说明此时已经是最后一个位置，可能创建的是文件或者文件夹，根据type来决定
                        newDirectory = newDirectory(path[i], type);
                    }
                    else {
                        newDirectory = newDirectory(path[i], 1);
                    }
                    if (Util.isNull(newDirectory)) {
                        return;
                    }
                    if ("/".equals(index.getPath())) {
                        newDirectory.setPath(index.getPath() + index.getFileName());
                        newDirectory.getIndexFile().setPath(index.getPath() + index.getFileName());
                    }
                    else {
                        newDirectory.setPath(index.getPath() + "/" + index.getFileName());
                        newDirectory.getIndexFile().setPath(index.getPath() + "/" + index.getFileName());
                    }
                    newDirectory.getIndexFile().setParent(index);
                    index.getIndexFile().getChildren().add(newDirectory);
                    //更换当前目录
                    if (type != 0 || i != path.length - 1) {
                        index = newDirectory;
                    }
                    //如果是想要的共享文件，则将其放置到Share目录下
                    if (!newDirectory.getIndexFile().getIsCatalog() && newDirectory.getIndexFile().getIsPublic()) {
                        for (Index root : root) {
                            if ("Share".equals(root.getFileName())) {
                                root.getIndexFile().getChildren().add(new Index(null, newDirectory.getFileName(),
                                        newDirectory.getPath()));
                            }
                        }
                    }
                    continue;
                }
                else {
                    childDirectory = index.getIndexFile().getChildren();
                }
            }
            if (!Util.isNull(childDirectory)) {
                boolean isChange = false;
                for (Index child : childDirectory) {
                    if (child.getFileName().equals(path[i])) {
                        if (i == path.length - 1) {
                            //说明此时已经是最后一个位置，此时文件或文件夹重名了
                            System.out.println(child.getFileName() + " has existed！");
                            return;
                        }
                        //说明该目录存在，则更新当前目录
                        index = child;
                        isChange = true;
                        break;
                    }
                }
                if (!isChange) {
                    //说明当前目录不存在要查找的，需要新创建一个目录
                    Index newDirectory = null;
                    //创建一个目录或文件
                    if (i == path.length - 1) {
                        //查看当前目录是否在根目录下创建，是则不允许
                        if ("/".equals(index.getPath()) && Util.isNull(index.getFileName())) {
                            System.out.println("Cannot create files in the root directory！");
                            return;
                        }
                        //在Share文件夹下不可进行创建文件（夹）操作
                        if ("/".equals(index.getPath()) && "Share".equals(index.getFileName())) {
                            System.out.println("Cannot create files in the shared directory！");
                            return;
                        }
                        //说明此时已经是最后一个位置，可能创建的是文件或者文件夹，根据type来决定
                        newDirectory = newDirectory(path[i], type);
                    }
                    else {
                        newDirectory = newDirectory(path[i], 1);
                    }
                    if (Util.isNull(newDirectory)) {
                        return;
                    }
                    if ("/".equals(index.getPath())) {
                        newDirectory.setPath(index.getPath() + index.getFileName());
                        newDirectory.getIndexFile().setPath(index.getPath() + index.getFileName());
                    }
                    else {
                        newDirectory.setPath(index.getPath() + "/" + index.getFileName());
                        newDirectory.getIndexFile().setPath(index.getPath() + "/" + index.getFileName());
                    }
                    newDirectory.getIndexFile().setParent(index);
                    index.getIndexFile().getChildren().add(newDirectory);
                    //更换当前目录
                    if (newDirectory.getIndexFile().getIsCatalog() || (i == path.length - 1 && newDirectory.getIndexFile().getIsCatalog())) {
                        index = newDirectory;
                    }
                    //如果是想要的共享文件，则将其放置到Share目录下
                    if (!newDirectory.getIndexFile().getIsCatalog() && newDirectory.getIndexFile().getIsPublic()) {
                        for (Index root : root) {
                            root.getIndexFile().getChildren().add(new Index(null, newDirectory.getFileName(),
                                    newDirectory.getPath()));
                        }
                    }
                }
            }
        }
        //更新用户当前目录
        FileSystemApplication.userPath.remove(user);
        FileSystemApplication.userPath.put(user, index);
    }

    private Index newDirectory(String fileName, Integer type) {
        //先顺序查找位示图中空闲的位置
        List<Integer> freeLoc = findDatFreePos(1);
        if (freeLoc.size() != 0) {
            //一开始创建目录或文件只需要占用一个盘块
            //修改位示图状态
            Integer place = freeLoc.get(0);
            bitMap.getFBlocks()[place / COLUMN][place % COLUMN] = true;
            IndexFile IndexFile = null;
            if (type == 1) {
                //生成一个新的目录文件，以用户名起名, 同时生成索引文件
                IndexFile = new IndexFile(index++, null, true, true, fat.getFatBlocks()[freeLoc.get(0)], null,
                        Util.getCurrentTime(), -1, new LinkedList<>());
            }
            else if (type == 0) {
                Scanner scanner = new Scanner(System.in);
                String choice = null;
                while (true) {
                    System.out.println("Please select permissions for the file：0.private, 1.public");
                    choice = scanner.nextLine();
                    if (!"0".equals(choice) && !"1".equals(choice)) {
                        System.out.println("Input error, please retry！");
                    }
                    else {
                        break;
                    }
                }



                boolean b;

                if(choice.equals("1")){
                    b=true;
                }else{
                    b = false;
                }

                IndexFile = new IndexFile(index++, null, false, b, fat.getFatBlocks()[freeLoc.get(0)], null,
                        Util.getCurrentTime(), 0, null);
            }
            return new Index(IndexFile, fileName, null);
        }
        System.out.println("Disk space is not enough");
        return null;
    }


    private Index findOpenFile(String message, User user) {
        Index userPath = FileSystemApplication.userPath.get(user);
        String filePath = message.split(" ")[1];
        for (Index index : openFile) {
            String[] fileName = filePath.split("\\/");
            if (fileName[fileName.length - 1].equals(index.getFileName())) {
                //比较下路径
                String path;
                if (!"/".equals(userPath.getPath())) {
                    path = userPath.getPath() + "/" + userPath.getFileName();
                }
                else {
                    path = userPath.getPath() + userPath.getFileName();
                }
                for (int i = 0; i < fileName.length - 1; i++) {
                    path = path + "/" + fileName[i];
                }
                if (path.equals(index.getPath())) {
                    return index;
                }
                if (!index.getPath().substring(1).split("\\/")[0].equals(user.getName())) {
                    return index;
                }
            }
        }
        return null;
    }

    private Boolean isSharedFile(User user, Index index) {
        String userName = index.getPath().substring(1).split("/")[0];
        if (user.getName().equals(userName)) {
            return false;
        }
        return true;
    }

    private Boolean isLegal(User user) {
        Index userPath = FileSystemApplication.userPath.get(user);
        if ("Share".equals(userPath.getFileName()) && "/".equals(userPath.getPath())) {
            System.out.println("The Shared folder is not operational！");
            return false;
        }
        if ("/".equals(userPath.getPath()) && Util.isStringEmpty(userPath.getFileName())) {
            System.out.println("The Root folder is not operational！");
            return false;
        }
        return true;
    }

    private List<Integer> findDatFreePos(Integer num) {
        List<Integer> freePos = new ArrayList<>();
        //使用顺序查找法
        for (int i = 0; i < LINE; i++) {
            for (int j = 0; j < COLUMN; j++) {
                if (!bitMap.getFBlocks()[i][j]) {
                    //说明没被使用过
                    freePos.add(LINE * i + j);
                    num--;
                    if (num == 0) {
                        return freePos;
                    }
                }
            }
        }
        System.out.println("没有足够的空闲块!");
        return freePos;
    }

    private Index findDirectory(String message, User user, Integer type) {
        String[] path = message.split(" ")[1].split("\\/");
        Index userPath;
        if (type == 1) {
            userPath = new Index(null, null, "/");
        }
        else {
            userPath = FileSystemApplication.userPath.get(user);
        }
        Index index = new Index(userPath.getIndexFile(), userPath.getFileName(), userPath.getPath());
        List<Index> childDirectory = null;
        //校验欲查找的目录是否存在，存在则一级一级跳转
        for (int i = 0; i < path.length; i++) {
            if ("..".equals(path[i])) {
                //获取上一级目录
                if (!Util.isNull(index.getIndexFile())) {
                    if (Util.isNull(index.getIndexFile().getParent())) {
                        index.setIndexFile(null);
                        index.setFileName(null);
                        index.setPath("/");
                    }
                    else {
                        index = index.getIndexFile().getParent();
                    }
                }
                continue;
            }
            int isRoot = 0;
            //获取当前目录下的子目录
            if ("/".equals(index.getPath()) && Util.isNull(index.getFileName())) {
                childDirectory = root;
                isRoot = 1;
            }
            else if (!Util.isNull(index.getIndexFile())) {
                //说明是非根目录
                if (index.getIndexFile().getChildren().size() == 0) {
                    System.out.println(index.getFileName() + " 为空！");
                    return null;
                }
                else {
                    childDirectory = index.getIndexFile().getChildren();
                }
            }
            if (!Util.isNull(childDirectory)) {
                boolean isChange = false;
                for (Index child : childDirectory) {
                    if (child.getFileName().equals(path[i])) {
                        if (isRoot == 1) {
                            //判断下是否有权限
                            if (!user.getName().equals(path[i]) && !"Share".equals(path[i]) && type == 0) {
                                System.out.println("文件夹不可访问！");
                                return null;
                            }
                        }
                        //说明该目录存在，则更新当前目录
                        if (child.getIndexFile().getIsCatalog()) {
                            index = child;
                            isChange = true;
                            break;
                        }
                        else {
                            //说明是一个文件，不是文件夹
                            if (i != path.length - 1) {
                                System.out.println(child.getFileName() + " 是一个文件！");
                                return null;
                            }
                            return child;
                        }
                    }
                }
                if (!isChange) {
                    //说明当前目录不存在要查找的
                    System.out.println("不存在文件夹 " + path[i] + "！");
                    return null;
                }
            }
        }
        return index;
    }

    private void freeRoom(Index index) {
        if (index.getIndexFile().getIsCatalog()) {
            for (Index child : index.getIndexFile().getChildren()) {
                freeRoom(child);
            }
        }
        index.getIndexFile().setParent(null);
        FatBlock fatBlock = index.getIndexFile().getFirstBlock();
        int over = 0;
        //针对每一个文件分配表项、位示图和磁盘信息进行修改
        do {
            //修改位示图中的值
           bitMap.getFBlocks()[fatBlock.getBlockId() / COLUMN][fatBlock.getBlockId() % COLUMN] = false;
            if (fatBlock.getNextBlockId() == -1) {
                over = 1;
            }
            else {
                //继续修改下一个文件分配表项
                Integer next = fatBlock.getNextBlockId();
                fatBlock.setNextBlockId(-1);
                fatBlock = fat.getFatBlocks()[next];
            }
        } while (over == 0);
    }

}
