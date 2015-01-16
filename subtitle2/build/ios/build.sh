#!/bin/bash

#xcodebuild -workspace subtitle2.xcworkspace -scheme subtitle clean
#xcodebuild -workspace subtitle2.xcworkspace -scheme subtitle

#xcodebuild -workspace subtitle2.xcworkspace -scheme subtitle -sdk iphonesimulator clean
#xcodebuild -workspace subtitle2.xcworkspace -scheme subtitle -sdk iphonesimulator

cd subtitle2/subtitle

xcodebuild -project subtitle.xcodeproj clean

xcodebuild -project subtitle.xcodeproj

xcodebuild -project subtitle.xcodeproj -sdk iphonesimulator clean

xcodebuild -project subtitle.xcodeproj -sdk iphonesimulator


