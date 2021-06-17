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

    public static LinkedList<IndexFCBRow> root = new LinkedList<>();

    public static BitMap bitMap = new BitMap(LINE, COLUMN);
    public static FAT fat = new FAT(LINE, COLUMN);
    public static FileContent fileContent = new FileContent(LINE, COLUMN);

    private static LinkedList<IndexFCBRow> openFile = new LinkedList<>();

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

        IndexFile IndexFile = new IndexFile(index++, "\\", true, true, fat.getFatBlocks()[0], null,
                Util.getCurrentTime(), -1, new LinkedList<>());
        root.add(new IndexFCBRow(IndexFile, "share", "\\"));
    }

    @Override
    public void initDirectory(String directoryName) {

        for (IndexFCBRow indexFCBRow : root) {
            if (indexFCBRow.getFileName().equals(directoryName)) {
                return;
            }
        }

        List<Integer> freePos = findDatFreePos(1);
        if (freePos.size() == 1) {

            Integer place = freePos.get(0);
            bitMap.getFBlocks()[place / COLUMN][place % COLUMN] = true;

            IndexFile IndexFile = new IndexFile(index++, "\\", true, true, fat.getFatBlocks()[freePos.get(0)], null,
                    Util.getCurrentTime(), -1, new LinkedList<>());
            root.add(new IndexFCBRow(IndexFile, directoryName, "\\"));
        }
    }

    @Override
    public void getDirectory(User user) {
        IndexFCBRow indexFCBRow = FileSystemApplication.userPath.get(user);

        System.out.println();
        if ("\\".equals(indexFCBRow.getPath()) && Util.isNull(indexFCBRow.getFileName())) {


            for (IndexFCBRow nowIndexFCBRow : root) {

                if(nowIndexFCBRow.getIndexFile().getIsCatalog()){
                    System.out.println("FileFolder："+ nowIndexFCBRow.getFileName());
                }else{
                    System.out.println("File："+ nowIndexFCBRow.getFileName());
                }
            }
            System.out.println();
            return;
        }

        if (!Util.isNull(indexFCBRow.getIndexFile())) {

            if (indexFCBRow.getIndexFile().getChildren().size() == 0) {
                System.out.println("The current directory is empty!");
            }else {
                Iterator<IndexFCBRow> iterator = indexFCBRow.getIndexFile().getChildren().iterator();
                IndexFCBRow help = null;
                for (IndexFCBRow child : root) {
                    if ("share".equals(child.getFileName())) {
                        help = child;
                        break;
                    }
                }
                while (iterator.hasNext()){
                    IndexFCBRow childrenIndexFCBRow = iterator.next();

                    int isFind = 0;
                    if (!Util.isNull(childrenIndexFCBRow.getIndexFile())) {
                        if (childrenIndexFCBRow.getIndexFile().getIsCatalog() ||
                                (!childrenIndexFCBRow.getIndexFile().getIsCatalog() && !childrenIndexFCBRow.getIndexFile().getIsPublic())) {
                            isFind = 1;
                        }
                    }
                    if (isFind == 0){
                        for (IndexFCBRow remove : help.getIndexFile().getChildren()) {
                            if (remove.getFileName().equals(childrenIndexFCBRow.getFileName()) && remove.getPath().equals(childrenIndexFCBRow.getPath())) {
                                isFind = 1;
                                break;
                            }
                        }
                    }
                    if (isFind == 0) {
                        iterator.remove();
                    }
                }
                if (indexFCBRow.getIndexFile().getChildren().size() == 0) {
                    System.out.println("The filefolder is empty!");
                    return;
                }

                for (IndexFCBRow childrenIndexFCBRow : indexFCBRow.getIndexFile().getChildren()) {

                    if(indexFCBRow.getFileName().equals("share")){
                        System.out.println("File："+ childrenIndexFCBRow.getFileName());
                    }else if(childrenIndexFCBRow.getIndexFile()!=null){
                        if(childrenIndexFCBRow.getIndexFile().getIsCatalog()){
                            System.out.println("FileFolder："+ childrenIndexFCBRow.getFileName());
                        }else{
                            System.out.println("File："+ childrenIndexFCBRow.getFileName());
                        }
                    }
                }
            }

        }
        System.out.println();
    }

    @Override
    public void  changeDirectory(String message, User user) {

        IndexFCBRow indexFCBRow = findDirectory(message, user, 0);
        if (Util.isNull(indexFCBRow)) {
            return;
        }
        if (!Util.isNull(indexFCBRow.getIndexFile()) && !indexFCBRow.getIndexFile().getIsCatalog()) {
            System.out.println(indexFCBRow.getFileName() + " is file");
            return;
        }

        FileSystemApplication.userPath.remove(user);
        FileSystemApplication.userPath.put(user, indexFCBRow);
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

        IndexFCBRow indexFCBRow = findDirectory(message, user, 0);
        if (!Util.isNull(indexFCBRow)) {
            if (indexFCBRow.getIndexFile().getIsCatalog()) {
                System.out.println(indexFCBRow.getFileName() + " is directory");
            }else {
                if (indexFCBRow.getIndexFile().getStatus() != 2) {
                    indexFCBRow.getIndexFile().setStatus(1);

                    if (!isSharedFile(user, indexFCBRow)) {

                        FileSystemApplication.userPath.remove(user);
                        FileSystemApplication.userPath.put(user, indexFCBRow.getIndexFile().getParent());
                    }
                    openFile.add(indexFCBRow);
                    System.out.println("Open " + indexFCBRow.getFileName() + " Successfully!");
                }else {
                    System.out.println("Failed to open " + indexFCBRow.getFileName() + ", it's written by other user!");
                }
            }
        }
    }

    @Override
    public void closeFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }
        IndexFCBRow indexFCBRow = findOpenFile(message, user);
        if (!Util.isNull(indexFCBRow)) {
            if (indexFCBRow.getIndexFile().getIsCatalog()) {
                System.out.println(indexFCBRow.getFileName() + " is directory!");
            }
            else {
                if (indexFCBRow.getIndexFile().getStatus() != 2) {
                    indexFCBRow.getIndexFile().setStatus(0);
                    openFile.remove(indexFCBRow);
                    System.out.println("Close " + indexFCBRow.getFileName() + " Successfully!");
                }else {
                    System.out.println("Failed to close " + indexFCBRow.getFileName() + ", it's written by other user!");
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

        IndexFCBRow indexFCBRow = findOpenFile(message, user);
        if (!Util.isNull(indexFCBRow)) {
            if (indexFCBRow.getIndexFile().getIsCatalog()) {
                System.out.println(indexFCBRow.getFileName() + " is directory!");
            }
            else {
                switch (indexFCBRow.getIndexFile().getStatus()) {
                    case 0:
                        System.out.println("Please open file first!");
                        break;
                    case 1:
                        printFileContent(indexFCBRow);
                        break;
                    case 2:
                        System.out.println("Failed to read " + indexFCBRow.getFileName() + ", it's written by other user!");
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

        IndexFCBRow indexFCBRow = findOpenFile(message, user);
        if (!Util.isNull(indexFCBRow)) {
            if (indexFCBRow.getIndexFile().getIsCatalog()) {
                System.out.println(indexFCBRow.getFileName() + "is directory!");
            }
            else {
                switch (indexFCBRow.getIndexFile().getStatus()) {
                    case 0:
                        System.out.println("Please open the file firstly!");
                        break;
                    case 1:
                        indexFCBRow.getIndexFile().setStatus(2);
                        writeFile(indexFCBRow);
                        printFileContent(indexFCBRow);
                        break;
                    case 2:
                        System.out.println("Fail to read " + indexFCBRow.getFileName() + ", it's written by other user!");
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

        IndexFCBRow indexFCBRow = findDirectory(message, user, 0);
        if (Util.isNull(indexFCBRow)) {
            return;
        }

        if (Util.isNull(indexFCBRow.getIndexFile().getParent())) {
            System.out.println("You can't delete the root directory");
            return;
        }

        if (!user.getName().equals(indexFCBRow.getPath().substring(1).split("\\\\")[0])) {
            System.out.println("You haven't permission to delete this file");
            return;
        }

        if (FileSystemApplication.userPath.get(user).getPath().equals(indexFCBRow.getPath()) &&
                FileSystemApplication.userPath.get(user).getFileName().equals(indexFCBRow.getFileName())) {

            FileSystemApplication.userPath.remove(user);
            FileSystemApplication.userPath.put(user, indexFCBRow.getIndexFile().getParent());
        }
        indexFCBRow.getIndexFile().getParent().getIndexFile().getChildren().remove(indexFCBRow);

        if (indexFCBRow.getIndexFile().getIsPublic()) {
            IndexFCBRow help = null;
            for (IndexFCBRow child : root) {
                if ("share".equals(child.getFileName())) {
                    help = child;
                    break;
                }
            }
            for (IndexFCBRow remove : help.getIndexFile().getChildren()) {
                if (remove.getFileName().equals(indexFCBRow.getFileName())) {
                    help.getIndexFile().getChildren().remove(remove);
                    break;
                }
            }
        }

        freeRoom(indexFCBRow);
    }

    @Override
    public void linkFile(String message, User user) {
        if (!isLegal(user)) {
            return;
        }

        String fileName = message.split(" ")[1];
        IndexFCBRow help = null;
        for (IndexFCBRow child : root) {
            if ("share".equals(child.getFileName())) {
                help = child;
                break;
            }
        }
        IndexFCBRow indexFCBRow = null;
        if (!Util.isNull(help) && !Util.isNull(help.getIndexFile())) {
            if (help.getIndexFile().getChildren().size() == 0) {
                System.out.println("There are no files!");
                return;
            }
            int isFind = 0;
            for (IndexFCBRow file : help.getIndexFile().getChildren()) {
                if (file.getFileName().equals(fileName)) {
                    indexFCBRow = file;
                    isFind = 1;
                    break;
                }
            }
            if (isFind == 0) {
                System.out.println("Can't find the file" + fileName);
                return;
            }
        }
        if (!Util.isNull(indexFCBRow)) {
            //找到父目录
            indexFCBRow = findDirectory("cd " + indexFCBRow.getPath().substring(1), user, 1);
            IndexFCBRow userPath = FileSystemApplication.userPath.get(user);
            if (!Util.isNull(indexFCBRow) && !Util.isNull(indexFCBRow.getIndexFile())) {
                if (indexFCBRow.getIndexFile().getChildren().size() == 0) {
                    System.out.println("The file had been deleted!");
                    return;
                }
                int isFind = 0;
                for (IndexFCBRow file : indexFCBRow.getIndexFile().getChildren()) {
                    if (file.getFileName().equals(fileName)) {
                        indexFCBRow = file;
                        isFind = 1;
                        break;
                    }
                }
                if (isFind == 0) {
                    System.out.println("The file had been deleted!");
                    return;
                }
            }
            userPath.getIndexFile().getChildren().add(indexFCBRow);
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

    private void writeFile(IndexFCBRow indexFCBRow) {

        printFileContent(indexFCBRow);

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


        FatBlock fatBlock = indexFCBRow.getIndexFile().getFirstBlock();
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

        indexFCBRow.getIndexFile().setModifyTime(Util.getCurrentTime());
        indexFCBRow.getIndexFile().setStatus(1);
    }

    private void printFileContent(IndexFCBRow indexFCBRow) {
        System.out.println(indexFCBRow.getFileName() + " content：");

        FatBlock fatBlock = indexFCBRow.getIndexFile().getFirstBlock();
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
        IndexFCBRow userPath = FileSystemApplication.userPath.get(user);
        IndexFCBRow indexFCBRow = new IndexFCBRow(userPath.getIndexFile(), userPath.getFileName(), userPath.getPath());

        List<IndexFCBRow> childDirectory = null;

        for (int i = 0; i < path.length; i++) {

            if ("..".equals(path[i])) {

                if (!Util.isNull(indexFCBRow.getIndexFile())){

                    if (Util.isNull(indexFCBRow.getIndexFile().getParent())) {
                        indexFCBRow.setIndexFile(null);
                        indexFCBRow.setFileName(null);
                        indexFCBRow.setPath("\\");
                    }else {
                        indexFCBRow = indexFCBRow.getIndexFile().getParent();
                    }

                }
                continue;
            }


            if ("\\".equals(indexFCBRow.getPath()) && Util.isNull(indexFCBRow.getFileName())) {
                childDirectory = root;
            }else if (!Util.isNull(indexFCBRow.getIndexFile())) {

                if (indexFCBRow.getIndexFile().getChildren().size() == 0) {
                    IndexFCBRow newDirectory = null;

                    if (i == path.length - 1) {

                        if ("/".equals(indexFCBRow.getPath()) && Util.isNull(indexFCBRow.getFileName())) {
                            System.out.println("Can't create files in the root!");
                            return;
                        }

                        if ("\\".equals(indexFCBRow.getPath()) && "share".equals(indexFCBRow.getFileName())) {
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


                    if ("\\".equals(indexFCBRow.getPath())) {
                        newDirectory.setPath(indexFCBRow.getPath() + indexFCBRow.getFileName());
                        newDirectory.getIndexFile().setPath(indexFCBRow.getPath() + indexFCBRow.getFileName());
                    }else {
                        newDirectory.setPath(indexFCBRow.getPath() + "\\" + indexFCBRow.getFileName());
                        newDirectory.getIndexFile().setPath(indexFCBRow.getPath() + "\\" + indexFCBRow.getFileName());
                    }
                    newDirectory.getIndexFile().setParent(indexFCBRow);
                    indexFCBRow.getIndexFile().getChildren().add(newDirectory);




                    if (type != 0 || i != path.length - 1) {
                        indexFCBRow = newDirectory;
                    }

                    if (!newDirectory.getIndexFile().getIsCatalog() && newDirectory.getIndexFile().getIsPublic()) {
                        for (IndexFCBRow root : root) {
                            if ("share".equals(root.getFileName())) {
                                root.getIndexFile().getChildren().add(new IndexFCBRow(null, newDirectory.getFileName(), newDirectory.getPath()));
                            }
                        }
                    }
                    continue;
                }else {
                    childDirectory = indexFCBRow.getIndexFile().getChildren();
                }


            }



            if (!Util.isNull(childDirectory)) {
                boolean isChange = false;
                for (IndexFCBRow child : childDirectory) {
                    if (child.getFileName().equals(path[i])) {
                        if (i == path.length - 1) {

                            System.out.println(child.getFileName() + " has existed!");
                            return;
                        }

                        indexFCBRow = child;
                        isChange = true;
                        break;
                    }
                }


                if (!isChange) {

                    IndexFCBRow newDirectory = null;

                    if (i == path.length - 1) {

                        if ("\\".equals(indexFCBRow.getPath()) && Util.isNull(indexFCBRow.getFileName())) {
                            System.out.println("Can't create files in the root!");
                            return;
                        }

                        if ("\\".equals(indexFCBRow.getPath()) && "share".equals(indexFCBRow.getFileName())) {
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


                    if ("\\".equals(indexFCBRow.getPath())) {
                        newDirectory.setPath(indexFCBRow.getPath() + indexFCBRow.getFileName());
                        newDirectory.getIndexFile().setPath(indexFCBRow.getPath() + indexFCBRow.getFileName());
                    } else {
                        newDirectory.setPath(indexFCBRow.getPath() + "\\" + indexFCBRow.getFileName());
                        newDirectory.getIndexFile().setPath(indexFCBRow.getPath() + "\\" + indexFCBRow.getFileName());
                    }
                    newDirectory.getIndexFile().setParent(indexFCBRow);

                    indexFCBRow.getIndexFile().getChildren().add(newDirectory);



                    if (newDirectory.getIndexFile().getIsCatalog() || (i == path.length - 1 && newDirectory.getIndexFile().getIsCatalog())) {
                        indexFCBRow = newDirectory;
                    }



                    if (!newDirectory.getIndexFile().getIsCatalog() && newDirectory.getIndexFile().getIsPublic()) {
                        for (IndexFCBRow root : root) {
                            root.getIndexFile().getChildren().add(new IndexFCBRow(null, newDirectory.getFileName(), newDirectory.getPath()));
                        }
                    }
                }
            }
        }

        FileSystemApplication.userPath.remove(user);
        FileSystemApplication.userPath.put(user, indexFCBRow);
    }

    private IndexFCBRow newDirectory(String fileName, Integer type) {

        List<Integer> freeLoc = findDatFreePos(1);
        if (freeLoc.size() != 0) {

            Integer place = freeLoc.get(0);
            bitMap.getFBlocks()[place / COLUMN][place % COLUMN] = true;
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
            return new IndexFCBRow(IndexFile, fileName, null);
        }
        System.out.println("Space isn't enough");
        return null;
    }


    private IndexFCBRow findOpenFile(String message, User user) {
        IndexFCBRow userPath = FileSystemApplication.userPath.get(user);
        String filePath = message.split(" ")[1];
        for (IndexFCBRow indexFCBRow : openFile) {
            String[] fileName = filePath.split("\\\\");
            if (fileName[fileName.length - 1].equals(indexFCBRow.getFileName())) {

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
                if (path.equals(indexFCBRow.getPath())) {
                    return indexFCBRow;
                }
                if (!indexFCBRow.getPath().substring(1).split("\\\\")[0].equals(user.getName())) {
                    return indexFCBRow;
                }
            }
        }
        return null;
    }

    private Boolean isSharedFile(User user, IndexFCBRow indexFCBRow) {
        String userName = indexFCBRow.getPath().substring(1).split("\\\\")[0];
        if (user.getName().equals(userName)) {
            return false;
        }
        return true;
    }

    private Boolean isLegal(User user) {
        IndexFCBRow userPath = FileSystemApplication.userPath.get(user);
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

    private IndexFCBRow findDirectory(String message, User user, Integer type) {
        String[] path = message.split(" ")[1].split("\\\\");
        IndexFCBRow userPath;
        if (type == 1) {
            userPath = new IndexFCBRow(null, null, "\\");
        }
        else {
            userPath = FileSystemApplication.userPath.get(user);
        }
        IndexFCBRow indexFCBRow = new IndexFCBRow(userPath.getIndexFile(), userPath.getFileName(), userPath.getPath());
        List<IndexFCBRow> childDirectory = null;

        for (int i = 0; i < path.length; i++) {
            if ("..".equals(path[i])) {

                if (!Util.isNull(indexFCBRow.getIndexFile())) {
                    if (Util.isNull(indexFCBRow.getIndexFile().getParent())) {
                        indexFCBRow.setIndexFile(null);
                        indexFCBRow.setFileName(null);
                        indexFCBRow.setPath("\\");
                    }
                    else {
                        indexFCBRow = indexFCBRow.getIndexFile().getParent();
                    }
                }
                continue;
            }
            int isRoot = 0;

            if ("\\".equals(indexFCBRow.getPath()) && Util.isNull(indexFCBRow.getFileName())) {
                childDirectory = root;
                isRoot = 1;
            }
            else if (!Util.isNull(indexFCBRow.getIndexFile())) {

                if (indexFCBRow.getIndexFile().getChildren().size() == 0) {
                    System.out.println(indexFCBRow.getFileName() + " is empty!");
                    return null;
                }
                else {
                    childDirectory = indexFCBRow.getIndexFile().getChildren();
                }
            }
            if (!Util.isNull(childDirectory)) {
                boolean isChange = false;
                for (IndexFCBRow child : childDirectory) {
                    if (child.getFileName().equals(path[i])) {
                        if (isRoot == 1) {
                            if (!user.getName().equals(path[i]) && !"share".equals(path[i]) && type == 0) {
                                System.out.println("You haven't permission to visit the file folder!");
                                return null;
                            }
                        }

                        if (child.getIndexFile().getIsCatalog()) {
                            indexFCBRow = child;
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
        return indexFCBRow;
    }

    private void freeRoom(IndexFCBRow indexFCBRow) {
        if (indexFCBRow.getIndexFile().getIsCatalog()) {
            for (IndexFCBRow child : indexFCBRow.getIndexFile().getChildren()) {
                freeRoom(child);
            }
        }
        indexFCBRow.getIndexFile().setParent(null);
        FatBlock fatBlock = indexFCBRow.getIndexFile().getFirstBlock();
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
