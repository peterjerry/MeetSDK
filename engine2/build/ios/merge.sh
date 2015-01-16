#!/bin/bash

OUTPUT_ARM=../../output/ios/v7/armv7
OUTPUT_I386=../../output/ios/v7/i386
OUTPUT_DIST=../../output/ios/v7

lipo -output $OUTPUT_DIST/libplayer.a  \
	-create \
		-arch armv7 $OUTPUT_ARM/libplayer.a \
		-arch i386 $OUTPUT_I386/libplayer.a
		
lipo -info $OUTPUT_DIST/libplayer.a
