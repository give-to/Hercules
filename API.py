#!/usr/bin/python
from pandas import DataFrame, read_excel
import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.model_selection import GridSearchCV
from sklearn.metrics import confusion_matrix
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.ensemble import AdaBoostClassifier
from sklearn import svm
from sklearn.metrics import roc_auc_score
from sklearn.metrics import accuracy_score
from sklearn.metrics import fbeta_score
from sklearn.metrics import recall_score
from sklearn.model_selection import GridSearchCV
from sklearn.decomposition import PCA
from sklearn.metrics import recall_score
from sklearn.metrics import f1_score
from sklearn.metrics import precision_score
from sklearn.metrics import accuracy_score
from imblearn.over_sampling import SMOTE
import matplotlib.pyplot as plt
import itertools
from sklearn.utils import shuffle
from sklearn.impute import SimpleImputer
from imblearn.over_sampling import SMOTE
from sklearn.model_selection import train_test_split, RandomizedSearchCV
from imblearn.pipeline import make_pipeline as imbalanced_make_pipeline
from sklearn.model_selection import GridSearchCV, cross_val_score, StratifiedKFold, learning_curve
from sklearn import svm
from sklearn.utils import shuffle
import xgboost as xgb
import sys
from sys import argv

    
    
if __name__ == '__main__':     
        testing_list = sys.path[0] + "/test.csv"

        testing = pd.read_csv(testing_list, encoding='latin1',index_col=False)
  
        X_test = testing.iloc[:,1:]
        id_test = testing.iloc[:,0]
        model = xgb.XGBClassifier()
        booster = xgb.Booster()
        booster.load_model(sys.path[0]+"/model.json")
        model._Booster = booster
        Y_pred = model.predict_proba(X_test)
        idList = id_test.tolist() 
        for X, Y in zip(idList, Y_pred):
            print("{},{}".format(X, Y[0]))
        #iter = 0
        #for Y in Y_pred:
            #print("{},{}".format(idList[iter], Y[0]))
            #iter = iter + 1
