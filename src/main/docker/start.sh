#!/bin/bash
set -e

umask 0002

java_default_props=("file.separator" "java.class.path" "java.home" "java.vendor" "java.vendor.url" "java.version" "line.separator" "os.arch" "os.name" "os.version" "path.separator" "user.dir" "user.home" "user.name" "_" "JAVA_HOME" "JAVA_VERSION" "HOSTNAME" "PATH" "HOME" "SHLVL" "PWD" "JAVA_OPTS")

declare -a j_opts

j_opts+=("-Dspring.profiles.active=default")

while IFS='=' read -r envvar_key envvar_value
do
    if [[ " ${java_default_props[@]} " =~ "${envvar_key}" ]]; then
      echo ""
    else
      if [[ ! -z $envvar_value ]]; then
            export envvar_key=$envvar_value
      fi
    fi
done < <(env)

echo "------------------------------"
echo "${j_opts[@]}"
echo "------------------------------"

java -jar "${j_opts[@]}" "/app.jar"