package iie.group5.features;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import iie.group5.APKprocess.ResProcess;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResSetsAndPool {
    private Map<String, ResProcess> anName2ResLib;
    // Record only the tf-idf values without path matching features
    private Map<String, Map<String, Double>> an2tfidf;
    // where the maximum value, used to normalize tf-idf
    private double maxTI;

    public ResSetsAndPool() throws IOException, CsvValidationException {
        anName2ResLib = new HashMap<>();
        an2tfidf = new HashMap<>();
        maxTI = 0;
        genResSets();
        genResPool();
    }

    private void genResSets() throws IOException, CsvValidationException {
        CSVReader reader = new CSVReader(new FileReader("src/main/resources/features/res_feature_sets.csv"));
        // The first line is the header
        String[] nextLine = reader.readNext();
        while ((nextLine = reader.readNext()) !=null){
            String anName = nextLine[0];
            String path = nextLine[1];
            String value = nextLine[2];
            Double weight = Double.parseDouble(nextLine[3]);
            processSet(anName, path, value, weight);
            if (weight.floatValue() > maxTI){
                maxTI = weight;
            }
        }
    }

    private void genResPool() throws IOException, CsvValidationException {
        CSVReader reader = new CSVReader(new FileReader("src/main/resources/features/res_pic_pool.csv"));
        // The first line is the header
        String[] nextLine = reader.readNext();
        while ((nextLine = reader.readNext()) !=null){
            String anName = nextLine[0];
            String picName = nextLine[2];
            if (!anName2ResLib.containsKey(anName)){
                ResProcess resP = new ResProcess();
                resP.addPicNames(picName);
                anName2ResLib.put(anName, resP);
            }else{
                anName2ResLib.get(anName).addPicNames(picName);
            }
        }
    }

    private void processSet(String anName, String path, String value, Double weight) {
        ResProcess resP;
        Map<String, Double> tfidfs;
        if (!anName2ResLib.containsKey(anName)) {
            resP = new ResProcess();
            resP.setHostPKG(anName);
            this.anName2ResLib.put(anName, resP);
            tfidfs = new HashMap<>();
            this.an2tfidf.put(anName, tfidfs);
        } else {
            resP = this.anName2ResLib.get(anName);
            tfidfs = this.an2tfidf.get(anName);
        }
        switch (path) {
            case "AndroidManifest.xml_manifest_package":
                resP.addAll_an(value);
                break;
            case "AndroidManifest.xml_manifest uses-permission_android:name":
                resP.addPermission(value);
                break;
            case "AndroidManifest.xml_manifest application provider_android:authorities":
                resP.addAuth(value);
                break;
            case "AndroidManifest.xml_manifest application receiver intent-filter action_android:name":
                resP.addIntentFilter(value);
                break;
            case "AndroidManifest.xml_manifest application activity_android:name":
                resP.addActivity(value);
                break;
            case "AndroidManifest.xml_manifest application service_android:name":
                resP.addService(value);
                break;
            case "AndroidManifest.xml_manifest application receiver_android:name":
                resP.addReceiver(value);
                break;
            case "res\\values\\values.xml_resources dimen_name":
                resP.addDimen(value);
                tfidfs.put(value, weight);
                break;
            case "res\\values\\values.xml_resources string_name":
                resP.addStringn(value);
                tfidfs.put(value, weight);
                break;
            case "res\\values\\values.xml_resources declare-styleable attr_name":
                resP.addAttr(value);
                tfidfs.put(value, weight);
                break;
            case "res\\values\\values.xml_resources style_name":
                resP.addStyle(value);
                tfidfs.put(value, weight);
                break;
        }
    }

    public Map<String, ResProcess> getAnName2ResLib() {
        return anName2ResLib;
    }

    public Map<String, Map<String, Double>> getAn2tfidf() {
        return an2tfidf;
    }

    public double getMaxTI() {
        return maxTI;
    }
}
