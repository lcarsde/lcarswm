#!/bin/bash

echo ""
echo "=========================================="
echo "lcarswm installation"
echo "=========================================="
echo ""
echo "This program requires:"
echo "* JDK >= 8"
echo "* libx11-dev"
echo "* libxpm-dev"
echo "* libxrandr-dev"
echo "* libpango1.0-dev"
echo "* libxml2-dev"
echo "* libx11-6"
echo "* libxpm4"
echo "* libxrandr2"
echo "* libpango1.0-0"
echo "* libxml2"
echo ""
echo "Recommended:"
echo "* fonts-ubuntu"
echo "* xterm"
echo "* x-display-manager"
echo ""

./gradlew build

cp "./build/bin/native/releaseExecutable/lcarswm.kexe" "/usr/bin/lcarswm.kexe"
cp -r "./src/nativeMain/resources/usr/*" "/usr/"
cp -r "./src/nativeMain/resources/etc/*" "/etc/"

mkdir -p "/usr/share/doc/lcarswm"
cp "./LICENSE" "/usr/share/doc/lcarswm/copyright"
