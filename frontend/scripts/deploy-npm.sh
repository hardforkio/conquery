#!/bin/bash
echo "npm publish ..."
echo "//registry.npmjs.org/:_authToken=${NPM_TOKEN}" > ~/.npmrc
cd ${TRAVIS_BUILD_DIR}/frontend/ && npm publish
