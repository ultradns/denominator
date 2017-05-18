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
                  echo "$2 - Test Passed"
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
                  echo "$2 - Test Passed"
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
                  echo "$2 - Test Passed"
        fi
}

validateGetResource()
{
        validate=`grep "$1" $operation_logfile | wc -l`
        if [ $validate -ne $2 ];then
                  echo -e "\e[1;31m$3 Test Failed. $1 Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$3 - Test Passed"
        fi
}

validateGetResourceData()
{
        validate=`grep -e "$1" -e "$2" $operation_logfile | wc -l`
        if [ $validate -ne 2 ];then
                  echo -e "\e[1;31m$3 Test Failed. $1, $2 Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$3 - Test Passed"
        fi
}

# Test Data
zoneName="testzone"`date +%s`".io."
resourceName1="dir_pool1`date +%s`.$zoneName"

# Endpoints
zoneAdd="zone add -n $zoneName -e test.email@neustar.biz -t 1111"
invalidGetResourceRecordWithOwnerType="record --zone $zoneName get --name invalid.io. --type A"
createResourceRecord="record --zone $zoneName add --name $resourceName1 --type A --data 10.1.1.1 --data 10.2.2.2"
getResourceRecordWithOwnerType="record --zone $zoneName get --name $resourceName1 --type A"
deleteResourceRecord="record --zone $zoneName delete --name $resourceName1 --type A"
deleteInvalidResourceRecord="record --zone $zoneName delete --name invalid.io. --type A"

zoneList="zone list -n $zoneName"
zoneDelete="zone delete -i "$zoneName

# E2E Script
echo "----------------------------------------------------------------------------------------"
echo "Test Scenario: Resource Record Sets - 05 - Delete"
echo "----------------------------------------------------------------------------------------"
echo "STARTING TESTS"
echo " "

# Add Zone
hitService "$zoneAdd" > $operation_logfile 2>> $api_calls_logfile
validateZoneAdd $zoneName "Add Zone"
echo "Zone Name = $zoneName"

#  Get resource record set with the owner of type A which does not exist
hitService "$invalidGetResourceRecordWithOwnerType" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 404 Not Found" "Get resource record set which does not exist"
cat $operation_logfile >> $api_calls_logfile

# Create a resource record set with the owner, type A by providing a two records
hitService "$createResourceRecord" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Create a resource record set with the owner, type A by providing a two records"
echo "Resource Name = $resourceName1"

#  Get resource record set with the owner of type A - two records
hitService "$getResourceRecordWithOwnerType" > $operation_logfile 2>> $api_calls_logfile
validateGetResourceData "10.1.1.1" "10.2.2.2" "Get resource record set with the owner of type A - multiple record"

#  Delete the resource record set with type A
hitService "$deleteResourceRecord" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Delete the resource record set with type A"

#  Get resource record set with the owner of type A - two records
hitService "$getResourceRecordWithOwnerType" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 404 Not Found" "Get resource record set with the owner of type A - two records - should not exist"
cat $operation_logfile >> $api_calls_logfile

#  Delete the resource record that doesnt exist
hitService "$deleteInvalidResourceRecord" > $operation_logfile 2>> $api_calls_logfile
validate ";; ok" "Delete the resource record that doesnt exist"

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