package iie.group5.APKprocess;

import com.android.dx.command.dexer.Main;

import java.io.File;
import java.io.IOException;

public class JarToSmalibyDX{
    //输入指定文件，输出到同名的dex文件中
    private String input;

    public JarToSmalibyDX(String input) {
        this.input = input;
    }

    public void trans2dex() throws IOException, InterruptedException {
        int ind = input.lastIndexOf("/");
        String dex = input.substring(0, ind);
        File file = new File(dex);
        file.mkdirs();
        String ret = dex + "/classes.dex";
        String[] args = new String[]{"--output="+ret,"--min-sdk-version=26","--no-warning", this.input};
        Main.main(args);
    }

}
