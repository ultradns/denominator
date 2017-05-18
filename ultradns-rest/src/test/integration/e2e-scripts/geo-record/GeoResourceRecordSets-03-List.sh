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

createGeoResourceTwoRecords(){
  curl -k -POST -s -H 'Content-Type:application/json' -H "Authorization: Bearer $1" --data '
  {
  "rdata": [
    "1.1.1.1",
    "test-cname.com."
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
      },
      {
        "geoInfo": {
          "name": "Asia",
          "codes": [
            "NP"
          ]
        },
        "ttl": 86400,
        "type": "CNAME"
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

validateListGeoZone()
{
        validate=`grep -e CNAME -we A $operation_logfile | wc -l`
        if [ $validate -ne 2 ];then
                  echo -e "\e[1;31m$1 Test Failed. CNAME, A Records Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$1 Test Passed"
        fi
}

validateGeoZoneResourceWithType()
{
        validate=`grep -we A $operation_logfile | wc -l`
        if [ $validate -ne 1 ];then
                  echo -e "\e[1;31m$1 Test Failed. A Record Expected. Not found \e[0m"   | tee -a $failedtests_logfile
                  fail=1
        else
                  echo "$1 Test Passed"
        fi
}

# Endpoints with Test Data
zoneName="testzone"`date +%s`".io."
resourceName="dir_pool`date +%s`.$zoneName"

zoneAdd="zone add -n $zoneName -e test.email@neustar.biz -t 1111"
listGeoZone="geo -z $zoneName list"
listInvalidGeoZone="geo -z invalid.io. list"
listZoneGeoResource="geo -z $zoneName get -n $resourceName -t A -g Europe"
listInvalidGeoResource="geo -z $zoneName get -n $resourceName -t A -g Abcd"
listGeoZoneWithType="geo -z $zoneName list -t A"
listGeoZoneResourceWithType="geo -z $zoneName list -t A -n $resourceName"
listInvalidGeoWithType="geo -z $zoneName list -t FRA -n $resourceName"
zoneList="zone list -n $zoneName"
zoneDelete="zone delete -i "$zoneName

# E2E Script
echo "----------------------------------------------------------------------------------------"
echo "Test Scenario: Geo Resource Record Sets - 03 - List"
echo "----------------------------------------------------------------------------------------"
echo "STARTING TESTS"
echo " "

# Add Zone
hitService "$zoneAdd" > $operation_logfile 2>> $api_calls_logfile
validateZoneAdd $zoneName "Add Zone"
echo "Zone Name = $zoneName"

# Add Geo Resource Records
getTokenFromGeoService
createGeoResourceTwoRecords $token $zoneName $resourceName > $operation_logfile 2>> $api_calls_logfile
validate "Successful" "Add Geo Resource Records"
echo "Resource Name = $resourceName"

# List geo records under a zonename
hitService "$listGeoZone" > $operation_logfile 2>> $api_calls_logfile
validateListGeoZone "List Geo Resource Records"

# List geo records of a non existing zone
hitService "$listInvalidGeoZone" > $operation_logfile 2> $operation_logfile
validate "error: zone invalid.io. not found" "List geo records of a non existing zone"
cat $operation_logfile >> $api_calls_logfile

# List geo records under a zonename, ownername, groupname, type A
hitService "$listZoneGeoResource" > $operation_logfile 2>> $api_calls_logfile
validate "$resourceName" "List geo records under a zonename, ownername, groupname, type A"

# List geo records under a zonename, ownername, non existing groupname, with type A
hitService "$listInvalidGeoResource" > $operation_logfile 2>> $api_calls_logfile
# No Output

# List geo records under a zonename, and of type A
hitService "$listGeoZoneWithType" > $operation_logfile 2>> $api_calls_logfile
validateListGeoZone "List geo records under a zonename, and of type A"

# List geo records under a zonename with ownername and of type A
hitService "$listGeoZoneResourceWithType" > $operation_logfile 2>> $api_calls_logfile
validateGeoZoneResourceWithType "List geo records under a zonename with ownername and of type A"

# List geo records under a zonename with ownername and of type A
hitService "$listInvalidGeoWithType" > $operation_logfile 2>> $api_calls_logfile
# No Output

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