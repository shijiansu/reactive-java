#!/bin/bash

# utilities to print with prefix
function log {
  local message=$1
  echo "[PROJECT TEST] $message"
}

# execute maven clean test if there is pom.xml
function execute_maven {
  local d="$1"
  # use "pom.xml" to tell if maven project
  if [[ -f pom.xml ]]; then
    printf "[PROJECT TEST] %s: " "$d" # printf not to printing a line seperator
    # choose the maven exector, use maven wrapper or maven installed in local
    if [[ -f mvnw ]]; then response=$(./mvnw clean test); else response=$(mvn clean test); fi
    # use "BUILD SUCCESS" as successful indicator
    if [[ "$(echo "$response" | grep "BUILD SUCCESS")" != "" ]]; then # success
      echo "Test successfully... ..."
      succ=$((succ + 1))
    else
      echo "Test failed!!!"
      failed=$((failed + 1))
    fi
  fi
}

repo_test_report="$1" # taking from command parameter
BASEDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
echo "--------------------------------------------------------------------------------"
log "Execute 'maven clean test' for current or the sub-folders"
log "This can be improved by introducing a script but not directly calling maven so that it can include gradle project as well"
log "How to use:"
log "1. If there is command parameter, then it will enable 'repo_test_report' appending succ/failed results after execution;"
log "2. It uses 'src' to indicate if current project is the maven projecet, or it is parent project with sub-folder of maven projects;"
log "3. It is able to use parent maven project with maven modules,"
log "   inside the parent maven project, there are sub-folders of maven projects."
log "   Refer to 'microservice-dubbo - 1_dubbo-springboot2':"
log "   microservice-dubbo"
log "     |- run-repo-test.sh"
log "     |- run-project-test.sh"
log "     |- 1_dubbo-springboot2 (parent maven project)"
log "        |- run-project-test.sh"
log "        |- pom.xml (not with src, so will look for sub-folder to execute maven test)"
log "        |- dubbo-service"
log "          |- src"
log "          |- pom.xml"
log "        |- dubbo-provider"
log "        |- dubbo-consumer"
log "4. succ/failed count is with total of current 'run-project-test.sh' folder (total of  current maven project or sub-folders);"
log "5. This script can be execute without trigger by 'run-repo-test.sh', for individual project execution only."
log "Command: run-project-test.sh [repo_test_report]"
echo "--------------------------------------------------------------------------------"
log "v0.0.2 - 20200321"
log "e.g. "
log "$BASEDIR"
log ""

# execute all maven project(s) in this folder
succ=0
failed=0

# if it is singel project
if [[ -d "src" ]]; then
  project_name="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
  project_name=${project_name##*/}  # current folder without full path, for print out only
  execute_maven "$project_name"
else
# for multiple projects
  for d in *; do
    if [[ -d "$d" ]]; then
      cd "$d" || exit
      execute_maven "$d"
      cd "$BASEDIR" || exit
    fi
  done
fi

echo ""
log "Total success: $succ; Total failed: $failed"
echo ""

# append the project test result into repo rest report with formatted by tab
if [[ -f "$repo_test_report" ]]; then
  printf "%-50s   SUCCESS: %2d   FAILED: %2d\n" "${BASEDIR##*/}" $succ $failed >> "$repo_test_report"
fi
