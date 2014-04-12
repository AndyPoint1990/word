/**
 * 
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.apdplat.word.vector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 从语料库中抽取文本
 * @author 杨尚川
 */
public class ExtractText {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractText.class);
    private static final AtomicInteger WORD_COUNT = new AtomicInteger();   
    private static final AtomicInteger CHAR_COUNT = new AtomicInteger();    

    public static void main(String[] args){
        String output = "target/word.txt";
        if(args.length == 1){
            output = args[0];
        }
        extractFromCorpus(output);
    }
    /**
     * 从语料库中抽取内容
     */
    private static void extractFromCorpus(String output){
        String zipFile = "src/main/resources/corpus/corpora.zip";        
        LOGGER.info("开始从语料库中抽取文本");
        long start = System.currentTimeMillis();        
        try{
            analyzeCorpus(zipFile, output);
        } catch (IOException ex) {
            LOGGER.info("抽取失败："+ex.getMessage());
        }
        long cost = System.currentTimeMillis() - start;
        LOGGER.info("完成抽取，耗时："+cost+"毫秒");
        LOGGER.info("抽取出的总字符数目为："+CHAR_COUNT.get()+"，总词数目为："+WORD_COUNT.get());
    }
    /**
     * 分析语料库
     * @param zipFile 压缩的语料库
     * @throws IOException 
     */
    private static void analyzeCorpus(String zipFile, String output) throws IOException{
        try (FileSystem fs = FileSystems.newFileSystem(Paths.get(zipFile), ExtractText.class.getClassLoader());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output),"utf-8"));) {
            for(Path path : fs.getRootDirectories()){                
                LOGGER.info("处理目录："+path);
                Files.walkFileTree(path, new SimpleFileVisitor<Path>(){

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        LOGGER.info("处理文件："+file);
                        // 拷贝到本地文件系统
                        Path temp = Paths.get("target/corpus-"+System.currentTimeMillis()+".txt");
                        Files.copy(file, temp, StandardCopyOption.REPLACE_EXISTING);
                        extractText(temp, writer);
                        return FileVisitResult.CONTINUE;
                    }
                    
                });
            }
        }
    }
    /**
     * 从语料库中抽取文本保存到指定文件
     * @param file 语料库
     * @param writer 输出文件
     */
    private static void extractText(Path file, BufferedWriter writer){
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile()),"utf-8"));){
            String line;
            while( (line = reader.readLine()) != null ){
                //去除首尾空白字符
                line = line.trim();
                //忽略空行
                if(!"".equals(line)){
                    //词和词之间以空格隔开
                    String[] words = line.split("  ");
                    if(words == null){
                        //忽略不符合规范的行
                        continue;
                    }
                    for(String word : words){
                        String[] attr = word.split("/");
                        if(attr == null || attr.length < 1){
                            //忽略不符合规范的词
                            continue;
                        }
                        //去掉[和]
                        String item = attr[0].replace("[", "").replace("]", "");
                        writer.write(item+" ");
                        //词数目
                        WORD_COUNT.incrementAndGet();
                        //字符数目
                        CHAR_COUNT.addAndGet(item.length());
                    }
                    //换行
                    writer.write("\n");
                }
            }
        }catch(Exception e){
            LOGGER.info("从语料库 "+file+" 中抽取文本失败："+e.getMessage());
        }        
    }
}
