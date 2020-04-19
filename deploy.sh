#!/bin/bash

TRAVIS_TAG="0.0"

echo "build debian package for lcarswm-${TRAVIS_TAG}"

# create debian package construction directory
mkdir -p "build/deb/lcarswm-${TRAVIS_TAG}"

# copy debian packaging files
cp -r "debian" "build/deb/lcarswm-${TRAVIS_TAG}/debian"

# create software directories
mkdir -p "build/deb/lcarswm-${TRAVIS_TAG}/usr/bin"

# copy software resources
cp "build/bin/native/releaseExecutable/lcarswm.kexe" "build/deb/lcarswm-${TRAVIS_TAG}/usr/bin/"
cp -r "build/processedResources/native/main/usr/share" "build/deb/lcarswm-${TRAVIS_TAG}/usr/share"


echo "Deploying debian package of lcarswm-${TRAVIS_TAG}"

cd "build/deb/lcarswm-${TRAVIS_TAG}"

debuild -us -uc
