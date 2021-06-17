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

    public static LinkedList<FCB> root = new LinkedList<>();

    public static BitMap bitMap = new BitMap(LINE, COLUMN);
    public static FAT fat = new FAT(LINE, COLUMN);
    public static FileContent fileContent = new FileContent(LINE, COLUMN);

    private static LinkedList<FCB> openFile = new LinkedList<>();

    static {
        //初始化文件状态
        for (int i = 0; i < LINE; i++) {
            for (int j = 0; j < COLUMN; j++) {
                bitMap.getIsUse()[i][j] = false;
                fileContent.getContent()[i][j] = null;
            }
        }

        for (int i = 0; i < LINE * COLUMN; i++) {
            fat.getFatBlocks()[i] =  new FatBlock(i,-1);
        }

        bitMap.getIsUse()[0][0] = true;

        IndexFile IndexFile = new IndexFile(index++, "\\", true, true, fat.getFatBlocks()[0], null,
                Util.getCurrentTime(), -1, new LinkedList<>());
        root.add(new FCB(IndexFile, "share", "\\"));
    }

    @Override
    public void initDirectory(String directoryName) {

        for (FCB FCB : root) {
            if (FCB.getFileName().equals(directoryName)) {
                return;
            }
        }

        List<Integer> freePos = findDatFreePos(1);
        if (freePos.size() == 1) {

            Integer place = freePos.get(0);
            bitMap.getIsUse()[place / COLUMN][place % COLUMN] = true;

            IndexFile IndexFile = new IndexFile(index++, "\\", true, true, fat.getFatBlocks()[freePos.get(0)], null,
                    Util.getCurrentTime(), -1, new LinkedList<>());
            root.add(new FCB(IndexFile, directoryName, "\\"));
        }
    }

    @Override
    public void getDirectory(User user) {
        FCB FCB = FileSystemApplication.userPath.get(user);

        System.out.println();
        if ("\\".equals(FCB.getPath()) && Util.isNull(FCB.getFileName())) {


            for (FCB nowFCB : root) {

                if(nowFCB.getIndexFile().getIsCatalog()){
                    System.out.println("FileFolder："+ nowFCB.getFileName());
                }else{
                    System.out.println("File："+ nowFCB.getFileName());
                }
            }
            System.out.println();
            return;
        }

        if (!Util.isNull(FCB.getIndexFile())) {

            if (FCB.getIndexFile().getChildren().size() == 0) {
                System.out.println("The current directory is empty!");
            }else {
                Iterator<FCB> iterator = FCB.getIndexFile().getChildren().iterator();
                FCB help = null;
                for (FCB child : root) {
                    if ("share".equals(child.getFileName())) {
                        help = child;
                        break;
                    }
                }
                while (iterator.hasNext()){
                    FCB childrenFCB = iterator.next();

                    int isFind = 0;
                    if (!Util.isNull(childrenFCB.getIndexFile())) {
                        if (childrenFCB.getIndexFile().getIsCatalog() ||
                                (!childrenFCB.getIndexFile().getIsCatalog() && !childrenFCB.getIndexFile().getIsPublic())) {
                            isFind = 1;
                        }
                    }
                    if (isFind == 0){
                        for (FCB remove : help.getIndexFile().getChildren()) {
                            if (remove.getFileName().equals(childrenFCB.getFileName()) && remove.getPath().equals(childrenFCB.getPath())) {
                                isFind = 1;
                                break;
                            }
                        }
                    }
                    if (isFind == 0) {
                        iterator.remove();
                    }
                }
                if (FCB.getIndexFile().getChildren().size() == 0) {
                    System.out.println("The filefolder is empty!");
                    return;
                }

                for (FCB childrenFCB : FCB.getIndexFile().getChildren()) {

                    if(FCB.getFileName().equals("share")){
                        System.out.println("File："+ childrenFCB.getFileName());
                    }else if(childrenFCB.getIndexFile()!=null){
                        if(childrenFCB.getIndexFile().getIsCatalog()){
                            System.out.println("FileFolder："+ childrenFCB.getFileName());
                        }else{
                            System.out.println("File："+ childrenFCB.getFileName());
                        }
                    }
                }
            }

        }
        System.out.println();
    }

    @Override
    public void  changeDirectory(String message, User user) {

        FCB FCB = findDirectory(message, user, 0);
        if (Util.isNull(FCB)) {
            return;
        }
        if (!Util.isNull(FCB.getIndexFile()) && !FCB.getIndexFile().getIsCatalog()) {
            System.out.println(FCB.getFileName() + " is file");
            return;
        }

        FileSystemApplication.userPath.remove(user);
        FileSystemApplication.userPath.put(user, FCB);
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

        FCB FCB = findDirectory(message, user, 0);
        if (!Util.isNull(FCB)) {
            if (FCB.getIndexFile().getIsCatalog()) {
                System.out.println(FCB.getFileName() + " is directory");
            }else {
                if (FCB.getIndexFile().getStatus() != 2) {
                    FCB.getIndexFile().setStatus(1);

                    if (!isSharedFile(user, FCB)) {

                        FileSystemApplication.userPath.remove(user);
                        FileSystemApplication.userPath.put(user, FCB.getIndexFile().getParent());
                    }
                    openFile.add(FCB);
                    System.out.println("Open " + FCB.getFileName() + " Successfully!");
                }else {
                    System.out.println("Failed to open " + FCB.getFileName() + ", it's written by other user!");
                }
            }
        }
    }

    @Override
    public void closeFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        FCB FCB = findOpenFile(message, user);
        if (!Util.isNull(FCB)) {
            if (FCB.getIndexFile().getIsCatalog()) {
                System.out.println(FCB.getFileName() + " is directory!");
            }
            else {
                if (FCB.getIndexFile().getStatus() != 2) {
                    FCB.getIndexFile().setStatus(0);
                    openFile.remove(FCB);
                    System.out.println("Close " + FCB.getFileName() + " Successfully!");
                }else {
                    System.out.println("Failed to close " + FCB.getFileName() + ", it's written by other user!");
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

        FCB FCB = findOpenFile(message, user);
        if (!Util.isNull(FCB)) {
            if (FCB.getIndexFile().getIsCatalog()) {
                System.out.println(FCB.getFileName() + " is directory!");
            }
            else {
                switch (FCB.getIndexFile().getStatus()) {
                    case 0:
                        System.out.println("Please open file first!");
                        break;
                    case 1:
                        printFileContent(FCB);
                        break;
                    case 2:
                        System.out.println("Failed to read " + FCB.getFileName() + ", it's written by other user!");
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

        FCB FCB = findOpenFile(message, user);
        if (!Util.isNull(FCB)) {
            if (FCB.getIndexFile().getIsCatalog()) {
                System.out.println(FCB.getFileName() + "is directory!");
            }
            else {
                switch (FCB.getIndexFile().getStatus()) {
                    case 0:
                        System.out.println("Please open the file firstly!");
                        break;
                    case 1:
                        FCB.getIndexFile().setStatus(2);
                        writeFile(FCB);
                        printFileContent(FCB);
                        break;
                    case 2:
                        System.out.println("Fail to read " + FCB.getFileName() + ", it's written by other user!");
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

        FCB FCB = findDirectory(message, user, 0);
        if (Util.isNull(FCB)) {
            return;
        }

        if (Util.isNull(FCB.getIndexFile().getParent())) {
            System.out.println("You can't delete the root directory");
            return;
        }

        if (!user.getName().equals(FCB.getPath().substring(1).split("\\\\")[0])) {
            System.out.println("You haven't permission to delete this file");
            return;
        }

        if (FileSystemApplication.userPath.get(user).getPath().equals(FCB.getPath()) &&
                FileSystemApplication.userPath.get(user).getFileName().equals(FCB.getFileName())) {

            FileSystemApplication.userPath.remove(user);
            FileSystemApplication.userPath.put(user, FCB.getIndexFile().getParent());
        }
        FCB.getIndexFile().getParent().getIndexFile().getChildren().remove(FCB);

        if (FCB.getIndexFile().getIsPublic()) {
            FCB help = null;
            for (FCB child : root) {
                if ("share".equals(child.getFileName())) {
                    help = child;
                    break;
                }
            }
            for (FCB remove : help.getIndexFile().getChildren()) {
                if (remove.getFileName().equals(FCB.getFileName())) {
                    help.getIndexFile().getChildren().remove(remove);
                    break;
                }
            }
        }

        freeRoom(FCB);
    }

    @Override
    public void linkFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }

        String fileName = message.split(" ")[1];
        FCB help = null;
        for (FCB child : root) {
            if ("share".equals(child.getFileName())) {
                help = child;
                break;
            }
        }
        FCB FCB = null;
        if (!Util.isNull(help) && !Util.isNull(help.getIndexFile())) {
            if (help.getIndexFile().getChildren().size() == 0) {
                System.out.println("There are no files!");
                return;
            }
            int isFind = 0;
            for (FCB file : help.getIndexFile().getChildren()) {
                if (file.getFileName().equals(fileName)) {
                    FCB = file;
                    isFind = 1;
                    break;
                }
            }
            if (isFind == 0) {
                System.out.println("Can't find the file" + fileName);
                return;
            }
        }
        if (!Util.isNull(FCB)) {
            //找到父目录
            FCB = findDirectory("cd " + FCB.getPath().substring(1), user, 1);
            FCB userPath = FileSystemApplication.userPath.get(user);
            if (!Util.isNull(FCB) && !Util.isNull(FCB.getIndexFile())) {
                if (FCB.getIndexFile().getChildren().size() == 0) {
                    System.out.println("The file had been deleted!");
                    return;
                }
                int isFind = 0;
                for (FCB file : FCB.getIndexFile().getChildren()) {
                    if (file.getFileName().equals(fileName)) {
                        FCB = file;
                        isFind = 1;
                        break;
                    }
                }
                if (isFind == 0) {
                    System.out.println("The file had been deleted!");
                    return;
                }
            }
            userPath.getIndexFile().getChildren().add(FCB);
        }
    }

    @Override
    public void showBitMap() {
        System.out.println();
        for(int i=0;i<COLUMN;i++){
            for(int j=0;j<LINE;j++){
                if(bitMap.getIsUse()[i][j]){
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

    private void writeFile(FCB FCB) {

        printFileContent(FCB);

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


        FatBlock fatBlock = FCB.getIndexFile().getFirstBlock();
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
                    bitMap.getIsUse()[place / COLUMN][place % COLUMN] = true;
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

        FCB.getIndexFile().setModifyTime(Util.getCurrentTime());
        FCB.getIndexFile().setStatus(1);
    }

    private void printFileContent(FCB FCB) {
        System.out.println(FCB.getFileName() + " content：");

        FatBlock fatBlock = FCB.getIndexFile().getFirstBlock();
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
        FCB userPath = FileSystemApplication.userPath.get(user);
        FCB FCB = new FCB(userPath.getIndexFile(), userPath.getFileName(), userPath.getPath());

        List<FCB> childDirectory = null;

        for (int i = 0; i < path.length; i++) {

            if ("..".equals(path[i])) {

                if (!Util.isNull(FCB.getIndexFile())){

                    if (Util.isNull(FCB.getIndexFile().getParent())) {
                        FCB.setIndexFile(null);
                        FCB.setFileName(null);
                        FCB.setPath("\\");
                    }else {
                        FCB = FCB.getIndexFile().getParent();
                    }

                }
                continue;
            }


            if ("\\".equals(FCB.getPath()) && Util.isNull(FCB.getFileName())) {
                childDirectory = root;
            }else if (!Util.isNull(FCB.getIndexFile())) {

                if (FCB.getIndexFile().getChildren().size() == 0) {
                    FCB newDirectory = null;

                    if (i == path.length - 1) {

                        if ("/".equals(FCB.getPath()) && Util.isNull(FCB.getFileName())) {
                            System.out.println("Can't create files in the root!");
                            return;
                        }

                        if ("\\".equals(FCB.getPath()) && "share".equals(FCB.getFileName())) {
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


                    if ("\\".equals(FCB.getPath())) {
                        newDirectory.setPath(FCB.getPath() + FCB.getFileName());
                        newDirectory.getIndexFile().setPath(FCB.getPath() + FCB.getFileName());
                    }else {
                        newDirectory.setPath(FCB.getPath() + "\\" + FCB.getFileName());
                        newDirectory.getIndexFile().setPath(FCB.getPath() + "\\" + FCB.getFileName());
                    }
                    newDirectory.getIndexFile().setParent(FCB);
                    FCB.getIndexFile().getChildren().add(newDirectory);




                    if (type != 0 || i != path.length - 1) {
                        FCB = newDirectory;
                    }

                    if (!newDirectory.getIndexFile().getIsCatalog() && newDirectory.getIndexFile().getIsPublic()) {
                        for (FCB root : root) {
                            if ("share".equals(root.getFileName())) {
                                root.getIndexFile().getChildren().add(new FCB(null, newDirectory.getFileName(), newDirectory.getPath()));
                            }
                        }
                    }
                    continue;
                }else {
                    childDirectory = FCB.getIndexFile().getChildren();
                }


            }



            if (!Util.isNull(childDirectory)) {
                boolean isChange = false;
                for (FCB child : childDirectory) {
                    if (child.getFileName().equals(path[i])) {
                        if (i == path.length - 1) {

                            System.out.println(child.getFileName() + " has existed!");
                            return;
                        }

                        FCB = child;
                        isChange = true;
                        break;
                    }
                }


                if (!isChange) {

                    FCB newDirectory = null;

                    if (i == path.length - 1) {

                        if ("\\".equals(FCB.getPath()) && Util.isNull(FCB.getFileName())) {
                            System.out.println("Can't create files in the root!");
                            return;
                        }

                        if ("\\".equals(FCB.getPath()) && "share".equals(FCB.getFileName())) {
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


                    if ("\\".equals(FCB.getPath())) {
                        newDirectory.setPath(FCB.getPath() + FCB.getFileName());
                        newDirectory.getIndexFile().setPath(FCB.getPath() + FCB.getFileName());
                    } else {
                        newDirectory.setPath(FCB.getPath() + "\\" + FCB.getFileName());
                        newDirectory.getIndexFile().setPath(FCB.getPath() + "\\" + FCB.getFileName());
                    }
                    newDirectory.getIndexFile().setParent(FCB);

                    FCB.getIndexFile().getChildren().add(newDirectory);



                    if (newDirectory.getIndexFile().getIsCatalog() || (i == path.length - 1 && newDirectory.getIndexFile().getIsCatalog())) {
                        FCB = newDirectory;
                    }



                    if (!newDirectory.getIndexFile().getIsCatalog() && newDirectory.getIndexFile().getIsPublic()) {
                        for (FCB root : root) {
                            root.getIndexFile().getChildren().add(new FCB(null, newDirectory.getFileName(), newDirectory.getPath()));
                        }
                    }
                }
            }
        }

        FileSystemApplication.userPath.remove(user);
        FileSystemApplication.userPath.put(user, FCB);
    }

    private FCB newDirectory(String fileName, Integer type) {

        List<Integer> freeLoc = findDatFreePos(1);
        if (freeLoc.size() != 0) {

            Integer place = freeLoc.get(0);
            bitMap.getIsUse()[place / COLUMN][place % COLUMN] = true;
            IndexFile IndexFile = null;
            if (type == 1) {

                IndexFile = new IndexFile(index++, null, true, true, fat.getFatBlocks()[freeLoc.get(0)], null,
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

                IndexFile = new IndexFile(index++, null, false, b, fat.getFatBlocks()[freeLoc.get(0)], null,
                        Util.getCurrentTime(), 0, null);
            }
            return new FCB(IndexFile, fileName, null);
        }
        System.out.println("Space isn't enough");
        return null;
    }


    private FCB findOpenFile(String message, User user) {
        FCB userPath = FileSystemApplication.userPath.get(user);
        String filePath = message.split(" ")[1];
        for (FCB FCB : openFile) {
            String[] fileName = filePath.split("\\\\");
            if (fileName[fileName.length - 1].equals(FCB.getFileName())) {

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
                if (path.equals(FCB.getPath())) {
                    return FCB;
                }
                if (!FCB.getPath().substring(1).split("\\\\")[0].equals(user.getName())) {
                    return FCB;
                }
            }
        }
        return null;
    }

    private Boolean isSharedFile(User user, FCB FCB) {
        String userName = FCB.getPath().substring(1).split("\\\\")[0];
        if (user.getName().equals(userName)) {
            return false;
        }
        return true;
    }

    private Boolean isLegal(User user) {
        FCB userPath = FileSystemApplication.userPath.get(user);
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
                if (!bitMap.getIsUse()[i][j]) {

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

    private FCB findDirectory(String message, User user, Integer type) {
        String[] path = message.split(" ")[1].split("\\\\");
        FCB userPath;
        if (type == 1) {
            userPath = new FCB(null, null, "\\");
        }
        else {
            userPath = FileSystemApplication.userPath.get(user);
        }
        FCB FCB = new FCB(userPath.getIndexFile(), userPath.getFileName(), userPath.getPath());
        List<FCB> childDirectory = null;

        for (int i = 0; i < path.length; i++) {
            if ("..".equals(path[i])) {

                if (!Util.isNull(FCB.getIndexFile())) {
                    if (Util.isNull(FCB.getIndexFile().getParent())) {
                        FCB.setIndexFile(null);
                        FCB.setFileName(null);
                        FCB.setPath("\\");
                    }
                    else {
                        FCB = FCB.getIndexFile().getParent();
                    }
                }
                continue;
            }
            int isRoot = 0;

            if ("\\".equals(FCB.getPath()) && Util.isNull(FCB.getFileName())) {
                childDirectory = root;
                isRoot = 1;
            }
            else if (!Util.isNull(FCB.getIndexFile())) {

                if (FCB.getIndexFile().getChildren().size() == 0) {
                    System.out.println(FCB.getFileName() + " is empty!");
                    return null;
                }
                else {
                    childDirectory = FCB.getIndexFile().getChildren();
                }
            }
            if (!Util.isNull(childDirectory)) {
                boolean isChange = false;
                for (FCB child : childDirectory) {
                    if (child.getFileName().equals(path[i])) {
                        if (isRoot == 1) {
                            if (!user.getName().equals(path[i]) && !"share".equals(path[i]) && type == 0) {
                                System.out.println("You haven't permission to visit the file folder!");
                                return null;
                            }
                        }

                        if (child.getIndexFile().getIsCatalog()) {
                            FCB = child;
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
        return FCB;
    }

    private void freeRoom(FCB FCB) {
        if (FCB.getIndexFile().getIsCatalog()) {
            for (FCB child : FCB.getIndexFile().getChildren()) {
                freeRoom(child);
            }
        }
        FCB.getIndexFile().setParent(null);
        FatBlock fatBlock = FCB.getIndexFile().getFirstBlock();
        int over = 0;

        do {

           bitMap.getIsUse()[fatBlock.getBlockId() / COLUMN][fatBlock.getBlockId() % COLUMN] = false;
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
