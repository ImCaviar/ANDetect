package iie.group5.structure;

import java.util.ArrayList;
import java.util.List;

public class Twig {
    private List<PackagePoint> twig;
    private List<Edge> edges;

    public Twig(List<PackagePoint> twig) {
        this.twig = twig;
        this.edges = new ArrayList<>();
        for (int i=0; i<twig.size()-1; i++){
            Edge edge = new Edge(this.twig.get(i+1).getLabel(), this.twig.get(i).getLabel());
            this.edges.add(edge);
        }
    }

    public boolean isEdgeIn(Edge edge){
        boolean result = false;
        for (Edge e : this.edges){
            if (e.equals(edge)){
                result = true;
                break;
            }
        }
        return result;
    }

    public PackagePoint getLeaf(){
        return this.twig.get(0);
    }

    public List<PackagePoint> getTwig() {
        return twig;
    }
}
