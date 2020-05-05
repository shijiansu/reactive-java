#!/bin/bash

# utilities to print with prefix
function log() {
  local message=$1
  echo "[REPO TEST] $message"
}

# execute project test if there is pom.xml
function execute_project_test() {
  local r="$1" # repo-test-report
  test="$(pwd)/run-project-test.sh"
  # because this script is a wrapper script, the real execution logic would be 'run-project-test.sh'
  if [[ -f "${test}" ]]; then
    /bin/bash "${test}" "$1"
  fi
}

clear
BASEDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
echo "-------------------------------------------------------------------------------------------"
echo "  ____                ____                      _ _                     _____         _    "
echo " |  _ \ _   _ _ __   |  _ \ ___ _ __   ___  ___(_) |_ ___  _ __ _   _  |_   _|__  ___| |_  "
echo " | |_) | | | | '_ \  | |_) / _ \ '_ \ / _ \/ __| | __/ _ \| '__| | | |   | |/ _ \/ __| __| "
echo " |  _ <| |_| | | | | |  _ <  __/ |_) | (_) \__ \ | || (_) | |  | |_| |   | |  __/\__ \ |_  "
echo " |_| \_\\__,_|_| |_| |_| \_\___| .__/ \___/|___/_|\__\___/|_|   \__, |   |_|\___||___/\__| "
echo "                               |_|                              |___/                      "
echo "-------------------------------------------------------------------------------------------"
# http://patorjk.com/software/taag/#p=display&f=Standard&t=Run%20Repository%20Test
log "Execute 'run-project-test.sh' for current or the sub-folders"
log "This is the wrapper of 'run-project-test.sh'"
log "How to use:"
log "1. By default it is single project because there is always 'run-project-test.sh' in root folder;"
log "2. Put '.not-project-test' to root folder, then it will scaner sub-folders;"
log "3. Print the test report with total succ/failed project test"
log "4. Enable 2 levels by adding '.not-project-test' to root folder and copying 'run-project-test.sh' to sub-folders"
log "   Refer to 'microservice-dubbo:"
log "   microservice-dubbo"
log "     |- .not-project-test (skip current folder - run-project-test.sh)"
log "     |- run-repo-test.sh"
log "     |- run-project-test.sh (will be skipped)"
log "       |- 1_dubbo-springboot2"
log "          |- run-project-test.sh (copied from root folder - will be triggered)"
log "          |- pom.xml"
log "          |- dubbo-service"
log "          |- dubbo-provider"
log "          |- dubbo-consumer"
log "Command: run-repo-test.sh"
echo "--------------------------------------------------------------------------------"
log "v0.0.1 - 20200321"
log "$BASEDIR"
log ""

# test report - to handle the test result from 'run-project-test.sh'
test_report="/tmp/repo-test-report-${BASEDIR##*/}-$(date "+%s")"
touch "$test_report"

# this part can be enahnced in the future
# if it is singel project
# '.not-project-test' is a file additionally indicates a overriding of not to execute as single project 
if [[ -f "run-project-test.sh" && ! -f ".not-project-test" ]]; then
  execute_project_test "$test_report"
else
  # for multiple projects
  for d in *; do
    if [[ -d "$d" ]]; then
      cd "$d" || exit
      execute_project_test "$test_report"
      cd "$BASEDIR" || exit
    fi
  done
fi

# read lines from test report
echo "-------------------------------------------------------------------------------------------"
log "TEST REPORT"
log ""
while IFS= read -r line; do
  log "$line"
done <"$test_report"
echo "-------------------------------------------------------------------------------------------"
