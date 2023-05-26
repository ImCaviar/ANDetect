package iie.group5.structure;

public class Edge {
    private String parent;
    private String child;

    public Edge(String parent, String child) {
        this.parent = parent;
        this.child = child;
    }

    public boolean equals(Edge edge){
        if (this.parent.equals(edge.getParent()) && this.child.equals(edge.getChild())){
            return true;
        }
        return false;
    }

    public String getParent() {
        return parent;
    }

    public String getChild() {
        return child;
    }
}
