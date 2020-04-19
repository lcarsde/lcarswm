#!/bin/bash

TRAVIS_TAG="0.0"
TIME="$(date '+%a, %d %b %Y %T %z')"

echo "run build for deployment"
./gradlew build

echo "build debian package for lcarswm-${TRAVIS_TAG}"

# create debian package construction directory
mkdir -p "build/deb/lcarswm-${TRAVIS_TAG}"

# copy software resources
cp -r "src/nativeMain/resources/usr" "build/deb/lcarswm-${TRAVIS_TAG}/usr"
cp "build/bin/native/releaseExecutable/lcarswm.kexe" "build/deb/lcarswm-${TRAVIS_TAG}/usr/bin/"

cd "build/deb"

echo "create tarball"
tar -czvf "lcarswm_${TRAVIS_TAG}.orig.tar.gz" "lcarswm-${TRAVIS_TAG}"
tar -ztvf "lcarswm_${TRAVIS_TAG}.orig.tar.gz"

cd ../..

# copy debian packaging files
cp -r "debian" "build/deb/lcarswm-${TRAVIS_TAG}/debian"

echo "Deploying debian package of lcarswm-${TRAVIS_TAG}"

cd "build/deb/lcarswm-${TRAVIS_TAG}"

sed -i "s/%version%/${TRAVIS_TAG}/" "debian/changelog"
sed -i "s/%time%/${TIME}/" "debian/changelog"

debuild -us -uc

cd ..
ls -l
mkdir "deploy"
mv "lcarswm_${TRAVIS_TAG}.orig.tar.gz" "deploy/lcarswm-${TRAVIS_TAG}.tar.gz"
mv lcarswm_*.deb deploy/
