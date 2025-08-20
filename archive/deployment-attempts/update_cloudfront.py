#!/usr/bin/env python3
import json
import subprocess
import sys

# Read current config
with open('map-frontend/current-config.json', 'r') as f:
    config_data = json.load(f)

etag = config_data['ETag']
config = config_data['DistributionConfig']

# Add backend origin
backend_origin = {
    "Id": "eb-backend",
    "DomainName": "chla-api-env-v2.eba-9aiqcppx.us-west-2.elasticbeanstalk.com",
    "OriginPath": "",
    "CustomHeaders": {
        "Quantity": 0
    },
    "CustomOriginConfig": {
        "HTTPPort": 80,
        "HTTPSPort": 443,
        "OriginProtocolPolicy": "https-only",
        "OriginSslProtocols": {
            "Quantity": 3,
            "Items": ["TLSv1", "TLSv1.1", "TLSv1.2"]
        },
        "OriginReadTimeout": 30,
        "OriginKeepaliveTimeout": 5
    },
    "ConnectionAttempts": 3,
    "ConnectionTimeout": 10,
    "OriginShield": {
        "Enabled": False
    }
}

# Check if backend origin already exists
backend_exists = any(origin['Id'] == 'eb-backend' for origin in config['Origins']['Items'])
if not backend_exists:
    config['Origins']['Items'].append(backend_origin)
    config['Origins']['Quantity'] = len(config['Origins']['Items'])

# Add cache behavior for /api/*
api_behavior = {
    "PathPattern": "/api/*",
    "TargetOriginId": "eb-backend",
    "TrustedSigners": {
        "Enabled": False,
        "Quantity": 0
    },
    "TrustedKeyGroups": {
        "Enabled": False,
        "Quantity": 0
    },
    "ViewerProtocolPolicy": "redirect-to-https",
    "AllowedMethods": {
        "Quantity": 7,
        "Items": ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"],
        "CachedMethods": {
            "Quantity": 2,
            "Items": ["HEAD", "GET"]
        }
    },
    "SmoothStreaming": False,
    "Compress": True,
    "LambdaFunctionAssociations": {
        "Quantity": 0
    },
    "FunctionAssociations": {
        "Quantity": 0
    },
    "FieldLevelEncryptionId": "",
    "ForwardedValues": {
        "QueryString": True,
        "Cookies": {
            "Forward": "all"
        },
        "Headers": {
            "Quantity": 1,
            "Items": ["*"]
        },
        "QueryStringCacheKeys": {
            "Quantity": 0
        }
    },
    "MinTTL": 0,
    "DefaultTTL": 0,
    "MaxTTL": 0,
    "CachePolicyId": "4135ea2d-6df8-44a3-9df3-4b5a84be39ad",  # Managed-CachingDisabled
    "OriginRequestPolicyId": "88a5eaf4-2fd4-4709-b370-b4c650ea3fcf"  # Managed-CORS-S3Origin
}

# Check if cache behaviors exist
if 'CacheBehaviors' not in config:
    config['CacheBehaviors'] = {
        'Quantity': 0,
        'Items': []
    }
elif 'Items' not in config['CacheBehaviors']:
    config['CacheBehaviors']['Items'] = []

# Check if /api/* behavior already exists
api_behavior_exists = any(behavior.get('PathPattern') == '/api/*' for behavior in config['CacheBehaviors'].get('Items', []))
if not api_behavior_exists:
    # Remove conflicting fields if using managed policies
    if 'CachePolicyId' in api_behavior:
        api_behavior.pop('ForwardedValues', None)
        api_behavior.pop('MinTTL', None)
        api_behavior.pop('DefaultTTL', None)
        api_behavior.pop('MaxTTL', None)
    
    config['CacheBehaviors']['Items'].insert(0, api_behavior)  # Insert at beginning for priority
    config['CacheBehaviors']['Quantity'] = len(config['CacheBehaviors']['Items'])

# Save updated config
with open('updated-config.json', 'w') as f:
    json.dump(config, f, indent=2)

print(f"Config updated. ETag: {etag}")
print("Now run: aws cloudfront update-distribution --id E2W6EECHUV4LMM --distribution-config file://updated-config.json --if-match {etag} --profile personal")
