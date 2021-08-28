#!/bin/bash

export LC_ALL=en_US.UTF-8
TIME="$(date '+%a, %d %b %Y %T %z')"
YEAR="$(date '+%Y')"

echo "preparing debian package build for $1-${CIRCLE_TAG}"

# create debian package construction directory
mkdir -p "build/deb/$1-${CIRCLE_TAG}"

# copy software resources
cp -r gradle "build/deb/$1-${CIRCLE_TAG}/gradle"
cp -r src "build/deb/$1-${CIRCLE_TAG}/src"
cp build.gradle.kts "build/deb/$1-${CIRCLE_TAG}/"
cp CHANGELOG "build/deb/$1-${CIRCLE_TAG}/"
cp gradle.properties "build/deb/$1-${CIRCLE_TAG}/"
cp gradlew "build/deb/$1-${CIRCLE_TAG}/"
cp LICENSE "build/deb/$1-${CIRCLE_TAG}/"
cp readme.md "build/deb/$1-${CIRCLE_TAG}/"
cp settings.gradle.kts "build/deb/$1-${CIRCLE_TAG}/"

cd "build/deb"

echo "create tarball"
tar -czf "$1_${CIRCLE_TAG}.orig.tar.gz" "$1-${CIRCLE_TAG}"
tar -ztf "$1_${CIRCLE_TAG}.orig.tar.gz"

cd ../..

# copy debian packaging files
cp -r "debian" "build/deb/$1-${CIRCLE_TAG}/debian"

echo "building debian package of $1-${CIRCLE_TAG}"

cd "build/deb/$1-${CIRCLE_TAG}"

sed -i "s/%version%/${CIRCLE_TAG}/" "debian/changelog"
sed -i "s/%time%/${TIME}/" "debian/changelog"
sed -i "s/%year%/${YEAR}/g" "debian/copyright"

debuild -us -uc

cd ..
ls -l
mkdir "deploy"
mv "$1_${CIRCLE_TAG}_amd64.deb" deploy/

cd ../..

echo "deb and tar.gz files are in build/deb/deploy"