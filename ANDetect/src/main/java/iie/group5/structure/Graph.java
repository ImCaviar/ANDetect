package iie.group5.structure;

import java.util.*;

public class Graph {
    //图中原始节点，不变
    private Map<String, Vertex> vertices;
    private List<Vertex> vIDs;
    //社区网络顶点计数器，节点ID，也是节点在vertices列表中的索引
    private int vtCount;
    //记录社区C中所有节点、指向社区C中所有节点的权和，在多层Louvain中动态更新
    private Map<Integer, List<Integer>> commV;
    private Map<Integer, Integer> commW;

    public Graph() {
        this.vertices = new HashMap<>();
        this.vIDs = new ArrayList<>();
        this.commV = new HashMap<>();
        this.commW = new HashMap<>();
        this.vtCount = 0;
    }

    //社区网络中的节点
    public class Vertex{
        private Integer ID;
        private String label;
        private Integer communityID;
        //outEdges是V的出边，inEdges是V的入边，节点ID<->权重
        private Map<Integer, Integer> outEdges;
        private Map<Integer, Integer> inEdges;

        public Vertex(Integer ID, String label) {
            this.ID = ID;
            this.label = label;
            this.outEdges = new HashMap<>();
            this.inEdges = new HashMap<>();
        }

        //为顶点添加出边
        public void addOutEdge(int weight, Integer toID){
            //如果已经存在该边，则叠加权重；如果不存在，则创建该边
            if (this.outEdges.size() == 0 || !this.outEdges.containsKey(toID)){
                this.outEdges.put(toID, weight);
            }else{
                int newWeight = this.outEdges.get(toID);
                this.outEdges.put(toID, newWeight + weight);
            }
        }

        //为顶点添加入边
        public void addInEdge(int weight, Integer inID){
            //如果已经存在该边，则叠加权重；如果不存在，则创建该边
            if (this.inEdges.size() == 0 || !this.inEdges.containsKey(inID)){
                this.inEdges.put(inID, weight);
            }else{
                int newWeight = this.inEdges.get(inID);
                this.inEdges.put(inID, newWeight + weight);
            }
        }

        //查询本节点的所有出度
        public int selectOutWeight(){
            int outWeight = 0;
            if (this.outEdges.size() > 0){
                for (int w : this.outEdges.values()){
                    outWeight += w;
                }
            }
            return outWeight;
        }

        //查询本节点的所有入度
        public int selectInWeight(){
            int inWeight = 0;
            if (this.inEdges.size() > 0){
                for (int w : this.inEdges.values()){
                    inWeight += w;
                }
            }
            return inWeight;
        }

        public Integer getID() {
            return ID;
        }

        public String getLabel() {
            return label;
        }

        public Map<Integer, Integer> getOutEdges() {
            return outEdges;
        }

        public Map<Integer, Integer> getInEdges() {
            return inEdges;
        }

        public Integer getCommunityID() {
            return communityID;
        }

        public void setCommunityID(Integer communityID) {
            this.communityID = communityID;
        }
    }

    //由真实节点构建图结构，需要Label！！！

    //在图中插入未出现过的节点，同步插入，VertexID=this.vertices.index=this.labelsV.index
    public void insertVertex(String label){
        if (!this.vertices.containsKey(label)){
            Vertex vertex = new Vertex(this.vtCount++, label);
            this.vertices.put(label, vertex);
            this.vIDs.add(vertex);
        }
    }

    //在图中插入边，前提是fromLabel和toLabel都在图中存在
    public void insertEdge(String fromLabel, String toLabel, int weight){
        Vertex fromV = this.vertices.get(fromLabel);
        Vertex toV = this.vertices.get(toLabel);
        if (fromV == null || toV == null){
            System.out.printf("Insert Edge Error! Don't have %s or %s.", fromLabel, toLabel);
        }else{
            fromV.addOutEdge(weight, toV.getID());
            toV.addInEdge(weight, fromV.getID());
        }
    }

//    //由超级节点构建图结构，不需要Label！！！也不需要记录this.vertices了
//
//    //插入节点时返回节点ID
//    private Integer insertSuperV(){
//        Vertex vertex = new Vertex(this.vtCount++, "");
//        this.vIDs.add(vertex);
//        return vertex.getID();
//    }
//
//    private void insertSuperE(Integer fromID, Integer toID, int weight){
//        Vertex fromV = this.vIDs.get(fromID);
//        Vertex toV = this.vIDs.get(toID);
//        fromV.addOutEdge(weight, toID);
//        toV.addInEdge(weight, fromID);
//    }

    //计算图中所有边的权重之和
    private int sumWeight(){
        int sw = 0;
        for (Vertex v : this.vIDs){
            sw += v.selectOutWeight();
        }
        return sw;
    }

    //给定fromID和toID，返回边权重
    private int findWeight(Integer fromID, Integer toID){
        int result = 0;
        Vertex fromV = this.vIDs.get(fromID);
        if (fromV.getOutEdges().containsKey(toID)){
            result = fromV.getOutEdges().get(toID);
        }
        return result;
    }

    //查询给定label是否为图中Vertex
    public boolean inGraph(String label){
        return this.vertices.containsKey(label);
    }

    //查询给定label对应的社区ID
    public Integer commIDbyLable(String label){
        Integer commID = -1;
        if (!inGraph(label)){
            System.out.printf("%s not in community!", label);
        }else{
            Vertex v = this.vertices.get(label);
            commID = v.getCommunityID();
        }
        return commID;
    }

    //查询给定label对应节点的社区贡献度
    public int commContribute(String label){
        int contri = 0;
        if (!inGraph(label)){
            System.out.printf("%s not in community!", label);
        }else{
            Vertex v = this.vertices.get(label);
            Integer commID = v.getCommunityID();
            for (Integer in : v.getInEdges().keySet()){
                if (this.vIDs.get(in).getCommunityID().equals(commID)){
                    contri += 1;
                }
            }
            for (Integer out : v.getOutEdges().keySet()){
                if (this.vIDs.get(out).getCommunityID().equals(commID)){
                    contri += 1;
                }
            }
        }
        return contri;
    }

    //test...delete 查询给定label对应的节点Vertex的输入节点label和输出节点label
    public String[] getInOutLabels(String label){
        String[] result = new String[]{"",""};
        if (!inGraph(label)){
            System.out.printf("%s not in community!", label);
        }else{
            Vertex v = this.vertices.get(label);
            Map<Integer,Integer> inEdges = v.getInEdges();
            Map<Integer,Integer> outEdges = v.getOutEdges();
            if (inEdges.size() > 0){
                for (Integer inID : inEdges.keySet()){
                    Vertex inV = this.vIDs.get(inID);
                    result[0] += inV.getLabel() + " ";
                }
            }
            if (outEdges.size() > 0){
                for (Integer outID : outEdges.keySet()){
                    Vertex outV = this.vIDs.get(outID);
                    result[1] += outV.getLabel() + " ";
                }
            }
        }
        return result;
    }

    //通过图中节点ID获取该节点的社区ID
    public Integer getCommbyV(Integer vID){
        Vertex vertex = this.vIDs.get(vID);
        return vertex.getCommunityID();
    }

    public Map<String, Vertex> getVertices() {
        return vertices;
    }

    public Integer getVtCount() {
        return this.vtCount;
    }

    //Louvain社区划分，返回每个节点属于的社区

    //计算将节点V划入社区comm的模块度增量deltaQ
    private double deltaQ(Integer VID, Integer commID, double w){
        int k_i_in = weight_iC(VID, commID);
        int sumTOT = this.commW.get(commID);
        int k_i = this.vIDs.get(VID).selectInWeight();
        return 2*k_i_in - sumTOT*k_i/w;
    }

    //计算整个社区的模块度Q
    private double calQ(double w){
        double Q = 0;
        for (Vertex i : this.vIDs){
            for (Vertex j : this.vIDs){
                if (i != j && i.getCommunityID().equals(j.getCommunityID())){
                    double w_ij = findWeight(i.getID(), j.getID());
                    int sisj = i.selectInWeight() * j.selectOutWeight();
                    Q += w_ij - sisj/w;
                }
            }
        }
        return Q / w;
    }

    //初始化社区映射
    private void initC(){
        for (Vertex v : this.vIDs){
            int commID = v.getCommunityID();
            if (!this.commV.containsKey(commID)){
                List<Integer> vs = new ArrayList<>();
                vs.add(v.getID());
                this.commV.put(commID, vs);
                int weight = v.selectInWeight();
                this.commW.put(commID, weight);
            }else{
                List<Integer> vs = this.commV.get(commID);
                vs.add(v.getID());
                this.commV.put(commID, vs);
                int weight = v.selectInWeight();
                this.commW.put(commID, weight + this.commW.get(commID));
            }
        }
    }

    //计算从节点i指向社区C的权和
    private int weight_iC(int vID, int commID){
        List<Integer> toV = this.commV.get(commID);
        int weight = 0;
        for (Integer j : toV){
            weight += findWeight(vID, j);
        }
        return weight;
    }

    //单层Louvain实现
    public double singleLouvain(){
        //初始化社区编号，每个节点的社区编号与其ID相等
        for (Vertex v : this.vIDs){
            v.setCommunityID(v.getID());
        }
        //初始化社区映射
        double w = sumWeight();
        initC();
        double lastQ = calQ(w);
        boolean flag = true;
        while (flag){
            flag = false;
            for (Vertex v : this.vIDs){
                Integer vID = v.getID();
                if (v.getOutEdges().size() > 0){
                    double maxQ = 0;
                    Integer maxNeigh = -1;
                    Integer oldCommID = v.getCommunityID();
                    for (Integer neigh : v.getOutEdges().keySet()){
                        Integer neighComm = this.vIDs.get(neigh).getCommunityID();
                        if (!oldCommID.equals(neighComm)){
                            //尝试将节点v划入节点neigh所在社区
                            double deltaQ = deltaQ(vID, neighComm, w);
                            if (deltaQ > maxQ){
                                maxQ = deltaQ;
                                maxNeigh = neighComm;
                            }
                        }
                    }
                    //如果maxNeigh不为-1，则将当前节点划入maxNeigh所在社区
                    if (maxNeigh != -1){
                        v.setCommunityID(maxNeigh);
                        //更新this.commV
                        List<Integer> oldComm = this.commV.get(oldCommID);
                        List<Integer> newComm = this.commV.get(maxNeigh);
                        oldComm.remove(v.getID());
                        newComm.add(v.getID());
                        this.commV.put(oldCommID, oldComm);
                        this.commV.put(maxNeigh, newComm);
                        //更新this.commW
                        int oldWeight = this.commW.get(oldCommID);
                        int newWeight = this.commW.get(maxNeigh);
                        this.commW.put(oldCommID, oldWeight-v.selectInWeight());
                        this.commW.put(maxNeigh, newWeight+v.selectInWeight());
                    }
                    if (maxQ > 0){
                        flag = true;
                    }
                }
            }
            double allQ = calQ(w);
            double deltaQ = allQ-lastQ;
            if (deltaQ<=0){
                break;
            }else {
                lastQ = allQ;
            }
        }
        return lastQ;
    }

    //统计现存的社区列表，以及每个社区的所有节点包名
    private Map<Integer, List<String>> countComm(){
        Map<Integer, List<String>> commLabel = new HashMap<>();

        for (Vertex v : this.vIDs){
            Integer commID = v.getCommunityID();
            String label = v.getLabel();
            if (!commLabel.containsKey(commID)){
                List<String> newlabel = new ArrayList<>();
                newlabel.add(label);
                commLabel.put(commID, newlabel);
            }else{
                commLabel.get(commID).add(label);
            }
        }
        return commLabel;
    }

    //输出一共有多少个社区以及每个社区的所有节点包名
    public void outputComm(){
        Map<Integer, List<String>> commLabel = countComm();
        System.out.printf("Total %d communities!\n", commLabel.size());
        for (Integer commID : commLabel.keySet()){
            System.out.printf("Community: %d\n", commID);
            for (String label : commLabel.get(commID)){
                System.out.printf("%s ", label);
            }
            System.out.println("\n--------------------");
        }
    }

//    //融合当前图社区中的节点形成超级节点，插入边，生成新的图结构，原始图节点所在社区ID<->新融合社区ID
//    private Map<Integer, Integer> mergeComm(Graph graph){
//        Map<Integer, Integer> old2new = new HashMap<>();
//        for (Integer oldVID : this.commV.keySet()){
//            Integer newVID = graph.insertSuperV();
//            old2new.put(oldVID, newVID);
//        }
//        //遍历当前图节点，插入边权重
//        for (Vertex fromV : this.vIDs){
//            for (Integer toID : fromV.getOutEdges().keySet()){
//                Vertex toV = this.vIDs.get(toID);
//                Integer fromC = old2new.get(fromV.getCommunityID());
//                Integer toC = old2new.get(toV.getCommunityID());
//                int weight = fromV.getOutEdges().get(toID);
//                graph.insertSuperE(fromC, toC, weight);
//            }
//        }
//        return old2new;
//    }
//
//    //多层Louvain实现
//    public void multiLouvain(){
//        //基于真实节点实现单层Louvain
//        double Q = singleLouvain();
//        while (true){
//            //构建新图，融合当前图中节点
//            Graph graph = new Graph();
//            //超级节点ID<->当前图中社区
//            Map<Integer, Integer> old2new = mergeComm(graph);
//            double nowQ = graph.singleLouvain();
//            if (Q > nowQ){
//                break;
//            }else{
//                //将超级节点的社区ID赋给当前图中节点
//                for (Integer oldID : old2new.keySet()){
//                    Integer newID = graph.getCommbyV(old2new.get(oldID));
//                    List<Integer> vlist = this.commV.get(oldID);
//                    for (Integer i : vlist){
//                        this.vIDs.get(i).setCommunityID(newID);
//                    }
//                }
//                Q = nowQ;
//            }
//        }
//        //输出当前社区划分结果
//        outputComm();
//    }
}
