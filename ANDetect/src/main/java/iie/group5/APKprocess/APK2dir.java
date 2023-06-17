package iie.group5.APKprocess;

import brut.apktool.Main;
import brut.common.BrutException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class APK2dir {
    private String apkPath;
    private String outputDir;
    private List<String> smaliPath;
    private String AMPath;
    private String pubPath;

    public APK2dir(String apkPath) {
        this.apkPath = apkPath;
        this.outputDir = apkPath.replace(".apk","");
        this.smaliPath = new ArrayList<>();
        this.AMPath = "";
        this.pubPath = "";
    }

    public void decodeAPK() throws BrutException, IOException {
        String[] args = new String[]{"d",this.apkPath,"-o",outputDir};
        Main.main(args);
        // If there is more than one smali under output, put all these smali files into the smali folder
        File op = new File(outputDir);
        File[] subfiles = op.listFiles();
        if (subfiles!=null){
            for (File f : subfiles){
                String fname = f.getName();
                if (f.isDirectory() && fname.contains("smali")){
                    smaliPath.add(f.getPath());
                }
            }
        }
        // If there is an AM.xml file and a public.xml file, return the path, otherwise return empty
        AMPath = outputDir + "\\AndroidManifest.xml";
        File tmp = new File(AMPath);
        if (!tmp.exists()){
            AMPath = "";
        }
        pubPath = outputDir + "\\res\\values\\public.xml";
        tmp = new File(pubPath);
        if (!tmp.exists()){
            pubPath = "";
        }
    }

    public String getOutputDir() {
        return outputDir;
    }

    public List<String> getSmaliPath() {
        return smaliPath;
    }

    public String getAMPath() {
        return AMPath;
    }

    public String getPubPath() {
        return pubPath;
    }
}
