#!/bin/bash

function execute_maven {
  local d="$1"
  if [[ -f pom.xml ]]; then # maven project
    printf "%s: " "$d" # no new line
    if [[ -f mvnw ]]; then response=$(./mvnw clean test); else response=$(mvn clean test); fi
    if [[ "$(echo "$response" | grep "BUILD SUCCESS")" != "" ]]; then # success
      echo "Test successfully... ..."
      succ=$((succ + 1))
    else
      echo "Test failed!!!"
      failed=$((failed + 1))
    fi
  fi
}

echo ""
echo "---------------------------------------------------------------------------"
echo "  ____                ____            _           _     _____         _    "
echo " |  _ \ _   _ _ __   |  _ \ _ __ ___ (_) ___  ___| |_  |_   _|__  ___| |_  "
echo " | |_) | | | | '_ \  | |_) | '__/ _ \| |/ _ \/ __| __|   | |/ _ \/ __| __| "
echo " |  _ <| |_| | | | | |  __/| | | (_) | |  __/ (__| |_    | |  __/\__ \ |_  "
echo " |_| \_\\__,_|_| |_| |_|   |_|  \___// |\___|\___|\__|   |_|\___||___/\__| "
echo "                                   |__/                                    "
echo "---------------------------------------------------------------------------"
# http://patorjk.com/software/taag/#p=display&f=Standard&t=Run%20Project%20Test
echo "v0.0.1 - 20200313"
echo ""

BASEDIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
# echo "$BASEDIR"

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

echo "--------------------------------------------------------------------------------"
echo "[SUMMARY] Total success: $succ; Total failed: $failed"
echo "--------------------------------------------------------------------------------"
