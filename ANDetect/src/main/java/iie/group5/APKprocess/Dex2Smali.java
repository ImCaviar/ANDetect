package iie.group5.APKprocess;

import org.jf.baksmali.Baksmali;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.MultiDexContainer;

import java.io.File;
import java.io.IOException;

//输入classes.dex输出到指定文件夹
public class Dex2Smali {
    private File inputDex;
    private File outputSmali;
    private Integer mApiLevel;

    public Dex2Smali(String inputDex, String outputSmali) {
        this.inputDex = new File(inputDex);
        this.outputSmali = new File(outputSmali);
        this.mApiLevel = 0;
        transDex2Smali();
    }

    private void transDex2Smali(){
        try {
            BaksmaliOptions options = new BaksmaliOptions();
            options.deodex = false;
            options.implicitReferences = false;
            options.parameterRegisters = true;
            options.localsDirective = true;
            options.sequentialLabels = true;
            options.debugInfo = true;
            options.codeOffsets = false;
            options.accessorComments = false;
            options.registerInfo = 0;
            options.inlineResolver = null;
            int jobs = Runtime.getRuntime().availableProcessors();
            if (jobs > 6) {
                jobs = 6;
            }
            MultiDexContainer<? extends DexBackedDexFile> container = DexFileFactory.loadDexContainer(this.inputDex, this.mApiLevel > 0 ? Opcodes.forApi(this.mApiLevel) : null);
            MultiDexContainer.DexEntry dexEntry = container.getEntry((String)container.getDexEntryNames().get(0));

            assert dexEntry != null;

            DexBackedDexFile dexFile = (DexBackedDexFile)dexEntry.getDexFile();
            Baksmali.disassembleDexFile(dexFile, this.outputSmali, jobs, options);
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}
