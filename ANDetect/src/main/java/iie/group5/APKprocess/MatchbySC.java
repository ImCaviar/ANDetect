package iie.group5.APKprocess;

import iie.group5.features.SmithWaterman;
import iie.group5.structure.*;
import iie.group5.structure.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    //返回匹配度
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
            //删除没有matchEdge的Twig
            for (Edge me : this.matchEdge){
                this.twigAN.removeIf(twig -> !twig.isEdgeIn(me));
                this.twigSubM.removeIf(twig -> !twig.isEdgeIn(me));
            }
            compDM();
        }else{
            this.DM = 0.0;
        }
    }

    private void compCode(){
        //根据两个module分配其节点下对应的smali文件
        smali2map(this.AN.getSmalis(), this.an2smali);
        smali2map(this.subM.getSmalis(), this.sub2smali);
        //比较两个smali列表中的代码匹配度
        int lenSL = an2sub.keySet().size();
        if (lenSL == 0){
            this.CM = 0.0;
        }else{
            for (Integer anID : an2sub.keySet()){
                if (an2smali.containsKey(anID)){
                    Integer subID = an2sub.get(anID);
                    List<Smali> anSL = an2smali.get(anID);
                    List<Smali> subSL = sub2smali.get(subID);
                    this.CM += smaliListSim(anSL, subSL);
                }
            }
            this.CM = this.CM/lenSL;
        }
    }

    //处理Module生成Edge列表和Twig列表，isAN表示是否为广告网络库
    private void genEdges(Module module, boolean isAN){
        Map<Integer,PackagePoint> i2p = module.getPackagePoints();
        for (Integer id : i2p.keySet()){
            PackagePoint pp = i2p.get(id);
            List<Integer> children = pp.getChildrenID();
            if (children.size() == 0){
                //叶子节点，找到twig
                Twig twig = new Twig(module.findTwigbyID(id));
                if (isAN){
                    this.twigAN.add(twig);
                }else{
                    this.twigSubM.add(twig);
                }
            }else{
                //非叶子节点，只要不是smali或者classes就可以加入边（该节点和所有子节点）
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

    //计算节点对匹配度，以AN为基准
    private void compEM(){
        double matched = 0;
        int allEdges = this.edgeAN.size();
        //每配对一个，就删除edgeSubM中的一个值
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

    //计算树枝匹配深度均值，以AN为基准
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
                //只有当最大匹配度大于0.5时才消除子模块树枝，并插入节点对
                if (maxDM > 0.75){
                    insertPP(ant, maxTwig);
                    this.twigSubM.remove(maxTwig);
                    depthSUM += maxDM;
                }
            }
            this.DM = depthSUM/allTwigs;
        }
    }

    //插入匹配树枝的节点对
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

    //比较两个树枝的匹配深度
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

    //根据smali的ppID将其分配到不同节点对应的list中
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

    //输入两个Smali列表，如果存在同名文件，则比较同名文件的匹配度并删除，对于剩余的不同名文件，则两两匹配，删除最大匹配对，直到一个列表为空
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

    //输入两个Smali文件，比较文件的匹配度
    private double match2Smali(Smali anS, Smali subS){
        //计算字段匹配度
        String[] anF = anS.getField();
        String[] subF = subS.getField();
        SmithWaterman sw = new SmithWaterman(anF, subF);
        double matchedF = sw.pctMacth();
        int lenF = anF.length;
        //计算方法匹配度
        String[] anM = anS.getMethod();
        String[] subM = subS.getMethod();
        sw = new SmithWaterman(anM, subM);
        double matchedM = sw.pctMacth();
        int lenM = anM.length;
        if (lenF == 0 && lenM == 0){
            return 0;
        }else if (lenF ==0 && lenM != 0){
            return matchedM/lenM;
        }else if (lenF !=0 && lenM == 0){
            return matchedF/lenF;
        }else{
            return 0.3*matchedF/lenF + 0.7*matchedM/lenM;
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
