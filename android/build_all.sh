#!/bin/sh
set -e

#
# See: http://boinc.berkeley.edu/trac/wiki/AndroidBuildClient#
#

# Script to compile everything BOINC needs for Android
ANDROID_TC=~/android-tc
HOME=~/private_boinc
./buildAndroidBOINC-CI.sh --cache_dir $ANDROID_TC --build_dir $HOME/3rdParty --arch arm
./buildAndroidBOINC-CI.sh --cache_dir $ANDROID_TC --build_dir $HOME/3rdParty --arch arm64
./buildAndroidBOINC-CI.sh --cache_dir $ANDROID_TC --build_dir $HOME/3rdParty --arch x86
./buildAndroidBOINC-CI.sh --cache_dir $ANDROID_TC --build_dir $HOME/3rdParty --arch x86_64
