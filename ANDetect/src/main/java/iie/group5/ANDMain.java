package iie.group5;

import iie.group5.APKprocess.*;
import iie.group5.TTPprocess.SCprocess;
import iie.group5.TTPprocess.SerialObj;
import iie.group5.TTPprocess.XGBmodel;
import iie.group5.features.ResSetsAndPool;
import iie.group5.structure.Module;
import iie.group5.structure.PackagePoint;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;


// Enter the path to an APK and analyze if it contains AN libraries
public class ANDMain {
    public static void main(String[] args) throws Exception{
        File file = new File("E:\\2022HOME\\experiments\\data\\experiment\\errors\\B35C2D14FF191C37AD725653FA71CE58E28E51F1712D0AFDF5BDBFB96A2661A7.apk");
        String output = "E:\\2022HOME\\experiments\\data\\experiment\\errors\\test.txt";

        long startT = System.currentTimeMillis();
//        File [] subFiles = file.listFiles();
        File [] subFiles = new File[]{file};
        for (File f: subFiles){
            if (f.getName().endsWith(".apk")){
                System.out.println(f.getName());
                String path = f.getPath();
                try {
                    parseAPK(path, output);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        long endT = System.currentTimeMillis();
        System.out.printf("AND-NEN analyzing takes %d s......\n", (endT-startT)/10000);

    }

    // Parse APK
    private static void parseAPK(String path, String output) throws Exception {
        // Output the result to output
        File f = new File(output);
        FileOutputStream fos = new FileOutputStream(f, true);
        OutputStreamWriter osw = new OutputStreamWriter(fos);

        String apkFile = path;
        // Unzip the target folder, or delete it if it already exists
        String apkUnzip = apkFile.replace(".apk","\\");
        File tmpF = new File(apkUnzip);
        if (tmpF.exists()){
            FileUtils.deleteQuietly(tmpF);
        }

        // Detecting decompile time
        long startT = System.currentTimeMillis();
        // Decompile APK by Apktool 2.7.0
        APK2dir a2d = new APK2dir(path);
        a2d.decodeAPK();

        System.out.println("Decode APK by Apktool 2.7.0.");

        // Parsing resource files
        String AM_path = a2d.getAMPath();
        ResProcess resProcess = new ResProcess();
        String pub_path = a2d.getPubPath();
        String output_path = a2d.getOutputDir();
        // Extract resource file features and get the host application package name
        String hostPKG = resProcess.parseRes(AM_path, pub_path, output_path);
        long endT = System.currentTimeMillis();
        System.out.printf("APK parsing takes %d s......\n", (endT-startT)/1000);

        // Perform module decoupling and count the time required to decouple
        startT = System.currentTimeMillis();
        List<String> smaliRoot = a2d.getSmaliPath();
        MDecouple mDecouple = new MDecouple(smaliRoot);
        mDecouple.decoupling(hostPKG);
        osw.write(path+"\n");
        osw.write("[Encrypted APK]: ");
        if (!mDecouple.isShelled()){
            osw.write("Not Encrypted!\n");
            // Decoupling
            endT = System.currentTimeMillis();
            System.out.printf("Module Decoupling takes %d s......\n", (endT-startT)/1000);
            // AN identification for each module
            System.out.println("Recognize Ad Networks Libraries...");
            List<Module> modules = mDecouple.getModules();
            SerialObj sobj = new SerialObj();
            sobj.setSerPath();
            osw.write("[ANDetect-N Result]: \n");
            // No duplicate identified AN libraries are allowed for each APK
            List<String> hasMatched = new ArrayList<>();
            for (Module m : modules){
                // Mismatch empty package name
                if (m.getName().equals("")){
                    continue;
                }
                // Find the candidate set, no candidate set and then determine whether it is an advertising library (new library)
                // Try to match the module name to the ser name, candidate >= 1 ser with similar name, read the Module in it to match
                sobj.inputSER();
                List<Module> candiM = sobj.getModClass();
                List<Module> candi = getCandiAN(candiM, m);
                if (candi.size() > 0){
                    // Read the Modules in candi and match them, select the version number with the largest match and output it
                    // Compress the system type of m. The threshold is set to 0.3
                    m.simpleType();
                    double maxTMP = 0;
                    String maxFirstV = "";
                    List<String> matchedFirst = new ArrayList<>();
                    // For AN with multiple version numbers, the first round compares the most similar version of "package name + version number X.?.?" and then compare the most similar version of "package name + version number X.Y.Z" in the second round.
                    for (Module cm : candi){
                        if (cm.getVersion().contains(".")){
                            String firstELE = cm.getName() + "_" + cm.getVersion().split("\\.")[0];
                            if (!matchedFirst.contains(firstELE)){
                                MatchbySC msc = new MatchbySC(cm,m);
                                double pctM = msc.compAll();
                                if (pctM > maxTMP){
                                    maxTMP = pctM;
                                    maxFirstV = firstELE;
                                }
                                matchedFirst.add(firstELE);
                            }
                        }
                    }

                    double maxMatch = 0.3;
                    String maxM = "";
                    String maxV = "";
                    for (Module cm : candiM){
                        String firstELE = "";
                        if (cm.getVersion().contains(".")){
                            firstELE = cm.getName() + "_" + cm.getVersion().split("\\.")[0];
                        }
                        if (!maxFirstV.equals("") && !firstELE.equals(maxFirstV)){
                            continue;
                        }
                        MatchbySC msc = new MatchbySC(cm,m);
                        double pctM = msc.compAll();
                        if (pctM > maxMatch){
                            maxMatch = pctM;
                            maxM = cm.getName();
                            maxV = cm.getVersion();
                        }
                    }
                    // Output the most matching version number, not if the version number is empty
                    if (!maxM.equals("") && !hasMatched.contains(maxM)){
                        if (!maxV.equals("")){
                            osw.write(maxM + " == version =>" + maxV + "\n");
                        }else{
                            osw.write(maxM + "\n");
                        }
                        hasMatched.add(maxM);
                    }
                    // Clear the mods for reading ser
                    sobj.clearModClass();
                }else{
                    // Set string class matching threshold
                    boolean isAN = recogAN(m, 3);
                    if (isAN  && !hasMatched.contains(m.getName())){
                        // There is no candidate AN, which means it is an off-whitelist package
                        osw.write(m.getName().replaceAll("/", ".") + " => New Ad Lib\n");
                        hasMatched.add(m.getName());
                    }
                }
            }
        }else {
            // Resource matching for encrypted APKs
            osw.write("Encrypted!\n");
            ResSetsAndPool rsap = new ResSetsAndPool();
            double maxTI = rsap.getMaxTI();
            Map<String, ResProcess> an2ResLib = rsap.getAnName2ResLib();
            Map<String, Map<String, Double>> an2tfidf = rsap.getAn2tfidf();
            MatchbyRes mr = new MatchbyRes(resProcess, an2ResLib, an2tfidf, maxTI);
            // Set different thresh and pc to choose the best hyperparameters
            double thresh = 0.1;
            double pc = 0.1;
            Map<String, Double> ANs = mr.compMaxMatch(thresh, pc);
            // If the ANs have the same prefix, the highest score is obtained
            endT = System.currentTimeMillis();
            System.out.printf("Res analyzing takes %d s......\n", (endT-startT)/1000);
            osw.write("[ANDetect-E Result]: \n");
            if (ANs.size() == 0){
                osw.write("No matched Ad Networks Lib in whitelist by Res.\n");
            }else{
            osw.write("Match these Ad Networks Libs in whitelist by Res:\n");
                for (String r : ANs.keySet()){
                    osw.write(r + "\n");
                }
            }
            System.out.println("Result has been written in " + output);
            osw.write("\n");
        }
        osw.close();
    }

    // Enter a submodule of type Module to identify if it is AN, true means AN
    private static boolean recogAN(Module subM, int str_thresh) throws Exception {
        SCprocess scp = new SCprocess();
        List<float[]> arrays = scp.outputSA(subM);
        XGBmodel xgbM = new XGBmodel();
        float[] results = xgbM.classify(arrays.get(2),arrays.get(3),arrays.get(4));
        // The closer the result is to 1, the higher the probability of AN; if one of the results is greater than 0.9 and there is an item greater than 0 in arrays.get(0), then it is determined to be AN.
        boolean API = true;
        boolean str = false;
        for (int i=0; i<results.length; i++){
            if (results[i] < 0.9){
                API = false;
            }
        }
        int count_str = 0;
        for (int i=0; i<arrays.get(0).length; i++){
            if (arrays.get(0)[i] > 0){
                count_str ++;
            }
        }
        if (count_str >= str_thresh){
            str = true;
        }
        return API && str;
    }

    // Select the set of ad network name candidates in ANs that are most similar to pkg, possibly empty; if there is packet name confusion, find the set of candidates that satisfy the constraint
    private static List<Module> getCandiAN(List<Module> ANs, Module module){
        List<Module> candiAN = new ArrayList<>();
        // Find package names greater than 2 items in the module to jointly match candidate libraries
        String pkg2 = module.getName().replaceAll("/",".");
        // Is it confusing
        boolean isConf = isConfuse(pkg2);
        if (isConf){
            addCandiPKG(candiAN, ANs, module);
        }else{
            addCandiAN(candiAN, ANs, pkg2);
            for (PackagePoint pp : module.getPackagePoints().values()){
                String pkgName = pp.getPkgName().replaceAll("/",".");
                String tmp = pkgName.replaceAll("\\.","");
                if ((pkgName.length() - tmp.length()) >= 2){
                    addCandiAN(candiAN, ANs, pkgName);
                }
            }
        }
        return candiAN;
    }

    // Adding candidate AN libraries for obfuscation pkg
    private static void addCandiPKG(List<Module> candiAN, List<Module> ANs, Module module){
        for (Module an : ANs){
            if (an.getSmalis().size() > module.getSmalis().size()){
                continue;
            }else if (module.getPackagePoints().keySet().size() > 1){
                // Find all leaf node IDs of AN library
                List<Integer> ANleaves = new ArrayList<>();
                for (PackagePoint pp : an.getPackagePoints().values()){
                    if (pp.getChildrenID().size() == 0){
                        ANleaves.add(pp.getId());
                    }
                }
                boolean flag = true;
                for (PackagePoint pp : module.getPackagePoints().values()){
                    boolean tmpFlag = true;
                    if (pp.getChildrenID().size() == 0){
                        List<PackagePoint> pkgLeaf = module.findTwigbyID(pp.getId());
                        Iterator<Integer> iterator = ANleaves.iterator();
                        while (iterator.hasNext()){
                            Integer anl = iterator.next();
                            List<PackagePoint> ANLeaf = an.findTwigbyID(anl);
                            if (pkgLeaf.size() == ANLeaf.size()){
                                for (int i=0; i<pkgLeaf.size(); i++){
                                    if (module.findSmalibyID(pkgLeaf.get(i).getId()).size() > an.findSmalibyID(ANLeaf.get(i).getId()).size()){
                                        tmpFlag = false;
                                        break;
                                    }
                                }
                            }else{
                                tmpFlag = false;
                            }
                            if (tmpFlag){
                                iterator.remove();
                                break;
                            }
                        }
                        if (!tmpFlag){
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag){
                    candiAN.add(an);
                }
            }else{
                // Package Flattening
                candiAN.add(an);
            }
        }
    }

    // Add candidate packages and select the most matching AN library
    private static void addCandiAN(List<Module> candiAN, List<Module> ANs, String pkgName){
        double max = 0;
        Module maxS = null;
        for (Module aa : ANs){
            String an = aa.getName();
            double pctM = compare2pkgs(an, pkgName);
            if (pctM == 1.0){
                candiAN.add(aa);
            } else if (pctM > max && pctM >= 0.5){
                max = pctM;
                maxS = aa;
            }
        }
        if (candiAN.size() == 0 && max >= 0.5){
            candiAN.add(maxS);
        }
    }

    // Compare two package names to see if they match, with package names splitting by '.'
    private static double compare2pkgs(String pkg1, String pkg2){
        double result = 0;
        // If the 2nd string element matches, it is considered to have a 1/2 probability of matching
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
        }else if (pkg1.equals(pkg2)){
            result = 1;
        }
        return result;
    }

    // Determine if there is package name confusion
    private static boolean isConfuse(String pkgName){
        boolean flag = true;
        if (pkgName.contains("/")){
            String[] splitName = pkgName.split("/");
            for (int i=0; i<splitName.length; i++){
                if (splitName[i].length()>1){
                    flag = false;
                }
            }
        }else if (pkgName.length()>1){
            flag = false;
        }
        return flag;
    }
}
