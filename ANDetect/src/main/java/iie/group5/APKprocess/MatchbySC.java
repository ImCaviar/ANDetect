package iie.group5.APKprocess;

import iie.group5.features.SmithWaterman;
import iie.group5.structure.*;
import iie.group5.structure.Module;

import java.util.*;

//利用结构、代码特征进行匹配
public class MatchbySC {
    private Module AN;
    private Module subM;
    private Double EM;
    private Double DM;
    private Double CM;
    private List<Edge> edgeAN;
    private List<Edge> edgeSubM;
    private List<Edge> matchEdge;
    private List<Twig> twigAN;
    private List<Twig> twigSubM;
    private Map<Integer, Integer> an2sub;
    private Map<Integer, List<Smali>> an2smali;
    private Map<Integer, List<Smali>> sub2smali;

    public MatchbySC(Module AN, Module subM) {
        this.AN = AN;
        this.subM = subM;
        this.EM = 0.0;
        this.DM = 0.0;
        this.CM = 0.0;
        this.an2sub = new HashMap<>();
        this.edgeAN = new ArrayList<>();
        this.edgeSubM = new ArrayList<>();
        this.matchEdge = new ArrayList<>();
        this.twigAN = new ArrayList<>();
        this.twigSubM = new ArrayList<>();
        this.an2smali = new HashMap<>();
        this.sub2smali = new HashMap<>();
    }

    // Return Match
    public double compAll(){
        compStruct();
        compCode();
        return (this.EM+this.DM+this.CM)/3;
    }

    private void compStruct(){
        genEdges(this.AN, true);
        genEdges(this.subM, false);
        compEM();
        if (this.EM > 0){
            // Remove Twig without matchEdge with iterator
            deleteNoMatchEdge(this.twigAN);
            deleteNoMatchEdge(this.twigSubM);
            compDM();
        }else{
            this.DM = 0.0;
        }
    }

    private void compCode(){
        // Assign the corresponding smali files under the two modules according to their nodes
        smali2map(this.AN.getSmalis(), this.an2smali);
        smali2map(this.subM.getSmalis(), this.sub2smali);
        // Compare the code matches in two smali lists
        int lenSL = an2sub.keySet().size();
        if (lenSL == 0){
            this.CM = 0.0;
        }else{
            for (Integer anID : an2sub.keySet()){
                Integer subID = an2sub.get(anID);
                if (an2smali.containsKey(anID) && sub2smali.containsKey(subID)){
                    List<Smali> anSL = an2smali.get(anID);
                    List<Smali> subSL = sub2smali.get(subID);
                    this.CM += smaliListSim(anSL, subSL);
                }
            }
            this.CM = this.CM/lenSL;
        }
    }

    // Process Module to generate Edge list and Twig list, isAN indicates if it is an ad network library
    private void genEdges(Module module, boolean isAN){
        Map<Integer,PackagePoint> i2p = module.getPackagePoints();
        for (Integer id : i2p.keySet()){
            PackagePoint pp = i2p.get(id);
            List<Integer> children = pp.getChildrenID();
            if (children.size() == 0){
                // Leaf node, find twig
                Twig twig = new Twig(module.findTwigbyID(id));
                if (isAN){
                    this.twigAN.add(twig);
                }else{
                    this.twigSubM.add(twig);
                }
            }else{
                // Non-leaf nodes, as long as they are not smali or classes can join edges (this node and all children)
                if (!pp.getLabel().contains("smali") && !pp.getLabel().contains("classes")){
                    for (Integer childID : children){
                        Edge edge = new Edge(pp.getLabel(), i2p.get(childID).getLabel());
                        if (isAN){
                            this.edgeAN.add(edge);
                        }else{
                            this.edgeSubM.add(edge);
                        }
                    }
                }
            }
        }
    }

    // Calculate the node pair matching degree to AN
    private void compEM(){
        double matched = 0;
        int allEdges = this.edgeAN.size();
        // Delete one value in edgeSubM for each pair
        for (Edge ane : this.edgeAN){
            if (this.edgeSubM.size() == 0){
                break;
            }else{
                for (Edge sube : this.edgeSubM){
                    if (ane.equals(sube)){
                        matched ++;
                        this.matchEdge.add(sube);
                        this.edgeSubM.remove(sube);
                        break;
                    }
                }
            }
        }
        this.EM = matched/allEdges;
    }

    // Calculate the mean value of branch matching depth, based on AN
    private void compDM(){
        double depthSUM = 0;
        int allTwigs = this.twigAN.size();
        if (allTwigs == 0){
            this.DM = 0.0;
        }else{
            for (Twig ant : this.twigAN){
                double maxDM = 0;
                Twig maxTwig = null;
                for (Twig subt : this.twigSubM){
                    double pct = pctDM(ant, subt);
                    if (pct > maxDM){
                        maxDM = pct;
                        maxTwig = subt;
                    }
                }
                // Eliminate submodule branches and insert node pairs only if the maximum match is greater than 0.5
                if (maxDM > 0.75){
                    insertPP(ant, maxTwig);
                    this.twigSubM.remove(maxTwig);
                    depthSUM += maxDM;
                }
            }
            this.DM = depthSUM/allTwigs;
        }
    }

    // Insert node pairs of matching tree branches
    private void insertPP(Twig ant, Twig subt){
        int ind = 0;
        while (ind < ant.getTwig().size() && ind < subt.getTwig().size()){
            if (ant.getTwig().get(ind).getLabel().equals(subt.getTwig().get(ind).getLabel())){
                this.an2sub.put(ant.getTwig().get(ind).getId(), subt.getTwig().get(ind).getId());
            }else {
                break;
            }
            ind ++;
        }
    }

    // Compare the matching depth of two branches
    private double pctDM(Twig ant, Twig subt){
        double matched = 0;
        int antSize = ant.getTwig().size();
        int ind = 0;
        while (ind < ant.getTwig().size() && ind < subt.getTwig().size()){
            if (ant.getTwig().get(ind).getLabel().equals(subt.getTwig().get(ind).getLabel())){
                matched ++;
            }else {
                break;
            }
            ind ++;
        }
        return matched/antSize;
    }

    // Assign smali to different node lists according to its ppID
    private void smali2map(List<Smali> smalis, Map<Integer, List<Smali>> id2smali){
        for (Smali s : smalis){
            Integer pID = s.getPpID();
            if (!id2smali.containsKey(pID)){
                List<Smali> stmp = new ArrayList<>();
                stmp.add(s);
                id2smali.put(pID, stmp);
            }else{
                id2smali.get(pID).add(s);
            }
        }
    }

    // Input two Smali lists, if there are files with the same name, compare the matches of files with the same name and delete them, for the remaining files with different names, match them two by two and delete the largest matching pair until a list is empty
    private double smaliListSim(List<Smali> anSL, List<Smali> subSL){
        double sumSim = 0;
        double ansAll = anSL.size();
        while (anSL.size()>0 && subSL.size()>0){
            Smali ans = anSL.get(0);
            double max = 0;
            Smali maxS = new Smali();
            for (Smali subs : subSL){
                double tmp = match2Smali(ans, subs);
                if (tmp > max){
                    max = tmp;
                    maxS = subs;
                }
            }
            sumSim += max;
            anSL.remove(ans);
            subSL.remove(maxS);
        }
        return sumSim/ansAll;
    }

    // Enter two Smali files and compare the file matches
    private double match2Smali(Smali anS, Smali subS){
        // Calculate field matching degree
        String[] anF = anS.getField();
        String[] subF = subS.getField();
        SmithWaterman sw = new SmithWaterman(anF, subF);
        double matchedF = sw.pctMacth();
        int lenF = anF.length;
        // Calculation method matching degree
        String[] anM = anS.getMethod();
        String[] subM = subS.getMethod();
        sw = new SmithWaterman(anM, subM);
        double matchedM = sw.pctMacth();
        int lenM = anM.length;
        if (lenF == 0 && lenM == 0){
            return 0;
        }else if (lenF == 0 && lenM != 0){
            return matchedM/lenM;
        }else if (lenM == 0){
            return matchedF/lenF;
        }else{
            return 0.3*matchedF/lenF + 0.7*matchedM/lenM;
        }
    }

    // Remove the twig that does not contain the matching edge with an iterator
    private void deleteNoMatchEdge(List<Twig> twig){
        Iterator<Twig> iterator = twig.iterator();
        while (iterator.hasNext()){
            Twig tw = iterator.next();
            // del controls whether to delete Twig
            boolean del = true;
            for (Edge me : this.matchEdge){
                if (tw.isEdgeIn(me)){
                    del = false;
                }
            }
            if (del){
                iterator.remove();
            }
        }
    }

    public Double getEM() {
        return EM;
    }

    public Double getDM() {
        return DM;
    }

    public Double getCM() {
        return CM;
    }
}
