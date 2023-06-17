package iie.group5.structure;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

//Smali file structure
public class Smali implements Serializable {
    //class name
    private String clazz;
    //super
    private transient String supper;
    // Each item in the method array is composed of access rights, a list of parameter types, and return types
    private String[] method;
    // Each item in the field array is stitched together by whether it is static, access rights, and field type
    private String[] field;
    // Functions called in smali files, without distinguishing between system functions and custom functions
    private transient String[] invoke;
    // The method index from which the method called in the smali file is derived
    private transient int[] invokeFromMethod;
    // Whether the method called in the smali file is from a system function 1: from a system function; 0: a custom function
    private transient int[] invokeFromSys;
    // The leaf node ID where the smali file is located
    private Integer ppID;

    private final int maxlen = 20;
    // Access Permissions
    private final String[] access = new String[]{"public","protected","default","private"};
    // Type descriptors
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

        Arrays.fill(invokeFromMethod, -1);
        Arrays.fill(invokeFromSys, 1);
    }

    // Analyze the smali file according to its path and generate method field sys_invoke pkg_invoke respectively
    public void analyzeSmali(String filePath){
        try {
            List<String> allLines = Files.readAllLines(Paths.get(filePath));
            int methodInd = 0;
            for (String line: allLines){
                // Split the string with one or more spaces and remove the empty elements
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

    // Handling class names/super
    private String analyzeCS(String[] elements){
        String result = elements[elements.length-1];
        result = result.replaceFirst("L","");
        result = result.replaceAll(";","");
        return result;
    }

    // Handles field fields, returning "field access rights field type"
    private String analyzeField(String[] elements){
        String result = "";
        int inda = getIndex(this.access, elements[1]);
        if (inda == -1){
            result += "2 ";
        }else{
            result += inda + " ";
        }
        if (getIndex(elements, "static") != -1){
            result += "s ";
        }
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

    // Processes the method field, returning "Class access rights List of parameter types Return type"
    private String analyzeMethod(String[] elements){
        String result = "";
        int inda = getIndex(this.access, elements[1]);
        if (inda == -1){
            result += "2 ";
        }else{
            result += inda + " ";
        }
        if (getIndex(elements, "static") != -1){
            result += "s ";
        }
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

    // Handles invoke function calls in method, returning "calling function package name -> function name"
    private String analyzeInvoke(String[] elements){
        String[] tmp = elements[elements.length-1].split("->");
        String pkg = tmp[0];
        String func = tmp[1];

        int ind0 = pkg.indexOf('L');
        int ind3 = pkg.indexOf(";");
        if (ind0 == -1 && ind3 == -1){
            return "";
        }
        pkg = pkg.substring(ind0+1, ind3);

        int ind1 = func.indexOf(">");
        int ind2 = func.indexOf("(");
        func = func.substring(ind1+1, ind2);
        return pkg + "->" + func;
    }

    // Splitting the type in a string
    private String getTypes(String type) {
        String result = "";
        if (type.contains("/")) {
            int ind = type.indexOf("L");
            if (ind == 0) {

                result = type.replaceFirst("L", "") + " ";
                return result;
            } else if (ind > 0 && ind < type.indexOf("/")) {

                for (int i = 0; i < ind; i++) {
                    String sub = type.substring(i, i + 1);
                    if (getIndex(this.types, sub) != -1) {
                        result += sub + " ";
                    }
                }
                result += type.substring(ind+1) + " ";
                return result;
            }else{
                return type + " ";
            }
        }else{
            if (type.contains("L") && type.indexOf("L") == 0){
                type = type.substring(1);
            }
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

    // Find if an element is in the String list, return the index if it is, or -1 if it is not.
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

    // Add a string to the String array
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

    // Open a new String array
    private String[] newString(String[] oldList, String newele){
        int oldlen = oldList.length;
        String[] newList = new String[oldlen+1];
        for (int i=0; i<oldlen; i++){
            newList[i] = oldList[i];
        }
        newList[oldlen] = newele;
        return newList;
    }

    // Adding numbers to an int array
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

    // New int arrays
    private int[] newInt(int[] oldList, int newele){
        int oldlen = oldList.length;
        int[] newList = new int[oldlen+1];
        for (int i=0; i<oldlen; i++){
            newList[i] = oldList[i];
        }
        newList[oldlen] = newele;
        return newList;
    }

    // Delete the specified element from the String array
    private String[] deleteNull(String[] strings){
        ArrayList<String> strList = new ArrayList<>();
        for (int i=0; i<strings.length; i++){
            if (strings[i].length() > 0){
                strList.add(strings[i]);
            }
        }
        return strList.toArray(new String[strList.size()]);
    }

    // De-duplicate the invoke and the methodID it belongs to
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

    // Set the invoke function to a custom function and change the value of the corresponding position of invokeFromSys to 0
    public void setInvoketoCtm(int index){
        this.invokeFromSys[index] = 0;
    }

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
