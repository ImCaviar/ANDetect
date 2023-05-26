package iie.group5;

import iie.group5.APKprocess.*;
import iie.group5.TTPprocess.SCprocess;
import iie.group5.TTPprocess.SerialObj;
import iie.group5.TTPprocess.XGBmodel;
import iie.group5.features.ResSetsAndPool;
import iie.group5.structure.Module;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//输入一个APK的路径，分析其中是否包含广告网络第三方库
public class ANDMain {
    public static void main(String[] args) throws Exception{
        //获取当前的APK文件所在路径，后面可以改成通过jar包的参数进行输入
        if (args.length < 4){
            System.out.print("Please input command: java -jar ANDetect.jar -a [APK PATH] -o [RESULT PATH]");
        }else if (args[0].equals("-a") && args[2].equals("-o")){
            if (args[1].contains(".apk")){
                parseAPK(args[1], args[3]);
            }else{
                System.out.print("Please input command: java -jar ANDetect.jar -a [APK PATH] -o [RESULT PATH]");
            }
        }else{
            System.out.print("Please input command: java -jar ANDetect.jar -a [APK PATH] -o [RESULT PATH]");
        }
    }

    //解析APK
    private static void parseAPK(String path, String output) throws Exception {
        //输出结果到output
        File f = new File(output);
        FileOutputStream fos = new FileOutputStream(f);
        OutputStreamWriter osw = new OutputStreamWriter(fos);

        String apkFile = path;
        //解压目标文件夹，如果文件夹已经存在则删除
        String apkUnzip = apkFile.replace(".apk","/");
        File tmpF = new File(apkUnzip);
        if (tmpF.exists()){
            FileUtils.deleteQuietly(tmpF);
        }

        //解压缩APK
        unZip(apkFile, apkUnzip);
        System.out.println("APK unzip over.");

        //调用baksmali将classes.dex转换成smali代码，这里用的是baksmali-2.5.2
        //找到所有classes.dex，设置classes.dex所在路径和输出路径

        List<String> classes = findClasses(apkUnzip);
        String smaliRoot = apkUnzip + "smali/";
        for (String s : classes){
            String dexFile = apkUnzip.concat(s);
            String subName = s.replace(".dex", "");
            String smaliOutput = smaliRoot.concat(subName);
            //将classes.dex转换成smali代码
            genSmali(dexFile, smaliOutput);
        }
        System.out.println("Transfer classes.dex to Smali files.");

        //将二进制的AndroidManifest.xml转换成文本文件
        String AMpath = apkUnzip + "AndroidManifest.xml";
        String AMoutput = apkUnzip + "AndroidManifest_out.xml";
        ResProcess resProcess = new ResProcess();
        resProcess.transAM(AMpath, AMoutput);
        System.out.println("Analyze AndroidManifest.xml.");
        //将二进制的resources.arsc转换成public.xml
        String ARSCpath = apkUnzip + "resources.arsc";
        String ARSCoutput = apkUnzip + "public.xml";
        resProcess.transARSC(ARSCpath, ARSCoutput);
        System.out.println("Parse resources.arsc to public.xml.");
        //提取资源文件特征，获取主机应用程序包名
        String hostPKG = resProcess.parseRes(AMoutput, ARSCoutput, apkUnzip);

        //进行模块解耦，统计解耦所需时间
        MDecouple mDecouple = new MDecouple(smaliRoot);
        mDecouple.decoupling(hostPKG);
        osw.write("[Encrypted APK]: ");
        if (mDecouple.isShelled()){
            osw.write("Encrypted!\n");
        }else {
            osw.write("Not Encrypted!\n");
            //先解耦，再对每个模块进行AN识别
            System.out.println("Recognize Ad Networks Libraries...");
            List<Module> modules = mDecouple.getModules();
            SerialObj sobj = new SerialObj();
            sobj.setSerPath();
            osw.write("[SC Detect Result]: ");
            int count = 0;
            for (Module m : modules){
                boolean isAN = recogAN(m, 2);
                if (isAN){
                    //如果是AN，则对模块的结构和代码特征进行匹配
                    count ++;
                    String pkgName = m.getName().replaceAll("/",".");
                    //尝试把模块名称对应到ser名称，候选>=1个名字相似的ser，读取其中的Module进行匹配
                    List<String> ANs = sobj.getSerPath();
                    List<String> candi = getCandiAN(ANs, pkgName);
                    if (candi.size() == 0){
                        //没有候选AN，说明是白名单外的包
                        osw.write(pkgName + " => not in whitelist;");
                    }else{
                        //读取candi中的Modules并进行匹配，选择匹配度最大的版本号输出
                        List<Module> candiM = new ArrayList<>();
                        for (String c : candi){
                            sobj.inputSERsingle(c);
                        }
                        candiM = sobj.getModClass();
                        //压缩m的系统类型
                        m.simpleType();
                        double maxMatch = 0;
                        Module maxM = null;
                        for (Module cm : candiM){
                            MatchbySC msc = new MatchbySC(cm,m);
                            double pctM = msc.compAll();
                            if (pctM > maxMatch){
                                maxMatch = pctM;
                                maxM = cm;
                            }
                        }
                        //输出最匹配的版本号，版本号为空就不输出
                        if (maxM != null){
                            if (!maxM.getVersion().equals("")){
                                osw.write(maxM.getName() + " == version =>" + maxM.getVersion() + ";");
                            }else{
                                osw.write(maxM.getName() + ";");
                            }
                        }
                        //清空读取ser的module
                        sobj.clearModClass();
                    }
                }
            }
            if (count == 0){
                osw.write("");
            }
        }
        //进行资源特征匹配
        ResSetsAndPool rsap = new ResSetsAndPool();
        Map<String, ResProcess> an2ResLib = rsap.getAnName2ResLib();
        Map<String, Map<String, Double>> an2tfidf = rsap.getAn2tfidf();
        MatchbyRes mr = new MatchbyRes(resProcess, an2ResLib, an2tfidf);
        Map<String, Double> ANs = mr.compMaxMatch(2);
        osw.write("\n[Res Detect Result]: ");
        if (ANs.size() == 0){
            osw.write("");
        }else{
            for (String r : ANs.keySet()){
                osw.write(r + ";");
            }
        }
        osw.close();
    }

    //解压缩APK
    private static void unZip(String fileName, String filePath) throws Exception{
        //生成压缩文件对象
        ZipFile zipFile = new ZipFile(fileName);
        //枚举，循环读zip中的每个文件
        Enumeration emu = zipFile.entries();
        while (emu.hasMoreElements()){
            ZipEntry entry = (ZipEntry) emu.nextElement();
            //判断是否为文件夹
            if (entry.isDirectory()){
                //创建文件夹路径并生成
                File f = new File(filePath + entry.getName());
                f.mkdirs();
                continue;
            }
            BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
            //是文件的话
            File file = new File(filePath + entry.getName());
            //得到文件的父目录
            File parent = file.getParentFile();
            //如果父目录没建立则先建立父目录路径
            if (parent != null && (!parent.exists())){
                parent.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos, 1024);
            byte[] buf = new byte[1024];
            int len;
            while ((len = bis.read(buf,0,1024)) != -1){
                fos.write(buf,0,len);
            }
            bos.flush();
            bos.close();
            bis.close();
        }
        zipFile.close();
    }

    //找到文件夹中所有的classes.dex文件
    private static List<String> findClasses(String filePath)throws Exception{
        File file = new File(filePath);
        File [] subFiles = file.listFiles();
        List<String> classes = new ArrayList<>();
        for (File f : subFiles){
            //找到.dex文件
            String filename = f.getName();
            if (filename.contains(".dex") && filename.contains("classes")){
                classes.add(filename);
            }
        }
        return classes;
    }

    //将classes.dex转换成smali代码
    private static void genSmali(String dexFile, String smaliOutput){
        new Dex2Smali(dexFile, smaliOutput);
    }

    //输入一个Module类型的子模块，识别其是否为AN，true表示为AN
    private static boolean recogAN(Module subM, int str_thresh) throws Exception {
        SCprocess scp = new SCprocess();
        List<float[]> arrays = scp.outputSA(subM);
        XGBmodel xgbM = new XGBmodel();
        float[] results = xgbM.classify(arrays.get(2),arrays.get(3),arrays.get(4));
        //result越接近1，表示为AN的概率越大；认为results中有一者大于0.9并且arrays.get(0)中有大于0的项，则判定为AN
        boolean API = false;
        boolean str = false;
        for (int i=0; i<results.length; i++){
            if (results[i] > 0.9){
                API = true;
            }
        }
        int count_str = 0;
        for (int i=0; i<arrays.get(0).length; i++){
            if (arrays.get(0)[i] > 0){
                str = true;
            }
        }
        if (count_str >= str_thresh){
            str = true;
        }
        return API && str;
    }

    //选择ANs中与pkg最相似的广告网络名称候选集合，有可能为空
    private static List<String> getCandiAN(List<String> ANs, String pkg2){
        List<String> candiAN = new ArrayList<>();
        double max = 0;
        String maxS = "";
        for (String an : ANs){
            double pctM = compare2pkgs(an, pkg2);
            if (pctM == 1.0){
                candiAN.add(an);
            } else if (pctM > max && pctM >= 0.5){
                max = pctM;
                maxS = an;
            }
        }
        if (candiAN.size() == 0 && max >= 0.5){
            candiAN.add(maxS);
        }
        return candiAN;
    }

    //比较两个包名是否匹配，包名以.分割
    private static double compare2pkgs(String pkg1, String pkg2){
        double result = 0;
        //如果第2个字符串元素匹配，则认为有1/2的匹配概率
        if (!pkg1.contains(".") && pkg2.contains(".")){
            String ele2 = pkg2.split("\\.")[0];
            if (pkg1.equals(ele2)){
                result = 1;
            }
        }else if (pkg1.contains(".") && pkg2.contains(".")){
            String[] ele1 = pkg1.split("\\.");
            String[] ele2 = pkg2.split("\\.");
            int len = ele1.length-1;
            int sum = 0;
            for (int i=1; i<ele1.length; i++){
                sum += Math.pow(2,i);
            }
            for (int i=1; i<ele1.length && i<ele2.length; i++){
                if (ele1[i].equals(ele2[i])){
                    result += Math.pow(2,len-i+1);
                }
            }
            result = result/sum;
        }
        return result;
    }
}
