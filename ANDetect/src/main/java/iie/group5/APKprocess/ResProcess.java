package iie.group5.APKprocess;

import com.codyi.xml2axml.test.Main;
import com.hq.arscresourcesparser.arsc.ArscFile;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

//处理资源文件，包括AndroidManifest.xml和assets/res文件夹下的资源文件，并生成资源特征
public class ResProcess {
    //F1 AndroidManifest.xml_manifest_package 全域匹配android:name
    private HashSet<String> all_an;
    //F2 AndroidManifest.xml_manifest uses-permission_android:name 完全路径匹配-固定特征
    private HashSet<String> permission;
    //F3 AndroidManifest.xml_manifest application provider_android:authorities 完全路径匹配-固定特征
    private HashSet<String> auth;
    //F4 AndroidManifest.xml_manifest application receiver intent-filter action_android:name 完全路径匹配-固定特征
    private HashSet<String> intentFilter;
    //F5 AndroidManifest.xml_manifest application activity_android:name 完全路径匹配-可变特征
    private HashSet<String> activity;
    //F6 AndroidManifest.xml_manifest application service_android:name 完全路径匹配-可变特征
    private HashSet<String> service;
    //F7 AndroidManifest.xml_manifest application receiver_android:name 完全路径匹配-可变特征
    private HashSet<String> receiver;
    //F8 res\values\values.xml_resources dimen_name 无路径匹配
    private HashSet<String> dimen;
    //F9 res\values\values.xml_resources string_name 无路径匹配
    private HashSet<String> stringn;
    //F10 res\values\values.xml_resources declare-styleable attr_name 无路径匹配
    private HashSet<String> attr;
    //F11 res\values\values.xml_resources style_name 无路径匹配
    private HashSet<String> style;
    //资源图片名称池
    private HashSet<String> picNames;
    //host应用程序包名
    private String hostPKG;

    public ResProcess() {
        this.all_an = new HashSet<>();
        this.permission = new HashSet<>();
        this.auth = new HashSet<>();
        this.intentFilter = new HashSet<>();
        this.activity = new HashSet<>();
        this.service = new HashSet<>();
        this.receiver = new HashSet<>();
        this.dimen = new HashSet<>();
        this.stringn = new HashSet<>();
        this.attr = new HashSet<>();
        this.style = new HashSet<>();
        this.picNames = new HashSet<>();
    }

    //传入AM.xml所在地址，和输出地址，对AM.xml的二进制文件进行解析，并将其转换成文本文件
    public void transAM(String binFile, String outputAM) throws Exception{
        //直接调用xml2axml-2.0.1.jar包的main函数
        Main am = new Main();
        String[] args = new String[]{"d",binFile, outputAM};
        am.main(args);
    }

    //传入resources.arsc所在地址和输出地址，对resources.arcs进行解析，输出xml文件
    public void transARSC(String binFile, String outputARSC) throws Exception{
        //模仿ArscResourcesParser.class
        String xml = "";
        File file = new File(binFile);
        FileInputStream fin = new FileInputStream(file);
        byte[] buf = new byte[(int)file.length()];
        fin.read(buf);
        ArscFile arscFile = new ArscFile();
        arscFile.parse(buf);
        xml = arscFile.buildPublicXml();
        fin.close();
        //处理xml
        String newXML = "";
        String[] xml_line = xml.split("\n");
        for (int i=0; i<xml_line.length; i++){
            int ind = xml_line[i].lastIndexOf(" data=");
            if (ind != -1){
                xml_line[i] = xml_line[i].substring(0, ind) + "/>";
            }
            xml_line[i] += "\n";
            newXML += xml_line[i];
        }
        //输出xml保存到outputARSC中
        File output = new File(outputARSC);
        FileOutputStream fos = new FileOutputStream(output);
        fos.write(newXML.getBytes());
        fos.close();
    }

    //解析资源文件，提取特征列表，返回host应用程序包名
    public String parseRes(String AMFile, String ARSCFile, String zipPath) throws Exception {
        parseAM(AMFile);
        parseARSC(ARSCFile);
        paresPic(zipPath);
        return getHostPKG();
    }

    //解析AM.xml文件，提取特征F1-F7
    public void parseAM(String AMFile) throws Exception {
        //创建Reader对象，加载xml
        SAXBuilder reader = new SAXBuilder();
        InputStream is = new FileInputStream(new File(AMFile));
        Document document = reader.build(is);
        //获取根节点，记录路径（父元素和子元素间用空格分隔，元素和属性间用_分隔）和属性值
        Map<String,List<String>> ele2attr = new HashMap<>();
        Element root = document.getRootElement();
        recGetEle(root, "", ele2attr);
        //解析ele2attr中的元素和属性值
        for (String key : ele2attr.keySet()){
            String prefix = key.split("_")[0];
            String suffix = key.split("_")[1];
            List<String> list = ele2attr.get(key);
            for (String l : list){
                if (suffix.equals("android:name")){
                    this.all_an.add(l);
                    if (prefix.equals("manifest uses-permission")){
                        this.permission.add(l);
                    }else if (prefix.equals("manifest application receiver intent-filter action")){
                        this.intentFilter.add(l);
                    }else if (prefix.equals("manifest application activity")){
                        this.activity.add(l);
                    }else if (prefix.equals("manifest application service")){
                        this.service.add(l);
                    }else if (prefix.equals("manifest application receiver")){
                        this.receiver.add(l);
                    }
                }else if (suffix.equals("android:authorities") && prefix.equals("manifest application provider")){
                    this.auth.add(l);
                }else if (suffix.equals("package") && prefix.equals("manifest")){
                    this.hostPKG = l;
                }
            }
        }
    }

    //递归获取元素和属性值
    private void recGetEle(Element element, String path, Map<String,List<String>> ele2attr){
        //如果element有属性，则插入ele2attr中
        List<Attribute> attributes = element.getAttributes();
        String pre;
        if (!path.equals("")){
            pre = path + " " + element.getQualifiedName();
        }else{
            pre = element.getName();
        }
        if (attributes.size() > 0){
            for (Attribute a : attributes){
                String key = pre+"_"+a.getQualifiedName();
                if (ele2attr.containsKey(key)){
                    ele2attr.get(key).add(a.getValue());
                }else{
                    List<String> list = new ArrayList<>();
                    list.add(a.getValue());
                    ele2attr.put(key, list);
                }
            }
        }
        //递归子节点
        List<Element> children = element.getChildren();
        for (Element child : children){
            recGetEle(child, pre, ele2attr);
        }
    }

    //解析public.xml文件，提取特征F8-F11
    private void parseARSC(String ARSCFile) throws Exception {
        //创建Reader对象，加载xml
        SAXBuilder reader = new SAXBuilder();
        InputStream is = new FileInputStream(new File(ARSCFile));
        Document document = reader.build(is);
        //获取根节点，记录路径（父元素和子元素间用空格分隔，元素和属性间用_分隔）和属性值
        Element root = document.getRootElement();
        List<Element> children = root.getChildren();
        //解析子元素中的元素和属性值
        for (Element child : children){
            String ele = child.getName();
            if (!ele.equals("public")){
                continue;
            }
            List<Attribute> attributes = child.getAttributes();
            if (attributes.size() > 0){
                int flag = 0;
                for (Attribute a : attributes){
                    if (a.getQualifiedName().equals("type")){
                        switch (a.getValue()){
                            case "dimen":
                                flag = 1;
                                break;
                            case "string":
                                flag = 2;
                                break;
                            case "attr":
                                flag = 3;
                                break;
                            case "style":
                                flag = 4;
                                break;
                            default:
                                break;
                        }
                    }else if (a.getQualifiedName().equals("name")){
                        switch (flag){
                            case 1:
                                this.dimen.add(a.getValue());
                                break;
                            case 2:
                                this.stringn.add(a.getValue());
                                break;
                            case 3:
                                this.attr.add(a.getValue());
                                break;
                            case 4:
                                this.style.add(a.getValue());
                                break;
                        }
                    }
                }
            }
        }
    }

    //根据解压主目录，找到资源图片目录，提取资源图片名称池
    private void paresPic(String zipPath){
        //遍历assets文件夹下的所有文件，res下关于drawable中的所有字符串名称
        String suffix1 = zipPath + "assets";
        String suffix2 = zipPath + "res";
        File file1 = new File(suffix1);
        File file2 = new File(suffix2);
        if (!file2.exists()){
            suffix2 = zipPath + "r";
            file2 = new File(suffix2);
        }
        findPic(file1);
        File [] subfile2 = file2.listFiles();
        if (subfile2 != null){
            for(File f : subfile2){
                String filename = f.getName();
                if (filename.contains("drawable")){
                    findPic(f);
                }
            }
        }
    }

    //遍历文件夹下的所有文件，并存入this.picNames中
    private void findPic(File file){
        if (file != null && file.exists()){
            String filename = file.getName();
            if (file.isDirectory()){
                //是文件夹就继续向下遍历，是文件就判断其前缀后缀
                File [] subFiles = file.listFiles();
                if (subFiles != null){
                    for (File f : subFiles){
                        //递归，本节点的id作为下一节点的parentId
                        findPic(f);
                    }
                }
            }else{
                //去除公共前缀，查找公共后缀
                List<String> common_prefix = Arrays.asList("btn","abc","ic","icon");
                List<String> pic_suffix = Arrays.asList("xbm","tif","pjp","svgz","jpg","jpeg","ico","tiff","gif","svg","jfif","webp","png","bmp","pjpeg","avif");
                if (filename.contains("_")){
                    String prefix = filename.split("_")[0];
                    if (common_prefix.contains(prefix)){
                        return;
                    }
                }
                if (filename.contains(".")){
                    int ind = filename.lastIndexOf(".");
                    String suffix = filename.substring(ind+1);
                    if (pic_suffix.contains(suffix)){
                        this.picNames.add(filename.substring(0, ind));
                    }
                }
            }
        }
    }

    public HashSet<String> getAll_an() {
        return all_an;
    }

    public void addAll_an(String all_an) {
        this.all_an.add(all_an);
    }

    public HashSet<String> getPermission() {
        return permission;
    }

    public void addPermission(String permission) {
        this.permission.add(permission);
    }

    public HashSet<String> getAuth() {
        return auth;
    }

    public void addAuth(String auth) {
        this.auth.add(auth);
    }

    public HashSet<String> getIntentFilter() {
        return intentFilter;
    }

    public void addIntentFilter(String intentFilter) {
        this.intentFilter.add(intentFilter);
    }

    public HashSet<String> getActivity() {
        return activity;
    }

    public void addActivity(String activity) {
        this.activity.add(activity);
    }

    public HashSet<String> getService() {
        return service;
    }

    public void addService(String service) {
        this.service.add(service);
    }

    public HashSet<String> getReceiver() {
        return receiver;
    }

    public void addReceiver(String receiver) {
        this.receiver.add(receiver);
    }

    public HashSet<String> getDimen() {
        return dimen;
    }

    public void addDimen(String dimen) {
        this.dimen.add(dimen);
    }

    public HashSet<String> getStringn() {
        return stringn;
    }

    public void addStringn(String stringn) {
        this.stringn.add(stringn);
    }

    public HashSet<String> getAttr() {
        return attr;
    }

    public void addAttr(String attr) {
        this.attr.add(attr);
    }

    public HashSet<String> getStyle() {
        return style;
    }

    public void addStyle(String style) {
        this.style.add(style);
    }

    public HashSet<String> getPicNames() {
        return picNames;
    }

    public void addPicNames(String picNames) {
        this.picNames.add(picNames);
    }

    public String getHostPKG() {
        return hostPKG;
    }

    public void setHostPKG(String hostPKG) {
        this.hostPKG = hostPKG;
    }
}
