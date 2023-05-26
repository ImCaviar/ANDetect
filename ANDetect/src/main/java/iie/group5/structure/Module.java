package iie.group5.structure;

import java.io.Serializable;
import java.util.*;

//一个模块由结构树和叶子节点下的smali文件组成
public class Module implements Serializable {
    //模块名称
    private String name;
    //叶子节点组成的HashMap 节点ID<->节点
    private transient Map<Integer,PackagePoint> packagePoints;
    //叶子节点组成的List，用于序列化
    private List<PackagePoint> PPs;
    //smali文件id组成的list
    private List<Smali> smalis;
    //深度优先遍历得到的未终止节点ID
    private transient List<Integer> dfsPPList;
    //第三方库版本号，无为空
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
        //初始化，所有PP都在dfs序列中
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

    //序列化后的Module恢复完整结构
    public void getFull(){
        this.packagePoints = new HashMap<>();
        for (PackagePoint pp : this.PPs){
            pp.setPkgName("");
            this.packagePoints.put(pp.getId(), pp);
        }
        this.dfsPPList = new ArrayList<>();
        genChildren();
        //初始化，所有PP都在dfs序列中
        List<PackagePoint> ppList = genDFS(findRoot());
        for (PackagePoint pp : ppList){
            dfsPPList.add(pp.getId());
        }
    }

    //输入树的根节点，以深度优先遍历的方式构成树结构List，递归实现
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
            System.out.println("Point "+root.getId()+" has no child.");
        }
        if (children.size() == 0){
            return;
        }
        for ( Integer i : children){
            recDFS(findPPbyID(i),dfsList);
        }
    }

    //输入树的根节点，以广度优先遍历的方式构成树结构List，队列实现
    private List<PackagePoint> genBFS(PackagePoint root){
        //广度优先遍历得到的节点列表
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

    //为每个节点生成子节点
    private void genChildren(){
        for (Integer id : this.packagePoints.keySet()){
            List<Integer> children = findChildrenbyID(id);
            this.packagePoints.get(id).setChildrenID(children);
            int ind = this.PPs.indexOf(this.packagePoints.get(id));
            this.PPs.get(ind).setChildrenID(children);
        }
    }

    //根据模块中的PP列表，找到其中的根节点
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

    //输入节点ID值，找到节点
    private PackagePoint findPPbyID(Integer ID){
        PackagePoint node = new PackagePoint();
        if (!this.packagePoints.containsKey(ID)){
            System.out.println("Cannot find node by ID!");
        }else{
            node = this.packagePoints.get(ID);
        }
        return node;
    }

    //输入节点ID值，找到父节点，如果父节点ID为null则返回null
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

    //输入节点ID值，找到其子节点列表
    private List<Integer> findChildrenbyID(Integer ID){
        List<Integer> children = new ArrayList<>();
        for (PackagePoint i : this.packagePoints.values()){
            if (i.getParentId() != null && i.getParentId().longValue() == ID.longValue()){
                children.add(i.getId());
            }
        }
        return children;
    }

    //根据节点ID值，找到其下的所有smali
    public List<Smali> findSmalibyID(Integer ID){
        List<Smali> s = new ArrayList<Smali>();
        for (Smali i : this.smalis){
            if (i.getPpID().longValue() == ID.longValue()){
                s.add(i);
            }
        }
        return s;
    }

    //根据节点ID找到其完整包名
    public String findPKGbyID(Integer ID){
        PackagePoint pp = this.packagePoints.get(ID);
        return pp.getPkgName();
    }

    //设置节点ID的完整包名
    public void setPKGbyID(Integer ID, String pkgName){
        PackagePoint pp = this.packagePoints.get(ID);
        pp.setPkgName(pkgName);
    }

    //根据class对应的包名，找到对应的叶子节点，例如"com.a.b"找到以"b"为标签的节点，返回节点ID值
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

    //递归，根据包名进行树枝label匹配，输入标签列表+某个节点，判断节点及其子节点中是否有匹配的标签构成“树枝”，匹配则返回叶子节点的ID
    private Integer matchLables(String[] labels, PackagePoint pp){
        Integer ppID = null;
        if (pp.getLabel().equals(labels[0])){
            if (labels.length == 1){
                //返回叶子节点ID
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

    //从给定节点一直找其父节点直到根节点
    public void findParenttoRoot(Integer ID){
        Integer rootID = findRoot().getId();
        while (ID != null && !ID.equals(rootID)){
            PackagePoint pp = this.packagePoints.get(ID);
            System.out.printf(pp.getLabel() + "--->");
            ID = pp.getParentId();
        }
    }

    //更新dfsPPList
    public void updatePPList(){
        List<PackagePoint> ppList = genDFS(findRoot());
        this.dfsPPList = new ArrayList<>();
        for (PackagePoint pp : ppList){
            //还没终止，则继续插入newPPList
            if (!pp.isTerm()){
                this.dfsPPList.add(pp.getId());
            }
        }
    }

    //根据给定的节点ID找到“树枝”，其中“树枝”不包含“samli”和"classes"所在节点，ignorePKG表示是否忽略PKGName
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

    //根据给定的节点ID往下找，直到某节点有不止1个子节点为止，返回该节点
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

    //解锁给定节点ID的子节点
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

    //输出模块的树状结构z，深度优先递归
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
        //按空格位数输出root节点label
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

    //设置模块中节点贡献度
    public void setContribute(Integer ID, int contribute){
        PackagePoint pp = findPPbyID(ID);
        pp.setContribute(contribute);
    }

    //根据给定节点ID，计算以其为根节点构建子树所有节点的社区贡献度
    public int getCTBbyID(Integer ID){
        PackagePoint root = findPPbyID(ID);
        List<PackagePoint> allPP = genBFS(root);
        int ctb = 0;
        for (PackagePoint pp : allPP){
            ctb += pp.getContribute();
        }
        return ctb;
    }

    //给定节点ID，找到离该节点最近的叶子节点的距离
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

    //简化method的参数类型、返回类型；field的字段类型；invoke的类（自定义类型用类所在父节点的ID表示；系统类型用最后一个/类名表示），用于压缩库
    //改成只压缩系统类型
    public void simpleType(){
        //记录自定义类
        Map<String, Smali> ctm = new HashMap<>();
        for (Smali s : this.smalis){
            String clazz = s.getClazz();
            ctm.put(clazz, s);
        }
        //处理每个smali文件的field和method
        for (Smali s : this.smalis){
            //简化field
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
            //简化method
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
            //简化invoke
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

    //改成只压缩系统类型
    private String replaceType(Map<String, Smali> ctm, String type){
        String rep = type;
//        if (ctm.containsKey(type)){
//            int id = ctm.get(type).getPpID();
//            rep = Integer.toString(id);
//        }else if (type.contains("/")){
//            int ind = type.lastIndexOf("/");
//            rep = type.substring(ind+1);
//        }
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
