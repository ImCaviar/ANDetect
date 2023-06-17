package iie.group5.features;

import java.util.Arrays;
import java.util.Stack;

// Calculate sequence matching with SW algorithm
public class SmithWaterman {
    private int[][] H;
    private boolean[][] notEmpty;
    // A sequence of two strings to be matched
    private String[] anInv;
    private String[] subInv;
    // Score for blank, match, mismatch
    private static int SPACE;
    private static int MATCH;
    private static int DISMATCH;
    // Coordinates of the maximum match
    private int maxIndM, maxIndN;
    // Final matching sequence
    private Stack<String> stk1, stk2;

    public SmithWaterman(String[] anInv, String[] subInv) {
        this.anInv = anInv;
        this.subInv = subInv;
        int m = this.anInv.length;
        int n = this.subInv.length;
        this.H = new int[m+1][n+1];
        // Initialize to false
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

    // Calculate the sequence matching degree
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

    // Serialize the string in the stack
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
