#!/bin/bash 
LOCALVER=`git rev-list HEAD | wc -l | awk '{print $1}'`
VER=r$LOCALVER
VER="$VER $(git rev-list HEAD -n 1 | cut -c 1-7)"
GIT_VERSION=$VER
build_version=`echo ${GIT_VERSION}`
cd ./meet

tpl_header=version.tpl
new_header=version.h
rm -f $new_header
cp $tpl_header $new_header

if [ `uname` = 'Darwin' ]; then
    sed -i '' "s/GIT_VER/$build_version/" ${new_header}

else
	sed -i "s/GIT_VER/$build_version/" ${new_header}
fi
