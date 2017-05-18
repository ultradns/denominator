# Declarations
operation_logfile="service.log"
failedtests_logfile="failed_tests.log"
api_calls_logfile="api_call.log"
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

# Function to validate ZoneList service call
# $1 = Field 1 to valdiate
# $2 = Field 2 to valdiate
# $3 = Test Case Name
validateZoneListAll()
{
        validate=`grep -woe $1 -woe $2 $operation_logfile | wc -l`
        if [ $validate -ne 4 ];then
                  echo -e "\e[1;31m$3 Test Failed. $1, $2 Expected. Not found \e[0m"  | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$3 Test Passed"
        fi
}

# Test Data
zoneName1="testzone1-"`date +%s`".io."
zoneName2="testzone2-"`date +%s`".io."

# Endpoints 
zoneAdd1="zone add -n $zoneName1 -e test-1.email@neustar.biz -t 1111"
zoneAdd2="zone add -n $zoneName2 -e test-2.email@neustar.biz -t 1111"
zoneList="zone list -n $zoneName1"
zoneListNonExisting="zone list -n zoneNonExisting.io."
zoneListAll="zone list"
zoneDelete1="zone delete -i "$zoneName1
zoneDelete2="zone delete -i "$zoneName2

echo "----------------------------------------------------------------------------------------"
echo "Test Scenario: Zone - 02 - List"
echo "----------------------------------------------------------------------------------------"
echo "STARTING TESTS"
echo " "
# Add Zone 1
hitService "$zoneAdd1" > $operation_logfile 2> $api_calls_logfile
validateZoneAdd $zoneName1 "Add Zone 1"
echo "Zone Name = $zoneName1"

# Add Zone 2
hitService "$zoneAdd2" > $operation_logfile 2> $api_calls_logfile
validateZoneAdd $zoneName2 "Add Zone 2"
echo "Zone Name = $zoneName2"

# List All Zones
hitService "$zoneListAll" > $operation_logfile 2> $api_calls_logfile
validateZoneListAll $zoneName1 $zoneName2 "List All Zones"

# List Zone 1
hitService "$zoneList" > $operation_logfile 2>> $api_calls_logfile
validateZoneList $zoneName1 "List Zone 1"

# List non existing Zone
hitService "$zoneListNonExisting" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 404 Not Found" "List non existing Zone"

# Delete Zone 1
hitService "$zoneDelete1" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 204 No Content" "Delete Zone 1"
cat $operation_logfile >> $api_calls_logfile

# Delete Zone 2
hitService "$zoneDelete2" > $operation_logfile 2> $operation_logfile
validate "HTTP/1.1 204 No Content" "Delete Zone 2"
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