package iie.group5.APKprocess;

import com.opencsv.exceptions.CsvValidationException;
import iie.group5.features.StringAndAPI;

import java.io.IOException;
import java.util.*;

public class MatchbyRes {
    private ResProcess apkRes;
    private Map<String, ResProcess> anRes;
    private Map<String, Map<String, Double>> anWeights;
    private float maxMatched;
    private String matchAN;
    private List<String> AdKeys;
    private double maxTI;

    public MatchbyRes(ResProcess apkRes, Map<String, ResProcess> anRes, Map<String, Map<String, Double>> anWeights, double maxTI) throws IOException, CsvValidationException {
        this.apkRes = apkRes;
        this.anRes = anRes;
        this.anWeights = anWeights;
        this.maxMatched = 0;
        StringAndAPI saa = new StringAndAPI();
        this.AdKeys = saa.getStrings();
        this.maxTI = maxTI;
    }

    // thresh is the threshold, pc is the value of omega2/omega1
    public Map<String, Double> compMaxMatch(double thresh, double pc){
        Map<String, Double> result = new HashMap<>();
        for (String anName : this.anRes.keySet()){
            double sim = compSim(this.anRes.get(anName), this.apkRes, pc);
            //template
            if (sim >= thresh){
                // Determine if there are similar prefixes, and take the one with the largest sim value
                String cmp = matchPrefix(result, anName);
                if (cmp == null){
                    result.put(anName,sim);
                }else{
                    double rst_sim = result.get(cmp);
                    if (sim > rst_sim){
                        result.remove(cmp);
                        result.put(anName, sim);
                    }
                }
            }
        }
        return result;
    }

    private double compSim(ResProcess an, ResProcess apk, double pc){
        double f1_sim = fullDomain(an.getAll_an(), apk.getAll_an());
        double final_sim = f1_sim;
        if (an.getPermission().size()>0){
            double f2_sim = fullMatch(an.getPermission(), apk.getPermission());
            final_sim += pc*f2_sim;
        }
        if (an.getAuth().size()>0){
            double f3_sim = fullMatch(an.getAuth(), apk.getAuth());
            final_sim += pc*f3_sim;
        }
        if (an.getIntentFilter().size()>0){
            double f4_sim = fullMatch(an.getIntentFilter(), apk.getIntentFilter());
            final_sim += pc*f4_sim;
        }
        if (an.getActivity().size()>0){
            double f5_sim = weightEdit(an.getActivity(), apk.getActivity());
            final_sim += f5_sim;
        }
        if (an.getService().size()>0){
            double f6_sim = weightEdit(an.getService(), apk.getService());
            final_sim += f6_sim;
        }
        if (an.getReceiver().size()>0){
            double f7_sim = weightEdit(an.getReceiver(), apk.getReceiver());
            final_sim += f7_sim;
        }
        if (an.getDimen().size()>0){
            double f8_sim = ignorePath(an.getHostPKG(), an.getDimen(), apk.getDimen());
            final_sim += pc*f8_sim;
        }
        if (an.getStringn().size()>0){
            double f9_sim = ignorePath(an.getHostPKG(), an.getStringn(), apk.getStringn());
            final_sim += pc*f9_sim;
        }
        if (an.getAttr().size()>0){
            double f10_sim = ignorePath(an.getHostPKG(), an.getAttr(), apk.getAttr());
            final_sim += pc*f10_sim;
        }
        if (an.getStyle().size()>0){
            double f11_sim = ignorePath(an.getHostPKG(), an.getStyle(), apk.getStyle());
            final_sim += pc*f11_sim;
        }
        if (an.getPicNames().size()>0){
            double pic_sim = fullMatch(an.getPicNames(), apk.getPicNames());
            final_sim += pc*pic_sim;
        }
        return final_sim/(4+8*pc);
    }

    // Global Matching
    private double fullDomain(HashSet<String> anFD, HashSet<String> apkFD){
        double result = 0;
        for (String a : apkFD){
            for (String an : anFD){
                if (a.contains(an)){
                    result = 1;
                    break;
                }
            }
        }
        return result;
    }

    // No path matching, requires anIP.size()>0
    private double ignorePath(String anName, HashSet<String> anIP, HashSet<String> apkIP){
        double sumMatched = 0;
        int count = anIP.size();
        for (String an : anIP){
            for (String apk : apkIP){
                if (an.equals(apk)){
                    Map<String, Double> tfidfs = this.anWeights.get(anName);
                    //归一化
                    Double weight = tfidfs.get(an)/this.maxTI;
                    sumMatched += weight;
                    break;
                }
            }
        }
        return sumMatched/count;
    }

    // Full path matching-stable feature, resource pool matching, requires anFM.size()>0
    private double fullMatch(HashSet<String> anFM, HashSet<String> apkFM){
        double result = 0;
        if (apkFM.size() > 0){
            int match_count = 0;
            for (String a : apkFM){
                if (anFM.contains(a)){
                    match_count ++;
                }
            }
            result = exp(match_count);
        }
        return result;
    }

    // Full path matching-variable features, weighted edit distance, find maximum match mean, require anWE.size()>0
    private double weightEdit(HashSet<String> anWE, HashSet<String> apkWE){
        double sumMatch = 0;
        int count = anWE.size();
        for (String an : anWE){
            double maxMatch = 0;
            for (String apk : apkWE){
                if (an.equals(apk) || apk.indexOf(an) == 0){
                    maxMatch = 1;
                    break;
                }else if (an.contains(".") && apk.contains(".")){
                    String[] subAN = an.split("\\.");
                    String[] subAPK = apk.split("\\.");
                    if (subAN.length >= 3 && subAPK.length >= 3){
                        double sim = WE2Sim(subAN, subAPK);
                        if (sim > maxMatch){
                            maxMatch = sim;
                        }
                    }
                }
            }
            sumMatch += maxMatch;
        }
        return sumMatch/count;
    }

    // Customized activation functions
    private double exp(int match_count){
        double result = 0;
        if (match_count > 0){
            result = 1/(1+Math.exp(1-match_count));
        }
        return result;
    }

    // Calculate the string similarity based on the weighted edit distance of two strings with the same prefix
    private double WE2Sim(String[] subAN, String[] subAPK){
        double result = 0;
        // Return 0 if prefix is not equal
        for (int i=0; i<3; i++){
            if (!subAN[i].equals(subAPK[i])){
                return result;
            }
        }
        // Equal prefix but one string without suffix
        if (subAN.length == 3 || subAPK.length == 3){
            result = 1;
            return result;
        }
        // Prefixes are equal, and dynamic planning calculates the weighted edit distance of the suffixes
        int d;
        int anSufLen = subAN.length-3;
        int apkSufLen = subAPK.length-3;
        double[][] M = new double[anSufLen+1][apkSufLen+1];
        M[0][0] = 0;
        for (int i=1; i<anSufLen+1; i++){
            M[i][0] = weight(anSufLen, i-1);
        }
        for (int j=1; j<apkSufLen+1; j++){
            M[0][j] = rho(subAPK[3+j-1]) * weight(apkSufLen, j-1);
        }
        for (int i=1; i<anSufLen+1; i++){
            for (int j=1; j<apkSufLen+1; j++){
                if (subAN[3+i-1].equals(subAPK[3+j-1])){
                    d = 0;
                }else{
                    d = 1;
                }
                M[i][j] = min(M[i][j-1]+rho(subAPK[3+j-1])*weight(apkSufLen,j-1), M[i-1][j]+weight(apkSufLen,j), M[i-1][j-1]+d*rho(subAPK[3+j-1])*weight(apkSufLen,j-1));
            }
        }
        result = (2 - M[anSufLen][apkSufLen])/2;
        return result;
    }

    // Calculate the smallest of the three
    private double min(double a, double b, double c){
        double result;
        if (a < b){
            result = a;
        }else{
            result = b;
        }
        if (result > c){
            result = c;
        }
        return result;
    }

    // Calculation factor rho
    private int rho(String subStr){
        int result = 1;
        for (int i=0; i<AdKeys.size(); i++){
            if (AdKeys.get(i).toLowerCase().equals(subStr.toLowerCase())){
                result = 0;
                break;
            }
        }
        return result;
    }

    // Calculate the weight, len is the suffix length, ind is the string section index, 0 to len-1
    private double weight(int len, int ind){
        double deno = 0;
        for (int i=0; i<len; i++){
            deno += Math.pow(2,i);
        }
        double mole = Math.pow(2, len-ind-1);
        return mole/deno;
    }

    // Returns the string in map with prefix match >= 3
    private String matchPrefix(Map<String, Double> map, String str){
        String ret = null;
        if (str.contains(".")){
            String[] tmp = str.split("\\.");
            String tmp_str = "";
            for (int i=0; i<tmp.length && i<2; i++){
                tmp_str += tmp[i] + ".";
            }
            str = tmp_str.substring(0, tmp_str.length()-1);
        }
        for (String m : map.keySet()){
            if (m.indexOf(str) == 0){
                ret = m;
                break;
            }
        }
        return ret;
    }

}
