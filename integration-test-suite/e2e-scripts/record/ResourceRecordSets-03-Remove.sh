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

validateGetResourceData()
{
        validate=`grep -e "$1" -e "$2" $operation_logfile | wc -l`
        if [ $validate -ne 2 ];then
                  echo -e "\e[1;31m$3 Test Failed. $1, $2 Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$3 Test Passed"
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

# Endpoints
zoneAdd="zone add -n $zoneName -e test.email@neustar.biz -t 1111"
invalidGetResourceRecordWithOwnerType="record --zone $zoneName get --name invalid.io. --type A"
getResourceRecordWithOwnerType="record --zone $zoneName get --name $resourceName1 --type A"
createResourceRecord="record --zone $zoneName add --name $resourceName1 --type A --data 10.1.1.1 --data 10.2.2.2 --data 10.3.3.3 --data 10.4.4.4"
removeResourceRecord1="record --zone $zoneName remove --name $resourceName1 --type A --data 10.4.4.4"
removeResourceRecord2="record --zone $zoneName remove --name $resourceName1 --type A --data 10.1.1.1 --data 10.2.2.2 --data 10.3.3.3"

zoneList="zone list -n $zoneName"
zoneDelete="zone delete -i "$zoneName

# E2E Script
echo "----------------------------------------------------------------------------------------"
echo "Test Scenario: Resource Record Sets - 03 - Remove"
echo "----------------------------------------------------------------------------------------"
echo "STARTING TESTS"
echo " "

# Add Zone
hitService "$zoneAdd" > $operation_logfile 2>> $api_calls_logfile
validateZoneAdd $zoneName "Add Zone"
echo "Zone Name = $zoneName"

#  Get resource record set with the owner of type A which does not exist
hitService "$invalidGetResourceRecordWithOwnerType" > $operation_logfile 2>> $operation_logfile
validate "HTTP/1.1 404 Not Found" "Get resource record set which does not exist"
cat $operation_logfile >> $api_calls_logfile

# Create a resource record set with the owner, type A by providing a multiple record
hitService "$createResourceRecord" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Create a resource record set with the owner, type A by providing a multiple record"
echo "Resource Name = $resourceName1"

#  Get resource record set with the owner of type A - multiple records
hitService "$getResourceRecordWithOwnerType" > $operation_logfile 2>> $api_calls_logfile
validateGetResource "$resourceName1" "4" "Get resource record set with the owner of type A - 4 records"

# Remove a record from a resource record set with the owner, type A
hitService "$removeResourceRecord1" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Remove a record from a resource record set with the owner, type A"

#  Get resource record set with the owner of type A
hitService "$getResourceRecordWithOwnerType" > $operation_logfile 2>> $api_calls_logfile
validateGetResource "$resourceName1" "3" "Get resource record set with the owner of type A - 3 records"

# Remove 3 records from a resource record set with the owner, type A
hitService "$removeResourceRecord2" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Remove 3 records from a resource record set with the owner, type A"

#  Get resource record set with the owner of type A
hitService "$getResourceRecordWithOwnerType" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 404 Not Found" "Get resource record set with the owner of type A - 0 records"
cat $operation_logfile >> $api_calls_logfile

# Remove records from a non existing resource record set with the owner, type A
hitService "$removeResourceRecord1" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Remove records from a non existing resource record set with the owner, type A"

#  Get resource record set with the owner of type A
hitService "$getResourceRecordWithOwnerType" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 404 Not Found" "Get resource record set with the owner of type A - 0 records"
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