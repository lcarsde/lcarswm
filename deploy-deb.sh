#!/bin/bash

export LC_ALL=en_US.UTF-8
TIME="$(date '+%a, %d %b %Y %T %z')"
YEAR="$(date '+%Y')"
CIRCLE_TAG=21.3

echo "preparing debian package build for lcarswm-${CIRCLE_TAG}"

# create debian package construction directory
mkdir -p "build/deb/lcarswm-${CIRCLE_TAG}"

# copy software resources
cp -r gradle "build/deb/lcarswm-${CIRCLE_TAG}/gradle"
cp -r src "build/deb/lcarswm-${CIRCLE_TAG}/src"
cp build.gradle.kts "build/deb/lcarswm-${CIRCLE_TAG}/"
cp CHANGELOG "build/deb/lcarswm-${CIRCLE_TAG}/"
cp gradle.properties "build/deb/lcarswm-${CIRCLE_TAG}/"
cp gradlew "build/deb/lcarswm-${CIRCLE_TAG}/"
cp LICENSE "build/deb/lcarswm-${CIRCLE_TAG}/"
cp readme.md "build/deb/lcarswm-${CIRCLE_TAG}/"
cp settings.gradle.kts "build/deb/lcarswm-${CIRCLE_TAG}/"

cd "build/deb"

echo "create tarball"
tar -czf "lcarswm_${CIRCLE_TAG}.orig.tar.gz" "lcarswm-${CIRCLE_TAG}"
tar -ztf "lcarswm_${CIRCLE_TAG}.orig.tar.gz"

cd ../..

# copy debian packaging files
cp -r "debian" "build/deb/lcarswm-${CIRCLE_TAG}/debian"

echo "building debian package of lcarswm-${CIRCLE_TAG}"

cd "build/deb/lcarswm-${CIRCLE_TAG}"

sed -i "s/%version%/${CIRCLE_TAG}/" "debian/changelog"
sed -i "s/%time%/${TIME}/" "debian/changelog"
sed -i "s/%year%/${YEAR}/g" "debian/copyright"

debuild -us -uc

cd ..
ls -l
mkdir "deploy"
mv lcarswm_*.deb deploy/

cd ../..

echo "deb and tar.gz files are in build/deb/deploy"