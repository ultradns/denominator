# Declarations
operation_logfile="service.log"
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

getTokenFromGeoService()
{
token=`curl -k -POST -s --data "grant_type=password&username=npp-rest-test3a&password=Denominator_rest" https://test-restapi.ultradns.com/v2/authorization/token | awk -F'"' '{ print $12 }'`
}

createGeoResourceOneRecord(){
  curl -k -POST -s -H 'Content-Type:application/json' -H "Authorization: Bearer $1" --data '{
  "rdata": [
    "1.1.1.1"
  ],
  "profile": {
    "@context": "http://schemas.ultradns.com/DirPool.jsonschema",
    "description": "Great Geo Pool",
    "rdataInfo": [
      {
        "geoInfo": {
          "name": "Europe",
          "codes": [
            "ES"
          ]
        },
        "ttl": 86400,
        "type": "A"
      }
    ]
  }
}' https://test-restapi.ultradns.com/v2/zones/$2/rrsets/A/$3
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

# Function to validate AddTerritory service calls
# $1 = Field to valdiate
# $2 = Test Case Name
validateAddTerritory()
{
        grep revised $operation_logfile | grep -q $1
        if [ $? != 0 ];then
                  echo -e "\e[1;31m$2 Test Failed. $1 Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$2 Test Passed"
        fi
}

# Function to validate GeoList service calls
# $1 = Field to valdiate
# $2 = Test Case Name
validateGeoList()
{
        tail -1 $operation_logfile | grep -q $1
        if [ $? != 0 ];then
                  echo -e "\e[1;31m$2 Test Failed. $1 Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$2 Test Passed"
        fi
}

# Function to validate GeoList service calls after adding territory
# $1 = Field to valdiate
# $2 = Test Case Name
validateGeoListAfterAddingTerritory()
{
        tail -1 $operation_logfile | grep $1 | grep -q $2
        if [ $? != 0 ];then
                  echo -e "\e[1;31m$2 Test Failed. $1 Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$3 Test Passed"
        fi
}

# Function to validate GeoList service calls after adding territory - dry run
# $1 = Field to valdiate
# $2 = Test Case Name
validateGeoListAfterAddingTerritoryDryRun()
{
        tail -1 $operation_logfile | grep $1 | grep -qv $2
        if [ $? != 0 ];then
                  echo -e "\e[1;31m$2 Test Failed. $1 Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$3 Test Passed"
        fi
}

# Test Data
zoneName="TestZone"`date +%s`".io."
resourceName="dir_pool`date +%s`.$zoneName"

# Endpoints
zoneAdd="zone add -n $zoneName -e test.email@neustar.biz -t 1111"
listGeoResource="geo -z $zoneName get -n $resourceName -t A -g Europe"
dryRunAddTerritory="geo -z $zoneName add -n $resourceName -t A -g Europe -r "{\"Europe\":[\"Sweden\"]}" --dry-run"
addTerritory="geo -z $zoneName add -n $resourceName -t A -g Europe -r "{\"Europe\":[\"Sweden\"]}""
addInvalidRegion="geo -z $zoneName add -n $resourceName -t A -g Europe -r "{\"Europe\":[\"abcd\"]}" --validate-regions"
zoneList="zone list -n $zoneName"
zoneDelete="zone delete -i "$zoneName

# E2E Script
echo "----------------------------------------------------------------------------------------"
echo "Test Scenario: Geo Resource Record Sets - 01 - Add Territory"
echo "----------------------------------------------------------------------------------------"
echo "STARTING TESTS"
echo " "
# Add Zone
hitService "$zoneAdd" > $operation_logfile 2> $api_calls_logfile
validateZoneAdd $zoneName "Add Zone"
echo "Zone Name = $zoneName"

# Add Geo Resource Records
getTokenFromGeoService
createGeoResourceOneRecord $token $zoneName $resourceName > $operation_logfile 2> $operation_logfile
validate "Successful" "Add Geo Resource Records"
echo "Resource Name = $resourceName"
cat $operation_logfile >> $api_calls_logfile

# List Geo Resource Records based on Zone Name
hitService "$listGeoResource" > $operation_logfile 2>> $api_calls_logfile
validateGeoList "$resourceName" "List Geo Resource Records"

# Add Region or Territory - Dry Run
hitService "$dryRunAddTerritory" > $operation_logfile 2>> $api_calls_logfile
validateAddTerritory "Sweden" "Add Territory dry-run"

# List Geo Resource Records based on Zone Name
hitService "$listGeoResource" > $operation_logfile 2>> $api_calls_logfile
validateGeoListAfterAddingTerritoryDryRun "$resourceName" "Sweden" "List Geo Resource Records - Verify Territory not added"

# Add Region or Territory - Actual
hitService "$addTerritory" > $operation_logfile 2>> $api_calls_logfile
validateAddTerritory "Sweden" "Add Territory Actual"

# List Geo Resource Records based on Zone Name
hitService "$listGeoResource" > $operation_logfile 2>> $api_calls_logfile
validateGeoListAfterAddingTerritory "$resourceName" "Sweden" "List Geo Resource Records - Verify Added Territory"

# Add Region or Territory - Invalid Region
hitService "$addInvalidRegion" > $operation_logfile 2> $operation_logfile
validate "unsupported territories" "Add Region or Territory - Invalid Region"
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