package iie.group5.structure;

import java.io.Serializable;
import java.util.*;

// A module consists of a structure tree and smali files under the leaf nodes
public class Module implements Serializable {
    // Module Name
    private String name;
    // HashMap composed of leaf nodes Node ID <-> Node
    private transient Map<Integer,PackagePoint> packagePoints;
    // List of leaf nodes for serialization
    private List<PackagePoint> PPs;
    // List of smali file ids
    private List<Smali> smalis;
    // The unterminated node ID obtained by depth-first traversal
    private transient List<Integer> dfsPPList;
    //Third party library version number
    private String version;

    public Module() {
        this.name = "";
        this.version = "";
        this.packagePoints = new HashMap<>();
    }

    public Module(List<PackagePoint> packagePoints, List<Smali> smalis){
        this.name = "";
        this.version = "";
        this.PPs = packagePoints;
        this.packagePoints = new HashMap<>();
        for (PackagePoint pp : packagePoints){
            this.packagePoints.put(pp.getId(), pp);
        }
        this.smalis = smalis;
        this.dfsPPList = new ArrayList<>();
        genChildren();
        // Initialization, all PPs are in the dfs sequence
        List<PackagePoint> ppList = genDFS(findRoot());
        for (PackagePoint pp : ppList){
            dfsPPList.add(pp.getId());
        }
    }

    public Module(Map<Integer, PackagePoint> packagePoints, List<Smali> smalis) {
        this.name = "";
        this.version = "";
        this.packagePoints = packagePoints;
        this.PPs = new ArrayList<>();
        for (PackagePoint pp : packagePoints.values()){
            this.PPs.add(pp);
        }
        this.smalis = smalis;
        this.dfsPPList = new ArrayList<>();
        genChildren();
        List<PackagePoint> ppList = genDFS(findRoot());
        for (PackagePoint pp : ppList){
            dfsPPList.add(pp.getId());
        }
    }

    // Restore the complete structure of the serialized Module
    public void getFull(){
        this.packagePoints = new HashMap<>();
        for (PackagePoint pp : this.PPs){
            pp.setPkgName("");
            this.packagePoints.put(pp.getId(), pp);
        }
        this.dfsPPList = new ArrayList<>();
        genChildren();
        // Initialization, all PPs are in the dfs sequence
        List<PackagePoint> ppList = genDFS(findRoot());
        for (PackagePoint pp : ppList){
            dfsPPList.add(pp.getId());
        }
    }

    // The root node of the input tree is traversed in a depth-first manner to form the tree structure List, which is implemented recursively
    private List<PackagePoint> genDFS(PackagePoint root){
        List<PackagePoint> dfsList = new ArrayList<>();
        recDFS(root, dfsList);
        return dfsList;
    }

    private void recDFS(PackagePoint root, List<PackagePoint> dfsList){
        if (root == null){
            return;
        }
        dfsList.add(root);
        List<Integer> children = root.getChildrenID();
        if (children == null){
            System.out.println("aaa");
        }
        if (children.size() == 0){
            return;
        }
        for ( Integer i : children){
            recDFS(findPPbyID(i),dfsList);
        }
    }

    // The root node of the input tree is traversed in a breadth-first manner to form the tree structure List, and the queue is implemented
    private List<PackagePoint> genBFS(PackagePoint root){
        // List of nodes obtained by breadth-first traversal
        List<PackagePoint> bfsList = new ArrayList<PackagePoint>();
        if (root == null){
            return null;
        }

        Queue<PackagePoint> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()){
            PackagePoint node = queue.poll();
            bfsList.add(node);
            List<Integer> children = node.getChildrenID();
            if (children.size() > 0){
                for (Integer i : children){
                    queue.add(findPPbyID(i));
                }
            }
        }
        return bfsList;
    }

    // Generate child nodes for each node
    private void genChildren(){
        for (Integer id : this.packagePoints.keySet()){
            List<Integer> children = findChildrenbyID(id);
            this.packagePoints.get(id).setChildrenID(children);
            int ind = this.PPs.indexOf(this.packagePoints.get(id));
            this.PPs.get(ind).setChildrenID(children);
        }
    }

    // Find the root node according to the PP list in the module
    public PackagePoint findRoot(){
        PackagePoint root = new PackagePoint();
        for (PackagePoint i : this.packagePoints.values()){
            if (i.getParentId() != null){
                continue;
            }else{
                root = i;
                return root;
            }
        }
        System.out.println("Cannot find root node!");
        return root;
    }

    // Enter the node ID value and find the node
    private PackagePoint findPPbyID(Integer ID){
        PackagePoint node = new PackagePoint();
        if (!this.packagePoints.containsKey(ID)){
            System.out.println("Cannot find node by ID!");
        }else{
            node = this.packagePoints.get(ID);
        }
        return node;
    }

    // Enter the node ID value, find the parent node, and return null if the parent node ID is null
    private PackagePoint findParentbyID(Integer ID){
        PackagePoint node = findPPbyID(ID);
        Integer parentID;
        if (node.getParentId() != null){
            parentID = node.getParentId();
            PackagePoint parent = findPPbyID(parentID);
            return parent;
        }else {
            return null;
        }
    }

    // Enter the node ID value and find its child node list
    private List<Integer> findChildrenbyID(Integer ID){
        List<Integer> children = new ArrayList<>();
        for (PackagePoint i : this.packagePoints.values()){
            if (i.getParentId() != null && i.getParentId().longValue() == ID.longValue()){
                children.add(i.getId());
            }
        }
        return children;
    }

    // Based on the node ID value, find all smali under it
    public List<Smali> findSmalibyID(Integer ID){
        List<Smali> s = new ArrayList<Smali>();
        for (Smali i : this.smalis){
            if (i.getPpID().longValue() == ID.longValue()){
                s.add(i);
            }
        }
        return s;
    }

    // Find the full package name based on the node ID
    public String findPKGbyID(Integer ID){
        PackagePoint pp = this.packagePoints.get(ID);
        return pp.getPkgName();
    }

    // Set the full package name of the node ID
    public void setPKGbyID(Integer ID, String pkgName){
        PackagePoint pp = this.packagePoints.get(ID);
        pp.setPkgName(pkgName);
    }

    // Find the corresponding leaf node according to the package name of the class, e.g. "com.a.b" find the node with the label "b" and return the node ID value
    public Integer findIDbyPKGname(String pkgname){
        String[] labels = pkgname.split(".");
        PackagePoint root = findRoot();
        List<PackagePoint> dfsList = genDFS(root);
        Integer leafID = null;
        for (PackagePoint pp : dfsList){
            leafID = matchLables(labels, pp);
            if (leafID != null){
                System.out.println("The leaf node's label is " + findPPbyID(leafID).getLabel());
                break;
            }
        }
        return leafID;
    }

    // recursive, according to the package name for tree branch label matching, input the list of labels + a node, determine whether there are matching labels in the node and its children to form a "tree branch", match then return the ID of the leaf node
    private Integer matchLables(String[] labels, PackagePoint pp){
        Integer ppID = null;
        if (pp.getLabel().equals(labels[0])){
            if (labels.length == 1){
                // Return the leaf node ID
                ppID = pp.getId();
            }else{
                List<Integer> children = pp.getChildrenID();
                for (Integer child : children){
                    ppID = matchLables(Arrays.copyOfRange(labels, 1, labels.length), findPPbyID(child));
                }
            }
        }
        return ppID;
    }

    // Find the parent node from the given node up to the root node
    public void findParenttoRoot(Integer ID){
        Integer rootID = findRoot().getId();
        while (ID != null && !ID.equals(rootID)){
            PackagePoint pp = this.packagePoints.get(ID);
            System.out.printf(pp.getLabel() + "--->");
            ID = pp.getParentId();
        }
    }

    // Update dfsPPList
    public void updatePPList(){
        List<PackagePoint> ppList = genDFS(findRoot());
        this.dfsPPList = new ArrayList<>();
        for (PackagePoint pp : ppList){
            // If not yet terminated, then continue inserting newPPList
            if (!pp.isTerm()){
                this.dfsPPList.add(pp.getId());
            }
        }
    }

    // Find the "branch" based on the given node ID, where the "branch" does not contain the nodes where "smali" and "classes" are located
    public List<PackagePoint> findTwigbyID(Integer ID){
        List<PackagePoint> ppList = new ArrayList<>();
        PackagePoint leafPP = findPPbyID(ID);
        while (leafPP != null){
            if (leafPP.getPkgName().equals("") && (leafPP.getLabel().contains("smali") || leafPP.getLabel().contains("classes"))){
                break;
            }else{
                ppList.add(leafPP);
                leafPP = findParentbyID(leafPP.getId());
            }
        }
        return ppList;
    }

    // Find the node based on the given node ID, until a node has more than 1 child node, then return the node
    public PackagePoint findSingle(Integer ID){
        PackagePoint pp = findPPbyID(ID);
        while (true){
            List<Integer> childIDs = pp.getChildrenID();
            if (childIDs.size() == 1){
                pp = findPPbyID(childIDs.get(0));
            }else{
                break;
            }
        }
        return pp;
    }

    // Unlock the child node with the given node ID
    public void unlockChild(Integer ID){
        PackagePoint pp = findPPbyID(ID);
        List<Integer> children = pp.getChildrenID();
        if (children.size() > 0){
            for (Integer child : children){
                PackagePoint ch = findPPbyID(child);
                ch.setTerm(false);
            }
        }
    }

    // Tree structure of output module z, depth-first recursive
    public void showTree(){
        PackagePoint root = findRoot();
        System.out.printf("%s\n",root.getLabel());
        int len = root.getLabel().length();
        List<Integer> children = root.getChildrenID();
        if (children.size() > 0){
            for (Integer i : children){
                showDFS(findPPbyID(i), len);
            }
        }
    }

    private void showDFS(PackagePoint root, int spaceLen){
        if (root == null){
            return;
        }
        // Output root node label by number of spaces
        String space = "";
        for (int i=0; i<spaceLen; i++){
            space += " ";
        }
        System.out.printf("%s|-----%s\n",space, root.getLabel());
        spaceLen += 6 + root.getLabel().length();
        List<Integer> children = root.getChildrenID();
        if (children.size() == 0){
            return;
        }
        for ( Integer i : children){
            showDFS(findPPbyID(i),spaceLen);
        }
    }

    // Set the node contribution in the module
    public void setContribute(Integer ID, int contribute){
        PackagePoint pp = findPPbyID(ID);
        pp.setContribute(contribute);
    }

    // Based on the given node ID, calculate the community contribution of all nodes in the subtree constructed with it as the root node
    public int getCTBbyID(Integer ID){
        PackagePoint root = findPPbyID(ID);
        List<PackagePoint> allPP = genBFS(root);
        int ctb = 0;
        for (PackagePoint pp : allPP){
            ctb += pp.getContribute();
        }
        return ctb;
    }

    // Given a node ID, find the distance of the nearest leaf node to that node
    public int dis2leaf(Integer ID){
        PackagePoint pp = findPPbyID(ID);
        if (pp.getChildrenID().size() == 0){
            return 0;
        }
        List<PackagePoint> allChild = genBFS(pp);
        int near = 10086;
        for (PackagePoint p : allChild){
            if (p.getChildrenID().size() == 0){
                int dis = compDis(p, pp);
                if (dis < near){
                    near = dis;
                }
            }
        }
        return near;
    }

    private int compDis(PackagePoint child, PackagePoint PPParent){
        int result = 0;
        while (child.getParentId() != null){
            if (child.equals(PPParent)){
                break;
            }else{
                PackagePoint father = findPPbyID(child.getParentId());
                child = father;
                result ++;
            }
        }
        return result;
    }

    // Simplify the parameter type and return type of method; the field type of field; the class of invoke (custom type is represented by the ID of the parent node where the class is located; system type is represented by the last / class name) for compressed library
    public void simpleType(){
        // Record custom classes
        Map<String, Smali> ctm = new HashMap<>();
        for (Smali s : this.smalis){
            String clazz = s.getClazz();
            ctm.put(clazz, s);
        }
        // Handle the field and method of each smali file
        for (Smali s : this.smalis){
            String[] fields = s.getField();
            if (fields.length>0){
                for (int i=0; i<fields.length; i++){
                    String[] tmps = fields[i].split(" ");
                    String rep = replaceType(ctm, tmps[tmps.length-1]);
                    int ind = fields[i].lastIndexOf(" ");
                    fields[i] = fields[i].substring(0,ind+1)+rep;
                }
            }
            s.setField(fields);
            String[] methods = s.getMethod();
            for (int i=0; i<methods.length; i++){
                String[] tmps = methods[i].split(" ");
                int start = 1;
                String newmtd = tmps[0];
                if (tmps[1].equals("s")){
                    start = 2;
                    newmtd = tmps[0] + " " + tmps[1];
                }
                for (int j=start; j<tmps.length; j++){
                    String tmp = replaceType(ctm,tmps[j]);
                    newmtd += " " + tmp;
                }
                s.setMethod(newmtd, i);
            }
            String[] invokes = s.getInvoke();
            if (invokes.length>0){
                for (int i=0; i<invokes.length; i++){
                    String[] tmps = invokes[i].split("->");
                    String rep = replaceType(ctm, tmps[0]);
                    if (tmps.length == 1){
                        invokes[i] = rep + "->";
                    }else{
                        invokes[i] = rep + "->" + tmps[1];
                    }
                }
            }
            s.setInvoke(invokes);
            s.deleteRepInv();
        }
    }

    // Change to compress only the system type
    private String replaceType(Map<String, Smali> ctm, String type){
        String rep = type;
        if (!ctm.containsKey(type) && type.contains("/")){
            int ind = type.lastIndexOf("/");
            rep = type.substring(ind+1);
        }
        return rep;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Integer, PackagePoint> getPackagePoints() {
        return packagePoints;
    }

    public void setPackagePoints(Map<Integer, PackagePoint> packagePoints) {
        this.packagePoints = packagePoints;
    }

    public List<Smali> getSmalis() {
        return smalis;
    }

    public void setSmalis(List<Smali> smalis) {
        this.smalis = smalis;
    }

    public List<Integer> getDfsPPList() {
        return dfsPPList;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
