package com.atom.simHash;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class MySimHash {
    private String tokens; //字符串
    private BigInteger strSimHash;//字符产的hash值
    private int hashbits = 64; // 分词后的hash数;


    public MySimHash(String tokens) {
        this.tokens = tokens;
        this.strSimHash = this.simHash();
    }

    /**
     * 这个是对整个字符串进行hash计算
     */
    private BigInteger simHash() {
        int[] v = new int[this.hashbits];

        List<Term> termList = StandardTokenizer.segment(this.tokens); // 对字符串进行分词

        //对分词的一些特殊处理 : 比如: 根据词性添加权重 , 过滤掉标点符号 , 过滤超频词汇等;
        Map<String, Integer> weightOfNature = new HashMap<String, Integer>(); // 词性的权重
        weightOfNature.put("n", 2); //给名词的权重是2;
        Map<String, String> stopNatures = new HashMap<String, String>();//停用的词性 如一些标点符号之类的;
        stopNatures.put("w", ""); //
        int overCount = 5; //设定超频词汇的界限 ;
        Map<String, Integer> wordCount = new HashMap<String, Integer>();

        for (Term term : termList) {
            String word = term.word; //分词字符串
            String nature = term.nature.toString(); // 分词属性;

            //  过滤超频词
            if (wordCount.containsKey(word)) {
                int count = wordCount.get(word);

                if (count > overCount) {
                    continue;
                }

                wordCount.put(word, count + 1);
            } else {
                wordCount.put(word, 1);
            }

            // 过滤停用词性
            if (stopNatures.containsKey(nature)) {
                continue;
            }

            // 2、将每一个分词hash为一组固定长度的数列.比如 64bit 的一个整数.
            BigInteger t = this.hash(word);
            for (int i = 0; i < this.hashbits; i++) {
                BigInteger bitmask = new BigInteger("1").shiftLeft(i);
                // 3、建立一个长度为64的整数数组(假设要生成64位的数字指纹,也可以是其它数字),
                // 对每一个分词hash后的数列进行判断,如果是1000...1,那么数组的第一位和末尾一位加1,
                // 中间的62位减一,也就是说,逢1加1,逢0减1.一直到把所有的分词hash数列全部判断完毕.
                int weight = 1;  //添加权重

                if (weightOfNature.containsKey(nature)) {
                    weight = weightOfNature.get(nature);
                }

                if (t.and(bitmask).signum() != 0) {
                    // 这里是计算整个文档的所有特征的向量和
                    v[i] += weight;
                } else {
                    v[i] -= weight;
                }
            }
        }
        BigInteger fingerprint = new BigInteger("0");
        for (int i = 0; i < this.hashbits; i++) {
            if (v[i] >= 0) {
                fingerprint = fingerprint.add(new BigInteger("1").shiftLeft(i));
            }
        }
        return fingerprint;
    }


    /**
     * 对单个的分词进行hash计算;
     * @param source 词
     * @return 哈希
     */
    private BigInteger hash(String source) {
        if (source == null || source.length() == 0) {
            return new BigInteger("0");
        } else {
            /**
             * 当source 的长度过短，会导致hash算法失效，因此需要对过短的词补偿
             */
            while (source.length() < 3) {
                source = source + source.charAt(0);
            }
            char[] sourceArray = source.toCharArray();
            BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
            BigInteger m = new BigInteger("1000003");
            BigInteger mask = new BigInteger("2").pow(this.hashbits).subtract(new BigInteger("1"));
            for (char item : sourceArray) {
                BigInteger temp = BigInteger.valueOf((long) item);
                x = x.multiply(m).xor(temp).and(mask);
            }
            x = x.xor(new BigInteger(String.valueOf(source.length())));
            if (x.equals(new BigInteger("-1"))) {
                x = new BigInteger("-2");
            }
            return x;
        }
    }

    /**
     * 计算海明距离,海明距离越小说明越相似;
     * @param other 另一个文章
     * @return 海明距离
     */
    private int hammingDistance(MySimHash other) {
        BigInteger m = new BigInteger("1").shiftLeft(this.hashbits).subtract(
                new BigInteger("1"));
        BigInteger x = this.strSimHash.xor(other.strSimHash).and(m);
        int tot = 0;
        while (x.signum() != 0) {
            tot += 1;
            x = x.and(x.subtract(new BigInteger("1")));
        }
        return tot;
    }

    public double getSemblance(MySimHash s2 ){
        double i = this.hammingDistance(s2);
        return 1 - i/this.hashbits;
    }

    public static void main(String[] args) throws InterruptedException {

        long startTime = new Date().getTime();

        //从命令行参数获取路径
        String originalTextPath = args[0];
        String plagiarismTextPath = args[1];
        String answerPath = args[2];

        MySimHash originalTextHash = new MySimHash(TextHandler.read(originalTextPath));
        MySimHash plagiarismTextHash = new MySimHash(TextHandler.read(plagiarismTextPath));

        double result = originalTextHash.getSemblance(plagiarismTextHash);
        System.out.println("两篇文章的相似率为：" + result);

        try{
            TextHandler.write(answerPath, result+"");
        }catch (IOException e){
            System.out.println("写入答案出错！");
            System.exit(1);
        }

        long endTime = new Date().getTime();
        System.out.println("本程序运行 " + (endTime - startTime) + " 毫秒完成。" );

    }

}
