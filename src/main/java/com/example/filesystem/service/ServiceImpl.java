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

    public static LinkedList<IndexFile> root = new LinkedList<>();

    public static BitMap bitMap = new BitMap(LINE, COLUMN);
    public static FAT fat = new FAT(LINE, COLUMN);
    public static FileContent fileContent = new FileContent(LINE, COLUMN);

    private static LinkedList<IndexFile> openFile = new LinkedList<>();

    static {
        //初始化文件状态
        for (int i = 0; i < LINE; i++) {
            for (int j = 0; j < COLUMN; j++) {
                bitMap.getFBlocks()[i][j] = false;
                fileContent.getContent()[i][j] = null;
            }
        }

        for (int i = 0; i < LINE * COLUMN; i++) {
            fat.getFatBlocks()[i] =  new FatBlock(i,-1);
        }

        bitMap.getFBlocks()[0][0] = true;

        FSFile FSFile = new FSFile(index++, "\\", true, true, fat.getFatBlocks()[0], null,
                Util.getCurrentTime(), -1, new LinkedList<>());
        root.add(new IndexFile(FSFile, "share", "\\"));
    }

    @Override
    public void initDirectory(String directoryName) {

        for (IndexFile indexFile : root) {
            if (indexFile.getFileName().equals(directoryName)) {
                return;
            }
        }

        List<Integer> freePos = findDatFreePos(1);
        if (freePos.size() == 1) {

            Integer place = freePos.get(0);
            bitMap.getFBlocks()[place / COLUMN][place % COLUMN] = true;

            FSFile FSFile = new FSFile(index++, "\\", true, true, fat.getFatBlocks()[freePos.get(0)], null,
                    Util.getCurrentTime(), -1, new LinkedList<>());
            root.add(new IndexFile(FSFile, directoryName, "\\"));
        }
    }

    @Override
    public void getDirectory(User user) {
        IndexFile indexFile = FileSystemApplication.userPath.get(user);

        System.out.println();
        if ("\\".equals(indexFile.getPath()) && Util.isNull(indexFile.getFileName())) {


            for (IndexFile nowIndexFile : root) {

                if(nowIndexFile.getFSFile().getIsCatalog()){
                    System.out.println("FileFolder："+ nowIndexFile.getFileName());
                }else{
                    System.out.println("File："+ nowIndexFile.getFileName());
                }
            }
            System.out.println();
            return;
        }

        if (!Util.isNull(indexFile.getFSFile())) {

            if (indexFile.getFSFile().getChildren().size() == 0) {
                System.out.println("The current directory is empty!");
            }else {
                Iterator<IndexFile> iterator = indexFile.getFSFile().getChildren().iterator();
                IndexFile help = null;
                for (IndexFile child : root) {
                    if ("share".equals(child.getFileName())) {
                        help = child;
                        break;
                    }
                }
                while (iterator.hasNext()){
                    IndexFile childrenIndexFile = iterator.next();

                    int isFind = 0;
                    if (!Util.isNull(childrenIndexFile.getFSFile())) {
                        if (childrenIndexFile.getFSFile().getIsCatalog() ||
                                (!childrenIndexFile.getFSFile().getIsCatalog() && !childrenIndexFile.getFSFile().getIsPublic())) {
                            isFind = 1;
                        }
                    }
                    if (isFind == 0){
                        for (IndexFile remove : help.getFSFile().getChildren()) {
                            if (remove.getFileName().equals(childrenIndexFile.getFileName()) && remove.getPath().equals(childrenIndexFile.getPath())) {
                                isFind = 1;
                                break;
                            }
                        }
                    }
                    if (isFind == 0) {
                        iterator.remove();
                    }
                }
                if (indexFile.getFSFile().getChildren().size() == 0) {
                    System.out.println("The filefolder is empty!");
                    return;
                }

                for (IndexFile childrenIndexFile : indexFile.getFSFile().getChildren()) {

                    if(indexFile.getFileName().equals("share")){
                        System.out.println("File："+ childrenIndexFile.getFileName());
                    }else if(childrenIndexFile.getFSFile()!=null){
                        if(childrenIndexFile.getFSFile().getIsCatalog()){
                            System.out.println("FileFolder："+ childrenIndexFile.getFileName());
                        }else{
                            System.out.println("File："+ childrenIndexFile.getFileName());
                        }
                    }
                }
            }

        }
        System.out.println();
    }

    @Override
    public void  changeDirectory(String message, User user) {

        IndexFile indexFile = findDirectory(message, user, 0);
        if (Util.isNull(indexFile)) {
            return;
        }
        if (!Util.isNull(indexFile.getFSFile()) && !indexFile.getFSFile().getIsCatalog()) {
            System.out.println(indexFile.getFileName() + " is file");
            return;
        }

        FileSystemApplication.userPath.remove(user);
        FileSystemApplication.userPath.put(user, indexFile);
    }

    @Override
    public void createDirectory(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        createNewDirectoryOrFile(message, user, 1);
    }

    @Override
    public void createFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        createNewDirectoryOrFile(message, user, 0);
    }

    @Override
    public void openFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }

        IndexFile indexFile = findDirectory(message, user, 0);
        if (!Util.isNull(indexFile)) {
            if (indexFile.getFSFile().getIsCatalog()) {
                System.out.println(indexFile.getFileName() + " is directory");
            }else {
                if (indexFile.getFSFile().getStatus() != 2) {
                    indexFile.getFSFile().setStatus(1);

                    if (!isSharedFile(user, indexFile)) {

                        FileSystemApplication.userPath.remove(user);
                        FileSystemApplication.userPath.put(user, indexFile.getFSFile().getParent());
                    }
                    openFile.add(indexFile);
                    System.out.println("Open " + indexFile.getFileName() + " Successfully!");
                }else {
                    System.out.println("Failed to open " + indexFile.getFileName() + ", it's written by other user!");
                }
            }
        }
    }

    @Override
    public void closeFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        IndexFile indexFile = findOpenFile(message, user);
        if (!Util.isNull(indexFile)) {
            if (indexFile.getFSFile().getIsCatalog()) {
                System.out.println(indexFile.getFileName() + " is directory!");
            }
            else {
                if (indexFile.getFSFile().getStatus() != 2) {
                    indexFile.getFSFile().setStatus(0);
                    openFile.remove(indexFile);
                    System.out.println("Close " + indexFile.getFileName() + " Successfully!");
                }else {
                    System.out.println("Failed to close " + indexFile.getFileName() + ", it's written by other user!");
                }
            }
        }
        else {
            System.out.println("You don't open the file!");
        }
    }

    @Override
    public void readFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }

        IndexFile indexFile = findOpenFile(message, user);
        if (!Util.isNull(indexFile)) {
            if (indexFile.getFSFile().getIsCatalog()) {
                System.out.println(indexFile.getFileName() + " is directory!");
            }
            else {
                switch (indexFile.getFSFile().getStatus()) {
                    case 0:
                        System.out.println("Please open file first!");
                        break;
                    case 1:
                        printFileContent(indexFile);
                        break;
                    case 2:
                        System.out.println("Failed to read " + indexFile.getFileName() + ", it's written by other user!");
                }
            }
        }
        else {
            System.out.println("You don't open the file!");
        }
    }

    @Override
    public void writeFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }

        IndexFile indexFile = findOpenFile(message, user);
        if (!Util.isNull(indexFile)) {
            if (indexFile.getFSFile().getIsCatalog()) {
                System.out.println(indexFile.getFileName() + "is directory!");
            }
            else {
                switch (indexFile.getFSFile().getStatus()) {
                    case 0:
                        System.out.println("Please open the file firstly!");
                        break;
                    case 1:
                        indexFile.getFSFile().setStatus(2);
                        writeFile(indexFile);
                        printFileContent(indexFile);
                        break;
                    case 2:
                        System.out.println("Fail to read " + indexFile.getFileName() + ", it's written by other user!");
                }
            }
        }
        else {
            System.out.println("You don't open the file!");
        }
    }

    @Override
    public void deleteFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }

        IndexFile indexFile = findDirectory(message, user, 0);
        if (Util.isNull(indexFile)) {
            return;
        }

        if (Util.isNull(indexFile.getFSFile().getParent())) {
            System.out.println("You can't delete the root directory");
            return;
        }

        if (!user.getName().equals(indexFile.getPath().substring(1).split("\\\\")[0])) {
            System.out.println("You haven't permission to delete this file");
            return;
        }

        if (FileSystemApplication.userPath.get(user).getPath().equals(indexFile.getPath()) &&
                FileSystemApplication.userPath.get(user).getFileName().equals(indexFile.getFileName())) {

            FileSystemApplication.userPath.remove(user);
            FileSystemApplication.userPath.put(user, indexFile.getFSFile().getParent());
        }
        indexFile.getFSFile().getParent().getFSFile().getChildren().remove(indexFile);

        if (indexFile.getFSFile().getIsPublic()) {
            IndexFile help = null;
            for (IndexFile child : root) {
                if ("share".equals(child.getFileName())) {
                    help = child;
                    break;
                }
            }
            for (IndexFile remove : help.getFSFile().getChildren()) {
                if (remove.getFileName().equals(indexFile.getFileName())) {
                    help.getFSFile().getChildren().remove(remove);
                    break;
                }
            }
        }

        freeRoom(indexFile);
    }

    @Override
    public void linkFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }

        String fileName = message.split(" ")[1];
        IndexFile help = null;
        for (IndexFile child : root) {
            if ("share".equals(child.getFileName())) {
                help = child;
                break;
            }
        }
        IndexFile indexFile = null;
        if (!Util.isNull(help) && !Util.isNull(help.getFSFile())) {
            if (help.getFSFile().getChildren().size() == 0) {
                System.out.println("There are no files!");
                return;
            }
            int isFind = 0;
            for (IndexFile file : help.getFSFile().getChildren()) {
                if (file.getFileName().equals(fileName)) {
                    indexFile = file;
                    isFind = 1;
                    break;
                }
            }
            if (isFind == 0) {
                System.out.println("Can't find the file" + fileName);
                return;
            }
        }
        if (!Util.isNull(indexFile)) {
            //找到父目录
            indexFile = findDirectory("cd " + indexFile.getPath().substring(1), user, 1);
            IndexFile userPath = FileSystemApplication.userPath.get(user);
            if (!Util.isNull(indexFile) && !Util.isNull(indexFile.getFSFile())) {
                if (indexFile.getFSFile().getChildren().size() == 0) {
                    System.out.println("The file had been deleted!");
                    return;
                }
                int isFind = 0;
                for (IndexFile file : indexFile.getFSFile().getChildren()) {
                    if (file.getFileName().equals(fileName)) {
                        indexFile = file;
                        isFind = 1;
                        break;
                    }
                }
                if (isFind == 0) {
                    System.out.println("The file had been deleted!");
                    return;
                }
            }
            userPath.getFSFile().getChildren().add(indexFile);
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
            System.out.println("userName or password is empty");
            return null;
        }

        User user = mapper.getUser(name,password);

        if(user!=null){
            System.out.println("Login successfully!");
            return user;
        }else{
            System.out.println("userName or password is wrong , Please enter again.");
            return null;
        }


    }

    private void writeFile(IndexFile indexFile) {

        printFileContent(indexFile);

        //“#”截止输入
        Scanner scanner = new Scanner(System.in);
        StringBuffer result = new StringBuffer();
        while (true) {
            String content = scanner.nextLine() + "\r\n";
            if ("#\r\n".equals(content)) {
                break;
            }
            result.append(content);
        }


        FatBlock fatBlock = indexFile.getFSFile().getFirstBlock();
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

        if (fileContent.getContent()[number / COLUMN][number % COLUMN] == null) {
            fileContent.getContent()[number / COLUMN][number % COLUMN] = "";
        }

        int leftover = BLOCKSIZE - fileContent.getContent()[number / COLUMN][number % COLUMN].length();

        if (leftover > 0) {
            if (result.length() > leftover) {
                fileContent.getContent()[number / COLUMN][number % COLUMN] += "\r\n" + result.substring(0, leftover);
            }
            else {
                fileContent.getContent()[number / COLUMN][number % COLUMN] += "\r\n" + result;
            }
        }

        if (result.length() > leftover) {

            int num = (result.length() - leftover) / BLOCKSIZE + 1;

            int length = result.length() - leftover;
            for (int i = 0; i < num; i++) {

                List<Integer> freeLoc = findDatFreePos(1);
                if (freeLoc.size() != 0) {

                    Integer place = freeLoc.get(0);
                    bitMap.getFBlocks()[place / COLUMN][place % COLUMN] = true;
                    fatBlock.setNextBlockId(place);
                    if (fileContent.getContent()[place / COLUMN][place % COLUMN] == null) {
                        fileContent.getContent()[place / COLUMN][place % COLUMN] = "";
                    }
                    if (length > BLOCKSIZE) {
                        fileContent.getContent()[place / COLUMN][place % COLUMN] += result.substring(leftover, leftover + BLOCKSIZE);
                        leftover += BLOCKSIZE;
                        length -= BLOCKSIZE;
                    }
                    else {
                        fileContent.getContent()[place / COLUMN][place % COLUMN] += result.substring(leftover, leftover + length);
                    }
                    fatBlock = fat.getFatBlocks()[place];
                }
                else {
                    System.out.println("Space isn't enough");
                    break;
                }
            }
        }

        indexFile.getFSFile().setModifyTime(Util.getCurrentTime());
        indexFile.getFSFile().setStatus(1);
    }

    private void printFileContent(IndexFile indexFile) {
        System.out.println(indexFile.getFileName() + " content：");

        FatBlock fatBlock = indexFile.getFSFile().getFirstBlock();
        StringBuffer content = new StringBuffer();
        int over = 0;
        do {
            Integer number = fatBlock.getBlockId();
            if (fatBlock.getNextBlockId() == -1) {
                over = 1;
            }
            else {

                Integer next = fatBlock.getNextBlockId();
                fatBlock = fat.getFatBlocks()[next];
            }
            if (fileContent.getContent()[number / COLUMN][number % COLUMN] != null) {
                content.append(fileContent.getContent()[number / COLUMN][number % COLUMN]);
            }
        } while (over == 0);
        if (content.length() != 0) {
            System.out.println(content);
        }
    }

    private void createNewDirectoryOrFile(String message, User user, Integer type) {

        String[] path = message.split(" ")[1].split("\\\\");
        IndexFile userPath = FileSystemApplication.userPath.get(user);
        IndexFile indexFile = new IndexFile(userPath.getFSFile(), userPath.getFileName(), userPath.getPath());

        List<IndexFile> childDirectory = null;

        for (int i = 0; i < path.length; i++) {

            if ("..".equals(path[i])) {

                if (!Util.isNull(indexFile.getFSFile())){

                    if (Util.isNull(indexFile.getFSFile().getParent())) {
                        indexFile.setFSFile(null);
                        indexFile.setFileName(null);
                        indexFile.setPath("\\");
                    }else {
                        indexFile = indexFile.getFSFile().getParent();
                    }

                }
                continue;
            }


            if ("\\".equals(indexFile.getPath()) && Util.isNull(indexFile.getFileName())) {
                childDirectory = root;
            }else if (!Util.isNull(indexFile.getFSFile())) {

                if (indexFile.getFSFile().getChildren().size() == 0) {
                    IndexFile newDirectory = null;

                    if (i == path.length - 1) {

                        if ("/".equals(indexFile.getPath()) && Util.isNull(indexFile.getFileName())) {
                            System.out.println("Can't create files in the root!");
                            return;
                        }

                        if ("\\".equals(indexFile.getPath()) && "share".equals(indexFile.getFileName())) {
                            System.out.println("Can't create files in the share!");
                            return;
                        }

                        newDirectory = newDirectory(path[i], type);
                    }else {
                        newDirectory = newDirectory(path[i], 1);
                    }


                    if (Util.isNull(newDirectory)) {
                        return;
                    }


                    if ("\\".equals(indexFile.getPath())) {
                        newDirectory.setPath(indexFile.getPath() + indexFile.getFileName());
                        newDirectory.getFSFile().setPath(indexFile.getPath() + indexFile.getFileName());
                    }else {
                        newDirectory.setPath(indexFile.getPath() + "\\" + indexFile.getFileName());
                        newDirectory.getFSFile().setPath(indexFile.getPath() + "\\" + indexFile.getFileName());
                    }
                    newDirectory.getFSFile().setParent(indexFile);
                    indexFile.getFSFile().getChildren().add(newDirectory);




                    if (type != 0 || i != path.length - 1) {
                        indexFile = newDirectory;
                    }

                    if (!newDirectory.getFSFile().getIsCatalog() && newDirectory.getFSFile().getIsPublic()) {
                        for (IndexFile root : root) {
                            if ("share".equals(root.getFileName())) {
                                root.getFSFile().getChildren().add(new IndexFile(null, newDirectory.getFileName(), newDirectory.getPath()));
                            }
                        }
                    }
                    continue;
                }else {
                    childDirectory = indexFile.getFSFile().getChildren();
                }


            }



            if (!Util.isNull(childDirectory)) {
                boolean isChange = false;
                for (IndexFile child : childDirectory) {
                    if (child.getFileName().equals(path[i])) {
                        if (i == path.length - 1) {

                            System.out.println(child.getFileName() + " has existed!");
                            return;
                        }

                        indexFile = child;
                        isChange = true;
                        break;
                    }
                }


                if (!isChange) {

                    IndexFile newDirectory = null;

                    if (i == path.length - 1) {

                        if ("\\".equals(indexFile.getPath()) && Util.isNull(indexFile.getFileName())) {
                            System.out.println("Can't create files in the root!");
                            return;
                        }

                        if ("\\".equals(indexFile.getPath()) && "share".equals(indexFile.getFileName())) {
                            System.out.println("Can't create files in the share!");
                            return;
                        }

                        newDirectory = newDirectory(path[i], type);
                    }else {
                        newDirectory = newDirectory(path[i], 1);
                    }


                    if (Util.isNull(newDirectory)) {
                        return;
                    }


                    if ("\\".equals(indexFile.getPath())) {
                        newDirectory.setPath(indexFile.getPath() + indexFile.getFileName());
                        newDirectory.getFSFile().setPath(indexFile.getPath() + indexFile.getFileName());
                    } else {
                        newDirectory.setPath(indexFile.getPath() + "\\" + indexFile.getFileName());
                        newDirectory.getFSFile().setPath(indexFile.getPath() + "\\" + indexFile.getFileName());
                    }
                    newDirectory.getFSFile().setParent(indexFile);

                    indexFile.getFSFile().getChildren().add(newDirectory);



                    if (newDirectory.getFSFile().getIsCatalog() || (i == path.length - 1 && newDirectory.getFSFile().getIsCatalog())) {
                        indexFile = newDirectory;
                    }



                    if (!newDirectory.getFSFile().getIsCatalog() && newDirectory.getFSFile().getIsPublic()) {
                        for (IndexFile root : root) {
                            root.getFSFile().getChildren().add(new IndexFile(null, newDirectory.getFileName(), newDirectory.getPath()));
                        }
                    }
                }
            }
        }

        FileSystemApplication.userPath.remove(user);
        FileSystemApplication.userPath.put(user, indexFile);
    }

    private IndexFile newDirectory(String fileName, Integer type) {

        List<Integer> freeLoc = findDatFreePos(1);
        if (freeLoc.size() != 0) {

            Integer place = freeLoc.get(0);
            bitMap.getFBlocks()[place / COLUMN][place % COLUMN] = true;
            FSFile FSFile = null;
            if (type == 1) {

                FSFile = new FSFile(index++, null, true, true, fat.getFatBlocks()[freeLoc.get(0)], null,
                        Util.getCurrentTime(), -1, new LinkedList<>());
            }
            else if (type == 0) {
                Scanner scanner = new Scanner(System.in);
                String choice;
                while (true) {
                    System.out.println();
                    System.out.println("Please select permissions：0-private  1-public");
                    choice = scanner.nextLine();
                    if (!"0".equals(choice) && !"1".equals(choice)) {
                        System.out.println("Enter error . Please enter again.");
                    }
                    else {
                        break;
                    }
                }



                boolean b;

                if(choice.equals("1")){
                    b = true;
                }else{
                    b = false;
                }

                FSFile = new FSFile(index++, null, false, b, fat.getFatBlocks()[freeLoc.get(0)], null,
                        Util.getCurrentTime(), 0, null);
            }
            return new IndexFile(FSFile, fileName, null);
        }
        System.out.println("Space isn't enough");
        return null;
    }


    private IndexFile findOpenFile(String message, User user) {
        IndexFile userPath = FileSystemApplication.userPath.get(user);
        String filePath = message.split(" ")[1];
        for (IndexFile indexFile : openFile) {
            String[] fileName = filePath.split("\\\\");
            if (fileName[fileName.length - 1].equals(indexFile.getFileName())) {

                String path;
                if (!"\\".equals(userPath.getPath())) {
                    path = userPath.getPath() + "\\" + userPath.getFileName();
                }
                else {
                    path = userPath.getPath() + userPath.getFileName();
                }
                for (int i = 0; i < fileName.length - 1; i++) {
                    path = path + "\\" + fileName[i];
                }
                if (path.equals(indexFile.getPath())) {
                    return indexFile;
                }
                if (!indexFile.getPath().substring(1).split("\\\\")[0].equals(user.getName())) {
                    return indexFile;
                }
            }
        }
        return null;
    }

    private Boolean isSharedFile(User user, IndexFile indexFile) {
        String userName = indexFile.getPath().substring(1).split("\\\\")[0];
        if (user.getName().equals(userName)) {
            return false;
        }
        return true;
    }

    private Boolean isLegal(User user) {
        IndexFile userPath = FileSystemApplication.userPath.get(user);
        if ("share".equals(userPath.getFileName()) && "\\".equals(userPath.getPath())) {
            System.out.println("The share isn't operational!");
            return false;
        }
        if ("\\".equals(userPath.getPath()) && Util.isStringEmpty(userPath.getFileName())) {
            System.out.println("The root isn't operational!");
            return false;
        }
        return true;
    }

    private List<Integer> findDatFreePos(Integer num) {
        List<Integer> freePos = new ArrayList<>();

        for (int i = 0; i < LINE; i++) {
            for (int j = 0; j < COLUMN; j++) {
                if (!bitMap.getFBlocks()[i][j]) {

                    freePos.add(LINE * i + j);
                    num--;
                    if (num == 0) {
                        return freePos;
                    }
                }
            }
        }
        System.out.println("Space isn't enough.");
        return freePos;
    }

    private IndexFile findDirectory(String message, User user, Integer type) {
        String[] path = message.split(" ")[1].split("\\\\");
        IndexFile userPath;
        if (type == 1) {
            userPath = new IndexFile(null, null, "\\");
        }
        else {
            userPath = FileSystemApplication.userPath.get(user);
        }
        IndexFile indexFile = new IndexFile(userPath.getFSFile(), userPath.getFileName(), userPath.getPath());
        List<IndexFile> childDirectory = null;

        for (int i = 0; i < path.length; i++) {
            if ("..".equals(path[i])) {

                if (!Util.isNull(indexFile.getFSFile())) {
                    if (Util.isNull(indexFile.getFSFile().getParent())) {
                        indexFile.setFSFile(null);
                        indexFile.setFileName(null);
                        indexFile.setPath("\\");
                    }
                    else {
                        indexFile = indexFile.getFSFile().getParent();
                    }
                }
                continue;
            }
            int isRoot = 0;

            if ("\\".equals(indexFile.getPath()) && Util.isNull(indexFile.getFileName())) {
                childDirectory = root;
                isRoot = 1;
            }
            else if (!Util.isNull(indexFile.getFSFile())) {

                if (indexFile.getFSFile().getChildren().size() == 0) {
                    System.out.println(indexFile.getFileName() + " is empty!");
                    return null;
                }
                else {
                    childDirectory = indexFile.getFSFile().getChildren();
                }
            }
            if (!Util.isNull(childDirectory)) {
                boolean isChange = false;
                for (IndexFile child : childDirectory) {
                    if (child.getFileName().equals(path[i])) {
                        if (isRoot == 1) {
                            if (!user.getName().equals(path[i]) && !"share".equals(path[i]) && type == 0) {
                                System.out.println("You haven't permission to visit the file folder!");
                                return null;
                            }
                        }

                        if (child.getFSFile().getIsCatalog()) {
                            indexFile = child;
                            isChange = true;
                            break;
                        }
                        else {
                            if (i != path.length - 1) {
                                System.out.println(child.getFileName() + " is file!");
                                return null;
                            }
                            return child;
                        }
                    }
                }
                if (!isChange) {
                    System.out.println("Not exist " + path[i] + "!");
                    return null;
                }
            }
        }
        return indexFile;
    }

    private void freeRoom(IndexFile indexFile) {
        if (indexFile.getFSFile().getIsCatalog()) {
            for (IndexFile child : indexFile.getFSFile().getChildren()) {
                freeRoom(child);
            }
        }
        indexFile.getFSFile().setParent(null);
        FatBlock fatBlock = indexFile.getFSFile().getFirstBlock();
        int over = 0;

        do {

           bitMap.getFBlocks()[fatBlock.getBlockId() / COLUMN][fatBlock.getBlockId() % COLUMN] = false;
            if (fatBlock.getNextBlockId() == -1) {
                over = 1;
            }
            else {

                Integer next = fatBlock.getNextBlockId();
                fatBlock.setNextBlockId(-1);
                fatBlock = fat.getFatBlocks()[next];
            }
        } while (over == 0);
    }

}
