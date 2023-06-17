package iie.group5.structure;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//APK或模块的包结构，也是树结构
public class PackagePoint implements Serializable {
    // Node ID
    private Integer id;
    // Node labels, also known as package names
    private String label;
    // Parent node ID, set to null if it is the root node
    private Integer parentId;
    // List of child node IDs
    private transient List<Integer> childrenID;
    // The full package name represented by the node
    private transient String pkgName;
    // Community contribution, the number of connections to other nodes in the community
    private transient Integer contribute;
    // Terminator to stop processing
    private transient boolean term;

    public PackagePoint() {
    }

    public PackagePoint(Integer id, String label, Integer parentId) {
        this.id = id;
        this.label = label;
        this.parentId = parentId;
        this.childrenID = new ArrayList<>();
        this.pkgName = "";
        this.term = false;
        this.contribute = 0;
    }

    public PackagePoint(PackagePoint packagePoint){
        this.id = packagePoint.getId();
        this.label = packagePoint.getLabel();
        this.parentId = packagePoint.getParentId();
        this.childrenID = packagePoint.getChildrenID();
        this.pkgName = packagePoint.getPkgName();
        this.term = false;
        this.contribute = packagePoint.getContribute();
    }

    // Add child node ID
    public void addChildID(Integer cID){
        this.childrenID.add(cID);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public List<Integer> getChildrenID() {
        return childrenID;
    }

    public void setChildrenID(List<Integer> childrenID) {
        this.childrenID = childrenID;
    }

    public String getPkgName() {
        return pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    public Integer getContribute() {
        return contribute;
    }

    public void setContribute(Integer contribute) {
        this.contribute = contribute;
    }

    public boolean isTerm() {
        return term;
    }

    public void setTerm(boolean term) {
        this.term = term;
    }
}
