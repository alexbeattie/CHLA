#!/usr/bin/env bash
set -euo pipefail

# Ensure all AWS commands use your personal profile by default
: "${AWS_PROFILE:=personal}"
: "${AWS_DEFAULT_REGION:=us-west-2}"
export AWS_PROFILE AWS_DEFAULT_REGION

echo "Using AWS_PROFILE=${AWS_PROFILE} AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION}"

