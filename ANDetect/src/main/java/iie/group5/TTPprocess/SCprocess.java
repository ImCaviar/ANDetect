package iie.group5.TTPprocess;

import com.opencsv.exceptions.CsvValidationException;
import iie.group5.features.JARnames;
import iie.group5.features.StringAndAPI;
import iie.group5.structure.Module;
import iie.group5.structure.PackagePoint;
import iie.group5.structure.Smali;

import java.io.*;
import java.util.*;

// Extract a total of five string features and API features from all third-party libraries and generate five csv tables
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

    // Given a module, output the five features of it
    public List<float[]> outputSA(Module module){
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
}
