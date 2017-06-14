# Set parent directory to help execution via jenkins
# parent_dir=`pwd`

# Set Config parameters
jar_path=$jar_dir"/cli/build/libs/denominator-cli-0.1.0-SNAPSHOT-fat.jar"
provider="ultradnsrest"
main_class="denominator.cli.Denominator"

# Set Logfile parameters
operation_logfile="$parent_dir/e2e-scripts/service.log"
failedtests_logfile="$parent_dir/e2e-scripts/failed_tests.log"
api_calls_logfile="$parent_dir/e2e-scripts/api_call.log"
chmod 777 $parent_dir/e2e-scripts/*.log

# Export above declarations to other scripts
export jar_path
export provider
export main_class
export parent_dir
export operation_logfile
export failedtests_logfile
export api_calls_logfile
