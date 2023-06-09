package iie.group5.features;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Read SDK_name and aar_jar_path from AD_Network_info.csv
public class JARnames {
    private Map<String, String> path2pkg;

    public JARnames() throws IOException, CsvValidationException {
        this.path2pkg = new HashMap<>();
        genMap();
    }

    public void genMap() throws IOException, CsvValidationException {
        CSVReader reader = new CSVReader(new FileReader("src/main/resources/AD_Network_info.csv"));
        String[] nextLine;
        while ((nextLine = reader.readNext()) !=null){
            this.path2pkg.put(nextLine[1], nextLine[0]);
        }
    }

    public Map<String, String> getPath2pkg() {
        return path2pkg;
    }
}
