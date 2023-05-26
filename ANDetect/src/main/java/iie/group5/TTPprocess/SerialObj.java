package iie.group5.TTPprocess;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import iie.group5.structure.Module;
import iie.group5.structure.PackagePoint;
import iie.group5.structure.Smali;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//序列化Module对象们，并存储在resources/features文件夹下
public class SerialObj {
    private Map<String, List<Module>> modClass;
    private Kryo kryo;
    private List<String> serPath;

    public SerialObj(){
        this.modClass = new HashMap<>();
        this.kryo = new Kryo();
        this.kryo.register(PackagePoint.class, 1);
        this.kryo.register(Smali.class, 2);
        this.kryo.register(Module.class, 3);
        this.kryo.register(ArrayList.class, 4);
        this.kryo.register(String[].class,5);
        this.kryo.register(int[].class,6);
        this.serPath = new ArrayList<>();
    }

    public SerialObj(List<Module> modules){
        this.modClass = new HashMap<>();
        for (Module m : modules){
            //把name限制在两个.之内
            String name = m.getName();
            if (m.getName().contains(".")){
                String[] tmp = m.getName().split("\\.");
                if (tmp.length > 3){
                    name = tmp[0];
                    for (int i=1; i<3; i++){
                        name += "." + tmp[i];
                    }
                }
            }
            if (!this.modClass.containsKey(name)){
                List<Module> newList = new ArrayList<>();
                newList.add(m);
                this.modClass.put(name, newList);
            }else{
                this.modClass.get(name).add(m);
            }
        }
        this.kryo = new Kryo();
        this.kryo.register(PackagePoint.class, 1);
        this.kryo.register(Smali.class, 2);
        this.kryo.register(Module.class, 3);
        this.kryo.register(ArrayList.class, 4);
        this.kryo.register(String[].class,5);
        this.kryo.register(int[].class,6);
    }

    public void outputSER() throws IOException {
        //创建序列化对象，根据模块名称把modules保存在不同文件中
        String prefix = "resources/features/ser/";
        for (String name : this.modClass.keySet()){
            String suffix = name + ".ser";
            writeIn(prefix+suffix, this.modClass.get(name));
        }
    }

    private void writeIn(String path, List<Module> modules) throws IOException {
        //用kryo序列化
        FileOutputStream fos = new FileOutputStream(path);
        Output output = new Output(fos);
        for (Module m : modules){
            this.kryo.writeClassAndObject(output, m);
        }
        output.flush();
        output.close();
    }

    public void inputSER() throws IOException{
        //读取序列化对象
        String prefix = "resources/features/ser/";
        File file = new File(prefix);
        File [] subFiles = file.listFiles();
        if (subFiles != null){
            for (File f : subFiles){
                //找到每一个ser文件并反序列化
                String ser = f.getPath();
                try {
                    readIn(ser);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                }catch (KryoException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void inputSERsingle(String serName) throws IOException{
        //读取序列化对象
        String path = "resources/features/ser/" + serName + ".ser";
        readIn(path);
    }

    private void readIn(String path) throws IOException{
        //获取ser文件名
        int inds = path.lastIndexOf("/");
        int inde = path.lastIndexOf(".ser");
        String name = path.substring(inds+1, inde);
        //用kryo序列化
        FileInputStream fis = new FileInputStream(path);
        Input input = new Input(fis);
        List<Module> modList = new ArrayList<>();
        while (true){
            Module mod = (Module) this.kryo.readClassAndObject(input);
            mod.getFull();
            modList.add(mod);
            if (input.position() == input.limit()){
                break;
            }
        }
        this.modClass.put(name, modList);
        input.close();
    }

    public List<Module> getModClass() {
        List<Module> result = new ArrayList<>();
        for (List<Module> tmp : this.modClass.values()){
            result.addAll(tmp);
        }
        return result;
    }

    public List<String> getSerPath() {
        return serPath;
    }

    public void setSerPath() {
        String prefix = "resources/features/ser/";
        File file = new File(prefix);
        File [] subFiles = file.listFiles();
        if (subFiles != null){
            for (File f : subFiles){
                //找到每一个ser文件并反序列化
                String ser = f.getName();
                this.serPath.add(ser.replace(".ser",""));
            }
        }
    }

    public void clearModClass(){
        this.modClass = new HashMap<>();
    }
}
