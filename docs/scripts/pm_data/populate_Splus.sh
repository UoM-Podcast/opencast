#!/bin/bash

# name of the SyllabusPlus database
db=splusCologne

# database user 
user=sPlusUser

# password of this user
pass=sPlusOpen

echo "Populate database $db"

mysql --password=$pass -u $user $db < sPlus-anon.sql

date=`date -I`
dsql=`date +%Y%m%d`

acts=`mysql --password=$pass -u $user $db -e "SELECT count(*) FROM V_ACTIVITY where StartDate >$dsql" -r -s -N `

s=`mysql --password=$pass -u $user $db -e "SELECT min(StartDate) FROM V_ACTIVITY" -r -s -N`
start=${s%% *}
e=`mysql --password=$pass -u $user $db -e "SELECT max(StartDate) FROM V_ACTIVITY" -r -s -N`
end=${e%% *}

du=`date +%s`
su=`date +%s -d $start`
eu=`date +%s -d $end`
mod=`date +-1%y1-`

diff=`let du -su`;

if [ $acts -lt "100" ]; then
  (( diff= ($du - $su) / (24*3600*7) ));
  echo "Update activities in database to start now"
  echo "Activities moved by $diff weeks"

  mysql --password=$pass -u $user $db -e "update V_ACTIVITY set StartDate=date_add(StartDate,interval $diff week);"
  mysql --password=$pass -u $user $db -e "update V_ACTIVITY set EndDate=date_add(EndDate,interval $diff week);"
  mysql --password=$pass -u $user $db -e "update V_ACTIVITY_DATETIME set StartDateTime=date_add(StartDateTime,interval $diff week);"
  mysql --password=$pass -u $user $db -e "update V_ACTIVITY_DATETIME set EndDateTime=date_add(EndDateTime,interval $diff week);"
  mysql --password=$pass -u $user $db -e "update V_MODULE Set UserText4 = replace(UserText4, '-1151-', '$mod');"

fi

