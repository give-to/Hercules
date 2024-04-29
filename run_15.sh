SCRIPT_DIR=$(pwd)
bugListFile=$SCRIPT_DIR/105_bugs_list.txt
ROOT_DIR=/CatenaD4jProjects

cat ${bugListFile} | while read item
do
    echo $item
    PID=`echo $item | awk '{split($1,arr,"_");print(arr[1])}'`
    BID=${item#*_}
    BID_NUM=`echo $item | awk '{split($1,arr,"_");print(arr[2])}'`
#    CID_NUM=`echo $item | awk '{split($1,arr,"_");print(arr[3])}'`
    PROJECT_DIR=${ROOT_DIR}/${PID}_${BID_NUM}
    echo ${PROJECT_DIR}
    # Checkout defects4j project
     if [ ! -d "${PROJECT_DIR}" ]; then
        echo "project dir is not exists"
        echo PID:${PID}:BID:${BID_NUM}
        defects4j checkout -p ${PID} -v ${BID_NUM}b -w ${PROJECT_DIR}
     fi
    cd ${PROJECT_DIR}
    defects4j compile
done

FLroot=$SCRIPT_DIR/15BugsResult
catenaD4jHome=/CatenaD4J
jarGenerate=Hercules-1.0-SNAPSHOT-jar-with-dependencies.jar
echo "java -cp $jarAll org.example.hercules.Main_NFL $ROOT_DIR $FLroot $catenaD4jHome $bugListFile"
java -cp $jarGenerate org.example.hercules.Main_NFL $ROOT_DIR $catenaD4jHome $FLroot $bugListFile

#validation
echo ${item}" Validation start"
python3 hercules_valid_v2.py ${ROOT_DIR}
echo ${item}" Validation end"
