#!/bin/bash
# Set parent directory to help execution via jenkins
# parent_dir=`pwd`

# URI, Username and Password provided to runner script as arguments
uri="$1"
username="$2"
password="$3"
export uri
export username
export password

# Execute config.sh to set configuration parameters such as jar_path, class, provider
. $parent_dir/config/config.sh

# Service call modifiers to hit Denominator
hitService="java -cp $jar_path $main_class -p $provider -u $uri -c $username -c $password"
hit=`echo $hitService`
export hit

# Print Test execution time in test_results report
echo "Test Execution Time: "`date` > test_results.log

# Scenario List

# Zone Scenarios

# Create, update, list and delete a zone
. $parent_dir/e2e-scripts/zone/Zone-01-CreateUpdate.sh | tee -a test_results.log

# List 2 created zones with list all
. $parent_dir/e2e-scripts/zone/Zone-02-List.sh | tee -a test_results.log

# Geo Record Scenarios

# Add territories to geo resource record set and list the records
. $parent_dir/e2e-scripts/geo-record/GeoResourceRecordSets-01-AddTerritories.sh | tee -a test_results.log

# Add territories to geo resource record set and and apply TTL to the records
. $parent_dir/e2e-scripts/geo-record/GeoResourceRecordSets-02-ApplyTTL.sh | tee -a test_results.log

# Add two records to geo resource and use various list modifiers to list the records
. $parent_dir/e2e-scripts/geo-record/GeoResourceRecordSets-03-List.sh | tee -a test_results.log

# Add geo records and list available regions for the records
. $parent_dir/e2e-scripts/geo-record/GeoResourceRecordSets-04-ListGeoParameters.sh | tee -a test_results.log

# Record Scenarios

# Create, get resource record set
. $parent_dir/e2e-scripts/record/ResourceRecordSets-01-Add_new.sh | tee -a test_results.log

# Create, get resource record set and replace existing resource record set
. $parent_dir/e2e-scripts/record/ResourceRecordSets-02-Replace.sh | tee -a test_results.log

# Create, get resource record set and remove existing resource record set
. $parent_dir/e2e-scripts/record/ResourceRecordSets-03-Remove.sh | tee -a test_results.log

# Create, get resource record set and apply TTL to resource record set
. $parent_dir/e2e-scripts/record/ResourceRecordSets-04-ApplyTTL.sh | tee -a test_results.log

# Create, get resource record set and delete existing resource record set
. $parent_dir/e2e-scripts/record/ResourceRecordSets-05-Delete.sh | tee -a test_results.log

# Create 4 resource record sets - A and NS, get resource record sets and list all records in a zone
. $parent_dir/e2e-scripts/record/ResourceRecordSets-05-Delete.sh | tee -a test_results.log
