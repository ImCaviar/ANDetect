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

// Module decoupling
public class MDecouple {
    //smali file path
    private List<File> file;
    // Initial module
    private Module initModule;
    // List of decoupled modules
    private List<Module> modules;
    // PackagePoint node counter
    private Integer ppCount;
    //PPID<->PP
    private Map<Integer, PackagePoint> packagePoints;
    // Global variables record all nodes under smali file
    private List<Smali> smalis;
    //PPID<->Community ID
    private Map<Integer, Integer> PPID2commID;
    // Reinforced or not
    private boolean shelled;


    public MDecouple(List<String> filePath) {
        this.file = new ArrayList<>();
        for (String f : filePath){
            File tmpF = new File(f);
            this.file.add(tmpF);
        }
        this.modules = new ArrayList<>();
        this.packagePoints = new HashMap<>();
        this.smalis = new ArrayList<>();
        this.PPID2commID = new HashMap<>();
        this.shelled = true;
        this.ppCount = 0;
    }

    // Access to decoupled modules
    public List<Module> getModules() {
        return modules;
    }

    public void decoupling(String hostPKG){
        // Iterate through all folders and files under file
        System.out.println("Building package TREEs and Analyzing Smali files!");
        // Removing the last . string after the last one will match better
        int ind = hostPKG.lastIndexOf(".");
        String host = "";
        if (ind != -1){
            hostPKG = hostPKG.substring(0, ind);
            host = hostPKG.replace(".", "\\");
        }
        // Building the root node
        Integer id = ppCount ++;
        PackagePoint packagePoint = new PackagePoint(id, "classes", null);
        this.packagePoints.put(id, packagePoint);
        // Generate more nodes for folders
        for (File f : this.file){
            findFiles(f, id, host);
        }
        if (this.shelled){
            // If is hardened and returned directly, not decoupled
            return;
        }

        // Initialize community
        Graph community = new Graph();
        // Generate function call graphs based on the custom functions called in each smali file
        genGraphbyPkg(community);
        // After building all the package nodes and smali nodes and generating the graph, generate the initialized Module
        this.initModule = new Module(this.packagePoints, this.smalis);

        // Louvain for community segmentation
        System.out.println("Module Decoupling by Louvain!");
        community.singleLouvain();
//        community.outputComm();
        // Bottom-up community consolidation
        mergeComm(community);
//        outputComm();
        // Segmentation sub-module
        splitSubModule();
//        outputNoName();
    }

    // Iterate through all files under file, excluding folders and files in hostPKG
    private void findFiles(File file, Integer parentId, String host){
        if (file != null && file.exists()){
            String filePath = file.getPath();
            if (!host.equals("") && filePath.contains(host)){
                this.shelled = false;
                return;
            }
            String fileName = file.getName();
            if (file.isDirectory()){
                // If file is a folder, build the node
                Integer id = ppCount ++;
                PackagePoint packagePoint = new PackagePoint(id, fileName, parentId);
                this.packagePoints.put(id, packagePoint);
                File [] subFiles = file.listFiles();
                if (subFiles != null){
                    for (File f : subFiles){
                        // Recursively, the id of this node is used as the parentId of the next node
                        findFiles(f, id, host);
                    }
                }
            }else if (fileName.endsWith(".smali")){
                // If file is a smali file, build the Smali class
                Smali smali = new Smali(parentId);
                smali.analyzeSmali(filePath);
                this.smalis.add(smali);
            }
        }
    }

    // Use package names as graph nodes
    private void genGraphbyPkg(Graph community){
        System.out.println("Add Vertices ......");
        for (Smali i : this.smalis){
            // Generate a node for each package, disregarding duplicate packages and disregarding empty packages
            Integer ppID = i.getPpID();
            String pkg = getPkg(i.getClazz());
            if (!community.inGraph(pkg) && !pkg.equals("")){
                community.insertVertex(pkg);
            }
            // Set the full package name for smali's parent node
            PackagePoint pp = this.packagePoints.get(ppID);
            if (pp.getPkgName().equals("")){
                pp.setPkgName(pkg);
            }
        }
        System.out.println("Generating Call Graph ......");
        for (Smali i: this.smalis){
            // Extract each class inheritance relationship in the package, generate edges, inheritance relationship edge weight of 5
            String supPkg = getPkg(i.getSupper());
            String pkg = getPkg(i.getClazz());
            if (community.inGraph(supPkg) && !supPkg.equals(pkg) && !pkg.equals("")){
                community.insertEdge(supPkg, pkg, 5);
            }
            // Extract the function call relationship between classes, generate edges, and count the generated edge weights
            String[] invokes = i.getInvoke();
            for (int ind=0; ind<invokes.length; ind++){
                if (invokes[ind] != null){
                    String classn = invokes[ind].split("->")[0];
                    if (classn.contains("[")){
                        classn = classn.replace("[", "");
                    }
                    // Add an edge only if the smali call is a custom function and the call is not to a function defined in the class
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

    // Get the package name of the class
    private String getPkg(String clazz){
        // Returns null if there is no package name
        String result = "";
        if (clazz.contains("/")){
            int ind = clazz.lastIndexOf("/");
            result = clazz.substring(0, ind);
        }
        return result;
    }

    // Bottom-up community consolidation
    private void mergeComm(Graph community){
        // Initialize the community of each PP node based on the result of community
        for (Integer i : this.packagePoints.keySet()){
            String pkg = this.packagePoints.get(i).getPkgName();
            Integer commID = -1;
            if (!pkg.equals("")){
                commID = community.commIDbyLable(pkg);
                int contribute = community.commContribute(pkg);
                this.initModule.setContribute(i, contribute);
            }
            this.PPID2commID.put(i, commID);
        }
        // Set flags
        boolean flag = true;
        while (flag){
            flag = false;
            // Get the list of unterminated PPs
            List<Integer> ppList = this.initModule.getDfsPPList();
            for (Integer i : ppList){
                Map<Integer, PackagePoint> ppMap = this.initModule.getPackagePoints();
                PackagePoint pp = ppMap.get(i);
                Integer ppCommID = this.PPID2commID.get(pp.getId());
                if (pp.getParentId() != null && this.PPID2commID.get(pp.getParentId()) != -1){
                    // If PP has a parent node and the parent node has a community ID, the parent node community ID->PP community ID
                    this.PPID2commID.put(pp.getId(), this.PPID2commID.get(pp.getParentId()));
                    String parentPkg = this.packagePoints.get(pp.getParentId()).getPkgName();
                    String pkgName = parentPkg + "/" + pp.getLabel();
                    this.initModule.setPKGbyID(pp.getId(), pkgName);
                    flag = true;
                    pp.setTerm(true);
                    // Unlock all children of the PP node
                    this.initModule.unlockChild(pp.getId());
                }else if (pp.getChildrenID().size() > 0 &&  ppCommID != -1){
                    // Terminate PP if it has child nodes with community IDs
                    // The node terminator in this.initModule can be changed by changing the pp
                    pp.setTerm(true);
                }else if(pp.getChildrenID().size() > 0 && ppCommID == -1){
                    // If the PP has child nodes and no community ID, the community ID of the child nodes that exceeds factor σ is used as the community ID of the PP, and the common part of these child node packet names is used as the PP node packet name
                    Map<Integer, Integer> commID2num = new HashMap<>();
                    Map<Integer, String> commID2pkg = new HashMap<>();
                    double sum = 0;
                    double sort = 0;
                    boolean process = true;
                    // If there is no community ID in the child node of PP, it will be processed again in the next round
                    for (Integer child : pp.getChildrenID()){
                        Integer childCommID = this.PPID2commID.get(child);
                        if (childCommID == -1){
                            process = false;
                            break;
                        }else {
                            if (!commID2num.containsKey(childCommID)){
                                commID2num.put(childCommID, 1);
                                String pkg = getPkg(this.initModule.findPKGbyID(child));
                                // PP node complete package name is empty, then stop merging
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
                        // If commID2pkg remains consistent and the number of /'s is greater than or equal to 2, the community attribution of the child nodes varies ===> set the community ID of the node to the community of the child node with the most nodes within the community
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
                    // PP has no child node, then terminate it
                    // If the PP does not have a community ID, it is because there are duplicate package names corresponding to community IDs in different paths, or it may be because the path where the PP is located is the path where host is located
                    pp.setTerm(true);
                }
            }
            this.initModule.updatePPList();
        }
    }

    // Given a map, return the community ID with the highest number of nodes in the community ID
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


    // Output all node IDs and their names in the same community
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

    // Merge all nodes under the same community ID into one module, and give the module pkg name
    private void splitSubModule(){
        // Record all node IDs in the same community
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
        // Construct submodules for all nodes in the same community and generate submodule names
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
                // The number of root nodes of the tree determines whether new nodes are needed, and if new nodes are needed, the parent node IDs of all root nodes of the subtree need to be changed and the module name set
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
                // Merge nodes with the same label and the same pkgName, and graft their children
                mergeSamePP(newPP, newSmali);
                // Process the nodes under newPP into new nodes
                for (int i=0; i<newPP.size(); i++){
                    newPP.set(i, new PackagePoint(newPP.get(i)));
                }
                Module module = new Module(newPP, newSmali);
                genModuleName(module);
                this.modules.add(module);
            }
        }
    }

    // Returns whether a list of PP nodes constitutes a complete tree, returning the root node of the tree
    private List<PackagePoint> getRootfromTrees(List<PackagePoint> trees){
        List<PackagePoint> roots = new ArrayList<>();
        // Iterate through each node, and if the parent node is in trees, continue to find its parent until the parent node of a node is not in trees, then insert it into roots
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

    // Build the module name based on the pkgName prefix of the node in the module
    private void genModuleName(Module module){
        PackagePoint root = module.findRoot();
        if (!root.getLabel().equals("")){
            // Not a new node, only a subtree
            PackagePoint pp = module.findSingle(root.getId());
            if (!pp.getPkgName().equals("") && pp.getPkgName().contains("/")){
                module.setName(pp.getPkgName());
                return;
            }
        }
        // Newly created node or findSingle node did not find package name, or only a short package name node, contains multiple subtrees, set several candidate names
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

    // Recursively find non-short single-byte, non-empty nodes
    private void recExcept(Module module, PackagePoint root, Map<String,Integer> map){
        List<Integer> childIDs = root.getChildrenID();
        Map<Integer,PackagePoint> tmp = module.getPackagePoints();
        for (Integer i : childIDs){
            PackagePoint pp = module.findSingle(i);
            String name = pp.getPkgName();
            if (name.equals("") || (!name.contains("/") && name.length()<4)){
                recExcept(module, tmp.get(i),map);
            }else{
                int weight = module.getCTBbyID(i);
                map.put(name, weight);
            }
        }
    }

    // Output all submodules whose name is an empty string
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

    // Merge nodes with the same label and the same pkgName, and graft their children
    private void mergeSamePP(List<PackagePoint> ppList, List<Smali> sList){
        // Store the parent node ID of each smali file
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
        // Store the ID of each PP node
        Map<Integer, PackagePoint> ID2PP = new HashMap<>();
        for (PackagePoint pp : ppList){
            ID2PP.put(pp.getId(), pp);
        }
        // Does any two nodes meet ①parentID consistent; ②label consistent; ③pkgName consistent and not empty
        for (int i1=0; i1<ppList.size(); i1++){
            for (int i2=i1+1; i2<ppList.size(); i2++){
                PackagePoint pp1 = ppList.get(i1);
                PackagePoint pp2 = ppList.get(i2);
                if (pp1.getParentId() == null || pp2.getParentId() == null){
                    continue;
                }
                if (!pp1.equals(pp2) && pp1.getParentId().equals(pp2.getParentId()) && pp1.getLabel().equals(pp2.getLabel()) && pp1.getPkgName().equals(pp2.getPkgName()) && !pp1.getPkgName().equals("")){
                    // Grafting a child node to a node with more children
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
