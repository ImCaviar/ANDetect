package iie.group5.APKprocess;

import iie.group5.structure.Graph;
import iie.group5.structure.Module;
import iie.group5.structure.PackagePoint;
import iie.group5.structure.Smali;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//模块解耦
public class MDecouple {
    //smali文件路径
    private File file;
    //初始模块
    private Module initModule;
    //解耦后的模块列表
    private List<Module> modules;
    //PackagePoint节点计数器
    private static Integer ppCount = 0;
    //PPID<->PP
    private Map<Integer, PackagePoint> packagePoints;
    //全局变量记录smali文件下的所有节点
    private List<Smali> smalis;
    //PPID<->社区ID
    private Map<Integer, Integer> PPID2commID;
    //是否加固
    private boolean shelled;


    //构造函数
    public MDecouple(String filePath) {
        this.file = new File(filePath);
        this.modules = new ArrayList<>();
        this.packagePoints = new HashMap<>();
        this.smalis = new ArrayList<>();
        this.PPID2commID = new HashMap<>();
        this.shelled = true;
    }

    //可获取解耦后的模块
    public List<Module> getModules() {
        return modules;
    }

    //模块解耦
    public void decoupling(String hostPKG){
        //遍历file下的所有文件夹和文件
        System.out.println("Building package TREEs and Analyzing Smali files!");
        //去除最后一个.后的字符串后匹配效果会更好
        int ind = hostPKG.lastIndexOf(".");
        hostPKG = hostPKG.substring(0, ind);
        String host = hostPKG.replace(".", "/");

        findFiles(this.file, null, host);
        if (this.shelled){
            //被加固，直接返回，不解耦
            return;
        }

        //初始化社区
        Graph community = new Graph();
        //根据每个smali文件中调用的自定义函数，生成函数调用图
        genGraphbyPkg(community);
        //构建完所有的包节点和smali节点、生成图后生成初始化的Module
        this.initModule = new Module(this.packagePoints, this.smalis);

        //Louvain算法进行社区划分
        System.out.println("Module Decoupling by Louvain!");
        community.singleLouvain();
//        community.outputComm();
        //自底向上合并社区
        mergeComm(community);
//        outputComm();
        //分隔子模块
        splitSubModule();
        outputNoName();
    }

    //遍历file下的所有文件，排除hostPKG中的文件夹和文件
    private void findFiles(File file, Integer parentId, String host){
        if (file != null && file.exists()){
            String filePath = file.getPath();
            if (filePath.contains(host)){
                this.shelled = false;
                return;
            }
            String fileName = file.getName();
            if (file.isDirectory()){
                //file是文件夹，构建节点
                Integer id = ppCount ++;
                PackagePoint packagePoint = new PackagePoint(id, fileName, parentId);
                this.packagePoints.put(id, packagePoint);
                File [] subFiles = file.listFiles();
                if (subFiles != null){
                    for (File f : subFiles){
                        //递归，本节点的id作为下一节点的parentId
                        findFiles(f, id, host);
                    }
                }
            }else if (fileName.endsWith(".smali")){
                //file是smali文件，构建Smali类
                Smali smali = new Smali(parentId);
                //!!!解析每个smali文件!!!
                smali.analyzeSmali(filePath);
                this.smalis.add(smali);
            }
        }
    }

    //用包名作为图节点
    private void genGraphbyPkg(Graph community){
        System.out.println("Add Vertices ......");
        for (Smali i : this.smalis){
            //为每个包生成一个节点，不考虑重复包，不考虑空包
            Integer ppID = i.getPpID();
            String pkg = getPkg(i.getClazz());
            if (!community.inGraph(pkg) && !pkg.equals("")){
                community.insertVertex(pkg);
            }
            //为smali的父节点设置完整包名
            PackagePoint pp = this.packagePoints.get(ppID);
            if (pp.getPkgName().equals("")){
                pp.setPkgName(pkg);
            }
        }
        System.out.println("Generating Call Graph ......");
        for (Smali i: this.smalis){
            //提取包中每个类继承关系，生成边，继承关系边权重为5
            String supPkg = getPkg(i.getSupper());
            String pkg = getPkg(i.getClazz());
            if (community.inGraph(supPkg) && !supPkg.equals(pkg) && !pkg.equals("")){
                community.insertEdge(supPkg, pkg, 5);
            }
            //提取类间函数调用关系，生成边，计次生成边权重
            String[] invokes = i.getInvoke();
            for (int ind=0; ind<invokes.length; ind++){
                if (invokes[ind] != null){
                    String classn = invokes[ind].split("->")[0];
                    if (classn.contains("[")){
                        classn = classn.replace("[", "");
                    }
                    //只有当该smali调用的是自定义函数，且调用的不是类中定义的函数时才添加边
                    classn = getPkg(classn);
                    if (community.inGraph(classn) && !pkg.equals("")){
                        i.setInvoketoCtm(ind);
                        if (!classn.equals(pkg)){
                            community.insertEdge(pkg, classn, 1);
                        }
                    }
                }
            }
        }
    }

    //获取类所在包名
    private String getPkg(String clazz){
        //如果没有包名则返回空
        String result = "";
        if (clazz.contains("/")){
            int ind = clazz.lastIndexOf("/");
            result = clazz.substring(0, ind);
        }
        return result;
    }

    //自底向上合并社区
    private void mergeComm(Graph community){
        //根据community的结果初始化每个PP节点的社区
        for (Integer i : this.packagePoints.keySet()){
            String pkg = this.packagePoints.get(i).getPkgName();
            Integer commID = -1;
            int contribute = 0;
            if (!pkg.equals("")){
                commID = community.commIDbyLable(pkg);
                contribute = community.commContribute(pkg);
                this.initModule.setContribute(i, contribute);
            }
            this.PPID2commID.put(i, commID);
        }
        //设置flag
        boolean flag = true;
        while (flag){
            flag = false;
            //获取未终止的PP列表
            List<Integer> ppList = this.initModule.getDfsPPList();
            for (Integer i : ppList){
                Map<Integer, PackagePoint> ppMap = this.initModule.getPackagePoints();
                PackagePoint pp = ppMap.get(i);
                Integer ppCommID = this.PPID2commID.get(pp.getId());
                if (pp.getParentId() != null && this.PPID2commID.get(pp.getParentId()) != -1){
                    // 如果PP有父节点且父节点有社区ID，则将PP节点的社区ID设置为父节点的社区ID
                    this.PPID2commID.put(pp.getId(), this.PPID2commID.get(pp.getParentId()));
                    String parentPkg = this.packagePoints.get(pp.getParentId()).getPkgName();
                    String pkgName = parentPkg + "/" + pp.getLabel();
                    this.initModule.setPKGbyID(pp.getId(), pkgName);
                    flag = true;
                    pp.setTerm(true);
                    this.initModule.unlockChild(pp.getId());
                }else if (pp.getChildrenID().size() > 0 &&  ppCommID != -1){
                    // 如果PP有社区ID但其父节点不存在或无社区ID，则将PP终止
                    //可以通过改变pp来改变this.initModule中的节点终止符
                    pp.setTerm(true);
                }else if(pp.getChildrenID().size() > 0 && ppCommID == -1){
                    // 如果PP有子节点且无社区ID，则将子节点中超过半数的社区ID作为PP的社区ID，并将这些子节点包名中的公共部分作为PP节点包名
                    Map<Integer, Integer> commID2num = new HashMap<>();
                    Map<Integer, String> commID2pkg = new HashMap<>();
                    double sum = 0;
                    double sort = 0;
                    boolean process = true;
                    // 如果PP的子节点中存在无社区ID，则下一轮再处理
                    for (Integer child : pp.getChildrenID()){
                        Integer childCommID = this.PPID2commID.get(child);
                        if (childCommID == -1){
                            process = false;
                            break;
                        }else {
                            if (!commID2num.containsKey(childCommID)){
                                commID2num.put(childCommID, 1);
                                String pkg = getPkg(this.initModule.findPKGbyID(child));
                                // PP节点完整包名为空，则停止合并
                                if (pkg.equals("")){
                                    pp.setTerm(true);
                                    process = false;
                                    break;
                                }
                                commID2pkg.put(childCommID, pkg);
                                sort += 1;
                            }else {
                                commID2num.put(childCommID, commID2num.get(childCommID)+1);
                            }
                            sum += 1;
                        }
                    }
                    if (process){
                        String lastPkg = "";
                        int pkgNum = 0;
                        double maxComm = 0;
                        int maxItem = -1;
                        String pkg = "";
                        for (Integer item : commID2num.keySet()){
                            double countComm = commID2num.get(item)/sum;
                            String pkg1 = commID2pkg.get(item);
                            if (countComm > maxComm){
                                maxComm = countComm;
                                maxItem = item;
                                pkg = pkg1;
                            }
                            if (!pkg.equals(lastPkg)){
                                lastPkg = pkg1;
                                pkgNum += 1;
                            }
                        }
                        if (maxComm > 1/sort || commID2num.keySet().size() == 1){
                            this.PPID2commID.put(pp.getId(), maxItem);
                            this.initModule.setPKGbyID(pp.getId(), pkg);
                            flag = true;
                            this.initModule.unlockChild(pp.getId());
                            break;
                        }
                        //如果commID2pkg保持一致，并且/的数量大于等于2，子节点的社区归属各不相同===>把节点的社区ID设置为社区内部节点最多的子节点社区
                        if (pkgNum == 1 && sum == sort){
                            String tmp = lastPkg.replace("/", "");
                            int count = lastPkg.length()-tmp.length();
                            if (count >= 2){
                                Integer countComm = countPPinComm(commID2num);
                                this.PPID2commID.put(pp.getId(), countComm);
                                this.initModule.setPKGbyID(pp.getId(), lastPkg);
                                flag = true;
                                this.initModule.unlockChild(pp.getId());
                            }
                        }
                        pp.setTerm(true);
                    }
                }else {
                    //PP无子节点，则将其终止
                    //如果PP无社区ID，则是因为不同路径下有重复的包名对应的社区ID
                    if (this.PPID2commID.get(pp.getId()) == -1){
                        this.initModule.findParenttoRoot(pp.getId());
                        System.out.printf("Point %d is leaf node and has no Community?\n", pp.getId());
                    }else {
                        pp.setTerm(true);
                    }
                }
            }
            this.initModule.updatePPList();
        }
    }

    //给定一个map，返回其中社区ID中节点数量最多的社区ID
    private Integer countPPinComm(Map<Integer,Integer> commID2num){
        Map<Integer, List<Integer>> commID2PPID = new HashMap<>();
        for (Integer i : this.PPID2commID.keySet()){
            Integer commID = this.PPID2commID.get(i);
            List<Integer> ppList;
            if (!commID2PPID.containsKey(commID)){
                ppList = new ArrayList<>();
            }else{
                ppList = commID2PPID.get(commID);
            }
            ppList.add(i);
            commID2PPID.put(commID, ppList);
        }
        int maxNum = 0;
        Integer maxComm = -1;
        for (Integer item : commID2num.keySet()){
            int len = commID2PPID.get(item).size();
            if (len > maxNum){
                maxNum = len;
                maxComm = item;
            }
        }
        return maxComm;
    }


    //输出同一社区中的所有节点ID及其名称
    private void outputComm(){
        Map<Integer, List<Integer>> commID2PPID = new HashMap<>();
        for (Integer i : this.PPID2commID.keySet()){
            Integer commID = this.PPID2commID.get(i);
            List<Integer> ppList;
            if (!commID2PPID.containsKey(commID)){
                ppList = new ArrayList<>();
            }else{
                ppList = commID2PPID.get(commID);
            }
            ppList.add(i);
            commID2PPID.put(commID, ppList);
        }
        for (Integer commID : commID2PPID.keySet()){
            List<Integer> ppList = commID2PPID.get(commID);
            if (ppList.size() > 0){
                System.out.printf("-----------Community %d----------\n", commID);
                for (Integer ppID : ppList){
                    System.out.printf("%d: %s\n", ppID, this.initModule.findPKGbyID(ppID));
                }
            }
        }
    }

    //合并同一社区ID下的所有节点为一个模块，并赋予该模块pkg名称
    private void splitSubModule(){
        //记录同一社区中所有节点ID
        Map<Integer, List<Integer>> commID2PPID = new HashMap<>();
        for (Integer i : this.PPID2commID.keySet()){
            Integer commID = this.PPID2commID.get(i);
            if (commID != -1){
                List<Integer> ppList;
                if (!commID2PPID.containsKey(commID)){
                    ppList = new ArrayList<>();
                }else{
                    ppList = commID2PPID.get(commID);
                }
                ppList.add(i);
                commID2PPID.put(commID, ppList);
            }
        }
        //对同一社区中的所有节点构造子模块，并生成子模块名称
        for (Integer commID : commID2PPID.keySet()){
            List<Integer> ppList = commID2PPID.get(commID);
            if (ppList.size() > 0){
                List<PackagePoint> newPP = new ArrayList<>();
                List<Smali> newSmali = new ArrayList<>();
                for (Integer ppID : ppList){
                    List<PackagePoint> twig = this.initModule.findTwigbyID(ppID);
                    for (PackagePoint pp : twig){
                        List<Smali> smalis = this.initModule.findSmalibyID(pp.getId());
                        if (!newPP.contains(pp)){
                            newPP.add(pp);
                            newSmali.addAll(smalis);
                        }
                    }
                }
                //树的根节点数量决定是否需要新建节点，需要新建节点，则需要改变所有子树根节点的父节点ID，设置模块名
                List<PackagePoint> roots = getRootfromTrees(newPP);
                if (roots.size() == 1){
                    for (PackagePoint pp : newPP){
                        if (roots.contains(pp)){
                            pp.setParentId(null);
                        }
                    }
                }else if (roots.size() > 1){
                    PackagePoint newp = new PackagePoint(0, "", null);
                    newPP.add(newp);
                    for (PackagePoint pp : newPP){
                        if (roots.contains(pp)){
                            pp.setParentId(0);
                        }
                    }
                }
                //合并同label和同pkgName的节点，嫁接其子节点
                mergeSamePP(newPP, newSmali);
                //把newPP下的节点处理成新节点
                for (int i=0; i<newPP.size(); i++){
                    newPP.set(i, new PackagePoint(newPP.get(i)));
                }
                Module module = new Module(newPP, newSmali);
                genModuleName(module);
//                module.showTree();
                this.modules.add(module);
            }
        }
    }

    //返回一个PP节点列表是否构成一棵完整的树，返回树的根节点
    private List<PackagePoint> getRootfromTrees(List<PackagePoint> trees){
        List<PackagePoint> roots = new ArrayList<>();
        //遍历每个节点，如果父节点在trees中，则继续找其父节点，直到某节点的父节点不在trees中，则插入到roots中
        for (PackagePoint pp : trees){
            PackagePoint tmp = pp;
            while (trees.contains(this.packagePoints.get(tmp.getParentId()))){
                tmp = this.packagePoints.get(tmp.getParentId());
            }
            if (!roots.contains(tmp)){
                roots.add(tmp);
            }
        }
        return roots;
    }

    //根据模块中节点的pkgName前缀构建模块名称
    private void genModuleName(Module module){
        PackagePoint root = module.findRoot();
        if (!root.getLabel().equals("")){
            //不是新建的节点，只有一棵子树
            PackagePoint pp = module.findSingle(root.getId());
            if (!pp.getPkgName().equals("") && pp.getPkgName().contains("/")){
                module.setName(pp.getPkgName());
                return;
            }
        }
        //新建的节点或者findSingle的节点没找到包名，包含多棵子树，设置几个候选名称
        Map<String,Integer> candi = new HashMap<>();
        recExcept(module, root, candi);
        int max = 0;
        String name = "";
        for (String s : candi.keySet()){
            int tmp = candi.get(s);
            if (tmp > max){
                max = tmp;
                name = s;
            }
        }
        module.setName(name);
    }

    //递归找非单字节、非空节点
    private void recExcept(Module module, PackagePoint root, Map<String,Integer> map){
        List<Integer> childIDs = root.getChildrenID();
        Map<Integer,PackagePoint> tmp = module.getPackagePoints();
        for (Integer i : childIDs){
            PackagePoint pp = module.findSingle(i);
            String name = pp.getPkgName();
            if (name.equals("") || !name.contains("/")){
                recExcept(module, tmp.get(i),map);
            }else{
                int weight = module.getCTBbyID(i);
                map.put(name, weight);
            }
        }
    }

    //输出所有name为空字符串的子模块
    private void outputNoName(){
        for (Module m : this.modules){
            if (m.getName().equals("")){
                List<Integer> allPP = m.getDfsPPList();
                System.out.printf("------------------\n");
                for (Integer i : allPP){
                    System.out.printf("%s %d\n", m.findPKGbyID(i),m.getCTBbyID(i));
                }
            }
        }
    }

    //合并同label和同pkgName的节点，嫁接其子节点
    private void mergeSamePP(List<PackagePoint> ppList, List<Smali> sList){
        //存储每个smali文件的父节点ID
        Map<Integer, List<Smali>> smali2parent = new HashMap<>();
        for (Smali s : sList){
            Integer pID = s.getPpID();
            if (!smali2parent.containsKey(pID)){
                List<Smali> smalis = new ArrayList<>();
                smalis.add(s);
                smali2parent.put(pID, smalis);
            }else{
                smali2parent.get(pID).add(s);
            }
        }
        //存储每个PP节点的ID
        Map<Integer, PackagePoint> ID2PP = new HashMap<>();
        for (PackagePoint pp : ppList){
            ID2PP.put(pp.getId(), pp);
        }
        //任意两个节点之间是否满足①parentID一致；②label一致；③pkgName一致且不为空
        for (int i1=0; i1<ppList.size(); i1++){
            for (int i2=i1+1; i2<ppList.size(); i2++){
                PackagePoint pp1 = ppList.get(i1);
                PackagePoint pp2 = ppList.get(i2);
                if (pp1.getParentId() == null || pp2.getParentId() == null){
                    continue;
                }
                if (!pp1.equals(pp2) && pp1.getParentId().equals(pp2.getParentId()) && pp1.getLabel().equals(pp2.getLabel()) && pp1.getPkgName().equals(pp2.getPkgName()) && !pp1.getPkgName().equals("")){
                    //把子节点嫁接到有更多子节点的节点上
                    if (pp1.getChildrenID().size() >= pp2.getChildrenID().size()){
                        Integer newParent = pp1.getId();
                        List<Integer> pp2children = pp2.getChildrenID();
                        for (Integer p2c : pp2children){
                            if (!ID2PP.containsKey(p2c)){
                                System.out.printf("PackagePoint's child %d not in ppList!", p2c);
                                break;
                            }
                            ID2PP.get(p2c).setParentId(newParent);
                            pp1.addChildID(p2c);
                        }
                        if (smali2parent.containsKey(pp2.getId())){
                            for (Smali s : smali2parent.get(pp2.getId())){
                                int ind = sList.indexOf(s);
                                sList.get(ind).setPpID(newParent);
                            }
                        }
                        ppList.remove(pp2);
                    }else{
                        Integer newParent = pp2.getId();
                        List<Integer> pp1children = pp1.getChildrenID();
                        for (Integer p2c : pp1children){
                            if (!ID2PP.containsKey(p2c)){
                                System.out.printf("PackagePoint's child %d not in ppList!", p2c);
                                break;
                            }
                            ID2PP.get(p2c).setParentId(newParent);
                            pp2.addChildID(p2c);
                        }
                        if (smali2parent.containsKey(pp1.getId())){
                            for (Smali s : smali2parent.get(pp1.getId())){
                                int ind = sList.indexOf(s);
                                sList.get(ind).setPpID(newParent);
                            }
                        }
                        ppList.remove(pp1);
                    }
                }
            }
        }
    }

    public boolean isShelled() {
        return shelled;
    }
}
