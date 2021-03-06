#!/bin/bash

# args:
# $1 the error code of the last command (should be explicitly passed)
# $2 the message to print in case of an error
# 
# an error message is printed and the script exists with the provided error code
function error_exit {
	echo "$2 : error code: $1"
	exit ${1}
}

export PATH=$PATH:/usr/sbin:/sbin:/usr/bin || error_exit $? "Failed on: export PATH=$PATH:/usr/sbin:/sbin"

sudo /etc/rc.d/init.d/mysqld stop || error_exit $? "Failed on: sudo /etc/rc.d/init.d/mysqld stop"

ps -ef | grep -i mysql | grep -ivE "gigaspaces|GSC|GSA|grep"

ps -ef | grep -iE "catalina" | grep -ivE "install|gigaspaces|GSC|GSA|grep" | awk '{print $2}' | xargs sudo kill -9

ps -ef | grep -i catalina | grep -ivE "gigaspaces|GSC|GSA|grep"