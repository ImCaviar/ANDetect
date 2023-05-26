package iie.group5.features;

import java.util.Arrays;
import java.util.Stack;

//用SW算法计算序列匹配度
public class SmithWaterman {
    private int[][] H;
    private boolean[][] notEmpty;
    //两个待匹配的字符串序列
    private String[] anInv;
    private String[] subInv;
    //空格、匹配、不匹配的得分
    private static int SPACE;
    private static int MATCH;
    private static int DISMATCH;
    //最大匹配度的坐标
    private int maxIndM, maxIndN;
    //最终匹配序列
    private Stack<String> stk1, stk2;

    public SmithWaterman(String[] anInv, String[] subInv) {
        this.anInv = anInv;
        this.subInv = subInv;
        int m = this.anInv.length;
        int n = this.subInv.length;
        this.H = new int[m+1][n+1];
        //初始化为false
        this.notEmpty = new boolean[m+1][n+1];
        SPACE = 1;
        MATCH = 3;
        DISMATCH = 0;
        this.maxIndM = 0;
        this.maxIndN = 0;
        stk1 = new Stack<>();
        stk2 = new Stack<>();
    }

    private int maxTriple(int a, int b, int c){
        int maxT;
        if (a >= b){
            maxT = a;
        }else{
            maxT = b;
        }
        if (c > maxT){
            maxT = c;
        }
        return maxT;
    }

    private void calMatrix(int m, int n){
        if (m == 0 || n == 0){
            this.H[m][n] = 0;
        }else{
            if (!this.notEmpty[m-1][n-1]){
                calMatrix(m-1, n-1);
            }
            if (!this.notEmpty[m][n-1]){
                calMatrix(m, n-1);
            }
            if (!this.notEmpty[m-1][n]){
                calMatrix(m-1, n);
            }
            if (this.anInv[m-1].equals(this.subInv[n-1])){
                this.H[m][n] = maxTriple(this.H[m-1][n-1]+MATCH, this.H[m][n-1]+SPACE, this.H[m-1][n]+SPACE);
            }else{
                this.H[m][n] = maxTriple(this.H[m-1][n-1]+DISMATCH, this.H[m][n-1]+SPACE, this.H[m-1][n]+SPACE);
            }
        }
        this.notEmpty[m][n] = true;
    }

    private void findMax(){
        int m = this.anInv.length + 1;
        int n = this.subInv.length + 1;
        int max = 0;
        for (int i=1; i<m; i++){
            for (int j=1; j<n; j++){
                if (this.H[i][j] > max){
                    max = this.H[i][j];
                    this.maxIndM = i;
                    this.maxIndN = j;
                }
            }
        }
    }

    private void traceBack(int m, int n){
        if (this.H[m][n] == 0){
            return;
        }
        if (this.anInv[m-1].equals(this.subInv[n-1])){
            stk1.add(this.anInv[m-1]);
            stk2.add(this.subInv[n-1]);
            traceBack(m-1, n-1);
        }else{
            if (this.H[m][n] == (this.H[m-1][n-1] + DISMATCH)){
                stk1.add(this.anInv[m-1]);
                stk2.add(this.subInv[n-1]);
                traceBack(m-1, n-1);
            }else if (this.H[m][n] == (this.H[m-1][n] + SPACE)){
                stk1.add(this.anInv[m-1]);
                stk2.add("-");
                traceBack(m-1, n);
            }else if (this.H[m][n] == (this.H[m][n-1] + SPACE)){
                stk1.add("-");
                stk2.add(this.subInv[n-1]);
                traceBack(m,n-1);
            }
        }
    }

    //计算序列匹配度
    public double pctMacth(){
        int m = this.anInv.length;
        int n = this.subInv.length;
        if (m == 0 || n == 0){
            return 0;
        }
        calMatrix(m, n);
        findMax();
        traceBack(this.maxIndM, this.maxIndN);
        double score = this.H[maxIndM][maxIndN];
        double sum = this.stk1.size() * MATCH;
        return score/sum;
    }

    //序列化栈中字符串
    public void outputSTK(){
        System.out.println("--------Seq 1-------");
        String seq1 = "";
        while (!this.stk1.empty()){
            seq1 += stk1.pop();
        }
        System.out.println(seq1);
        System.out.println("--------Seq 2-------");
        String seq2 = "";
        while (!this.stk2.empty()){
            seq2 += stk2.pop();
        }
        System.out.println(seq2);
    }
}
