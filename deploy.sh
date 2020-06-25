#!/bin/bash

export LC_ALL=en_US.UTF-8
TIME="$(date '+%a, %d %b %Y %T %z')"

echo "preparing debian package build for lcarswm-${TRAVIS_TAG}"

# create debian package construction directory
mkdir -p "build/deb/lcarswm-${TRAVIS_TAG}"

# copy software resources
cp -r doc "build/deb/lcarswm-${TRAVIS_TAG}/doc"
cp -r gradle "build/deb/lcarswm-${TRAVIS_TAG}/gradle"
cp -r src "build/deb/lcarswm-${TRAVIS_TAG}/src"
cp -r tools "build/deb/lcarswm-${TRAVIS_TAG}/tools"
cp build.gradle "build/deb/lcarswm-${TRAVIS_TAG}/"
cp CHANGELOG "build/deb/lcarswm-${TRAVIS_TAG}/"
cp gradlew "build/deb/lcarswm-${TRAVIS_TAG}/"
cp LICENSE "build/deb/lcarswm-${TRAVIS_TAG}/"
cp readme.md "build/deb/lcarswm-${TRAVIS_TAG}/"
cp settings.gradle "build/deb/lcarswm-${TRAVIS_TAG}/"

cd "build/deb"

echo "create tarball"
tar -czf "lcarswm_${TRAVIS_TAG}.orig.tar.gz" "lcarswm-${TRAVIS_TAG}"
tar -ztf "lcarswm_${TRAVIS_TAG}.orig.tar.gz"

cd ../..

# copy debian packaging files
cp -r "debian" "build/deb/lcarswm-${TRAVIS_TAG}/debian"

echo "building debian package of lcarswm-${TRAVIS_TAG}"

cd "build/deb/lcarswm-${TRAVIS_TAG}"

sed -i "s/%version%/${TRAVIS_TAG}/" "debian/changelog"
sed -i "s/%time%/${TIME}/" "debian/changelog"

debuild -us -uc

cd ..
ls -l
mkdir "deploy"
mv lcarswm_*.deb deploy/

cd ../..

echo "deb and tar.gz files are in build/deb/deploy"
