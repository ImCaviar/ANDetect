package iie.group5.APKprocess;

import com.android.tools.r8.D8;

import java.io.File;

public class JarToSmalibyD8{
    //输入指定文件，输出到同名的dex文件中
    private String input;

    public JarToSmalibyD8(String input) {
        this.input = input;
    }

    public void trans2dex(){
        int ind = input.lastIndexOf("/");
        String dex = input.substring(0, ind);
        File file = new File(dex);
        file.mkdirs();
        String[] args = new String[]{this.input, "--output", dex};
        D8.main(args);
    }
}
