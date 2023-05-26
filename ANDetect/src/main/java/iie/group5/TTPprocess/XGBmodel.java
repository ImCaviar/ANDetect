package iie.group5.TTPprocess;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

public class XGBmodel {
    private Booster api_count;
    private Booster api_clazz;
    private Booster api_method;

    public XGBmodel() throws XGBoostError {
        this.api_count = XGBoost.loadModel("resources/model/xgb_model_count.json");
        this.api_clazz = XGBoost.loadModel("resources/model/xgb_model_clazz.json");
        this.api_method = XGBoost.loadModel("resources/model/xgb_model_method.json");
    }

    //为一条数据进行分类 fr为0.0时表示AN，fr为1.0时表示NAN
    public float[] classify(float[] APIc, float[] APIcl, float[] APIm) throws Exception {
        float missing = 0.0f;
        DMatrix api_count_matrix = new DMatrix(APIc, 1, APIc.length, missing);
        float[][] ac_result = this.api_count.predict(api_count_matrix);

        DMatrix api_clazz_matrix = new DMatrix(APIcl, 1, APIcl.length, missing);
        float[][] acl_result = this.api_clazz.predict(api_clazz_matrix);

        DMatrix api_method_matrix = new DMatrix(APIm, 1, APIm.length, missing);
        float[][] am_result = this.api_method.predict(api_method_matrix);

        return new float[]{ac_result[0][0], acl_result[0][0], am_result[0][0]};
    }
}
