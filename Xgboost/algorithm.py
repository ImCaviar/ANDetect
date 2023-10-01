# -*- encoding:utf-8 -*-
# Use xgboost to implement AN recognition model
import pandas as pd
import xgboost as xgb
from sklearn import model_selection

def read_data(path):
    df = pd.read_csv(path)
    df = df.iloc[1:,:]
    # count AN and NAN
    count = df.iloc[:,-1].values
    an = 0
    nan = 0
    for c in count:
        if c == "AN":
            an += 1
        else:
            nan += 1
    print(an, nan)
    df = df.sample(frac=1.0).values
    trainx, testx, trainy, testy = model_selection.train_test_split(df[:,0:-1],df[:,-1], test_size=0.3, random_state=0)
    label_map = {"AN": 1, "NAN": 0}
    # deal trainy and testy
    trainy = [label_map[y] for y in trainy]
    testy = [label_map[y] for y in testy]
    return trainx, trainy, testx, testy

# initial model
def model(trainx, trainy, testx, testy, boost_round, learning_rate, saveM):
    params = {'booster': 'gbtree',
              'objective': 'binary:logistic',
              'eval_metric': 'auc',
              'max_depth': 20,
              'lambda': 0.1,
              'subsample': 0.75,
              'colsample_bytree': 0.75,
              'min_child_weight': 1,
              'eta': 0.025,
              'seed': 0,
              'nthread': 8,
              'silent': 1,
              'gamma': 0.15,
              'learning_rate': learning_rate}

    dtrain = xgb.DMatrix(trainx,label=trainy)
    dtest = xgb.DMatrix(testx)
    watchlist = [(dtrain,'train')]

    bst = xgb.train(params,dtrain,num_boost_round=boost_round,evals=watchlist)
    bst.save_model(saveM)
    # ypred = bst.predict(dtest)
    # ypred = [round(value) for value in ypred]
    # acc = metrics.accuracy_score(testy,ypred)

def generate_model(lr, br, file_type):
    path = "fea_data/API_" + file_type + "_fea.csv"
    saveM = "xgb_model_" + file_type + ".json"
    trainx, trainy, testx, testy = read_data(path)
    # save model
    model(trainx, trainy, testx, testy, br, lr, saveM)


if __name__ == '__main__':
    # generate 3 xgboost model
    generate_model(0.05, 150, "count")
    generate_model(0.025, 250, "clazz")
    generate_model(0.1, 100, "method")

