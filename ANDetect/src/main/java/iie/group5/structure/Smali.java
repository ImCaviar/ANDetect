package iie.group5.structure;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

//Smali文件结构
public class Smali implements Serializable {
    //class类名
    private String clazz;
    //super父类，继承关系
    private transient String supper;
    //method数组中的每一项都由访问权限、参数类型列表、返回类型拼接而成
    private String[] method;
    //field数组中的每一项都由是否静态、访问权限、字段类型拼接而成
    private String[] field;
    //smali文件中调用的函数，不区分系统函数和自定义函数
    private transient String[] invoke;
    //smali文件中调用的方法来自的方法索引
    private transient int[] invokeFromMethod;
    //smali文件中调用的方法是否来自系统函数 1：来自系统函数；0：自定义函数
    private transient int[] invokeFromSys;
    //smali文件所在叶子节点ID
    private Integer ppID;

    //定义常量
    private final int maxlen = 20;
    //访问权限，常量，其中什么也不写表示default，用0，1，2，3分别表示以下四个访问权限
    private final String[] access = new String[]{"public","protected","default","private"};
    //类型描述符，常量
    private final String[] types = new String[]{"Z","B","S","C","I","J","F","D","V","["};

    public Smali() {
    }

    public Smali(Integer ppID) {
        this.ppID = ppID;
        this.method = new String[maxlen];
        this.field = new String[maxlen];
        this.invoke = new String[maxlen];
        this.invokeFromMethod = new int[maxlen];
        this.invokeFromSys = new int[maxlen];

        //初始化invokeFromMethod所有值为-1
        Arrays.fill(invokeFromMethod, -1);
        //初始化invokeFromSys所有值为true
        Arrays.fill(invokeFromSys, 1);
    }

    //根据smali文件所在路径分析该smali文件，分别生成method field sys_invoke pkg_invoke
    public void analyzeSmali(String filePath){
        try {
            List<String> allLines = Files.readAllLines(Paths.get(filePath));
            int methodInd = 0;
            for (String line: allLines){
                //以一个或多个空格分割字符串，删除空元素
                String[] elements = line.split("\\s+");
                if (elements.length == 1 && elements[0].length() == 0){
                    continue;
                }
                elements = deleteNull(elements);
                switch (elements[0]){
                    case ".class":
                        setClazz(analyzeCS(elements));
                        break;
                    case ".super":
                        setSupper(analyzeCS(elements));
                        break;
                    case ".field":
                        String field = analyzeField(elements);
                        this.field = addString(this.field, field);
                        break;
                    case ".method":
                        String method = analyzeMethod(elements);
                        this.method = addString(this.method, method);
                        methodInd ++;
                        break;
                    default:
                        if (elements[0].contains("invoke-")){
                            String func = analyzeInvoke(elements);
                            if (!func.equals("")){
                                this.invoke = addString(this.invoke, func);
                                this.invokeFromMethod = addInt(this.invokeFromMethod, methodInd-1);
                                this.invokeFromSys = addInt(this.invokeFromSys, 1);
                            }
                        }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //处理class名称/super父类
    private String analyzeCS(String[] elements){
        String result = elements[elements.length-1];
        result = result.replaceFirst("L","");
        result = result.replaceAll(";","");
        return result;
    }

    //处理field字段，返回"字段访问权限 字段类型"
    private String analyzeField(String[] elements){
        String result = "";
        //.field后为字段访问权限，如果为空则是"default"
        int inda = getIndex(this.access, elements[1]);
        if (inda == -1){
            result += "2 ";
        }else{
            result += inda + " ";
        }
        //静态字段则加上"s"，小写！！！区分于短整数型
        if (getIndex(elements, "static") != -1){
            result += "s ";
        }
        //查找字段类型并处理，通常为:后的字符串
        for (int i=1; i<elements.length; i++){
            int ind = elements[i].indexOf(":");
            if (ind != -1){
                String type = elements[i].substring(ind+1);
                if (type.length()>=5 &&( type.indexOf("L")==0 || type.indexOf("L")==1)){
                    type = type.replaceFirst("L","");
                }
                if (type.indexOf(";")!=0){
                    type = type.replaceAll(";","");
                }
                result += type;
                break;
            }
        }
        return result;
    }

    //处理method字段，返回"类访问权限 参数类型列表 返回类型"
    private String analyzeMethod(String[] elements){
        String result = "";
        //.class后为类访问权限，如果为空则是"default"
        int inda = getIndex(this.access, elements[1]);
        if (inda == -1){
            result += "2 ";
        }else{
            result += inda + " ";
        }
        //静态字段则加上"static"
        if (getIndex(elements, "static") != -1){
            result += "s ";
        }
        //查找括号内的所有参数类型用_分隔，首先用;分割，然后判断是否存在多字符、“L”前是否有types中字母
        for (int i=1; i<elements.length; i++){
            int ind = elements[i].indexOf("(");
            if (ind != -1){
                String tmp = elements[i].split("\\(")[1];
                String type = tmp.split("\\)")[0];
                String ret = tmp.split("\\)")[1];
                if (type.length() > 0){
                    if (type.contains(";")){
                        String[] typeList = type.split(";");
                        for (String tl : typeList){
                            result += getTypes(tl);
                        }
                    }else{
                        result += getTypes(type);
                    }
                }
                //处理返回类型
                if (ret.indexOf("L") == 0 || ret.indexOf("L") == 1){
                    ret = ret.replaceFirst("L", "");
                }
                if (ret.contains(";")){
                    ret = ret.replaceAll(";","");
                }
                result += ret;
                break;
            }
        }
        return result;
    }

    //处理method中的invoke函数调用，返回"调用函数包名->函数名"
    private String analyzeInvoke(String[] elements){
        String[] tmp = elements[elements.length-1].split("->");
        String pkg = tmp[0];
        String func = tmp[1];
        //处理调用函数包名
        int ind0 = pkg.indexOf('L');
        int ind3 = pkg.indexOf(";");
        if (ind0 == -1 && ind3 == -1){
            return "";
        }
        pkg = pkg.substring(ind0+1, ind3);
        //处理调用函数名
        int ind1 = func.indexOf(">");
        int ind2 = func.indexOf("(");
        func = func.substring(ind1+1, ind2);
        return pkg + "->" + func;
    }

    //分割字符串中的type
    private String getTypes(String type) {
        String result = "";
        if (type.contains("/")) {
            int ind = type.indexOf("L");
            if (ind == 0) {
                //"L"在第一位，删除"L"
                result = type.replaceFirst("L", "") + " ";
                return result;
            } else if (ind > 0 && ind < type.indexOf("/")) {
                //"L"不在第一位，但在"/"前，说明"L"前还有别的类型
                for (int i = 0; i < ind; i++) {
                    String sub = type.substring(i, i + 1);
                    if (getIndex(this.types, sub) != -1) {
                        result += sub + " ";
                    }
                }
                result += type.substring(ind+1) + " ";
                return result;
            }else{
                //没有"L"，直接返回
                return type + " ";
            }
        }else{
            //"L"在第一位，删除"L"
            if (type.contains("L") && type.indexOf("L") == 0){
                type = type.substring(1);
            }
            //剩余类中包含$表明是自定义类
            if (type.contains("$")){
                int ind = type.indexOf("$");
                result += type.substring(ind);
                type = type.substring(0, ind);
            }
            for (int i = 0; i < type.length(); i++){
                String sub = type.substring(i, i + 1);
                if (getIndex(this.types, sub) != -1) {
                    result += sub + " ";
                }
            }
            return result;
        }
    }

    //查找某个元素是否在String列表中，在则返回索引，不在则返回-1
    private int getIndex(String[] list, String ele){
        int result = -1;
        if (list.length == 0){
            return result;
        }else{
            for (int i=0; i<list.length; i++){
                if (list[i].equals(ele)){
                    return i;
                }
            }
            return result;
        }
    }

    //在String数组中添加字符串
    private String[] addString(String[] list, String ele){
        int nowlen = list.length;
        if (nowlen > maxlen){
            return newString(list, ele);
        }else{
            for (int i=0; i<nowlen; i++){
                if (list[i] == null){
                    list[i] = ele;
                    return list;
                }
            }
            return newString(list, ele);
        }
    }

    //开辟新String数组
    private String[] newString(String[] oldList, String newele){
        int oldlen = oldList.length;
        String[] newList = new String[oldlen+1];
        for (int i=0; i<oldlen; i++){
            newList[i] = oldList[i];
        }
        newList[oldlen] = newele;
        return newList;
    }

    //在int数组中添加数字
    private int[] addInt(int[] list, int value){
        int nowlen = list.length;
        if (nowlen > maxlen){
            return newInt(list, value);
        }else{
            for (int i=0; i<nowlen; i++){
                if (list[i] == -1){
                    list[i] = value;
                    return list;
                }
            }
            return newInt(list, value);
        }
    }

    //开辟int新数组
    private int[] newInt(int[] oldList, int newele){
        int oldlen = oldList.length;
        int[] newList = new int[oldlen+1];
        for (int i=0; i<oldlen; i++){
            newList[i] = oldList[i];
        }
        newList[oldlen] = newele;
        return newList;
    }

    //删除String数组中的指定元素
    private String[] deleteNull(String[] strings){
        //定义一个List列表，循环赋值
        ArrayList<String> strList = new ArrayList<>();
        for (int i=0; i<strings.length; i++){
            if (strings[i].length() > 0){
                strList.add(strings[i]);
            }
        }
        return strList.toArray(new String[strList.size()]);
    }

    //对invoke和其所属methodID去重
    public void deleteRepInv(){
        if (this.invoke.length > 1){
            Map<Integer, List<String>> id2inv = new HashMap<>();
            int len = 0;
            for (int i=0; i<this.invoke.length; i++){
                String inv = this.invoke[i];
                int id = this.invokeFromMethod[i];
                if (!id2inv.containsKey(id)){
                    List<String> invs = new ArrayList<>();
                    invs.add(inv);
                    id2inv.put(id,invs);
                    len ++;
                }else if (!id2inv.get(id).contains(inv)){
                    id2inv.get(id).add(inv);
                    len ++;
                }
            }
            String[] newInv = new String[len];
            int[] newIfM = new int[len];
            int ind = 0;
            for (Integer id : id2inv.keySet()){
                for (String inv : id2inv.get(id)){
                    newIfM[ind] = id;
                    newInv[ind] = inv;
                    ind ++;
                }
            }
        }
    }

    //将invoke函数设置为自定义函数，修改invokeFromSys对应位置的值为0
    public void setInvoketoCtm(int index){
        this.invokeFromSys[index] = 0;
    }

    //设置、获取clazz supper method field sys_invoke pkg_invoke ppID
    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getSupper() {
        return supper;
    }

    public void setSupper(String supper) {
        this.supper = supper;
    }

    public String[] getMethod() {
        if (this.method.length > maxlen){
            return method;
        }else{
            int len = 0;
            for (int i=0; i<this.method.length; i++){
                if (this.method[i] == null){
                    len = i;
                    break;
                }
            }
            String[] new_m = new String[len];
            for (int i=0; i<len; i++){
                new_m[i] = this.method[i];
            }
            return new_m;
        }
    }

    public void setMethod(String method, int ind) {
        this.method[ind] = method;
    }

    public String[] getField() {
        if (this.field.length > maxlen){
            return field;
        }else{
            int len = 0;
            for (int i=0; i<this.field.length; i++){
                if (this.field[i] == null){
                    len = i;
                    break;
                }
            }
            String[] new_f = new String[len];
            for (int i=0; i<len; i++){
                new_f[i] = this.field[i];
            }
            return new_f;
        }
    }

    public void setField(String[] field) {
        this.field = field;
    }

    public String[] getInvoke() {
        if (this.invoke.length > maxlen){
            return invoke;
        }else{
            int len = 0;
            for (int i=0; i<this.invoke.length; i++){
                if (this.invoke[i] == null){
                    len = i;
                    break;
                }
            }
            String[] new_inv = new String[len];
            for (int i=0; i<len; i++){
                new_inv[i] = this.invoke[i];
            }
            return new_inv;
        }
    }

    public void setInvoke(String[] invoke) {
        this.invoke = invoke;
    }

    public int[] getInvokeFromMethod() {
        if (this.invokeFromMethod.length > maxlen){
            return invokeFromMethod;
        }else{
            int len = 0;
            for (int i=0; i<this.invokeFromMethod.length; i++){
                if (this.invokeFromMethod[i] == -1){
                    len = i;
                    break;
                }
            }
            int[] new_inv = new int[len];
            for (int i=0; i<len; i++){
                new_inv[i] = this.invokeFromMethod[i];
            }
            return new_inv;
        }
    }

    public void setInvokeFromMethod(int[] invokeFromMethod) {
        this.invokeFromMethod = invokeFromMethod;
    }

    public Integer getPpID() {
        return ppID;
    }

    public void setPpID(Integer ppID) {
        this.ppID = ppID;
    }
}
