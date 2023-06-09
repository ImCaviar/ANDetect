package iie.group5.features;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Retrieve the items in final_API_features.csv and final_string_features.csv.
public class StringAndAPI {
    private List<String> strings;
    private List<String> APIs;

    public StringAndAPI() throws IOException, CsvValidationException {
        this.strings = new ArrayList<>();
        this.APIs = new ArrayList<>();
        genStrings();
        genAPIs();
    }

    private void genStrings() throws IOException, CsvValidationException {
        CSVReader reader = new CSVReader(new FileReader("src/main/resources/features/final_string_features.csv"));
        String[] nextLine;
        while ((nextLine = reader.readNext()) !=null){
            this.strings.add(nextLine[0]);
        }
    }

    private void genAPIs() throws IOException, CsvValidationException {
        CSVReader reader = new CSVReader(new FileReader("src/main/resources/features/final_API_features.csv"));
        String[] nextLine;
        while ((nextLine = reader.readNext())!=null){
            this.APIs.add(nextLine[0]);
        }
    }

    public List<String> getStrings() {
        return strings;
    }

    public List<String> getAPIs() {
        return APIs;
    }
}
