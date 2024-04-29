SCRIPT_DIR=$(pwd)
ROOT_DIR=$1
#FLroot=$SCRIPT_DIR/105SampleBugsResult
#catenaD4jHome=/CatenaD4J
#bugListFile=$SCRIPT_DIR/105_bugs_list.txt
catenaD4jHome=$2
FLroot=$3
bugListFile=$4

cat ${bugListFile} | while read item
do
    echo $item
    PID=`echo $item | awk '{split($1,arr,"_");print(arr[1])}'`
    BID=${item#*_}
    BID_NUM=`echo $item | awk '{split($1,arr,"_");print(arr[2])}'`
    CID_NUM=`echo $item | awk '{split($1,arr,"_");print(arr[3])}'`
#    echo ${PID}:${BID_NUM}:${CID_NUM}
    PROJECT_DIR=${ROOT_DIR}/${PID}_${BID_NUM}_${CID_NUM}
    echo ${PROJECT_DIR}
    # Checkout CatenaD4J project
     if [ ! -d "${PROJECT_DIR}" ]; then
       # Script statements if $DIR not exists.
        echo "project dir is not exists"
        echo PID:${PID}:BID:${BID_NUM}:CID:${CID_NUM}
        catena4j checkout -p ${PID} -v ${BID_NUM}b${CID_NUM} -w ${PROJECT_DIR}
     fi
    cd ${PROJECT_DIR}
    defects4j compile
done

#Generate Patches
jarGeneratorAndRank=Hercules-1.0-SNAPSHOT-jar-with-dependencies.jar
echo "java -cp $jarGeneratorAndRank org.example.hercules.Main_NFL $ROOT_DIR $catenaD4jHome $FLroot $bugListFile"
java -cp $jarGeneratorAndRank org.example.hercules.Main_NFL $ROOT_DIR $catenaD4jHome $FLroot $bugListFile

#Validation
echo ${item}" Validation start"
python3 hercules_valid_v2.py ${ROOT_DIR}
echo ${item}" Validation end"
