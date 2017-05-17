# Declarations
operation_logfile="serivce.log"
failedtests_logfile="failed_tests.log"
api_calls_logfile="api_call.log"
fail=0
echo "Failed Tests" > $failedtests_logfile

# Function to hit denominator service
# $1 = Endpoint with Test Data
hitService()
{
eval "$hit $1"
}

# Function to validate service call
# $1 = Field to valdiate
# $2 = Test Case Name
validateZoneAdd()
{
        awk 'NR==2' $operation_logfile | grep -q $1   
        if [ $? != 0 ];then
                  echo -e "\e[1;31m$2 Test Failed. $1 Expected. Not found \e[0m" | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$2 Test Passed"
        fi
}

# Function to validate ZoneList service call
# $1 = Field to valdiate
# $2 = Test Case Name
validateZoneList()  
{
        validate=`grep -wo $1 $operation_logfile | wc -l`
        if [ $validate -ne 2 ];then
                  echo -e "\e[1;31m$2 Test Failed. $1 Expected. Not found \e[0m"  | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$2 Test Passed"
        fi
}

# Function to validate general service calls
# $1 = Field to valdiate
# $2 = Test Case Name
validate()
{
        grep -q "$1" $operation_logfile
        if [ $? != 0 ];then
                  echo -e "\e[1;31m$2 Test Failed. $1 Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$2 Test Passed"
        fi
}

validateGetResource()
{
        validate=`grep "$1" $operation_logfile | wc -l`
        if [ $validate -ne $2 ];then
                  echo -e "\e[1;31m$3 Test Failed. $1 Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$3 Test Passed"
        fi
}

# Test Data
zoneName="testzone"`date +%s`".io."
resourceName1="dir_pool1`date +%s`.$zoneName"
resourceName2="dir_pool2`date +%s`.$zoneName"

# Endpoints
zoneAdd="zone add -n $zoneName -e test.email@neustar.biz -t 1111"
invalidGetResourceRecordWithOwnerType="record --zone $zoneName get --name invalid.io. --type A"
getResourceRecordWithOwnerType1="record --zone $zoneName get --name $resourceName1 --type A"
getResourceRecordWithOwnerType2="record --zone $zoneName get --name $resourceName2 --type A"
createResourceRecordOne="record --zone $zoneName add --name $resourceName1 --type A --data 10.1.1.1"
createResourceRecordTwo="record --zone $zoneName add --name $resourceName2 --type A --data 10.2.2.2 --data 10.3.3.3 --data 10.4.4.4"
createResourceRecordThree="record --zone $zoneName add --name $resourceName1 --type A --data 10.2.2.2 --data 10.3.3.3 --data 10.4.4.4 --data 10.5.5.5"

zoneList="zone list -n $zoneName"
zoneDelete="zone delete -i "$zoneName

# E2E Script
echo "----------------------------------------------------------------------------------------"
echo "Test Scenario: Resource Record Sets - 01 - Add"
echo "----------------------------------------------------------------------------------------"
echo "STARTING TESTS"
echo " "

# Add Zone
hitService "$zoneAdd" > $operation_logfile 2>> $api_calls_logfile
validateZoneAdd $zoneName "Add Zone"
echo "Zone Name = $zoneName"

#  Get resource record set with the owner of type A
hitService "$invalidGetResourceRecordWithOwnerType" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 404 Not Found" "Get resource record set which does not exist"
cat $operation_logfile >> $api_calls_logfile

# Create a resource record set with the owner of type A by providing a single record
hitService "$createResourceRecordOne" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Create a resource record set with the owner of type A by providing a single record"
echo "Resource Name = $resourceName1"

#  Get resource record set with the owner of type A
hitService "$getResourceRecordWithOwnerType1" > $operation_logfile 2> $operation_logfile
validate "$resourceName1" "Get resource record set with the owner of type A - single record"
cat $operation_logfile >> $api_calls_logfile

# Create a resource record set with the owner of type A by providing multiple records
hitService "$createResourceRecordTwo" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Create a resource record set with the owner of type A by providing multiple records"
echo "Resource Name = $resourceName2"

#  Get resource record set with the owner of type A
hitService "$getResourceRecordWithOwnerType2" > $operation_logfile 2> $operation_logfile
validate "$resourceName2" "Get resource record set with the owner of type A - multiple records"
cat $operation_logfile >> $api_calls_logfile

# Add records to the existing resource record set with the owner of type A
hitService "$createResourceRecordThree" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Add records to the existing resource record set with the owner of type A"

#  Get resource record set with the owner of type A
hitService "$getResourceRecordWithOwnerType1" > $operation_logfile 2> $operation_logfile
validate "$resourceName1" "Get resource record set with the owner of type A - add to existing record"
cat $operation_logfile >> $api_calls_logfile

# Delete Zone
hitService "$zoneDelete" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 204 No Content" "Delete Zone"
cat $operation_logfile >> $api_calls_logfile

# List Deleted Zone
hitService "$zoneList" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 404 Not Found" "List Deleted Zone"
cat $operation_logfile >> $api_calls_logfile

echo " "
echo "TESTS COMPLETED"
echo "----------------------------------------------------------------------------------------"

# Return Zero/Non-Zero for script
if [ $fail -eq 1 ];then
    echo -e "\e[1;31mFound Test Failures \e[0m"
    echo " "
    cat $failedtests_logfile
    exit 1
else
    echo -e "\e[1;32mAll Tests successfully passed \e[0m"
fi
echo "----------------------------------------------------------------------------------------"