package com.atom.simHash;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class TextHandler{
    public static String read(String path){
        Path p = Paths.get(path);
        StringBuilder builder = new StringBuilder();
        try(Scanner input = new Scanner(p)){
            while (input.hasNext()){
                builder.append(input.nextLine());
            }
        }catch (IOException e){
            System.out.println("文件读取错误！");
            System.exit(1);
        }
        return builder.toString();
    }

    public static void write(String fileName, String result) throws IOException {
        FileOutputStream stream = new FileOutputStream(fileName, false);
        OutputStreamWriter arrayWriter = new OutputStreamWriter(stream);
        BufferedWriter output = new BufferedWriter(arrayWriter);
        output.write("两篇文章的相似率为：" + result);
        output.close();
    }
}
