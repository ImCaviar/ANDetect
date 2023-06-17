package iie.group5.structure;

import java.util.*;

public class Graph {
    // The original node in the graph, unchanged
    private Map<String, Vertex> vertices;
    private List<Vertex> vIDs;
    // The community network vertex counter, node ID, is also the index of the node in the vertices list
    private int vtCount;
    // Record all nodes in community C, the sum of weights pointing to all nodes in community C, and the sum of weights pointing to all nodes in community C. Dynamically update in multi-layer Louvain
    private Map<Integer, List<Integer>> commV;
    private Map<Integer, Integer> commW;
    private Map<Integer, Integer> commO;

    public Graph() {
        this.vertices = new HashMap<>();
        this.vIDs = new ArrayList<>();
        this.commV = new HashMap<>();
        this.commW = new HashMap<>();
        //add
        this.commO = new HashMap<>();
        this.vtCount = 0;
    }

    // Nodes in the community network
    public class Vertex{
        private Integer ID;
        private String label;
        private Integer communityID;
        // outEdges are the outgoing edges of V, inEdges are the incoming edges of V, node ID <-> weight
        private Map<Integer, Integer> outEdges;
        private Map<Integer, Integer> inEdges;

        public Vertex(Integer ID, String label) {
            this.ID = ID;
            this.label = label;
            this.outEdges = new HashMap<>();
            this.inEdges = new HashMap<>();
        }

        // Adding out edges to vertices
        public void addOutEdge(int weight, Integer toID){
            // If the edge already exists, the weights are superimposed; if not, the edge is created
            if (this.outEdges.size() == 0 || !this.outEdges.containsKey(toID)){
                this.outEdges.put(toID, weight);
            }else{
                int newWeight = this.outEdges.get(toID);
                this.outEdges.put(toID, newWeight + weight);
            }
        }

        // Adding an incoming edge to a vertex
        public void addInEdge(int weight, Integer inID){
            // If the edge already exists, the weights are superimposed; if not, the edge is created
            if (this.inEdges.size() == 0 || !this.inEdges.containsKey(inID)){
                this.inEdges.put(inID, weight);
            }else{
                int newWeight = this.inEdges.get(inID);
                this.inEdges.put(inID, newWeight + weight);
            }
        }

        // Query all out degrees of this node
        public int selectOutWeight(){
            int outWeight = 0;
            if (this.outEdges.size() > 0){
                for (int w : this.outEdges.values()){
                    outWeight += w;
                }
            }
            return outWeight;
        }

        // Query all the incoming degrees of this node
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


    // Insert nodes that have not appeared in the graph, synchronously, VertexID=this.vertices.index=this.labelsV.index
    public void insertVertex(String label){
        if (!this.vertices.containsKey(label)){
            Vertex vertex = new Vertex(this.vtCount++, label);
            this.vertices.put(label, vertex);
            this.vIDs.add(vertex);
        }
    }

    // Insert an edge in the graph, provided that both fromLabel and toLabel are present in the graph
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


    // Calculate the sum of the weights of all edges in the graph
    private int sumWeight(){
        int sw = 0;
        for (Vertex v : this.vIDs){
            sw += v.selectOutWeight();
        }
        return sw;
    }

    // Given fromID and toID, return the edge weights
    private int findWeight(Integer fromID, Integer toID){
        int result = 0;
        Vertex fromV = this.vIDs.get(fromID);
        if (fromV.getOutEdges().containsKey(toID)){
            result = fromV.getOutEdges().get(toID);
        }
        return result;
    }

    // Query whether the given label is the Vertex in the graph
    public boolean inGraph(String label){
        return this.vertices.containsKey(label);
    }

    // Query the community ID corresponding to the given label
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

    // Query the community contribution of the node corresponding to a given label
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

    //test...delete Query the input node label and output node label of the node Vertex corresponding to the given label
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

    // Get the community ID of the node by the node ID in the graph
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

    // Louvain, returning the community to which each node belongs

    private double deltaQ(Integer VID, Integer commID, double w){
        // Node VID Original Community
        Vertex tmpV = this.vIDs.get(VID);
        Integer origComm = tmpV.getCommunityID();
        int deltaW = weight_Ci(commID, VID) + weight_iC(VID, commID) - weight_Ci(origComm, VID) - weight_Ci(VID, origComm);
        int sumTOT = (tmpV.selectOutWeight() * this.commW.get(commID) + tmpV.selectInWeight() * this.commO.get(commID)) -
                (tmpV.selectOutWeight() * (this.commW.get(origComm) - tmpV.selectInWeight()) + tmpV.selectInWeight() * (this.commO.get(origComm) - tmpV.selectOutWeight()));
        return deltaW - sumTOT/w;
    }

    // Calculate the modularity of the whole community Q
    private double calQ(double w){
        double Q = 0;
        for (Vertex i : this.vIDs){
            for (Vertex j : this.vIDs){
                if (i != j && i.getCommunityID().equals(j.getCommunityID())){
                    double w_ij = findWeight(i.getID(), j.getID());
                    int sisj = i.selectOutWeight() * j.selectInWeight();
                    Q += w_ij - sisj/w;
                }
            }
        }
        return Q / w;
    }

    // Initializing Community Mapping
    private void initC(){
        for (Vertex v : this.vIDs){
            int commID = v.getCommunityID();
            if (!this.commV.containsKey(commID)){
                List<Integer> vs = new ArrayList<>();
                vs.add(v.getID());
                this.commV.put(commID, vs);
                int weight = v.selectInWeight();
                this.commW.put(commID, weight);
                //add
                weight = v.selectOutWeight();
                this.commO.put(commID, weight);
            }else{
                List<Integer> vs = this.commV.get(commID);
                vs.add(v.getID());
                this.commV.put(commID, vs);
                int weight = v.selectInWeight();
                this.commW.put(commID, weight + this.commW.get(commID));
                //add
                weight = v.selectOutWeight();
                this.commO.put(commID, weight + this.commO.get(commID));
            }
        }
    }

    // Compute the weight sum from node i to community C
    private int weight_iC(int vID, int commID){
        List<Integer> toV = this.commV.get(commID);
        int weight = 0;
        for (Integer j : toV){
            weight += findWeight(vID, j);
        }
        return weight;
    }

    // Compute the weight sum from community C to node i
    private int weight_Ci(int commID, int vID){
        List<Integer> fromV = this.commV.get(commID);
        int weight = 0;
        for (Integer j : fromV){
            weight += findWeight(j, vID);
        }
        return weight;
    }

    //rollback Restore the node community
    private void rollback(Map<Integer, Integer> origComm){
        for (Integer i : origComm.keySet()){
            Vertex v = this.vIDs.get(i);
            v.setCommunityID(origComm.get(i));
        }
    }

    //Single-Louvain
    public double singleLouvain(){
        // Initialize the community number, each node's community number is equal to its ID
        for (Vertex v : this.vIDs){
            v.setCommunityID(v.getID());
        }
        // Initializing Community Mapping
        double w = sumWeight();
        initC();
        double lastQ = calQ(w);
        boolean flag = true;
        while (flag){
            flag = false;
            // Record the original community number
            Map<Integer, Integer> id2comm = new HashMap<>();
            for (Vertex v : this.vIDs){
                Integer vID = v.getID();
                id2comm.put(vID, v.getCommunityID());
                if (v.getOutEdges().size() > 0){
                    double maxQ = 0;
                    Integer maxNeigh = -1;
                    Integer oldCommID = v.getCommunityID();
                    for (Integer neigh : v.getOutEdges().keySet()){
                        Integer neighComm = this.vIDs.get(neigh).getCommunityID();
                        if (!oldCommID.equals(neighComm)){
                            // Try to assign node v to the community where node neigh is located
                            double deltaQ = deltaQ(vID, neighComm, w);
                            if (deltaQ > maxQ){
                                maxQ = deltaQ;
                                maxNeigh = neighComm;
                            }
                        }
                    }
                    // If maxNeigh is not -1, then the current node is assigned to the community where maxNeigh is located
                    if (maxNeigh != -1){
                        v.setCommunityID(maxNeigh);
                        // Update this.commV
                        List<Integer> oldComm = this.commV.get(oldCommID);
                        List<Integer> newComm = this.commV.get(maxNeigh);
                        oldComm.remove(v.getID());
                        newComm.add(v.getID());
                        this.commV.put(oldCommID, oldComm);
                        this.commV.put(maxNeigh, newComm);
                        // Update this.commW
                        this.commW.put(oldCommID, this.commW.get(oldCommID)-v.selectInWeight());
                        this.commW.put(maxNeigh, this.commW.get(maxNeigh)+v.selectInWeight());
                        // Update this.commO
                        this.commO.put(oldCommID, this.commO.get(oldCommID)-v.selectOutWeight());
                        this.commO.put(maxNeigh, this.commO.get(maxNeigh)+v.selectOutWeight());
                    }
                    if (maxQ > 0){
                        flag = true;
                    }
                }
            }
            double allQ = calQ(w);
            double deltaQ = allQ-lastQ;
            if (deltaQ<=0){
                //add rollback
                rollback(id2comm);
                break;
            }else {
                lastQ = allQ;
            }
        }
        return lastQ;
    }

    // Count the list of existing communities and the names of all node packages for each community
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

    // Output how many communities there are and the names of all node packages for each community
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
}
