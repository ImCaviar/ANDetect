package iie.group5.TTPprocess;

import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import iie.group5.APKprocess.Dex2Smali;
import iie.group5.APKprocess.JarToSmalibyD8;
import iie.group5.APKprocess.JarToSmalibyDX;
import iie.group5.APKprocess.ResProcess;
import iie.group5.features.JARnames;
import iie.group5.features.StringAndAPI;
import iie.group5.structure.Module;
import iie.group5.structure.PackagePoint;
import iie.group5.structure.Smali;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//提取所有第三方库的字符串特征和API特征共五项，生成5张csv表格
public class SCprocess {
    private List<String> strings;
    private List<String> APIs;
    private int str_len;
    private int API_len;
    private List<float[]> str_count;
    private List<float[]> str_depth;
    private List<float[]> API_count;
    private List<float[]> API2clazz;
    private List<float[]> API2method;
    private List<String> label;
    private int ppCount;
    private Map<String,String> jarName;

    public SCprocess() throws IOException, CsvValidationException {
        StringAndAPI saa = new StringAndAPI();
        this.strings = saa.getStrings();
        this.APIs = saa.getAPIs();
        this.str_len = this.strings.size();
        this.API_len = this.APIs.size();
        //记录5项特征
        this.str_count = new ArrayList<>();
        this.str_depth = new ArrayList<>();
        this.API_count = new ArrayList<>();
        this.API2clazz = new ArrayList<>();
        this.API2method = new ArrayList<>();
        this.label = new ArrayList<>();
        this.ppCount = 0;
        JARnames jar = new JARnames();
        this.jarName = jar.getPath2pkg();
    }

    //处理所有的aar/jar文件
    public void processAll(String[] paths) throws Exception {
        if (paths.length == 1){
            String an_path = paths[0];
            processOnePath(an_path, "AN");
        }else if (paths.length == 2){
            String an_path = paths[0];
            String nan_path = paths[1];
            processOnePath(an_path, "AN");
            processOnePath(nan_path, "NAN");
        }
        //把结果输出到5个csv文件中
        float2CSV(this.str_count, "string_count_fea.csv", this.strings);
        float2CSV(this.str_depth, "string_depth_fea.csv", this.strings);
        float2CSV(this.API_count, "API_count_fea.csv", this.APIs);
        float2CSV(this.API2clazz, "API_clazz_fea.csv", this.APIs);
        float2CSV(this.API2method, "API_method_fea.csv", this.APIs);
    }

    public List<Module> processOnePath(String path, String label) throws Exception {
        List<Module> modules = new ArrayList<>();
        //找到path路径下所有aar文件和jar文件
        List<File> aarList = new ArrayList<>();
        List<File> jarList = new ArrayList<>();
        File anFile = new File(path);
        findFiles(anFile, aarList, jarList);
        for (File aar : aarList){
            processAAR(aar, label, modules);
        }
        for (File jar : jarList){
            processJAR(jar, label, modules);
        }
        return modules;
    }

    public void processAAR(File file, String lab, List<Module> modules) throws Exception {
        //获取版本号，如果有的话
        String aarname = file.getName();
        String version = "";
        Matcher m = Pattern.compile("\\d+(.\\d+)*").matcher(aarname);
        if (m.find()){
            version = m.group();
        }
        //改aar文件后缀名为zip，然后解压zip
        String filename = file.getPath();
        String newName = filename.replace(".aar",".zip");
        File newFile = new File(newName);
        copyFile(file, newFile);
        String unzipFile = newName.replace(".zip", "/");
        //找到名为classes.jar的包转为smali代码
        if (unZip(newName, unzipFile)){
            try {
                String smaliPath = jar2smali(unzipFile + "/classes.jar");
                if (!smaliPath.equals("")){
                    //解析smali文件，插入5大特征列表
                    Module module = parseSmali(smaliPath, lab);
                    //对于aar文件，把AM.xml文件下的package作为模块名称
                    String am = unzipFile + "/AndroidManifest.xml";
                    ResProcess resProcess = new ResProcess();
                    resProcess.parseAM(am);
                    String pkg = resProcess.getHostPKG();
                    if (pkg.equals("")){
                        System.out.printf("%s's AM.xml doesn't have package???",filename);
                        String[] folder = filename.split("/");
                        String f = folder[folder.length-2];
                        pkg = this.jarName.get(f);
                    }
                    module.setName(pkg);
                    module.setVersion(version);
                    module.simpleType();
                    modules.add(module);
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                FileUtils.deleteQuietly(new File(unzipFile));
                System.out.println("delete " + unzipFile);
            }
        }
        //最后要把文件夹下的zip和解压后的文件夹都给删掉
        newFile.delete();
    }

    public void processJAR(File file, String lab, List<Module> modules) throws Exception {
        try {
            //获取版本号，如果有的话
            String jarname = file.getName();
            String version = "";
            Matcher m = Pattern.compile("\\d+(.\\d+)*").matcher(jarname);
            if (m.find()){
                version = m.group();
            }
            //直接将jar解析到smali文件中
            String smaliPath = jar2smali(file.getPath());
            if (!smaliPath.equals("")){
                //解析smali文件，插入5大特征列表
                Module module = parseSmali(smaliPath, lab);
                //对于jar文件，直接查表找到package作为模块名称
                String[] folder = file.getPath().split("/");
                String f = folder[folder.length-2];
                module.setName(this.jarName.get(f));
                module.setVersion(version);
                module.simpleType();
                modules.add(module);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        //最后要把文件夹下的classes.dex和smali文件夹都删掉
        int ind = file.getPath().lastIndexOf("/");
        String dex = file.getPath().substring(0, ind) + "/classes.dex";
        String smali = file.getPath().substring(0, ind) + "/smali/";
        File dexFile = new File(dex);
        dexFile.delete();
        boolean result = FileUtils.deleteQuietly(new File(smali));
        System.out.printf("Delete %s %b.\n", smali, result);
    }

    private Module parseSmali(String path, String lab){
        //遍历smali文件夹下的所有路径，生成Module
        File file = new File(path);
        List<PackagePoint> packagePoints = new ArrayList<>();
        List<Smali> smalis = new ArrayList<>();
        this.ppCount = 0;
        findSmalis(file, null, packagePoints, smalis);
        Module module = new Module(packagePoints, smalis);
        //声明数组
        List<float[]> arrays = outputSA(module);
        //插入List中
        this.str_count.add(arrays.get(0));
        this.str_depth.add(arrays.get(1));
        this.API_count.add(arrays.get(2));
        this.API2clazz.add(arrays.get(3));
        this.API2method.add(arrays.get(4));
        this.label.add(lab);
        return module;
    }

    //给定一个module，输出其中的五项特征
    public List<float[]> outputSA(Module module){
        //声明数组
        List<float[]> result = new ArrayList<>();
        float[] str_c = new float[this.str_len];
        float[] str_d = new float[this.str_len];
        float[] API_c = new float[this.API_len];
        float[] API2c = new float[this.API_len];
        float[] API2m = new float[this.API_len];
        comp_str(module, str_c, str_d);
        comp_API(module, API_c, API2c, API2m);
        result.add(str_c);
        result.add(str_d);
        result.add(API_c);
        result.add(API2c);
        result.add(API2m);
        return result;
    }

    private void comp_str(Module module, float[] str_c, float[] str_d){
        Map<Integer, PackagePoint> ppM = module.getPackagePoints();
        List<Smali> smalis = module.getSmalis();
        int[] depth_count = new int[this.str_len];
        for (int i=0; i<this.str_len; i++){
            str_c[i] = 0;
            str_d[i] = 0;
            depth_count[i] = 0;
        }
        for (Integer i : ppM.keySet()){
            for (int j=0; j<this.strings.size(); j++){
                String ppLabel = ppM.get(i).getLabel().toLowerCase();
                String tmpStr = this.strings.get(j).toLowerCase();
                if (ppLabel.contains(tmpStr)){
                    str_c[j] += 1;
                    //计算该节点到最近叶子节点的深度+1
                    int depth = module.dis2leaf(i)+1;
                    str_d[j] += depth;
                    depth_count[j] += 1;
                }
            }
        }
        for (Smali s : smalis){
            for (int j=0; j<this.strings.size(); j++){
                String smaliLabel = s.getClazz().toLowerCase();
                String tmpStr = this.strings.get(j).toLowerCase();
                if (smaliLabel.contains(tmpStr)){
                    str_c[j] += 1;
                }
            }
        }
        for (int i=0; i<this.str_len; i++){
            if (depth_count[i] > 0){
                str_d[i] = str_d[i]/depth_count[i];
            }
        }
    }

    private void comp_API(Module module, float[] API_c, float[] API2c, float[] API2m){
        List<Smali> smalis = module.getSmalis();
        for (Smali s : smalis){
            Map<Integer, List<Integer>> inv2methods = new HashMap<>();
            boolean[] clazz = new boolean[this.API_len];
            for (int i=0; i<this.API_len; i++){
                clazz[i] = false;
            }
            String[] invokes = s.getInvoke();
            int[] inv_method = s.getInvokeFromMethod();
            for (int inv=0; inv<invokes.length; inv++){
                if(invokes[inv] == null){
                    System.out.println(s.getClazz());
                }
                String pkg = invokes[inv].split("->")[0];
                int method = inv_method[inv];
                pkg = pkg.replaceAll("/",".");
                int ind = this.APIs.indexOf(pkg);
                if (ind != -1){
                    API_c[ind] += 1;
                    clazz[ind] = true;
                    addEle2Map(inv2methods, ind, method);
                }
            }
            //统计API是否出现在smali s中，API出现在smali s的多少个类中
            for (int i=0; i<this.API_len; i++){
                if (clazz[i]){
                    API2c[i] += 1;
                }
                if (inv2methods.containsKey(i)){
                    int count = inv2methods.get(i).size();
                    API2m[i] += count;
                }
            }
        }
        //计算最终结果
        for (int i=0; i<this.API_len; i++){
            if (API_c[i] > 0 && (API2c[i] == 0 || API2m[i] == 0)){
                System.out.println("Something wrong in comp_API");
            }else if (API_c[i] == 0){
                API2c[i] = 0;
                API2m[i] = 0;
            }else{
                API2c[i] = API_c[i]/API2c[i];
                API2m[i] = API_c[i]/API2m[i];
            }
        }
    }

    private void addEle2Map(Map<Integer, List<Integer>> inv2methods, int ind, int method){
        if (!inv2methods.containsKey(ind)){
            List<Integer> methods = new ArrayList<>();
            methods.add(method);
            inv2methods.put(ind, methods);
        }else if (!inv2methods.get(ind).contains(method)){
            inv2methods.get(ind).add(method);
        }
    }

    //遍历file下的所有文件，排除hostPKG中的文件夹和文件
    private void findFiles(File file, List<File> aarList, List<File> jarList){
        if (file != null && file.exists()){
            String fileName = file.getName();
            if (file.isDirectory()){
                //文件夹则继续往下找
                File [] subFiles = file.listFiles();
                if (subFiles != null){
                    for (File f : subFiles){
                        //递归
                        findFiles(f, aarList, jarList);
                    }
                }
            }else if (fileName.endsWith(".aar")){
                aarList.add(file);
            }else if (fileName.endsWith(".jar")){
                jarList.add(file);
            }
        }
    }

    //将classes.dex转换成smali代码
    private void genSmali(String dexFile, String smaliOutput){
        try {
            new Dex2Smali(dexFile, smaliOutput);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //测试jadx是否能把class转换成smali，把jar解压，然后添加class列表
    private String jar2smali(String jarFile) throws Exception {
        //生成DEX文件，先尝试dx-new.jar（删除了System.exit()部分），不行再用d8.jar，还是不行就返回""
        int ind = jarFile.lastIndexOf("/");
        String dex = jarFile.substring(0, ind) + "/classes.dex";

        JarToSmalibyDX dx = new JarToSmalibyDX(jarFile);
        dx.trans2dex();

        File file1 = new File(dex);
        if (!file1.exists()){
            JarToSmalibyD8 d8 = new JarToSmalibyD8(jarFile);
            d8.trans2dex();
            File file2 = new File(dex);
            if (!file2.exists()){
                return "";
            }
        }

        //将classes转换成smali代码
        String output = jarFile.substring(0, ind) + "/smali";
        genSmali(dex, output);
        return output;
    }

    //复制一个文件到另一个文件中
    private void copyFile(File source, File dest) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(dest);
            byte[] buff = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buff))>0){
                output.write(buff, 0, bytesRead);
            }
        }finally {
            input.close();
            output.close();
        }
    }

    //解压缩zip文件
    private boolean unZip(String fileName, String filePath) throws Exception{
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
            int len = 0;
            while ((len = bis.read(buf,0,1024)) != -1){
                fos.write(buf,0,len);
            }
            bos.flush();
            bos.close();
            bis.close();
        }
        zipFile.close();
        return true;
    }

    //遍历file下的所有文件，排除hostPKG中的文件夹和文件
    private void findSmalis(File file, Integer parentId, List<PackagePoint> packagePoints, List<Smali> smalis){
        if (file != null && file.exists()){
            String filePath = file.getPath();
            String fileName = file.getName();
            if (file.isDirectory()){
                //file是文件夹，构建节点
                Integer id = this.ppCount ++;
                PackagePoint packagePoint = new PackagePoint(id, fileName, parentId);
                packagePoints.add(packagePoint);
                File [] subFiles = file.listFiles();
                if (subFiles != null){
                    for (File f : subFiles){
                        //递归，本节点的id作为下一节点的parentId
                        findSmalis(f, id, packagePoints, smalis);
                    }
                }
            }else if (fileName.endsWith(".smali")){
                //file是smali文件，构建Smali类
                Smali smali = new Smali(parentId);
                //!!!解析每个smali文件!!!
                smali.analyzeSmali(filePath);
                smalis.add(smali);
            }
        }
    }

    //写入列表到指定的csv文件中
    private void float2CSV(List<float[]> ints, String output, List<String> title) throws IOException {
        //把title转成字符串数组
        String[] titles = new String[title.size() + 1];
        for (int i=0; i<title.size(); i++){
            titles[i] = title.get(i);
        }
        titles[title.size()] = "label";
        output = "resources/" + output;
        CSVWriter csvWriter = new CSVWriter(new FileWriter(output));
        csvWriter.writeNext(titles);
        for (int i=0; i<ints.size(); i++){
            int len = ints.get(i).length;
            String[] data = new String[len + 1];
            for (int j=0; j<len; j++){
                data[j] = Integer.toString((int) ints.get(i)[j]);
            }
            data[len] = this.label.get(i);
            csvWriter.writeNext(data);
        }
        csvWriter.close();
    }
}
