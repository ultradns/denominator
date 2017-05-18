# Declarations
fail=0
echo "Failed Tests" > $failedtests_logfile

hitService()
{
eval "$hit $1"
}

# Function to validate ZoneAdd service call
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

# Test Data
zoneName="testzone"`date +%s`".io."

# Endpoints
zoneAdd="zone add -n $zoneName -e test.email@neustar.biz -t 1111"
zoneList="zone list -n $zoneName"
zoneUpdate="zone update -i "$zoneName" -e test-2.email@neustar.biz -t 9999"
zoneDelete="zone delete -i "$zoneName

# E2E Script
echo "----------------------------------------------------------------------------------------"
echo "Test Scenario: Zone - 01 - Create/Update"
echo "----------------------------------------------------------------------------------------"
echo "STARTING TESTS"
echo " "
# Add Zone
hitService "$zoneAdd" > $operation_logfile 2> $api_calls_logfile
validateZoneAdd $zoneName "Add Zone"
echo "Zone Name = $zoneName"

# List Zone
hitService "$zoneList" > $operation_logfile 2>> $api_calls_logfile
validateZoneList $zoneName "List Zone"

# Add Same zone again
hitService "$zoneAdd" > $operation_logfile 2>> $api_calls_logfile
validateZoneAdd $zoneName "Add $zoneName again to update existing zone."

# Update zone
hitService "$zoneUpdate" > $operation_logfile 2>> $api_calls_logfile
validate "test-2.email@neustar.biz" "Update Zone - change email"

# List Updated Zone
hitService "$zoneList" > $operation_logfile 2>> $api_calls_logfile
validateZoneList $zoneName "List Zone"

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